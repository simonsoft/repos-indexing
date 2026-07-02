/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;

public class IndexingProducer {

	@Produces
	@Singleton
	public IdStrategy createIdStrategy() {
		return new IdStrategyDefault();
	}
}
