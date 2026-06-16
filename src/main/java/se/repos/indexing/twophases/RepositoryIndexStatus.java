/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.util.Date;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.repository.ReposIndexingPerRepository;
import se.repos.indexing.solrj.SolrAdd;
import se.repos.indexing.solrj.SolrCommit;
import se.repos.indexing.solrj.SolrPingOp;
import se.repos.indexing.solrj.SolrQueryOp;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Stuff from the original {@link ReposIndexingImpl} that was needed in {@link ReposIndexingPerRepository}
 * but couldn't be extracted to {@link IndexingItemHandler}s within the current architecture.
 * 
 * Could be made a first class service but then it should also handle caching of results
 * (and get updated based on indexing progress)
 * so that any service could use it to ask for status.
 */
@Dependent
public class RepositoryIndexStatus {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private SolrClient repositem;
	private IdStrategy idStrategy;
	
	@Inject
	public RepositoryIndexStatus(@Named("repositem") SolrClient repositem, IdStrategy idStrategy) {
		this.repositem = repositem;
		this.idStrategy = idStrategy;
	}
	
	protected void solrAdd(SolrInputDocument doc) {
		if (doc.size() == 0) {
			throw new IllegalArgumentException("Detected attempt to index empty document");
		}
		if (doc == IndexingDocIncrementalSolrj.UPDATE_MODE_NO_CHANGES) {
			logger.warn("Index add was attempted in update mode but no changes have been made, got fields {}", doc.getFieldNames());
			return;
		}
		new SolrAdd(repositem, doc).run();
	}

	protected String quote(String fieldValue) {
		return '"' + fieldValue.replace("\"", "\\\"") + '"';
	}
	
	protected String getPropRevKey(String key) {
		return "proprev_".concat(key.replace(':', '.'));
	}

	/**
	 * @return started and completed revision, highest number
	 */
	public RepoRevision getIndexedRevisionHighestCompleted(CmsRepository repository) {
		logger.debug("Checking highest completed revision for {}", repository);
		return getIndexedRevision(repository, "true", ORDER.desc);
	}

	/**
	 * @return started but not completed revision, highest number
	 */
	public RepoRevision getIndexedRevisionHighestStarted(CmsRepository repository) {
		return getIndexedRevision(repository, "false", ORDER.desc);
	}

	/**
	 * @return started but not completed revision, lowedst number
	 */
	public RepoRevision getIndexedRevisionLowestStarted(CmsRepository repository) {
		return getIndexedRevision(repository, "false", ORDER.asc);
	}

	private RepoRevision getIndexedRevision(CmsRepository repository, String valComplete, ORDER order) {
		String repoid = idStrategy.getIdRepository(repository);
		logger.debug("Running revision query for {}, complete={}, order={}", repository, valComplete, order);
		SolrQuery query = new SolrQuery("type:commit AND complete:" + valComplete + " AND repoid:" + quote(repoid));
		query.setRows(1);
		query.setFields("rev", "revt");
		query.setSort("rev", order); // the timestamp might be in a different order in svn, if revprops or loading has been used irregularly
		// #1358 SolrOp provides retry logic.		
		QueryResponse resp = new SolrQueryOp(repositem, query).run();
		SolrDocumentList results = resp.getResults();
		if (results.getNumFound() == 0) {
			return null;
		}
		SolrDocument r = results.get(0);
		Long rev = (Long) r.getFieldValue("rev");
		if (rev == null) {
			throw new AssertionError("Commit entry lacks rev field: " + r);
		}
		Date revt = (Date) r.getFieldValue("revt");
		if (revt == null) {
			throw new AssertionError("Commit entry lacks revt field: " + r);
		}
		return new RepoRevision(rev, revt);
	}


	/**
	 * @return Commit ID field value
	 */
	protected String indexRevFlag(CmsRepository repository, RepoRevision revision, CmsItemProperties revprops, String error, boolean complete) {

		String id = idStrategy.getIdCommit(repository, revision);
		String repoid = idStrategy.getIdRepository(repository);
		SolrInputDocument docStart = new SolrInputDocument();
		docStart.addField("id", id);
		docStart.addField("repo", repository.getName());
		docStart.addField("repoid", repoid);
		docStart.addField("repoparent", repository.getParentPath());
		docStart.addField("repohost", repository.getHost());
		
		docStart.addField("type", "commit");
		docStart.addField("rev", idStrategy.getIdRevision(revision));
		docStart.addField("revt", revision.getDate());
		docStart.addField("complete", complete);
		docStart.addField("t", new Date());
		
		if (revprops != null) {
			// Add all revprops to proprev_* with transformation of ':'
			for (String key : revprops.getKeySet()) {
				docStart.addField(getPropRevKey(key), revprops.getString(key));
			}
			
			// Specific fields for Author and History Comment.
			if (revprops.containsProperty("svn:author")) {
				docStart.addField("revauthor", revprops.getString("svn:author"));
			}
			if (revprops.containsProperty("svn:log")) {
				docStart.addField("revcomment", revprops.getString("svn:log"));
			}
		}

		if (error != null) {
			docStart.addField("text_error", error);
		}
		
		this.solrAdd(docStart);
		return id;
	}

	public String indexRevStart(CmsRepository repository, RepoRevision revision, CmsItemProperties revprops, String error) {
		return indexRevFlag(repository, revision, revprops, error, false);
	}
	
	public String indexRevEmpty(CmsRepository repository,
			RepoRevision revision, CmsItemProperties revprops, String error) {
		return indexRevFlag(repository, revision, revprops, error, true);
	}
	
	public void indexRevStartAndCommit(CmsRepository repository,
			RepoRevision revision, CmsItemProperties revprops) {
		indexRevStart(repository, revision, revprops, null);
		// TODO: Consider commitWithin during add instead of commit.
		new SolrCommit(repositem).run();
	}
	
	public void indexPing() {
		new SolrPingOp(repositem).run();
	}

}
