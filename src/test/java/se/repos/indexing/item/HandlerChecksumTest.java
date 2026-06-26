/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class HandlerChecksumTest {

	@Test
	public void test() {
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.isFile()).thenReturn(true);
		IndexingDoc doc = mock(IndexingDoc.class);
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		InputStream content = new ByteArrayInputStream("testing\n".getBytes());
		when(p.getItem()).thenReturn(item);
		when(p.getContents()).thenReturn(content).thenThrow(new AssertionError("Should only read stream once"));
		when(p.getFields()).thenReturn(doc);
		new HandlerChecksum().handle(p);
		verify(doc, times(1)).addField("checksum_md5", "eb1a3227cdc3fedbaec2fe38bf6c044a");
		verify(doc, times(1)).addField("checksum_sha1", "9801739daae44ec5293d4e1f53d3f4d2d426d91c");
		verify(doc, times(1)).addField("checksum_sha256", "12a61f4e173fb3a11c05d6471f74728f76231b4a5fcd9667cef3af87a3ae4dc2");
	}

}
