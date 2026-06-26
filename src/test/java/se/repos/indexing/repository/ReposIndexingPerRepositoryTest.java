/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Test;
import org.mockito.Mock;

import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class ReposIndexingPerRepositoryTest {

	@Test
	public void testSyncRepoRevision() {
		CmsRepository repository = new CmsRepository("http://testing/svn/repo");
		ReposIndexing indexing1 = new ReposIndexingPerRepository(repository);
		ReposIndexing indexing2 = new ReposIndexingPerRepository(repository);
		
		// TODO how to make a threaded test that stops indexin1 inside status check and runs indexing2?
	}
	
	@Test
	public void testClear() throws SolrServerException, IOException {
		CmsRepository repository = new CmsRepository("http://testing/svn/repo");
		ReposIndexingPerRepository indexing = new ReposIndexingPerRepository(repository);
		
		SolrClient client = mock(SolrClient.class);
		UpdateResponse response = new UpdateResponse();
		response.setElapsedTime(10);
		when(client.deleteByQuery(any())).thenReturn(response);
		when(client.commit()).thenReturn(response);
		when(client.commit(anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(response);
		IndexAdminPerRepositoryRepositem admin = new IndexAdminPerRepositoryRepositem(repository, new IdStrategyDefault(), client);
		indexing.setIndexAdmin(admin);
		
		IndexingSchedule schedule = mock(IndexingSchedule.class);
		when(schedule.isComplete()).thenReturn(true).thenReturn(false);
		indexing.setIndexingSchedule(schedule);
		admin.clear();
		
		verify(schedule).isComplete(); // indicates that we ran the code that clears state
		
		try {
			admin.clear();
			// log a warning or throw exception when schedule is running?
		} catch (Exception e) {
			fail("Currently we've decided to only log a warning when clearing while schedule is still running");
		}
	}

}
