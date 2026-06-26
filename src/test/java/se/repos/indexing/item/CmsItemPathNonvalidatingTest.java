/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import se.simonsoft.cms.item.CmsItemPath;

/**
 * Should we try to paths through indexing even if we wouldn't accept them at creation?
 * We ignore items with invalid paths now, and log an exception.
 * Proably good because an index consumer may fail too.
 *
 * A reason to fix it is that validation of obsoleted items might crash, like:
11:52:35.494 [main] INFO  se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLookRepo - Derived 36 changeset items for revision 503/2010-05-12T11:42:22Z
Exception in thread "main" java.lang.IllegalStateException: Obsoleted revision 502/2010-05-12T11:38:33Z for D  /xml/product_documents/Installation Instructions/DVCompact/Sections for DVCompact/Installation|/DV Compact_technical-data_002.xml does not match 497/2010-05-12T11:03:32Zwhen it was last indexed
	at se.repos.indexing.item.HandlerHeadinfo.handle(HandlerHeadinfo.java:74)
	at se.repos.indexing.scheduling.HandlerIteration.proceed(HandlerIteration.java:87)
	at se.repos.indexing.scheduling.IndexingScheduleBlockingOnly.run(IndexingScheduleBlockingOnly.java:82)
	at se.repos.indexing.scheduling.IndexingScheduleBlockingOnly.add(IndexingScheduleBlockingOnly.java:56)
	at se.repos.indexing.repository.ReposIndexingPerRepository.sync(ReposIndexingPerRepository.java:222)
	at se.repos.indexing.standalone.IndexingDaemon.runOnce(IndexingDaemon.java:233)
	at se.repos.indexing.standalone.IndexingDaemon.run(IndexingDaemon.java:185)
	at se.repos.indexing.standalone.CommandLine.runDaemon(CommandLine.java:138)
	at se.repos.indexing.standalone.CommandLine.main(CommandLine.java:88)
 * 
 * The above however doesn't happen if indexing is restarted, so it is no blocker.
 * 
 * If we start to read previous item when indexing we need to account for previously skipped items,
 * or maybe try to index but flag them so then don't end up in reporting results.
 * 
 * If we do try to index invalid paths, should we use a CmsItemPath subclass? Give ChangesetReader a factory?
 */
public class CmsItemPathNonvalidatingTest {

	@Test
	@Ignore // Current spec is to validate and skip invalid, so reporting consumers don't get these invalid matches
	public void testFilenameEndingWithWhitespace() {
		new CmsItemPath("/xml/product_documents/Installation Instructions/DVCompact/Sections for DVCompact ");
	}
	
	@Test
	@Ignore // Current spec is to validate and skip invalid, so reporting consumers don't get these invalid matches
	public void testAnyCharsFromAnyFilesystem() {
		// except backslash
		new CmsItemPath("/folder/½!\"#¤%&()=?`¶@£${[]}±+΅~^*'|<>");
	}
	
	@Test
	public void testOddUnicode() {
		// TODO
	}

}
