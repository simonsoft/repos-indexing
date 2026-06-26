/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Date;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import se.repos.indexing.config.SolrRepositemProfile;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

@QuarkusTest
@TestProfile(SolrRepositemProfile.class)
public class SchemaRepositemTest {

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@AfterEach
	public void clearIndex() throws SolrServerException, IOException {
		repositem.deleteByQuery("*:*");
		repositem.commit();
	}
	
	@SuppressWarnings("unused")
	private void printHits(SolrQuery q) throws SolrServerException, IOException {
		System.out.println("--- solr contents " + q.toString() + " ---");
		SolrDocumentList results = getSolr().query(q).getResults();
		if (results.size() == 0) {
			System.out.println("empty");
		}
		for (SolrDocument d : results) {
			for (String f : d.getFieldNames()) {
				String v = "" + d.get(f);
				System.out.print(", " + f + ": " + v);
			}
			System.out.println("");
		}
	}

	/**
	 * @return instance for integration testing our logic with Solr.
	 */
	public SolrClient getSolr() {
		return repositem;
	}
	
	@Test
	public void testFilenameNumberedDelimiterNone() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "MAP12345678");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "TOP12345678");
		solr.add(doc2);
		solr.commit();
		
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:MAP12345678")).getResults().getNumFound());
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:TOP12345678")).getResults().getNumFound());
		
		assertEquals("lowercase match", 1, solr.query(new SolrQuery("name:map12345678")).getResults().getNumFound());
		assertEquals("lowercase match", 1, solr.query(new SolrQuery("name:top12345678")).getResults().getNumFound());
		
		// no split on number - debatable but splitting will generate very spurious hits when searching to product names etc
		assertEquals("no split on number", 0, solr.query(new SolrQuery("name:TOP")).getResults().getNumFound());
		assertEquals("no split on number", 0, solr.query(new SolrQuery("name:12345678")).getResults().getNumFound());
	}
	
	@Test
	public void testFilenameNumberedDelimiterSpace() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "MAP 12345678");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "TOP 12345678");
		solr.add(doc2);
		solr.commit();
		
		// exact match requires quotes in SolR 8
		assertEquals("no longer exact match", 2, solr.query(new SolrQuery("name:MAP\\ 12345678")).getResults().getNumFound());
		assertEquals("no longer exact match", 2, solr.query(new SolrQuery("name:TOP\\ 12345678")).getResults().getNumFound());
		
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"MAP 12345678\"")).getResults().getNumFound());
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"TOP 12345678\"")).getResults().getNumFound());
		
		// tokenized match (use edismax in order to use space with tokenized)
		// Not working: should provide more hits if actually tokenized.
		// Working in SolR 8
		assertEquals("different delimiter", 2, solr.query(new SolrQuery("name:MAP-12345678")).getResults().getNumFound());
		assertEquals("different delimiter", 2, solr.query(new SolrQuery("name:TOP-12345678")).getResults().getNumFound());

		assertEquals("split on space", 1, solr.query(new SolrQuery("name:TOP")).getResults().getNumFound());
		assertEquals("split on space", 1, solr.query(new SolrQuery("name:top")).getResults().getNumFound());
		assertEquals("split on space", 2, solr.query(new SolrQuery("name:12345678")).getResults().getNumFound());
	}
	
	@Test
	public void testFilenameNumberedDelimiterUnderscore() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "MAP_12345678");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "TOP_12345678");
		solr.add(doc2);
		solr.commit();
		
		// exact match requires quotes in SolR 8
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"MAP_12345678\"")).getResults().getNumFound());
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"TOP_12345678\"")).getResults().getNumFound());
		
		assertEquals("split on underscore", 1, solr.query(new SolrQuery("name:TOP")).getResults().getNumFound());
		assertEquals("split on underscore", 2, solr.query(new SolrQuery("name:12345678")).getResults().getNumFound());
		
		// no split on underscore - debatable but good when using numbering (have the option to use space if splitting is desired)
		// Now splitting on underscore (changed with SolR 8?)
		assertEquals("no split on underscore", 1, solr.query(new SolrQuery("name:TOP")).getResults().getNumFound());
		assertEquals("no split on underscore", 2, solr.query(new SolrQuery("name:12345678")).getResults().getNumFound());
	}
	
	@Test
	public void testFilenameNumberedDelimiterDash() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "MAP-12345678");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "TOP-12345678");
		solr.add(doc2);
		solr.commit();
		
		// exact match requires quotes in SolR 8
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"MAP-12345678\"")).getResults().getNumFound());
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"TOP-12345678\"")).getResults().getNumFound());
		// split on dash
		assertEquals("split on dash", 1, solr.query(new SolrQuery("name:TOP")).getResults().getNumFound());
		assertEquals("split on dash", 2, solr.query(new SolrQuery("name:12345678")).getResults().getNumFound());
		assertEquals("split on dash, even hit with quote query", 1, solr.query(new SolrQuery("name:\"TOP 12345678\"")).getResults().getNumFound());
		assertEquals("split on dash", 2, solr.query(new SolrQuery("name:TOP OR name:12345678")).getResults().getNumFound());
	}
	
	@Test
	public void testFilenameDescriptive() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "Large Machine");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "Small machine");
		solr.add(doc2);
		/* Used when experimenting
		SolrInputDocument doc3 = new SolrInputDocument();
		doc3.addField("id", "3");
		doc3.addField("pathnamebase", "large tractor");
		solr.add(doc3);
		*/
		solr.commit();
		
		// SolR 6: sow=true (exact match works with both quotes and escaping space)
		// SolR 7: sow=false ("enabling proper function of analysis filters")
		assertEquals("no longer exact match", 2, solr.query(new SolrQuery("name:Large\\ Machine")).getResults().getNumFound());
		assertEquals("no longer exact match", 2, solr.query(new SolrQuery("name:Small\\ machine")).getResults().getNumFound());

		// exact match requires quotes in SolR 8
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"Large Machine\"")).getResults().getNumFound());
		assertEquals("exact match", 1, solr.query(new SolrQuery("name:\"Small machine\"")).getResults().getNumFound());
		
		// tokenized match (analysis not working in SolR 6 and before)
		// analysis working in SolR 8
		assertEquals("different delimiter", 2, solr.query(new SolrQuery("name:Large-Machine")).getResults().getNumFound());
		assertEquals("different delimiter", 2, solr.query(new SolrQuery("name:Small_machine")).getResults().getNumFound());
		
		assertEquals("lowercase, exact match", 1, solr.query(new SolrQuery("name:\"large machine\"")).getResults().getNumFound());
		
		assertEquals("one term, first", 1, solr.query(new SolrQuery("name:large")).getResults().getNumFound());
		assertEquals("one term, not first", 2, solr.query(new SolrQuery("name:machine")).getResults().getNumFound());
		
		// This can't work since removing defaultSearchField = id.
		// It never worked as expected, only the first token was searched in 'name', following tokens searched in 'id'.
		/*
		assertEquals("", 1, solr.query(new SolrQuery("name:large huge machine")).getResults().getNumFound());
		*/
		// Can be achieved with OR
		assertEquals("need OR notation since removing defaultSearchField", 2, solr.query(new SolrQuery("name:large OR name:huge OR name:machine")).getResults().getNumFound());
		// Can be achieved with edismax
		assertEquals("need edismax since removing defaultSearchField", 2, solr.query(new SolrQuery("large huge machine").add("defType", "edismax").add("qf", "name")).getResults().getNumFound());
		assertEquals("need edismax since removing defaultSearchField", 2, solr.query(new SolrQuery("huge large odd machine").add("defType", "edismax").add("qf", "name")).getResults().getNumFound());
		
		assertEquals("one term", 2, solr.query(new SolrQuery("name:machine")).getResults().getNumFound());
		//assertEquals("term order", 1, solr.query(new SolrQuery("name:machine large")).getResults().getNumFound());

		// Makes no sense...
		// Resolved: made no sense because of defaultSearchField = id.
		/*
		assertEquals("all terms must match?", 0, solr.query(new SolrQuery("name:huge machine")).getResults().getNumFound());
		assertEquals("all terms must match - should be 0 hits??", 2, solr.query(new SolrQuery("name:machine huge")).getResults().getNumFound());
		*/
	}
	
	@Test
	public void testFilenameDescriptiveReverse() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "Machine Large");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "machine Small");
		solr.add(doc2);
		solr.commit();
		
		assertEquals("one term", 2, solr.query(new SolrQuery("name:machine")).getResults().getNumFound());
		assertEquals("one term", 1, solr.query(new SolrQuery("name:large")).getResults().getNumFound());
		assertEquals("one term", 1, solr.query(new SolrQuery("name:small")).getResults().getNumFound());

		assertEquals("one term, uppercase", 2, solr.query(new SolrQuery("name:MACHINE")).getResults().getNumFound());
		
		// exact match requires quotes in SolR 8
		assertEquals("no exact match reverse", 0, solr.query(new SolrQuery("name:\"Large Machine\"")).getResults().getNumFound());
		assertEquals("no exact match reverse", 0, solr.query(new SolrQuery("name:\"Small machine\"")).getResults().getNumFound());

		// Different in 8.8.0
		assertEquals("no exact match reverse", 2, solr.query(new SolrQuery("name:Large\\ Machine")).getResults().getNumFound());
		assertEquals("no exact match reverse", 2, solr.query(new SolrQuery("name:Small\\ machine")).getResults().getNumFound());
		
		assertEquals("no longer exact match", 2, solr.query(new SolrQuery("name:Machine\\ Large")).getResults().getNumFound());
		assertEquals("no longer exact match", 2, solr.query(new SolrQuery("name:machine\\ Small")).getResults().getNumFound());
		
		// tokenized match (analysis not working in SolR 6 and before)
		// analysis working in SolR 8
		assertEquals("tokenized match, different delimiter", 2, solr.query(new SolrQuery("name:Large-Machine")).getResults().getNumFound());
		assertEquals("tokenized match, different delimiter", 2, solr.query(new SolrQuery("name:Small_machine")).getResults().getNumFound());
	}
	
	@Test
	public void testFileextCase() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id", "1");
		doc1.addField("pathnamebase", "Machine Large");
		doc1.addField("pathext", "PNG");
		solr.add(doc1);
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id", "2");
		doc2.addField("pathnamebase", "machine Small");
		doc2.addField("pathext", "png");
		solr.add(doc2);
		solr.commit();
		
		assertEquals("one term", 2, solr.query(new SolrQuery("name:machine")).getResults().getNumFound());
		assertEquals("one term", 1, solr.query(new SolrQuery("name:large")).getResults().getNumFound());
		assertEquals("one term", 1, solr.query(new SolrQuery("name:small")).getResults().getNumFound());
	
		assertEquals("one term", 2, solr.query(new SolrQuery("pathext:png")).getResults().getNumFound());
		assertEquals("one term", 2, solr.query(new SolrQuery("pathext:PNG")).getResults().getNumFound());
		
		SolrDocumentList r = solr.query(new SolrQuery("pathext:png").setSort("pathnamebase", ORDER.asc)).getResults();
		SolrDocument r1 = r.get(0);
		SolrDocument r2 = r.get(1);
		assertEquals("case preserving but insensitive", "PNG", r1.getFieldValue("pathext"));
		assertEquals("Machine Large", r1.getFieldValue("pathnamebase"));
		assertEquals("png", r2.getFieldValue("pathext"));
	}
	
	
	@Test
	public void testPropertySearch() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("prop_svn.ignore", "ignore1\nignore2");
		doc.addField("prop_svn.externals", "ext1 ^/some/folder\next2\t^/some/other/folder");
		doc.addField("prop_custom.lang", "se-SE | de-SE | en-US");
		doc.addField("prop_custom.values", "whatever, Value,Wanted");
		doc.addField("prop_custom.values2", "Semi;colon ;Separated");
		doc.addField("prop_custom.tags", "testing validation JUnit");
		doc.addField("prop_custom.json", "[{\"jsonkey\":\"jsonval\"}, \"justval\"]");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should tokenize on newline", 1, solr.query(new SolrQuery("prop_svn.ignore:ignore2")).getResults().getNumFound());
		assertEquals("Should still match the full value", 1, solr.query(new SolrQuery("prop_svn.ignore:ignore1\\\nignore2")).getResults().getNumFound());
		//assertEquals("What id the full value has one token that doesn't match?", 0, solr.query(new SolrQuery("prop_svn.ignore:ignore1\nignore0")).getResults().getNumFound());
		assertEquals("Should tokenize on whitespace", 1, solr.query(new SolrQuery("prop_svn.externals:\"^/some/folder\"")).getResults().getNumFound());
		assertEquals("Should tokenize on tab", 1, solr.query(new SolrQuery("prop_svn.externals:\"^/some/other/folder\"")).getResults().getNumFound());
		assertEquals("Should match on line", 1, solr.query(new SolrQuery("prop_svn.externals:\"ext1 ^/some/folder\"")).getResults().getNumFound());
		assertEquals("What if a line has a mismatching token", 0, solr.query(new SolrQuery("prop_svn.externals:\"ext0 ^/some/folder\"")).getResults().getNumFound());
		//assertEquals("What if a line has a mismatching token that exists somewhere else", 0, solr.query(new SolrQuery("prop_svn.externals:\"ext2 ^/some/folder\"")).getResults().getNumFound());
		assertEquals("Could match on line regarless of whitespace", 1, solr.query(new SolrQuery("prop_svn.externals:\"ext2 ^/some/other/folder\"")).getResults().getNumFound());
		assertEquals("Should separate on comma", 1, solr.query(new SolrQuery("prop_custom.values:Value")).getResults().getNumFound());
		assertEquals("Should separate on semicolon", 1, solr.query(new SolrQuery("prop_custom.values2:Separated")).getResults().getNumFound());
		//how?//assertEquals("Should separate on pipe", 1, solr.query(new SolrQuery("prop_custom.lang:de-DE")).getResults().getNumFound());
		assertEquals("Should separate on whitespace", 1, solr.query(new SolrQuery("prop_custom.tags:JUnit")).getResults().getNumFound());
		
		/*
		assertEquals("Making property search case insensitive wouldn't be good when props contain URLs etc",
				0, solr.query(new SolrQuery("prop_custom.tags:junit")).getResults().getNumFound());
		*/
		assertEquals("Expecting property search to be case insensitive.",
				1, solr.query(new SolrQuery("prop_custom.tags:junit")).getResults().getNumFound());
	}
	
	@Test
	public void testFulltextSearchCamelCase() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word JavaClassName getMethodName getMethod2Name The ProductNAME followed by text");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match words in sequence", 1, solr.query(new SolrQuery("text:followed\\ by\\ text")).getResults().getNumFound());
		assertEquals("Should match quoted words in sequence", 1, solr.query(new SolrQuery("text:\"followed by text\"")).getResults().getNumFound());
		assertEquals("Should not match quoted words out of sequence", 0, solr.query(new SolrQuery("text:\"followed text\"")).getResults().getNumFound());

		
		assertEquals("Should match Java Class Name camelcase", 1, solr.query(new SolrQuery("text:JavaClassName")).getResults().getNumFound());
		assertEquals("Should match Java Class Name lowercase", 1, solr.query(new SolrQuery("text:javaclassname")).getResults().getNumFound());
		assertEquals("Should match Java Method Name camelcase", 1, solr.query(new SolrQuery("text:getMethodName")).getResults().getNumFound());
		assertEquals("Should match Java Method Name lowercase", 1, solr.query(new SolrQuery("text:getmethodname")).getResults().getNumFound());
		assertEquals("Should match Java Method Name wildcard", 1, solr.query(new SolrQuery("text:getmethod*")).getResults().getNumFound());

		assertEquals("Should match Java Method 2 Name camelcase", 1, solr.query(new SolrQuery("text:getMethod2Name")).getResults().getNumFound());
		assertEquals("Should match Java Method 2 Name lowercase", 1, solr.query(new SolrQuery("text:getmethod2name")).getResults().getNumFound());
		assertEquals("Should match Java Method 2 Name wildcard", 1, solr.query(new SolrQuery("text:getmethod2*")).getResults().getNumFound());

		assertEquals("Should match Product Name case-switch", 1, solr.query(new SolrQuery("text:ProductNAME")).getResults().getNumFound());
		assertEquals("Should match Product Name lowercase", 1, solr.query(new SolrQuery("text:productname")).getResults().getNumFound());
		assertEquals("Should match Product Name leading capital", 1, solr.query(new SolrQuery("text:Productname")).getResults().getNumFound());
		assertEquals("Should match Product Name in context (actually matching 'The')", 1, solr.query(new SolrQuery("text:The\\ ProductNAME\\ followed\\ by\\ text")).getResults().getNumFound());
		// Will fail if using preserveOriginal="1".
		assertEquals("Should match Product Name in context - Quoted", 1, solr.query(new SolrQuery("text:\"The ProductNAME followed by text\"")).getResults().getNumFound());
		assertEquals("Should match Product Name lowercase in context - Quoted", 1, solr.query(new SolrQuery("text:\"The Productname followed by text\"")).getResults().getNumFound());

		// Difficult to combine individual components with quoted search.
		/*
		assertEquals("Could match Product Name individual components", 1, solr.query(new SolrQuery("text:product")).getResults().getNumFound());
		assertEquals("Could match Product Name individual components", 1, solr.query(new SolrQuery("text:name")).getResults().getNumFound());
		assertEquals("Could match Product Name separated components", 1, solr.query(new SolrQuery("text:product name")).getResults().getNumFound());
		*/
	}

	@Test
	public void testFulltextSearchDelimiters() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word top-level");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match exact", 1, solr.query(new SolrQuery("text:top-level")).getResults().getNumFound());
		assertEquals("Should match exact - Quoted", 1, solr.query(new SolrQuery("text:\"top-level\"")).getResults().getNumFound());
		assertEquals("Could match exact - Quoted space instead of dash", 1, solr.query(new SolrQuery("text:\"top level\"")).getResults().getNumFound());
		assertEquals("Should match part 1", 1, solr.query(new SolrQuery("text:top")).getResults().getNumFound());
		assertEquals("Should match part 2", 1, solr.query(new SolrQuery("text:level")).getResults().getNumFound());
		
		// Below asserts just documents current behavior, would be fine if they also hit.
		assertEquals("Unlikely to match catenated", 0, solr.query(new SolrQuery("text:toplevel")).getResults().getNumFound());
	}
	
	
	@Test
	@Disabled("Stem Possessive is a feature of the WDF.")
	public void testFulltextSearchEnglishPossessive() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word Staffan's & Thomas' code");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match name 1", 1, solr.query(new SolrQuery("text:staffan")).getResults().getNumFound());
		assertEquals("Should match name 2", 1, solr.query(new SolrQuery("text:thomas")).getResults().getNumFound());
		
		// Likely only possible with WDF in both pipelines or without WDF.
		/*
		assertEquals("Should match possessive name 1", 1, solr.query(new SolrQuery("text:Staffan's")).getResults().getNumFound());
		assertEquals("Should match possessive name 2", 1, solr.query(new SolrQuery("text:Thomas'")).getResults().getNumFound());
		*/
		
		// Works when WDF is only in index pipeline.
		assertEquals("Could match quoted no possessive", 1, solr.query(new SolrQuery("text:\"Staffan & Thomas code\"")).getResults().getNumFound());
		
		// Likely only possible with WDF in query pipeline or without WDF.
		/*
		assertEquals("Could match quoted exact", 1, solr.query(new SolrQuery("text:\"Staffan's & Thomas' code\"")).getResults().getNumFound());
		*/
	}
	
	@Test
	public void testFulltextSearchNumbers() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word The SD500 product");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match exact product", 1, solr.query(new SolrQuery("text:SD500")).getResults().getNumFound());
		assertEquals("Should match exact product lowercase", 1, solr.query(new SolrQuery("text:sd500")).getResults().getNumFound());
		assertEquals("Should match exact product quoted", 1, solr.query(new SolrQuery("text:\"SD500\"")).getResults().getNumFound());
		assertEquals("Should match exact product wildcard", 1, solr.query(new SolrQuery("text:SD5*")).getResults().getNumFound());
		
		// WDF needs splitOnNumerics for these, which requires catenate or preserve for above asserts.
		/*
		assertEquals("Could match part 1", 1, solr.query(new SolrQuery("text:SD")).getResults().getNumFound());
		assertEquals("Could match part 2", 1, solr.query(new SolrQuery("text:500")).getResults().getNumFound());
		*/
		
		// These asserts document the desire to have less spurious hits
		assertEquals("Avoid matching other product SD200", 0, solr.query(new SolrQuery("text:SD200")).getResults().getNumFound());
		assertEquals("Avoid matching other product XX500", 0, solr.query(new SolrQuery("text:XX500")).getResults().getNumFound());

		
		assertEquals("Should match quoted context", 1, solr.query(new SolrQuery("text:\"The SD500 product\"")).getResults().getNumFound());
	}
	
	@Test
	public void testFulltextSearchNumbersHyphen() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "word The SD-500 product");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:word")).getResults().getNumFound());
		assertEquals("Should match exact product", 1, solr.query(new SolrQuery("text:SD-500")).getResults().getNumFound());
		assertEquals("Should match exact product lowercase", 1, solr.query(new SolrQuery("text:sd-500")).getResults().getNumFound());
		
		// With hyphen also the StandardTokenizer will split.
		assertEquals("Could match part 1", 1, solr.query(new SolrQuery("text:SD")).getResults().getNumFound());
		assertEquals("Could match part 2", 1, solr.query(new SolrQuery("text:500")).getResults().getNumFound());
		
		assertEquals("Could match quoted context", 1, solr.query(new SolrQuery("text:\"The SD-500 product\"")).getResults().getNumFound());
	}
	
	@Test
	public void testFulltextSearchEmail() throws Exception {
		SolrClient solr = getSolr();
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1");
		doc.addField("text", "contact support@example.com");
		solr.add(doc);
		solr.commit();
		
		assertEquals("Should match simple word", 1, solr.query(new SolrQuery("text:contact")).getResults().getNumFound());
		// Likely NOT possible with WDF in index pipeline.
		assertEquals("Should match exact email", 1, solr.query(new SolrQuery("text:support@example.com")).getResults().getNumFound());
		assertEquals("Should match exact email - Quoted", 1, solr.query(new SolrQuery("text:\"support@example.com\"")).getResults().getNumFound());
		
		assertEquals("Could match part 1", 1, solr.query(new SolrQuery("text:support")).getResults().getNumFound());
		assertEquals("Could match part 2", 1, solr.query(new SolrQuery("text:example.com")).getResults().getNumFound());
	}
	
	
	// Documents the effect of the caveat in http://wiki.apache.org/solr/Atomic_Updates
	@Test
	public void testHeadFlagUpdateEffect() throws Exception {
		SolrClient solr = getSolr();
		
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		doc.addField("id", "f#01");
		doc.addField("head", true);
		doc.addField("pathstat", "A");
		doc.addField("path", "dir/file.txt");
		doc.addField("pathext", "txt");
		doc.addField("text", "quite secret content, though searchable");
		solr.add(doc.getSolrDoc());
		solr.commit();
		assertEquals("Should be searchable on path", 1, solr.query(new SolrQuery("path:dir*")).getResults().getNumFound());
		assertEquals("Should be searchable on pathext", 1, solr.query(new SolrQuery("pathext:txt")).getResults().getNumFound());
		assertEquals("Should be searchable on text", 1, solr.query(new SolrQuery("text:secret")).getResults().getNumFound());
		
		IndexingDocIncrementalSolrj docd = new IndexingDocIncrementalSolrj();
		docd.addField("id", "f#02");
		docd.addField("head", true);
		docd.addField("pathstat", "D");
		docd.addField("path", "dir/file.txt");
		docd.addField("pathext", "txt");		
		doc.setUpdateMode(true);
		doc.setField("head", false);
		solr.add(docd.getSolrDoc());
		solr.add(doc.getSolrDoc());
		solr.commit();
		assertEquals("Both head and historical should be searchable on path",
				2, solr.query(new SolrQuery("path:dir*")).getResults().getNumFound());
		assertEquals("Both head and historical should be searchable on pathext",
				2, solr.query(new SolrQuery("pathext:txt")).getResults().getNumFound());
		assertEquals("Text search for historical has been scoped out, if made stored it might affect access control requirements",
				0, solr.query(new SolrQuery("text:secret")).getResults().getNumFound());
	}
	
	@Test
	public void testHandleRepeatedPathSegments() throws SolrServerException, IOException {
		SolrClient solr = getSolr();		
		// Not really unit testing the schema here because the path logic in the handler is too relevant - could be switched to 
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsRepository repo = new CmsRepository("http://ex.ampl:444/s/rep1");
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(new RepoRevision(35L, new Date()));
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath()).thenReturn(new CmsItemPath("/xml/something/xml/doc.xml"));
		when(item.getRevisionChanged()).thenReturn(new RepoRevision(35L, new Date()));
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getItem()).thenReturn(item);
		HandlerPathinfo handlerPathinfo = new HandlerPathinfo();
		handlerPathinfo.setIdStrategy(new IdStrategyDefault());
		handlerPathinfo.handle(p);
		solr.add(doc.getSolrDoc());
	}
	

	@Test
	@Disabled("Very incomplete.")
	public void testPathAnalysis() throws SolrServerException, IOException {
		SolrClient solr = getSolr();		
		// Not really unit testing the schema here because the path logic in the handler is too relevant - could be switched to 
		IndexingItemProgress p = mock(IndexingItemProgress.class);
		CmsRepository repo = new CmsRepository("http://ex.ampl:444/s/rep1");
		when(p.getRepository()).thenReturn(repo);
		when(p.getRevision()).thenReturn(new RepoRevision(35L, new Date()));
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		when(item.getPath())
			.thenReturn(new CmsItemPath("/dir/doc main.xml"))
			.thenReturn(new CmsItemPath("/dir sect/doc appendix.xml"));
			;
		IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
		when(p.getFields()).thenReturn(doc);
		when(p.getItem()).thenReturn(item);
		HandlerPathinfo handlerPathinfo = new HandlerPathinfo();
		handlerPathinfo.handle(p);
		solr.add(doc.getSolrDoc());
		
		fail("need to test effects of analyzed path* fields and confirm the need for name and extension");
	}
}
