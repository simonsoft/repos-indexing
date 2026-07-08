/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

public class ItemContentsStream implements ItemContentBufferStrategy {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ItemContentsStream.class);

	@SuppressWarnings("unused")
	private CmsContentsReader reader;

	@Inject
	public ItemContentsStream setCmsContentsReader(CmsContentsReader reader) {
		this.reader = reader;
		return this;
	}

	@Override
	public ItemContentBuffer getBuffer(RepoRevision revision, CmsItemPath path, IndexingDoc pathinfo) {
		Long size = (Long) pathinfo.getFieldValue("size");
		if (size == null) {
			throw new IllegalStateException("No size information in indexing doc " + path + ". Use a different buffer strategy.");
		}
		//return new BufferStream(reader, revision, path, size.intValue());
		throw new UnsupportedOperationException("Buffer impl based on easystream not available");
		// Did not provide any performance improvement (GC time was likely not significant enough).
		// Problems with failures for large files (or just eps?), likely over 2,5 MB approx.
		// - Might have been caused by Tika closing without reading the whole stream?
	}

	// http://io-tools.sourceforge.net/easystream/user_guide/convert_outputstream_to_inputstream.html
	/*
	public static class ItemContentsInputStreamFromOutputStream extends InputStreamFromOutputStream<String> {

		private CmsContentsReader reader;
		private RepoRevision revision;
		private CmsItemPath path;
		private int size;

		public ItemContentsInputStreamFromOutputStream(CmsContentsReader reader, RepoRevision revision, CmsItemPath path, int size) {
			this.reader = reader;
			this.revision = revision;
			this.path = path;
			this.size = size;
		}

		@Override
		protected String produce(OutputStream dataSink) throws Exception {
			logger.info("Contents into easystream ({}): {}", size, path);
			reader.getContents(revision, path, dataSink);
			return null;
		}
	};

	public static class BufferStream implements ItemContentBuffer {

		private CmsContentsReader reader;
		private RepoRevision revision;
		private CmsItemPath path;
		private int size;


		public BufferStream(CmsContentsReader reader, RepoRevision revision, CmsItemPath path, int size) {
			this.reader = reader;
			this.revision = revision;
			this.path = path;
			this.size = size;
		}

		@Override
		public InputStream getContents() {
			return new ItemContentsInputStreamFromOutputStream(reader, revision, path, size);
		}

		@Override
		public void destroy() {
		}

	}
	*/
}
