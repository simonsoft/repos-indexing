/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.item;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class IdStrategyDefaultTest {

	@Test
	public void testRepository() {
		IdStrategy strategy = new IdStrategyDefault();
		CmsRepository repo = new CmsRepository("http://some.host:123/svn/repo1");
		RepoRevision rev = new RepoRevision(1, new Date());
		assertEquals("some.host:123/svn/repo1/a/b.txt@0000000001", strategy.getId(repo, rev, new CmsItemPath("/a/b.txt")));
		// Do we ever use the repository ID directly? //assertNotEquals("repository id must be distinguished from root item id",
		//		strategy.getIdRepository(repo), strategy.getIdHead(repo, null));
		assertTrue("repoid should be prefix to item ids", strategy.getId(repo, rev, new CmsItemPath("/a/b.txt"))
				.startsWith(strategy.getIdRepository(repo)));
		assertTrue("repoid should be prefix to commit ids", strategy.getIdCommit(repo, rev)
				.startsWith(strategy.getIdRepository(repo)));
		assertTrue("repoid should be prefix to info ids", strategy.getIdEntry(repo, "someRepositoryField")
				.startsWith(strategy.getIdRepository(repo)));
	}
	
	// Too many assmuptions made on the internal calls in IdStrategy. Independent impl would be better. //@Test
	public void testSubclass() {
		IdStrategy strategy = new IdStrategyDefault() {

			@Override
			public String getIdRepository(CmsRepository repository) {
				return "x-svn://" + repository.getHost() + repository.getParentPath() + "/" + repository.getName() + "^";
			}

			@Override
			protected String getRootPath() {
				return "/"; // logical ID definition
			}

			@Override
			protected String getPegSeparator() {
				return "?p=";
			}
			
		};
		
		CmsItemId doc1 = new CmsItemIdArg("x-svn://my.host:1234/svn/demo1^/vvab/xml/documents/900108.xml").withPegRev(136L);
		assertEquals("x-svn://my.host:1234/svn/demo1^", strategy.getIdRepository(doc1.getRepository()));
		assertEquals("x-svn://my.host:1234/svn/demo1^/vvab/xml/documents/900108.xml", strategy.getIdHead(doc1));
		assertEquals("x-svn://my.host:1234/svn/demo1^/vvab/xml/documents/900108.xml?p=136", strategy.getId(doc1, new RepoRevision(136, new Date())));
		assertNotEquals("repository id must be distinguished from root item id",
				strategy.getIdRepository(doc1.getRepository()), strategy.getIdHead(doc1.getRepository(), null));
	}
	
	@Test
	public void testNonasciiPath() {
		IdStrategy strategy = new IdStrategyDefault();
		CmsRepository repo = new CmsRepository("http://some.host:123/svn/repo1");
		RepoRevision rev = new RepoRevision(1, new Date());
		
		assertEquals("some.host:123/svn/repo1/a%20b/c.txt@0000000001", strategy.getId(repo, rev, new CmsItemPath("/a b/c.txt")));
		
		assertEquals("some.host:123/svn/repo1/a/%3F.txt@0000000001", strategy.getId(repo, rev, new CmsItemPath("/a/?.txt"))); // quite possibly not a valid path
		
		assertEquals("some.host:123/svn/repo1/a@b/c.txt@0000000001", strategy.getId(repo, rev, new CmsItemPath("/a@b/c.txt"))); // @ should not be encoded in path section of URLs.
		
	}
	
	@Test
	public void testNonasciiRepository() {
		IdStrategy strategy = new IdStrategyDefault();
		CmsRepository repo = new CmsRepository("http://some.host:123/svn/repo%20space");
		RepoRevision rev = new RepoRevision(1, new Date());
		assertEquals("some.host:123/svn/repo%20space/a/b.txt@0000000001", strategy.getId(repo, rev, new CmsItemPath("/a/b.txt")));
		CmsRepository repoFromPath = new CmsRepository("http://some.host:123", "/svn", "repo space");
		// behavior still undefined in CmsRepository //assertEquals("some.host:123/svn/repo%20space/a/b.txt@1", strategy.getId(repoFromPath, rev, new CmsItemPath("/a/b.txt")));
	}

}
