/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;

import io.smallrye.common.annotation.Identifier;

public class RepositemSolrClientProducer {

	@Produces
	@ApplicationScoped
	@Named("repositem")
	@Identifier("repositem")
	public SolrClient createRepositemSolrClient(@Default Instance<SolrClient> solrClient) {
		return solrClient.get();
	}
}
