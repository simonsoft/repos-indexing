package se.repos.indexing.config;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SolrRepositemProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of(
				"quarkus.solr.devservices.core", "repositem",
				"quarkus.svn.enabled", "false",
				"quarkus.svn.devservices.enabled", "false",
				"quarkus.svn.hostname", "localhost",
				"quarkus.svn.port", "80",
				"quarkus.svn.username", "unused",
				"quarkus.svn.password", "unused");
	}
}
