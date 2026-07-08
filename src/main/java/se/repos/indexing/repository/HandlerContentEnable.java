/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import jakarta.inject.Inject;

import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.ItemContentBuffer;
import se.repos.indexing.item.ItemContentBufferDeleted;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemContentBufferFolder;
import se.repos.indexing.twophases.IndexingItemProgressPhases;

/**
 * Inserted into handler chain to enable contents access for later handlers.
 */
public class HandlerContentEnable extends IndexingItemHandlerInternal<IndexingItemProgressPhases> {

	private static final ItemContentBuffer ITEM_IS_FOLDER = new ItemContentBufferFolder();
	
	private static final ItemContentBuffer ITEM_IS_DELETED = new ItemContentBufferDeleted();
	
	private ItemContentBufferStrategy strategy;

	@Inject
	public HandlerContentEnable(ItemContentBufferStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public void handleInternal(IndexingItemProgressPhases progress) {
		progress.setContents(getBuffer(progress));
	}
	
	protected ItemContentBuffer getBuffer(IndexingItemProgress progress) {
		if (progress.getItem().isDelete()) {
			return ITEM_IS_DELETED;
		}
		if (progress.getItem().isFolder()) {
			return ITEM_IS_FOLDER;
		}
		return strategy.getBuffer(progress.getRevision(), progress.getItem().getPath(), progress.getFields());
	}

}
