/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.inject.Inject;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

/**
 * Only suitable for testing, will fill up the disk with temp files in large indexing jobs.
 */
public class ItemContentsNocache implements ItemContentBufferStrategy {

	private CmsContentsReader reader;
	
	@Inject
	public ItemContentsNocache setCmsContentsReader(CmsContentsReader reader) {
		this.reader = reader;
		return this;
	}
	
	@Override
	public ItemContentBuffer getBuffer(
			RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		return new BufferMinimizeMemoryUse(revision, path);
	}	
	
	/**
	 * Unless we adapt to different file sizes we need this kind of buffer.
	 */
	public class BufferMinimizeMemoryUse implements ItemContentBuffer {

		private RepoRevision revision;
		private CmsItemPath path;

		public BufferMinimizeMemoryUse(
				RepoRevision revision, CmsItemPath path) {
			this.revision = revision;
			this.path = path;
		}

		@Override
		public InputStream getContents() {
			// Found
			// http://ostermiller.org/convert_java_outputstream_inputstream.html
			// https://code.google.com/p/io-tools/wiki/Tutorial_EasyStream
			// But we'd better wait until we start adapting to file size
			File tempfile;
			try {
				tempfile = File.createTempFile("repos-indexing-contents-buffer", ".tmp");
			} catch (IOException e2) {
				throw new IllegalStateException("Failed to produce temp file destination for contents buffer");
			}
			tempfile.deleteOnExit(); // TODO does this work? Can we get notified on stream close? Do we reuse the file for subsequent reads?
			OutputStream out;
			try {
				out = new FileOutputStream(tempfile);
			} catch (FileNotFoundException e1) {
				throw new IllegalStateException("Failed to produce temp file destination for contents buffer");
			}
			reader.getContents(revision, path, out);
			try {
				return new FileInputStream(tempfile);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("Failed to produce readable input from temp file");
			}
		}

		@Override
		public void destroy() {
			throw new UnsupportedOperationException("Not implemented. This type of buffer is deprecated.");
		}
		
	}
	
}
