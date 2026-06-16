/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;
import se.simonsoft.svn.runtime.RepoId;
import se.simonsoft.svn.runtime.SvnConnectionConfig;

public class IndexingProducer {

	@Produces
	@RequestScoped
	public CmsRepository createCmsRepository(SvnConnectionConfig config, @RepoId String repoId) {
		return new CmsRepository("http://%s:%d%s/%s".formatted(
				config.hostname(), config.port(), config.repoparent(), repoId));
	}

	@Produces
	public IdStrategy createIdStrategy() {
		return new IdStrategyDefault();
	}
}
