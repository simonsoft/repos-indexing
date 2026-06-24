/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import se.repos.indexing.ReposIndexing;
import se.repos.indexing.config.MockableSvnDumpProfile;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.svn.runtime.SvnDump;

@QuarkusTest
@TestProfile(MockableSvnDumpProfile.class)
public class ReposIndexingPerRepositoryIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Inject
	ReposIndexing indexing;

	@Inject
	IndexingSchedule schedule;

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Inject
	Instance<ReposIndexingPerRepository> indexingInstances;

	@Inject
	Instance<CmsChangesetReader> changesetReaderInstances;
	
	private String getTail(Object id) {
		
		String str = (String) id;
		String[] split = str.split("/");
		return split[split.length-1];
	}
	
	@Test
	@ActivateRequestContext
	public void testMarkItemHead() throws SolrServerException, IOException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r3.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		indexing.sync(new RepoRevision(1, new Date(1)));

		// Verify r1 head:false
		SolrDocumentList r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		assertEquals(3, r1.size());
		assertEquals("/dir", r1.get(0).getFieldValue("path"));
		assertAllHeadFalse(r1);
		r1 = null;
		
		// Verify r1 head:true
		SolrDocumentList r1Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r1", 3, r1Head.size());
		assertEquals("folders are also head:true now", "/dir", r1Head.get(0).get("path"));
		assertEquals("...", "/dir/t2.txt", r1Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r1Head.get(2).get("path"));
		assertEquals("...", 1L, r1Head.get(0).get("rev"));
		assertEquals("...", 1L, r1Head.get(1).get("rev"));
		assertEquals("...", 1L, r1Head.get(2).get("rev"));
		assertEquals("...", 1L, r1Head.get(0).get("revc"));
		assertEquals("...", 1L, r1Head.get(1).get("revc"));
		assertEquals("...", 1L, r1Head.get(2).get("revc"));
		
		assertEquals("...", "folder", r1Head.get(0).get("type"));
		assertEquals("...", "file", r1Head.get(1).get("type"));
		assertEquals("...", "file", r1Head.get(2).get("type"));
		
		
		r1Head = null;
		
		// Verify r1 after r2 head:false
		indexing.sync(new RepoRevision(2, new Date(2)));
		SolrDocumentList r2r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();
		assertEquals(3, r2r1.size());
		assertAllHeadFalse(r2r1);
		r2r1 = null;
		
		// Verify r2 head:false
		SolrDocumentList r2 = repositem.query(new SolrQuery("id:*@0000000002").setSort("path", ORDER.asc)).getResults();
		assertEquals("all rev items have head=false, " + r2.get(0), false, r2.get(0).get("head"));
		assertAllHeadFalse(r2);
		assertEquals("id has rev when head=false, " + r2.get(0), "t1.txt@0000000002", getTail(r2.get(0).get("id")));
		assertEquals("idhead never has rev, " + r2.get(0), "t1.txt", getTail(r2.get(0).get("idhead")));
		// url never had revision before repos-indexing 0.20
		assertEquals("url has rev when head=false, " + r2.get(0), "t1.txt?p=2", getTail(r2.get(0).get("url")));
		assertEquals("urlhead never has rev, " + r2.get(0), "t1.txt", getTail(r2.get(0).get("urlhead")));
		assertEquals("url has rev when head=false, " + r2.get(0), "t1.txt?p=2", getTail(r2.get(0).get("urlpath")));
		assertEquals("urlhead never has rev, " + r2.get(0), "t1.txt", getTail(r2.get(0).get("urlpathhead")));
		assertNull("urlid is null because the handler is not in repos-indexing core, " + r2.get(0), r2.get(0).get("urlid"));
		r2 = null;
		
		// Verify r2 head:true
		SolrDocumentList r2Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r2", 3, r2Head.size());
		assertEquals("folders are also head:true now", "/dir", r2Head.get(0).get("path"));
		assertEquals("...", "/dir/t2.txt", r2Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r2Head.get(2).get("path"));
		assertEquals("...", 1L, r2Head.get(0).get("rev"));
		assertEquals("...", 1L, r2Head.get(1).get("rev"));
		assertEquals("...", 2L, r2Head.get(2).get("rev"));
		assertEquals("...", 1L, r2Head.get(0).get("revc"));
		assertEquals("...", 1L, r2Head.get(1).get("revc"));
		assertEquals("...", 2L, r2Head.get(2).get("revc"));
		assertEquals("id has no rev when head=true, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("id")));
		assertEquals("idhead never has rev, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("idhead")));
		assertEquals("id has no rev when head=true, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("url")));
		assertEquals("idhead never has rev, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("urlhead")));
		assertEquals("id has no rev when head=true, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("urlpath")));
		assertEquals("idhead never has rev, " + r2Head.get(2), "t1.txt", getTail(r2Head.get(2).get("urlpathhead")));
		assertNull("urlid is null because the handler is not in repos-indexing core, " + r2Head.get(2), r2Head.get(2).get("urlid"));
		r2Head = null;
		
				
		// Verify r1 after r3 head:false
		// everything from r1 should now have been replaced with later versions
		indexing.sync(new RepoRevision(3, new Date(3)));
		SolrDocumentList r3r1 = repositem.query(new SolrQuery("id:*@0000000001").setSort("path", ORDER.asc)).getResults();		
		assertEquals("/dir", r3r1.get(0).get("path"));
		assertEquals("/dir/t2.txt", r3r1.get(1).get("path"));
		assertEquals("A", r3r1.get(0).get("pathstat"));
		assertEquals("A", r3r1.get(1).get("pathstat")); // Passes despite JSON response contains "java.lang.Character:A"
		assertEquals("/t1.txt", r3r1.get(2).get("path"));
		assertEquals("Should have revauthor.", "solsson", r3r1.get(2).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "Two files with two lines each", r3r1.get(2).getFieldValue("revcomment"));
		assertNull("should be null, unable to reproduce", r3r1.get(2).getFieldValue("prop_svn.entry.uuid"));
		assertEquals("Revision 1 had only these files, nothing else should have been indexed on rev 1 since then", 3, r3r1.size());

		// TODO support folders assertEquals("Folder is deleted and thus no longer in head", false, r3r1.get(0).get("head"));
		assertEquals("Old file that is now gone because of folder delete should not be head", false, r3r1.get(1).get("head"));
		assertEquals("The file that was changed in r3 should now be marked as non-head", false, r3r1.get(2).get("head"));
		r3r1 = null;
		
		// Verify r2 after r3 head:false
		SolrDocumentList r3r2 = repositem.query(new SolrQuery("id:*@0000000002").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was only a file edit in rev 2", 1, r3r2.size());
		assertEquals("/t1.txt", r3r2.get(0).get("path"));
		assertEquals("Should have revauthor.", "test", r3r2.get(0).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "file modification", r3r2.get(0).getFieldValue("revcomment"));
		assertEquals("all rev items have head=false", false, r3r2.get(0).get("head"));
		//assertEquals("Rev 2 is still HEAD for this file", true, r3r2.get(0).get("head"));
		r3r2 = null;
		
		// Verify r3 head:false
		SolrDocumentList r3r3 = repositem.query(new SolrQuery("id:*@0000000003").setSort("path", ORDER.asc)).getResults();
		assertEquals("Moved folder in rev 3, 2*2 changes with derived file.", 4, r3r3.size());
		assertEquals("Deletions should be indexed so we know when an item disappeared", "/dir", r3r3.get(0).get("path"));
		assertEquals("Derived delete", "/dir/t2.txt", r3r3.get(1).get("path"));
		assertEquals("Folder copy", "/dir2", r3r3.get(2).get("path"));
		assertEquals("Derived", "/dir2/t2.txt", r3r3.get(3).get("path"));
		
		assertEquals("Should have revauthor.", "test", r3r3.get(0).getFieldValue("revauthor"));
		assertEquals("Should have revcomment.", "folder move without changes to the contained file", r3r3.get(0).getFieldValue("revcomment"));
		assertEquals("Should have revcauthor when not 'derived' from folder copy", "test", r3r3.get(0).getFieldValue("revcauthor"));
		assertEquals("Should have revccomment when not 'derived' from folder copy", "folder move without changes to the contained file", r3r3.get(0).getFieldValue("revccomment"));		
		assertEquals("Deletions should always be !head", false, r3r3.get(0).get("head"));
		assertEquals("Deletions should always be !head", false, r3r3.get(1).get("head"));
		assertEquals(false, r3r3.get(1).get("head"));
		assertAllHeadFalse(r3r3);
		r3r3 = null;

		// Verify r3 head:true
		SolrDocumentList r3Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r3", 3, r3Head.size());
		assertEquals("folders are also head:true now", "/dir2", r3Head.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r3Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r3Head.get(2).get("path"));
		assertEquals("...", 3L, r3Head.get(0).get("rev"));
		assertEquals("...", 3L, r3Head.get(1).get("rev"));
		assertEquals("...", 2L, r3Head.get(2).get("rev"));
		assertEquals("...", 3L, r3Head.get(0).get("revc"));
		assertEquals("...", 3L, r3Head.get(1).get("revc"));
		assertEquals("...", 2L, r3Head.get(2).get("revc"));
		r3Head = null;
		
		// Verify r4 commit info, where r4 does not actually exist (?).
		SolrDocumentList r4 = repositem.query(new SolrQuery("type:commit").setSort("rev", ORDER.asc)).getResults();
		assertEquals(0L, r4.get(0).getFieldValue("rev"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.log"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.author"));
		assertEquals(null, r4.get(0).getFieldValue("proprev_svn.date"));
		
		assertEquals(1L, r4.get(1).getFieldValue("rev"));
		assertEquals("Two files with two lines each", r4.get(1).getFieldValue("proprev_svn.log"));
		assertEquals("solsson", r4.get(1).getFieldValue("proprev_svn.author"));
		assertEquals("2012-09-27T12:05:34.040515Z", r4.get(1).getFieldValue("proprev_svn.date"));
		
		assertEquals(2L, r4.get(2).getFieldValue("rev"));
		assertEquals("file modification", r4.get(2).getFieldValue("proprev_svn.log"));
		assertEquals("test", r4.get(2).getFieldValue("proprev_svn.author"));
		assertEquals("2013-03-21T19:16:28.271167Z", r4.get(2).getFieldValue("proprev_svn.date"));
		
		assertEquals(3L, r4.get(3).getFieldValue("rev"));
		assertEquals("folder move without changes to the contained file", r4.get(3).getFieldValue("proprev_svn.log"));
		assertEquals("test", r4.get(3).getFieldValue("proprev_svn.author"));
		assertEquals("2013-03-21T19:16:42.295071Z", r4.get(3).getFieldValue("proprev_svn.date"));
		r4 = null;
		
		// TODO we could propedit on dir2 and check that rev 3 of it becomes !head
		
		// TODO if we now modify t2 then latest dir2 should still be head
		
		// TODO if we then delete dir2 in the next commit we can demonstrate the issue with marking folders as !head when files have changed in them; need for workaround
		} finally {
			schedule.stop();
		}
	}	

	
	// Demonstrate path revision of folders.
	// TODO: Should path revision behave same as Subversion?
	@Test
	@ActivateRequestContext
	public void testMarkItemHeadFolderFileModified() throws IOException, SolrServerException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r4-filemodified.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		indexing.sync(new RepoRevision(4, new Date(4)));
	
		// Verify r4 head:true
		SolrDocumentList r4Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r4", 3, r4Head.size());
		assertEquals("folders are also head:true now", "/dir2", r4Head.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r4Head.get(1).get("path"));
		assertEquals("...", "/t1.txt", r4Head.get(2).get("path"));
		assertEquals("TODO? path rev should be r4 due to modified file", 3L, r4Head.get(0).get("rev"));
		assertEquals("file modified in r4", 4L, r4Head.get(1).get("rev"));
		assertEquals("...", 2L, r4Head.get(2).get("rev"));
		assertEquals("commit rev should be r3", 3L, r4Head.get(0).get("revc"));
		assertEquals("file modified in r4", 4L, r4Head.get(1).get("revc"));
		assertEquals("...", 2L, r4Head.get(2).get("revc"));
		r4Head = null;
		} finally {
			schedule.stop();
		}
	}
	
	
	@Test
	@ActivateRequestContext
	public void testMarkItemHeadCopy() throws IOException, SolrServerException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r5-copy.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		// Confirmed that each revision is indexed with r5 as reference revision.
		indexing.sync(new RepoRevision(5, new Date(5)));
		
		// Test that we can reindex without failure.
		
		SolrDocumentList r5r4 = repositem.query(new SolrQuery("id:*@0000000004").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was a folder and a file rev 4", 2, r5r4.size());
		
		assertEquals("...", "/dir2-copy", r5r4.get(0).get("path"));
		assertEquals("...", "/dir2-copy/t2.txt", r5r4.get(1).get("path"));
		
		assertEquals("...", 4L, r5r4.get(0).get("rev"));
		assertEquals("...", 4L, r5r4.get(1).get("rev"));
		
		assertEquals("...", 4L, r5r4.get(0).get("revc"));
		assertEquals("...", 4L, r5r4.get(1).get("revc"));
		assertAllHeadFalse(r5r4);
		r5r4 = null;
		
		SolrDocumentList r5 = repositem.query(new SolrQuery("id:*@0000000005").setSort("path", ORDER.asc)).getResults();
		assertEquals("There was a file r5", 1, r5.size());
		assertEquals("...", "/dir2/t2.txt", r5.get(0).get("path"));
		assertEquals("...", "D", r5.get(0).get("pathstat"));
		assertAllHeadFalse(r5);
		
		// Verify r5 head:true
		SolrDocumentList r5Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r5", 4, r5Head.size());
		assertEquals("...", "/dir2", r5Head.get(0).get("path"));
		assertEquals("...", "/dir2-copy", r5Head.get(1).get("path"));
		assertEquals("...", "/dir2-copy/t2.txt", r5Head.get(2).get("path"));
		assertEquals("...", "/t1.txt", r5Head.get(3).get("path"));
		assertEquals("TODO? folder not indexed when containing file deleted", 3L, r5Head.get(0).get("rev"));
		assertEquals("...", 4L, r5Head.get(1).get("rev"));
		assertEquals("...", 4L, r5Head.get(2).get("rev"));
		assertEquals("...", 2L, r5Head.get(3).get("rev"));
		assertEquals("...", 3L, r5Head.get(0).get("revc"));
		assertEquals("...", 4L, r5Head.get(1).get("revc"));
		assertEquals("...", 4L, r5Head.get(2).get("revc"));
		assertEquals("...", 2L, r5Head.get(3).get("revc"));
		r5Head = null;
		} finally {
			schedule.stop();
		}
	}
	
	@Test
	@ActivateRequestContext
	public void testMarkItemHeadCopyDeleted() throws SolrServerException, IOException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r6-copydeleted.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		// Confirmed that each revision is indexed with r5 as reference revision.
		indexing.sync(new RepoRevision(6, new Date(6)));
				
		// Test that we can reindex without failure.
		System.out.println("Test that we can reindex without failure.");
		
		SolrDocumentList r6 = repositem.query(new SolrQuery("id:*@0000000006").setSort("path", ORDER.asc)).getResults();
		assertEquals("Restored a folder and two files in rev 6", 3, r6.size());
		assertEquals("...", "/dir2", r6.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r6.get(1).get("path"));
		assertEquals("...", "/dir2/t3.txt", r6.get(2).get("path"));
		assertAllHeadFalse(r6);
		r6 = null;
		
		// Verify r6 head:true
		SolrDocumentList r6Head = repositem.query(new SolrQuery("head:true").setSort("path", ORDER.asc)).getResults();
		assertEquals("head items after r6", 4, r6Head.size());
		assertEquals("...", "/dir2", r6Head.get(0).get("path"));
		assertEquals("...", "/dir2/t2.txt", r6Head.get(1).get("path"));
		assertEquals("...", "/dir2/t3.txt", r6Head.get(2).get("path"));
		assertEquals("...", "/t1.txt", r6Head.get(3).get("path"));
		assertEquals("folder restored", 6L, r6Head.get(0).get("rev"));
		assertEquals("...", 6L, r6Head.get(1).get("rev"));
		assertEquals("...", 6L, r6Head.get(2).get("rev"));
		assertEquals("...", 2L, r6Head.get(3).get("rev"));
		assertEquals("...", 6L, r6Head.get(0).get("revc"));
		assertEquals("...", 6L, r6Head.get(1).get("revc"));
		assertEquals("...", 6L, r6Head.get(2).get("revc"));
		assertEquals("...", 2L, r6Head.get(3).get("revc"));
		r6Head = null;
		} finally {
			schedule.stop();
		}
	}
	
	@Test
	@ActivateRequestContext
	public void testMarkItemHeadAddDeleted() throws SolrServerException, IOException {
		String dumpFileName = "se/repos/indexing/testrepo1r7-adddeleted.svndump";
		logger.info("Testing: {}", dumpFileName);
		QuarkusMock.installMockForType(new SvnDump(dumpFileName), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		// Indexing whole repo in batch does not reproduce the issue. 
		// Batch index first era of t1.txt.
		indexing.sync(new RepoRevision(5, new Date(5)));
		// Incrementally index remaining revisions.
		indexing.sync(new RepoRevision(6, new Date(6)));
		indexing.sync(new RepoRevision(7, new Date(7)));
		
		/*
		 * r4: Moves t1.txt to t1-renamed.txt
		 * r5: Deletes t1-renamed.txt
		 * r6: Adds a new t1.txt
		 * r7: Deletes the new t1.txt
		 */
		
		SolrDocumentList r7r7 = repositem.query(new SolrQuery("id:*@0000000007").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("Only a single file removed in rev 7", 1, r7r7.size());
		logger.info("Done: {}", dumpFileName);
		} finally {
			schedule.stop();
		}
	}
	
	@Test
	@ActivateRequestContext
	public void testMarkItemHeadAddDeletedSync4() throws SolrServerException, IOException {
		String dumpFileName = "se/repos/indexing/testrepo1r7-adddeleted.svndump";
		logger.info("Testing: {}", dumpFileName);
		QuarkusMock.installMockForType(new SvnDump(dumpFileName), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		// Indexing whole repo in batch does not reproduce the issue. 
		// Batch index first era of t1.txt.
		indexing.sync(new RepoRevision(4, new Date(4)));
		indexing.sync(new RepoRevision(5, new Date(5)));
		// Incrementally index remaining revisions.
		indexing.sync(new RepoRevision(6, new Date(6)));
		indexing.sync(new RepoRevision(7, new Date(7)));
		
		/*
		 * r4: Moves t1.txt to t1-renamed.txt
		 * r5: Deletes t1-renamed.txt
		 * r6: Adds a new t1.txt
		 * r7: Deletes the new t1.txt
		 */
		
		SolrDocumentList r7r7 = repositem.query(new SolrQuery("id:*@0000000007").setSort("path", ORDER.asc)).getResults();
		
		assertEquals("Only a single file removed in rev 7", 1, r7r7.size());
		logger.info("Done: {}", dumpFileName);
		} finally {
			schedule.stop();
		}
	}
	
	
	@SuppressWarnings("serial")
	@Test
	@ActivateRequestContext
	public void testAbortedRev() throws SolrServerException, IOException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r3.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		CmsChangesetReader changesetReader = spy(changesetReaderInstances.get());
		ReposIndexingPerRepository indexingProxy = indexingInstances.get();
		ReposIndexingPerRepository indexing = ClientProxy.unwrap(indexingProxy);
		indexing.setCmsChangesetReader(changesetReader);
		
		RepoRevision revision1 = RepoRevision.parse("1/2012-09-27T12:05:34.040515Z");
		RepoRevision revision2 = RepoRevision.parse("2/2013-03-21T19:16:28.271Z");
		RepoRevision revision3 = RepoRevision.parse("3/2013-03-21T19:16:42.295Z");
		
		// first indexing, two revisions in one sync
		indexing.sync(revision2);
		assertEquals("should have indexed up to the given revision", 2, indexing.getRevision().getNumber());
		QueryResponse r1 = repositem.query(new SolrQuery("type:commit").addSort("rev", ORDER.asc));
		assertEquals("Rev 0 should have been indexed in addition to 1 and 2", 3, r1.getResults().size());
		assertEquals("Rev 0 should be marked as completed", true, r1.getResults().get(0).getFieldValue("complete"));
		
		// second indexing
		indexing.sync(revision3);
		assertEquals("Revision 3 should have been indexed", 1,
				repositem.query(new SolrQuery("type:commit AND rev:3 AND complete:true")).getResults().getNumFound());
		
		// new indexing service, recover sync status
		indexingInstances.destroy(indexingProxy);
		ReposIndexingPerRepository indexing2Proxy = indexingInstances.get();
		ReposIndexingPerRepository indexing2 = ClientProxy.unwrap(indexing2Proxy);
		indexing2.setCmsChangesetReader(changesetReader);
		indexing2.sync(revision3); // same revision as before, because polling is done at sync
		assertNotNull("New indexing should poll for indexed revision",
				indexing2.getRevision());
		assertEquals("New indexing should poll for highest indexed (started) revision", 
				3, indexing2.getRevision().getNumber());
		
		// mess with the index to see how sync status is handled
		indexingInstances.destroy(indexing2Proxy);
		SolrInputDocument markAsFailed = new SolrInputDocument();
		markAsFailed.setField("id", r1.getResults().get(1).getFieldValue("id").toString().replace("#0000000001", "#0000000002"));
		markAsFailed.setField("complete", new HashMap<String, Boolean>() {{
			put("set", false);
		}});
		repositem.add(markAsFailed);
		repositem.commit();
		assertEquals("Service isn't required (or expected) to poll again, can assume no cuncurrent indexing",
				3, indexing.getRevision().getNumber());
		
		// index after incomplete (though normally rev 3 wouldn't exist if rev 2 is incomplete)
		ReposIndexingPerRepository indexing3Proxy = indexingInstances.get();
		ReposIndexingPerRepository indexing3 = ClientProxy.unwrap(indexing3Proxy);
		indexing3.setCmsChangesetReader(changesetReader);
		try {
			indexing3.sync(revision3);
			fail("Should throw exception because this is an index state that our code should never be able to produce, as it is expected to abort on any error");
		} catch (IllegalStateException e) {
			// expected
		}
		
		markAsFailed.setField("id", r1.getResults().get(1).getFieldValue("id").toString().replace("#0000000001", "#0000000003"));
		markAsFailed.setField("complete", new HashMap<String, Boolean>() {{
			put("set", false);
		}});
		repositem.add(markAsFailed);
		repositem.commit();
		indexing3.sync(revision3);
		assertEquals("Revision 2 and 3 should have been indexed", 4,
				repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().getNumFound());
		
		verify(changesetReader, times(1)).read(revision1, revision2); // first
		verify(changesetReader, times(1)).read(revision2); // first
		verify(changesetReader, times(1)).read(revision2, revision3); // after recover
		verify(changesetReader, times(2)).read(revision3); // second, no read needed for third, read again after recover
		
		// checking with verify is difficult, can also be done with capture
		ArgumentCaptor<RepoRevision> revsAlone = ArgumentCaptor.forClass(RepoRevision.class);
		verify(changesetReader, times(3)).read(revsAlone.capture());
		ArgumentCaptor<RepoRevision> revsWith = ArgumentCaptor.forClass(RepoRevision.class);
		ArgumentCaptor<RepoRevision> revsRef = ArgumentCaptor.forClass(RepoRevision.class);
		verify(changesetReader, times(2)).read(revsWith.capture(), revsRef.capture());
		assertEquals(revision1, revsWith.getAllValues().get(0));
		assertEquals(revision2, revsRef.getAllValues().get(0));
		assertEquals(revision2, revsAlone.getAllValues().get(0));
		assertEquals(revision3, revsAlone.getAllValues().get(1));
		assertEquals(revision2, revsWith.getAllValues().get(1));
		assertEquals(revision3, revsRef.getAllValues().get(1));
		assertEquals(revision3, revsAlone.getAllValues().get(2));
		indexingInstances.destroy(indexing3Proxy);
		} finally {
			schedule.stop();
		}
	}
	
	@Test
	@ActivateRequestContext
	public void testSyncTwice() throws SolrServerException, IOException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		ReposIndexingPerRepository indexingProxy = indexingInstances.get();
		ReposIndexingPerRepository indexing = ClientProxy.unwrap(indexingProxy);
		indexing.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		
		QueryResponse r1 = repositem.query(new SolrQuery("type:commit AND rev:1"));
		assertEquals(true, r1.getResults().get(0).getFieldValue("complete"));
		
		indexingInstances.destroy(indexingProxy);
		ReposIndexingPerRepository indexing2Proxy = indexingInstances.get();
		ReposIndexingPerRepository indexing2 = ClientProxy.unwrap(indexing2Proxy);
		assertTrue("This test is uninteresting if context has a singleton", indexing2 != indexing);
		indexing2.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		repositem.commit(); // to be sure that second sync doesn't do any solr operations
		
		QueryResponse r2 = repositem.query(new SolrQuery("type:commit AND rev:1"));
		assertEquals(true, r2.getResults().get(0).getFieldValue("complete"));
		indexingInstances.destroy(indexing2Proxy);
		} finally {
			schedule.stop();
		}
	}

	@Test
	@ActivateRequestContext
	public void testSyncHighLow() throws SolrServerException, IOException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r3.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		ReposIndexingPerRepository indexingProxy = indexingInstances.get();
		ReposIndexingPerRepository indexing = ClientProxy.unwrap(indexingProxy);
		indexing.sync(RepoRevision.parse("2/2013-03-21T19:16:28.271Z"));
		
		assertEquals(3, repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().size());
		
		indexingInstances.destroy(indexingProxy);
		ReposIndexingPerRepository indexing2Proxy = indexingInstances.get();
		ReposIndexingPerRepository indexing2 = ClientProxy.unwrap(indexing2Proxy);
		assertTrue("This test is uninteresting if context has a singleton", indexing2 != indexing);
		indexing2.sync(RepoRevision.parse("1/2012-09-27T12:05:34.040Z"));
		repositem.commit(); // to be sure that second sync doesn't do any solr operations
		
		assertEquals(3, repositem.query(new SolrQuery("type:commit AND complete:true")).getResults().size());
		indexingInstances.destroy(indexing2Proxy);
		} finally {
			schedule.stop();
		}
	}	
	
	@Test
	@ActivateRequestContext
	public void testIndexingModeNone() throws SolrServerException, IOException {
		QuarkusMock.installMockForType(new SvnDump("se/repos/indexing/testrepo1r3-indexing-mode-none.svndump"), SvnDump.class);
		repositem.deleteByQuery("*:*");
		repositem.commit();

		schedule.start();
		try {
		indexing.sync(RepoRevision.parse("3/2013-03-21T19:16:42.295Z"));
		assertEquals("should have indexed up to the given revision", 3, indexing.getRevision().getNumber());
		
		assertEquals("total head items is 3, but one is suppressed in r2", 3-1, repositem.query(new SolrQuery("head:true")).getResults().size());
		assertEquals("should have indexed rev 1 (without commit item)", 3, repositem.query(new SolrQuery("id:*@0000000001")).getResults().size());
		assertEquals("should have indexed rev 1, one item remains head but suppressed in r2", 1+3, repositem.query(new SolrQuery("rev:1")).getResults().size());
		assertEquals("should only index the commit for rev 2", 1, repositem.query(new SolrQuery("rev:2")).getResults().size());
		assertEquals("should have indexed rev 3 (commit item)", 1, repositem.query(new SolrQuery("id:*#0000000003")).getResults().size());
		assertEquals("should have indexed rev 3 (rev items)", 4, repositem.query(new SolrQuery("id:*@0000000003")).getResults().size());
		assertEquals("should have indexed rev 3 (head item)", 2, repositem.query(new SolrQuery("rev:3 AND head:true")).getResults().size());
		assertEquals("should have indexed rev 3", 1+4+2, repositem.query(new SolrQuery("rev:3")).getResults().size());
		} finally {
			schedule.stop();
		}
		
	}
	
	private void assertAllHeadFalse(SolrDocumentList docs) {
		for (int i = 0; i < docs.size(); i++) {
			assertEquals("at " + docs.get(i).get("path"), false, docs.get(i).get("head"));
		}
	}
	
}
