/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class IndexingScheduleTest {

	@Test
	public void testScheduleBlocking() {
		IndexingItemProgress item1 = mock(IndexingItemProgress.class);
		IndexingItemProgress item2 = mock(IndexingItemProgress.class);
		Set<IndexingItemProgress> changeset1 = new LinkedHashSet<IndexingItemProgress>();
		changeset1.add(item1);
		changeset1.add(item2);
		
		IndexingItemHandler handler1 = mock(IndexingItemHandler.class);
		IndexingItemHandler handler2 = mock(IndexingItemHandler.class);
		
		
	}
	
	@Test
	public void testSchedulePlainFifo() {
		
		
		
	}
	
	/**
	 * Tests how the run queue is built when there are {@link ScheduleAwaitNewer} entries in handler list.
	 */
	@Test
	public void testScheduleAwaitNewer() {
		
		// create 
		
		
	}

}
