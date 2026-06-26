/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;

import se.repos.indexing.IndexingDoc;

public class IndexingDocIncrementalSolrjTest {

	@Test
	public void testAddField() {
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		doc.setField("id", "test@1");
		doc.setField("fb", false);
		doc.addField("f1", 1L);
		doc.addField("f1", 2L);
		assertEquals(false, doc.getFieldValue("fb"));
		Collection<Object> f1 = doc.getFieldValues("f1");
		assertEquals(2, f1.size(), "addField should append multi values");
		assertTrue(doc.containsKey("f1"));
		assertTrue(doc.deepCopy().containsKey("f1"), "clone should contain all fields");
		assertTrue(doc.deepCopy().containsKey("fb"), "clone should contain all fields");
		doc.setField("fb", true);
		assertEquals(true, doc.getFieldValue("fb"), "setField should overwrite");
		doc.setField("f1", 3L);
		assertEquals(3L, doc.getFieldValue("f1"), "setField should remove old multiValue values");
		doc.setUpdateMode(true);
		assertNotNull(doc.deepCopy(), "clone should be allowed after update mode is changed to true");
		doc.setField("fb", false);
		assertEquals(false, doc.getFieldValue("fb"),
				"field value should still be retrievable in normal form after update mode is switched on, for use from other indexers");
		Object fb = doc.getSolrDoc().getFieldValue("fb");
		assertTrue(fb instanceof Map, "In updated mode new fields should use 'set' syntax, got " + fb.getClass());
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, Object> fbset = (Map) fb;
		assertEquals(1, fbset.size(), "solrj partial update syntax");
		assertEquals("set", fbset.keySet().iterator().next(), "solrj partial update syntax");
		assertEquals(false, fbset.get("set"));
		SolrInputDocument updateDoc = doc.getSolrDoc();
		assertTrue(updateDoc.containsKey("fb"), "solr doc should contain updated fields");
		assertFalse(updateDoc.containsKey("f1"), "after update the solr doc should not contain unchanged values");
		assertEquals("test@1", updateDoc.getFieldValue("id"), "doc should always contain id");
		assertTrue(updateDoc.getFieldValue("fb") instanceof Map, "solr doc should have the partial update syntax");
		try {
			doc.deepCopy();
			fail("Expecting deepCopy to fail after field update until we have decided what to do with update fields");
		} catch (UnsupportedOperationException e) {
			// expected
		}
		doc.setField("fx", "new");
		assertTrue(doc.getSolrDoc().containsKey("fx"), "new fields since update mode true was set should go into next solr doc");
		// TODO assertEquals("fields that do not exist in solr, should they get the update syntax?", "new", doc.getFieldValue("fx"));		
		doc.addField("f1", 4L);
		// TODO Collection<Object> f1u = doc.getFieldValues("f1");
		//assertTrue("Should have the value from before update", f1u.contains(3L));
		//assertTrue("Should have the value from after update", f1u.contains(4L));
		assertTrue(doc.getSolrDoc().containsKey("id"), "Solr doc should keep the id field in update mode");
		Collection<Object> f1solr = doc.getSolrDoc().getFieldValues("f1");
		assertEquals(1, f1solr.size(), "Solr multi value field should only have the value from after update");
		assertTrue(f1solr.iterator().next() instanceof Map, "The value should be a partial update");
		@SuppressWarnings("rawtypes")
		Map f1update = (Map) f1solr.iterator().next();
		assertEquals(4L, f1update.values().iterator().next());
		assertEquals("add", f1update.keySet().iterator().next(), "Solr add syntax");
	}

	@Test
	public void testUpdateModeNoChanges() {
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		doc.setField("id", "test@2");
		doc.setField("x", "y");
		doc.setUpdateMode(true);
		SolrInputDocument nochanges = doc.getSolrDoc();
		assertEquals(0, nochanges.size(), "Can not have any fields if there are no updates because solr would overwrite the id");
		assertTrue(nochanges == IndexingDocIncrementalSolrj.UPDATE_MODE_NO_CHANGES, "Solr can't handle empty document so we should specifically identify this case");
	}
	
	@Test
	public void testContentSize() {
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		assertEquals(0, ((IndexingDoc) doc).getContentSize());
		doc.setField("string20", "1234567890\n123456789");
		doc.setField("number1", 10L);
		doc.setField("object1", new Object());
		doc.setField("date1", new Date());
		doc.setField("null1", null);
		doc.addField("string2", "1");
		doc.addField("string2", "12");
		assertEquals(23, doc.getContentSize(), "Shuld sum up length of strings as approximate size");
		
		IndexingDoc doc2 = doc.deepCopy();
		doc2.addField("string2", "123");
		assertEquals(26, doc2.getContentSize(), "size should follow clone");
		
		
	}
	
}
