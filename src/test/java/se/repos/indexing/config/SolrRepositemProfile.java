/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SolrRepositemProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of(
				"quarkus.solr.devservices.core", "repositem",
				// This profile is Solr-only. SVN values are placeholders for the current SVN extension config mapping.
				"quarkus.svn.enabled", "false",
				"quarkus.svn.devservices.enabled", "false",
				"quarkus.svn.hostname", "localhost",
				"quarkus.svn.port", "80",
				"quarkus.svn.username", "unused",
				"quarkus.svn.password", "unused");
	}
}
