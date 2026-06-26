/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public class ItemContentsMemorySizeLimitTest {

	@Test
	public void test() throws IOException {
		RepoRevision rev = new RepoRevision(1, new Date());
		CmsItemPath path = new CmsItemPath("/file.txt");

		CmsContentsReader reader = new CmsContentsReader() {

			@Override
			public CmsItemProperties getProperties(RepoRevision arg0, CmsItemPath arg1) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void getContents(RepoRevision revision, CmsItemPath path,
					OutputStream out) {
				try {
					out.write("1234567890a".getBytes());
					out.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public CmsItemProperties getRevisionProperties(RepoRevision revision) {
				throw new UnsupportedOperationException();
			}
		};
		ItemContentsMemorySizeLimit buffer = new ItemContentsMemorySizeLimit();
		buffer.setCmsContentsReader(reader);
		buffer.setFileSizeInMemoryLimit(10);
		
		File tmp = File.createTempFile("test", this.getClass().getName());
		File expectedtempfolder = tmp.getParentFile();
		tmp.delete();
		int numbefore = expectedtempfolder.list().length;
		
		IndexingDoc doc = mock(IndexingDoc.class);
		when(doc.getFieldValue("size")).thenReturn(11L);
		ItemContentBuffer buf = buffer.getBuffer(rev, path, doc);
		InputStream c1 = buf.getContents();
		assertEquals("1".getBytes()[0], c1.read());
		assertEquals("Expected a new temp file", numbefore + 1, expectedtempfolder.list().length); // this can be affected by other system activities so if it fails a lot we must get the path from the buffer
		assertEquals("2".getBytes()[0], c1.read());
		assertEquals("Should allow re-read", "1".getBytes()[0], buf.getContents().read());
		buf.destroy();
		assertEquals("Temp file should have been deleted", numbefore, expectedtempfolder.list().length);
		try {
			buf.getContents();
			fail("Should not allow buffer read after destroy");
		} catch (IllegalStateException e) {
			// expected
		}
	}

}
