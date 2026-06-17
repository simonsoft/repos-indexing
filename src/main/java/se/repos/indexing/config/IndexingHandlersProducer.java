/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;

import se.repos.indexing.IndexingHandlers;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.HandlerHeadClone;
import se.repos.indexing.item.HandlerIndexTime;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.repository.HandlerContentDisable;
import se.repos.indexing.repository.HandlerContentEnable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesDisable;
import se.repos.indexing.repository.IndexingItemHandlerPropertiesEnable;
import se.repos.indexing.repository.MarkerRevisionComplete;
import se.repos.indexing.scheduling.ScheduleAwaitNewer;
import se.repos.indexing.scheduling.ScheduleBackground;
import se.repos.indexing.solrj.HandlerSendIncrementalSolrjRepositem;
import se.repos.indexing.solrj.HandlerSendSolrjRepositem;
import se.repos.indexing.solrj.MarkerCommitSolrjRepositem;
import se.repos.indexing.twophases.ItemContentsMemory;
import se.repos.indexing.twophases.ItemPropertiesImmediate;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

public class IndexingHandlersProducer {

	private static final IndexingHandlers.Group[] STANDARD_GROUP_ORDER = {
			IndexingHandlers.Group.Unblock,
			IndexingHandlers.Group.Structure,
			IndexingHandlers.Group.Fast,
			IndexingHandlers.Group.Nice,
			IndexingHandlers.Group.Content,
			IndexingHandlers.Group.Final
	};

	@Produces
	@RequestScoped
	public ItemContentBufferStrategy createItemContentBufferStrategy(CmsContentsReader contentsReader) {
		return new ItemContentsMemory().setCmsContentsReader(contentsReader);
	}

	@Produces
	@RequestScoped
	public ItemPropertiesBufferStrategy createItemPropertiesBufferStrategy(CmsContentsReader contentsReader) {
		return new ItemPropertiesImmediate().setCmsContentsReader(contentsReader);
	}

	@Produces
	@RequestScoped
	public Set<IndexingItemHandler> createIndexingItemHandlers(
			IdStrategy idStrategy,
			@Named("repositem") SolrClient repositem,
			CmsRepository repository,
			ItemContentBufferStrategy contentBufferStrategy,
			ItemPropertiesBufferStrategy propertiesBufferStrategy) {

		Set<IndexingItemHandler> handlers = new LinkedHashSet<>();
		for (IndexingHandlers.Group group : STANDARD_GROUP_ORDER) {
			for (Class<? extends IndexingItemHandler> handlerType : IndexingHandlers.STANDARD.get(group)) {
				handlers.add(createHandler(handlerType, idStrategy, repositem, repository, contentBufferStrategy, propertiesBufferStrategy));
			}
		}
		return handlers;
	}

	private IndexingItemHandler createHandler(
			Class<? extends IndexingItemHandler> handlerType,
			IdStrategy idStrategy,
			SolrClient repositem,
			CmsRepository repository,
			ItemContentBufferStrategy contentBufferStrategy,
			ItemPropertiesBufferStrategy propertiesBufferStrategy) {

		if (handlerType.equals(ScheduleBackground.class)) {
			return new ScheduleBackground();
		}
		if (handlerType.equals(HandlerIndexTime.class)) {
			return new HandlerIndexTime();
		}
		if (handlerType.equals(HandlerPathinfo.class)) {
			HandlerPathinfo handlerPathinfo = new HandlerPathinfo();
			handlerPathinfo.setIdStrategy(idStrategy);
			return handlerPathinfo;
		}
		if (handlerType.equals(IndexingItemHandlerPropertiesEnable.class)) {
			return new IndexingItemHandlerPropertiesEnable(propertiesBufferStrategy);
		}
		if (handlerType.equals(HandlerProperties.class)) {
			return new HandlerProperties();
		}
		if (handlerType.equals(IndexingItemHandlerPropertiesDisable.class)) {
			return new IndexingItemHandlerPropertiesDisable();
		}
		if (handlerType.equals(HandlerSendIncrementalSolrjRepositem.class)) {
			return new HandlerSendIncrementalSolrjRepositem(repositem);
		}
		if (handlerType.equals(ScheduleAwaitNewer.class)) {
			return new ScheduleAwaitNewer();
		}
		if (handlerType.equals(HandlerContentEnable.class)) {
			return new HandlerContentEnable(contentBufferStrategy);
		}
		if (handlerType.equals(HandlerChecksum.class)) {
			return new HandlerChecksum();
		}
		if (handlerType.equals(HandlerContentDisable.class)) {
			return new HandlerContentDisable();
		}
		if (handlerType.equals(HandlerHeadClone.class)) {
			HandlerHeadClone handlerHeadClone = new HandlerHeadClone(repository);
			handlerHeadClone.setSolrClient(repositem);
			return handlerHeadClone;
		}
		if (handlerType.equals(HandlerSendSolrjRepositem.class)) {
			return new HandlerSendSolrjRepositem(repositem);
		}
		if (handlerType.equals(MarkerRevisionComplete.class)) {
			return new MarkerRevisionComplete(repositem);
		}
		if (handlerType.equals(MarkerCommitSolrjRepositem.class)) {
			return new MarkerCommitSolrjRepositem(repositem);
		}
		throw new IllegalArgumentException("Unsupported indexing handler type: " + handlerType.getName());
	}
}
