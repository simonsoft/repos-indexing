/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import se.simonsoft.svn.runtime.SvnDumpConfig;

public class MockableSvnDumpProfile implements QuarkusTestProfile {

	public static final String DEFAULT_DUMP_PATH = "se/repos/indexing/testrepo1.svndump";

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of(
				SvnDumpConfig.DUMP_PATH, DEFAULT_DUMP_PATH,
				"quarkus.solr.devservices.core", "repositem");
	}
}
