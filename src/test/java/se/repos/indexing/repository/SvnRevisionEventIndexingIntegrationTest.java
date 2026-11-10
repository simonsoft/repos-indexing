/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import se.repos.indexing.config.SvnRevisionEventIndexingProfile;

@QuarkusTest
@TestProfile(SvnRevisionEventIndexingProfile.class)
public class SvnRevisionEventIndexingIntegrationTest {

	@Inject
	SVNRepository repository;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@AfterEach
	public void clearRepositem() throws SolrServerException, IOException {
		repositem.deleteByQuery("*:*");
		repositem.commit();
	}

	@Test
	@ActivateRequestContext
	public void testSvnRevisionEventTriggersIndexing() throws SVNException, SolrServerException, IOException {
		assertEquals(1L, repository.getLatestRevision());

		assertEquals(1, repositem.query(new SolrQuery("type:commit AND rev:1 AND complete:true")).getResults().getNumFound());
		assertEquals(3, repositem.query(new SolrQuery("head:true")).getResults().getNumFound());
	}
}
