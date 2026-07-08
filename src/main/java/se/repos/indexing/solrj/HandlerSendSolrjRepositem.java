/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import jakarta.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;

import jakarta.inject.Named;

public class HandlerSendSolrjRepositem extends HandlerSendSolrj {

	@Inject
	public HandlerSendSolrjRepositem(@Named("repositem") SolrClient core) {
		super(core);
	}

}
