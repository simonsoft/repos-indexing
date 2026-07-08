/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.scheduling;

import java.util.Collections;

import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.Marker;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.solrj.HandlerSendIncrementalSolrjRepositem;

/**
 * Ignores all markers and runs a standard indexing loop, each item in turn processed with all handlers.
 * Good for unit testing because no threads are needed but a standard handler chain can be used.
 */
@Singleton
public class IndexingScheduleBlockingOnly implements IndexingSchedule {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private boolean running = false;
	
	/**
	 * Blocking doesn't need a queue, but should support start and stop.
	 */
	@Override
	public IndexingSchedule start() throws IllegalStateException {
		if (running) {
			throw new IllegalStateException("Indexing already running");
		}
		running = true;
		logger.debug("Indexing is now running");
		return this;
	}

	@Override
	public void stop() throws IllegalStateException {
		if (!running) {
			throw new IllegalStateException("Indexing not running");
		}		
		running = false;
		logger.debug("Indexing is now stopped");
	}

	@Override
	public void add(IndexingUnit unit) {
		if (!running) {
			throw new IllegalStateException("Blocking schedule can only receive indexing units when started");
		}
		run(unit);
	}

	public void run(IndexingUnit unit) {
		HandlerIteration it = new HandlerIteration(new HandlerIteration.MarkerDecision() {
			
			@Override
			public boolean before(IndexingItemHandler handler, IndexingItemProgress item) {
				return 
						!(handler instanceof HandlerSendIncrementalSolrjRepositem); // skip because it reduces performance in blocking run
			}
			
			@Override
			public boolean before(Marker marker) {
				if (marker instanceof ScheduleBackground) return false;
				if (marker instanceof ScheduleAwaitNewer) return false;
				return true; // such as RevisionComplete
			}
			
			@Override
			public boolean after(Marker marker) {
				return true; // always continue though all items and handlers
			}
			
		});
		
		if (!it.proceed(unit)) {
			throw new AssertionError("Using blocking schedule so iteration should complete");
		}
	}
	
	@Override
	public Iterable<IndexingUnit> getQueue() {
		return Collections.emptyList();
	}

	@Override
	public int getQueueSize() {
		return 0;
	}

	@Override
	public boolean isComplete() {
		return getQueueSize() == 0;
	}
	
	
}
