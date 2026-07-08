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
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

/**
 * 
 * TODO we need a cleanup strategy for temp files, implement {@link IndexingEventAware} to get the notification needed
 */
public class ItemContentsMemorySizeLimit implements ItemContentBufferStrategy {
	
	private static final Logger logger = LoggerFactory.getLogger(ItemContentsMemorySizeLimit.class);
	
	public static final int DEFAULT_SIZE_LIMIT = 100000;
	
	private int limit = DEFAULT_SIZE_LIMIT;
	
	private CmsContentsReader reader;	
	
	@Inject
	public void setFileSizeInMemoryLimit(@Named("indexingFilesizeInMemoryLimitBytes") int bytes) {
		if (bytes > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Can not cache files larger than " + Integer.MAX_VALUE + " in memory");
		}
		this.limit = bytes;
	}
	
	@Inject
	public ItemContentsMemorySizeLimit setCmsContentsReader(CmsContentsReader reader) {
		this.reader = reader;
		return this;
	}
	
	@Override
	public ItemContentBuffer getBuffer(
			RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		Long size = (Long) pathinfo.getFieldValue("size");
		if (size == null) {
			throw new IllegalStateException("No size information in indexing doc " + path + ". Use a different buffer strategy.");
		}
		if (size <= limit) {
			return new ItemContentsMemory.BufferInMemory(reader, revision, path, size.intValue());
		} else {
			return new BufferTempFile(revision, path);
		}
	}

	/**
	 * Buffer to temp file.
	 */
	public class BufferTempFile implements ItemContentBuffer {

		private RepoRevision revision;
		private CmsItemPath path;

		private File tempfile = null;
		
		public BufferTempFile(
				RepoRevision revision, CmsItemPath path) {
			this.revision = revision;
			this.path = path;
		}

		@Override
		public InputStream getContents() {
			if (tempfile == null) {
				// Found
				// http://ostermiller.org/convert_java_outputstream_inputstream.html
				// https://code.google.com/p/io-tools/wiki/Tutorial_EasyStream
				// But we'd better wait until we start adapting to file size
				try {
					tempfile = File.createTempFile("repos-indexing-contents-buffer", ".tmp");
				} catch (IOException e2) {
					throw new IllegalStateException("Failed to produce temp file destination for contents buffer");
				}
				// deleteOnExit can be removed after IndexingItemHandlerContentBufferDisable is added to handler chains
				tempfile.deleteOnExit(); // TODO does this work? Can we get notified on stream close? Do we reuse the file for subsequent reads? TODO anyway it will fill up the disk for long indexing runs
				OutputStream out;
				try {
					out = new FileOutputStream(tempfile);
				} catch (FileNotFoundException e1) {
					throw new IllegalStateException("Failed to write to templ file for contents buffer");
				}
				reader.getContents(revision, path, out);			
			}
			try {
				return new FileInputStream(tempfile);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("Failed to produce readable input from temp file");
			}
		}

		@Override
		public void destroy() {
			if (tempfile == null) {
				logger.warn("Content buffer has never been initialized");
				return;
			}
			if (!tempfile.exists()) {
				logger.warn("Contents buffer file not found");
				return;
			}
			if (!tempfile.delete()) {
				throw new AssertionError("Failed to delete indexing item buffer " + tempfile);
			}
		}
		
	}	
	
}
