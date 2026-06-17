/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;

import se.repos.indexing.IndexingHandlers;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.twophases.ItemContentsMemory;
import se.repos.indexing.twophases.ItemPropertiesImmediate;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

public class IndexingHandlersProducerTest {

	@Test
	public void testProducedHandlerOrderMatchesStandard() {
		IndexingHandlersProducer producer = new IndexingHandlersProducer();
		SolrClient repositem = mock(SolrClient.class);
		CmsRepository repository = new CmsRepository("http://localhost/svn/test");
		IdStrategy idStrategy = new IdStrategyDefault();
		ItemContentBufferStrategy contentBufferStrategy = mock(ItemContentBufferStrategy.class);
		ItemPropertiesBufferStrategy propertiesBufferStrategy = mock(ItemPropertiesBufferStrategy.class);

		Set<IndexingItemHandler> handlers = producer.createIndexingItemHandlers(
				idStrategy, repositem, repository, contentBufferStrategy, propertiesBufferStrategy);

		assertEquals(standardHandlerTypes(), handlerTypes(handlers));
	}

	@Test
	public void testBufferStrategyProducers() {
		IndexingHandlersProducer producer = new IndexingHandlersProducer();
		CmsContentsReader contentsReader = mock(CmsContentsReader.class);

		assertInstanceOf(ItemContentsMemory.class, producer.createItemContentBufferStrategy(contentsReader));
		assertInstanceOf(ItemPropertiesImmediate.class, producer.createItemPropertiesBufferStrategy(contentsReader));
	}

	private List<Class<? extends IndexingItemHandler>> standardHandlerTypes() {
		List<Class<? extends IndexingItemHandler>> result = new ArrayList<>();
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Unblock));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Structure));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Fast));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Nice));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Content));
		add(result, IndexingHandlers.STANDARD.get(IndexingHandlers.Group.Final));
		return result;
	}

	private void add(List<Class<? extends IndexingItemHandler>> target, Iterable<Class<? extends IndexingItemHandler>> source) {
		for (Class<? extends IndexingItemHandler> handlerType : source) {
			target.add(handlerType);
		}
	}

	private List<Class<? extends IndexingItemHandler>> handlerTypes(Set<IndexingItemHandler> handlers) {
		List<Class<? extends IndexingItemHandler>> result = new ArrayList<>();
		for (IndexingItemHandler handler : handlers) {
			result.add(handler.getClass());
		}
		return result;
	}
}
