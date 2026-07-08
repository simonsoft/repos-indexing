/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;

public class MarkerOptimizeSolrjRepositem extends MarkerOptimizeSolrj {

	public MarkerOptimizeSolrjRepositem(@Named("repositem") SolrClient core) {
		super(core);
	}

}
