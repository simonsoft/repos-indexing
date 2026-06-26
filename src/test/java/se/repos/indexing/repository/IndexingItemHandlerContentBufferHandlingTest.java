/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.twophases.IndexingItemProgressPhases;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class IndexingItemHandlerContentBufferHandlingTest {

	@Test
	public void test() {
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		ItemContentBufferStrategy strategy = mock(ItemContentBufferStrategy.class);
		ItemContentBuffer buffer = mock(ItemContentBuffer.class);
		when(strategy.getBuffer(null, null, null)).thenReturn(buffer);
		ByteArrayInputStream b = new ByteArrayInputStream("".getBytes());
		when(buffer.getContents()).thenReturn(b);
		IndexingItemHandler enable = new HandlerContentEnable(strategy);
		IndexingItemHandler disable = new HandlerContentDisable();
		
		IndexingItemProgress progress = new IndexingItemProgressPhases(null,null,item,null); //repository, revision, item, fields);

		
		try {
			progress.getContents();
			fail("Should disallow content retrieval before enable");
		} catch (IllegalStateException e) {
			// expected
		}
		verifyZeroInteractions(buffer);
		enable.handle(progress);
		assertSame(b, progress.getContents());
		disable.handle(progress);
		verify(buffer).destroy();
		try {
			progress.getContents();
			fail("Should disallow content retrieval after disable");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Actually tests {@link IndexingItemHandlerInternal}.
	 */
	@Test
	public void testUnsupportedProgress() {
		ItemContentBufferStrategy strategy = mock(ItemContentBufferStrategy.class);
		IndexingItemHandler enable = new HandlerContentEnable(strategy);
		IndexingItemHandler disable = new HandlerContentDisable();
		
		IndexingItemProgress progressUnsupported = mock(IndexingItemProgress.class);
		try {
			enable.handle(progressUnsupported);
			fail("Expected exception on unsupported progress type");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Configuration error"), "Got " + e);
		}
		try {
			disable.handle(progressUnsupported);
			fail("Expected exception on unsupported progress type");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Configuration error"), "Got " + e);
		}
	}	
	
}
