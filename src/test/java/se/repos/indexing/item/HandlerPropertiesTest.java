/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;

public class HandlerPropertiesTest {

	@Test
	public void testHandle() {
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(p.getItem()).thenReturn(item);
		IndexingDoc doc = mock(IndexingDoc.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap();
		props.put("a:prop", "val");
		props.put("another:prop", "multi\nline");
		when(p.getFields()).thenReturn(doc);
		when(p.getProperties()).thenReturn(props);
		
		IndexingItemHandler handler = new HandlerProperties();
		handler.handle(p);
		verify(doc).setField("prop_a.prop", "val");
		verify(doc).setField("prop_another.prop", "multi\nline");
	}

}
