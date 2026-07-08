/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import java.util.LinkedList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexAdmin;
import se.repos.indexing.solrj.SolrCommit;
import se.repos.indexing.solrj.SolrDeleteByQuery;
import se.repos.indexing.solrj.SolrOptimize;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;

@Singleton // Notification receivers won't be notified if this isn't a singleton
public class IndexAdminPerRepositoryRepositem implements IndexAdmin {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private CmsRepository repository;
	private IdStrategy idStrategy;
	private SolrClient repositem;
	private List<IndexAdmin> postActions = new LinkedList<IndexAdmin>();
	
	@Inject
	public IndexAdminPerRepositoryRepositem(CmsRepository repository,
			IdStrategy idStrategy,
			@Named("repositem") SolrClient repositem) {
		this.repository = repository;
		this.idStrategy = idStrategy;
		this.repositem = repositem;
		logger.info("IndexAdmin initialized for repository {}", repository);
	}
	
	@Override
	public void addPostAction(IndexAdmin notificationReceiver) {
		this.postActions.add(notificationReceiver);
		logger.info("Added IndexAdmin notification to {}", notificationReceiver);
	}

	@Override
	public void clear() {
		String query = "repoid:\"" + idStrategy.getIdRepository(repository).replace("\"", "\\\"") + '"';
		logger.info("Clearing index for repository {} using query {}", repository, query);
		new SolrDeleteByQuery(repositem, query).run();
		new SolrCommit(repositem, false).run();
		for (IndexAdmin p : postActions) {
			p.clear();
		}
		if (postActions.size() == 0) {
			logger.warn("There are no notifications configured. Is there really no stateful indexing? Or is there multiple instances of admin? At {}", this);
		} 
		new SolrOptimize(repositem).run();
	}

}
