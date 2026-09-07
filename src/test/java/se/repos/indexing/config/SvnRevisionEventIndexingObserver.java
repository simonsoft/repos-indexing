/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@ApplicationScoped
public class SvnRevisionEventIndexingObserver {

	public static final String ENABLED = "repos.indexing.test.svn-revision-event-indexing.enabled";

	@Inject
	@ConfigProperty(name = ENABLED, defaultValue = "false")
	boolean enabled;

	@Inject
	ReposIndexing indexing;

	@Inject
	IndexingSchedule schedule;

	void onRevisionAvailable(@Observes SvnRevisionAvailableEvent event) {
		if (!enabled) {
			return;
		}
		schedule.start();
		try {
			indexing.sync(new RepoRevision(event.revision(), null));
		} finally {
			schedule.stop();
		}
	}
}
