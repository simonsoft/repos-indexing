/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class HandlerPathinfoTest {

	@Test
	public void testHandle() {
		HandlerPathinfo pathinfo = new HandlerPathinfo();
		pathinfo.setIdStrategy(new IdStrategyDefault());
		CmsRepository repo = new CmsRepository("https://h.ost:1080/svn/repo1");
		Date millisTrunc = RepoRevision.parseDate("2012-04-11T12:01:46.600807Z");
		RepoRevision rev = new RepoRevision(10L, millisTrunc);
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingItemProgress p = new IndexingItemStandalone(repo, rev, item);
		
		when(item.getPath()).thenReturn(new CmsItemPath("/my/dir/a file.txt"));
		when(item.isFile()).thenReturn(true);
		when(item.isFolder()).thenReturn(false);
		when(item.getRevisionChanged()).thenReturn(new RepoRevision(rev.getNumber() - 2, new Date(rev.getDate().getTime() - 1000)));
		
		when(item.isAdd()).thenReturn(true);
		when(item.isContent()).thenReturn(true);
		when(item.isContentModified()).thenReturn(false);
		when(item.isProperties()).thenReturn(true);
		when(item.isPropertiesModified()).thenReturn(false);
		
		pathinfo.handle(p);
		
		IndexingDoc f = p.getFields();
		assertEquals("/my/dir/a file.txt", f.getFieldValue("path"));
		assertEquals("a file.txt", f.getFieldValue("pathname"));
		assertEquals("a file", f.getFieldValue("pathnamebase"));
		assertEquals("/my/dir", f.getFieldValue("pathdir"));
		assertEquals("txt", f.getFieldValue("pathext"));
		assertEquals("/svn/repo1/my/dir/a file.txt", f.getFieldValue("pathfull"));
		Collection<Object> in = f.getFieldValues("pathin");
		assertEquals(2, in.size());
		Iterator<Object> init = in.iterator();
		assertEquals("/my/dir", init.next());
		assertEquals("/my", init.next());
		Collection<Object> fin = f.getFieldValues("pathfullin");
		assertEquals(4, fin.size());
		Iterator<Object> finit = fin.iterator();
		assertEquals("/svn/repo1/my/dir", finit.next());
		assertEquals("/svn/repo1/my", finit.next());
		assertEquals("/svn/repo1", finit.next());
		assertEquals("/svn", finit.next());
		// everything could be derived with copyFrom with pathfull, but for now we don't have all that analysis in Solr
		Collection<Object> part = f.getFieldValues("pathpart");
		Iterator<Object> partit = part.iterator();
		assertEquals("my", partit.next());
		assertEquals("dir", partit.next());
		assertEquals("a file.txt", partit.next());
		assertEquals(3, part.size());
		
		assertNull(f.getFieldValue("pathsegment0"));
		assertEquals("my", f.getFieldValue("pathsegment1"));
		assertEquals("dir", f.getFieldValue("pathsegment2"));
		assertEquals("a file.txt", f.getFieldValue("pathsegment3"));
		assertNull(f.getFieldValue("pathsegment4"));
		
		assertEquals("Must be String in order to get correct JSON from SolR", "A", f.getFieldValue("pathstat"));
		assertEquals(null, f.getFieldValue("pathstatprop")); // schema comment can be interpreted as "" but does it matter to search?
		assertEquals("file", f.getFieldValue("type"));
		
		assertEquals(rev.getNumber(), f.getFieldValue("rev"));
		assertEquals(rev.getDate(), f.getFieldValue("revt"));
		RepoRevision revActual = new RepoRevision((Long) f.getFieldValue("rev"), (Date) f.getFieldValue("revt"));
		assertEquals("formatting of date might truncate trailing 00", "2012-04-11T12:01:46.600", revActual.getTimeIso()); // The issue might be relates to JSON transfer.
		assertEquals(rev.getNumber() - 2, f.getFieldValue("revc"));
		assertEquals(new Date(rev.getDate().getTime() - 1000), f.getFieldValue("revct"));
		
		assertEquals("https://h.ost:1080/svn/repo1/my/dir/a%20file.txt?p=10", f.getFieldValue("url"));
		assertEquals("https://h.ost:1080/svn/repo1/my/dir/a%20file.txt", f.getFieldValue("urlhead"));
		assertEquals("/svn/repo1/my/dir/a%20file.txt?p=10", f.getFieldValue("urlpath"));
		assertEquals("/svn/repo1/my/dir/a%20file.txt", f.getFieldValue("urlpathhead"));
		
		assertEquals("repo1", f.getFieldValue("repo"));
		assertEquals("h.ost:1080", f.getFieldValue("repohost"));
		assertEquals("/svn", f.getFieldValue("repoparent"));
		assertEquals("h.ost:1080/svn/repo1", f.getFieldValue("repoid"));
	}

	@Test
	public void testHandlePropchangeAndCopy() {
		HandlerPathinfo pathinfo = new HandlerPathinfo();
		pathinfo.setIdStrategy(new IdStrategyDefault());
		CmsRepository repo = new CmsRepository("https://h.ost:1080/svn/repo1");
		RepoRevision rev = new RepoRevision(10L, new Date());
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingItemProgress p = new IndexingItemStandalone(repo, rev, item);
		
		when(item.getPath()).thenReturn(new CmsItemPath("/my/dir/a file.txt"));
		when(item.isFile()).thenReturn(true);
		when(item.isFolder()).thenReturn(false);
		when(item.getRevisionChanged()).thenReturn(new RepoRevision(rev.getNumber() - 2, new Date(rev.getDate().getTime() - 1000)));
		
		when(item.isAdd()).thenReturn(false);
		when(item.isContent()).thenReturn(false);
		when(item.isContentModified()).thenReturn(false);
		when(item.isProperties()).thenReturn(true);
		when(item.isPropertiesModified()).thenReturn(true);
		//when(item.isCopySource()).thenReturn(true);
		
		pathinfo.handle(p);
		
		IndexingDoc f = p.getFields();
		
		assertEquals(null, f.getFieldValue("pathstat"));
		/* #919 Suppressing "pathstatprop" field for now, challenging to determine via http (SVNKit 1.10.2).
		assertEquals("M", f.getFieldValue("pathstatprop")); // schema comment can be interpreted as "" but does it matter to search?
		*/
		//assertEquals(true, f.getFieldValue("copyhas")); // #789 #919 Suppressing "copyhas" field for now
		
		CmsChangesetItem item2 = mock(CmsChangesetItem.class);
		IndexingItemProgress p2 = new IndexingItemStandalone(repo, rev, item2);
		
		when(item2.getPath()).thenReturn(new CmsItemPath("/my/dir/another file.txt"));
		when(item2.isFile()).thenReturn(true);
		when(item2.isFolder()).thenReturn(false);
		when(item2.getRevisionChanged()).thenReturn(new RepoRevision(rev.getNumber() - 2, new Date(rev.getDate().getTime() - 1000)));
		
		when(item2.isAdd()).thenReturn(true);
		when(item2.isContent()).thenReturn(true);
		when(item2.isContentModified()).thenReturn(false);
		when(item2.isProperties()).thenReturn(true);
		when(item2.isPropertiesModified()).thenReturn(false);
		//when(item2.isCopySource()).thenReturn(false);
		when(item2.isCopy()).thenReturn(true);
		when(item2.getCopyFromPath()).thenReturn(new CmsItemPath("/my/dir/a file.txt"));
		when(item2.getCopyFromRevision()).thenReturn(new RepoRevision(rev.getNumber() - 4, new Date(rev.getDate().getTime() - 4000)));
		
		pathinfo.handle(p2);
		IndexingDoc f2 = p2.getFields();
		assertEquals("Must be String in order to get correct JSON from SolR", "A", f2.getFieldValue("pathstat"));
		assertEquals(null, f2.getFieldValue("pathstatprop"));
		//assertEquals(false, f2.getFieldValue("copyhas")); // #789 #919 Suppressing "copyhas" field for now
		assertEquals("/my/dir/a file.txt", f2.getFieldValue("copyfrom"));
		assertEquals(6L, f2.getFieldValue("copyfromrev"));
		assertNotNull(f2.getFieldValue("copyfromrevt"));
	}
	
	@Test
	public void testHandleUrl() {	
		HandlerPathinfo pathinfo = new HandlerPathinfo();
		pathinfo.setIdStrategy(new IdStrategyDefault());
		@SuppressWarnings("serial")
		CmsRepository repo = new CmsRepository("https://h.ost:1080/svn/repo1") {
			@Override
			protected String urlencodeSegment(String pathSegment) {
				return super.urlencodeSegment(pathSegment).replace('.', '$'); // very special encoding
			}
		};
		RepoRevision rev = new RepoRevision(10L, new Date());
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingItemProgress p = new IndexingItemStandalone(repo, rev, item);
		when(item.getPath()).thenReturn(new CmsItemPath("/my file.txt"));
		when(item.getRevisionChanged()).thenReturn(rev);
		
		pathinfo.handle(p);
		
		assertEquals("Should use CmsRepository's URL encoding",
				"https://h.ost:1080/svn/repo1/my%20file$txt", p.getFields().getFieldValue("urlhead"));
	}
	
	@Test
	public void testHandleDerived() {
		HandlerPathinfo pathinfo = new HandlerPathinfo();
		pathinfo.setIdStrategy(new IdStrategyDefault());
		CmsRepository repo = new CmsRepository("https://h.ost:1080/svn/repo1");
		RepoRevision rev = new RepoRevision(10L, new Date());
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingItemProgress p = new IndexingItemStandalone(repo, rev, item);
		
		when(item.getPath()).thenReturn(new CmsItemPath("/my/dir/a file.txt"));
		when(item.isDerived()).thenReturn(true);
		when(item.getRevisionChanged()).thenThrow(new UnsupportedOperationException("commit revision not supported for derived items in cms-backend-svnkit 1.1.0"));
		
		pathinfo.handle(p);
	}
	
}
