/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import se.simonsoft.svn.runtime.SvnDumpProducer;

public class SvnRevisionEventIndexingProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of(
				SvnDumpProducer.DUMP_PATH, MockableSvnDumpProfile.DEFAULT_DUMP_PATH,
				"quarkus.solr.devservices.core", "repositem",
				SvnRevisionEventIndexingObserver.ENABLED, "true");
	}
}
