/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class IndexingUnitTest {

	@Test
	public void test() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class);
		IndexingItemProgress item2 = mock(IndexingItemProgress.class);
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		IndexingItemHandler handler1 = mock(IndexingItemHandler.class);
		IndexingItemHandler handler2 = mock(IndexingItemHandler.class);
		
		Set<IndexingItemHandler> handlers = new LinkedHashSet<IndexingItemHandler>();
		handlers.add(handler1);
		handlers.add(handler2);
		
		IndexingUnit unit = new IndexingUnit(changeset1, handlers);
		assertNotNull(unit.getItems());
		assertEquals("should preserver order", item1, unit.getItems().iterator().next());
		
		assertEquals(handler1, unit.getHandlers(item1).next());
		assertTrue(unit.getHandlers(item1).hasNext());
		assertEquals("The iterator should remember the position so that handlers are only returned once per item",
				handler2, unit.getHandlers(item1).next());
		assertFalse(unit.getHandlers(item1).hasNext());
		
		Iterator<IndexingItemHandler> h2 = unit.getHandlers(item2);
		assertEquals(handler1, h2.next());
		assertEquals(handler2, h2.next());
		assertFalse(h2.hasNext());
		assertFalse(unit.getHandlers(item2).hasNext());
	}

	@Test
	public void testMarkerReappear() {
		List<IndexingItemHandler> handlers = new LinkedList<IndexingItemHandler>();
		
	}
	
}
