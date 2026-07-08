/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.twophases;

import jakarta.inject.Inject;

import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.properties.CmsItemProperties;

/**
 * Reads properties to memory immediately, assuming they are rather small and will always be needed.
 */
public class ItemPropertiesImmediate implements ItemPropertiesBufferStrategy {

	private CmsContentsReader reader;
	
	@Inject
	public ItemPropertiesImmediate setCmsContentsReader(CmsContentsReader reader) {
		this.reader = reader;
		return this;
	}

	@Override
	public CmsItemProperties getProperties(
			RepoRevision revision, CmsItemPath path) {
		
		return reader.getProperties(revision, path);
	}
	
}
