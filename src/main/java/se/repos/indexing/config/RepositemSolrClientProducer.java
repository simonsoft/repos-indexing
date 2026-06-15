/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;

public class RepositemSolrClientProducer {

	@Produces
	@ApplicationScoped
	@Named("repositem")
	@Any
	public SolrClient createRepositemSolrClient(@Default Instance<SolrClient> solrClient) {
		return solrClient.get();
	}
}
