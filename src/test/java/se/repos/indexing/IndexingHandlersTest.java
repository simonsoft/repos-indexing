/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.HandlerHeadClone;
import se.repos.indexing.item.HandlerIndexTime;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.repository.HandlerContentDisable;
import se.repos.indexing.repository.HandlerContentEnable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesDisable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesEnable;
import se.repos.indexing.repository.MarkerRevisionComplete;
import se.repos.indexing.scheduling.ScheduleAwaitNewer;
import se.repos.indexing.scheduling.ScheduleBackground;
import se.repos.indexing.solrj.HandlerSendIncrementalSolrjRepositem;
import se.repos.indexing.solrj.HandlerSendSolrjRepositem;
import se.repos.indexing.solrj.MarkerCommitSolrjRepositem;

public class IndexingHandlersTest {

	@Test
	public void testStandardHandlerOrder() {
		assertEquals(expectedHandlerTypes(), standardHandlerTypes());
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

	private List<Class<? extends IndexingItemHandler>> expectedHandlerTypes() {
		return Arrays.asList(
				ScheduleBackground.class,
				HandlerIndexTime.class,
				HandlerPathinfo.class,
				IndexingItemHandlerPropertiesEnable.class,
				HandlerProperties.class,
				IndexingItemHandlerPropertiesDisable.class,
				HandlerSendIncrementalSolrjRepositem.class,
				ScheduleAwaitNewer.class,
				HandlerContentEnable.class,
				HandlerChecksum.class,
				HandlerContentDisable.class,
				HandlerHeadClone.class,
				HandlerSendSolrjRepositem.class,
				MarkerRevisionComplete.class,
				MarkerCommitSolrjRepositem.class);
	}
}
