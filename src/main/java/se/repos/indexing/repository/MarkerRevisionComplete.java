/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.solrj.SolrAdd;

/**
 * Sends an update to the indexed revision doc that the revision is completely indexed.
 * 
 * Note that the marking of revision as started is not a handler, because it can be don prior to scheduling.
 */
public class MarkerRevisionComplete implements Marker {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	// http://mail-archives.apache.org/mod_mbox/lucene-solr-user/201209.mbox/%3C7E0464726BD046488B66D661770F9C2F01B02EFF0C@TLVMBX01.nice.com%3E
	@SuppressWarnings("serial")
	private final Map<String, Boolean> partialUpdateToTrue = new HashMap<String, Boolean>() {{
		put("set", true);
	}};
	
	private SolrClient repositem;

	private String commitIdCurrent = null;
	
	@Inject
	public MarkerRevisionComplete(@Named("repositem") SolrClient repositem) {
		this.repositem = repositem;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		String commitId = (String) progress.getFields().getFieldValue("revid");
		if (commitIdCurrent == null) {
			commitIdCurrent = commitId;
		} else {
			if (!commitIdCurrent.equals(commitId)) {
				throw new IllegalStateException("Revision overlap at " + commitIdCurrent + " and " + commitId + ". Not supported until per-revision handler support is added.");
			}
		}
	}

	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void trigger() {
		if (commitIdCurrent == null) {
			logger.info("Revision was empty. Should have been set complete when started.");
			return;
		}
		SolrInputDocument doc = new SolrInputDocument();
		doc.setField("id", commitIdCurrent);
		doc.setField("complete", partialUpdateToTrue);
		new SolrAdd(repositem, doc).run();
		logger.info("Marked revision {} as complete in index", commitIdCurrent);
		commitIdCurrent = null;
	}
	
	@Override
	public void ignore() {
	}	

}
