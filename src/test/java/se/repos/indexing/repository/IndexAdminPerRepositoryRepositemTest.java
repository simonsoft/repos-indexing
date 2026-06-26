/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.jupiter.api.Test;

import se.repos.indexing.IndexAdmin;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class IndexAdminPerRepositoryRepositemTest {

	@Test
	public void test() throws SolrServerException, IOException {
		CmsRepository repository = new CmsRepository("http://localhost:1234/svn/r");
		IdStrategy idStrategy = new IdStrategyDefault();
		SolrClient repositem = mock(SolrClient.class);
		UpdateResponse response = new UpdateResponse();
		response.setElapsedTime(10);
		when(repositem.deleteByQuery(any())).thenReturn(response);
		when(repositem.commit()).thenReturn(response);
		when(repositem.commit(anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(response);
		
		IndexAdmin indexAdmin = new IndexAdminPerRepositoryRepositem(repository, idStrategy, repositem);
		final List<Object> calls = new LinkedList<Object>();
		indexAdmin.addPostAction(new IndexAdmin() {
			@Override
			public void clear() {
				calls.add(null);
			}
			@Override
			public void addPostAction(IndexAdmin notificationReceiver) {
				fail("Should not be called for receivers");
			}
		});
		
		indexAdmin.clear();
		
		verify(repositem).deleteByQuery("repoid:\"localhost:1234/svn/r\"");
		verify(repositem).commit(false, true, false);
		assertEquals(1, calls.size(), "Should notify");
	}

}
