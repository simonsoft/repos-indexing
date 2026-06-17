/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import se.repos.indexing.item.HandlerChecksum;
import se.repos.indexing.item.HandlerHeadClone;
import se.repos.indexing.item.HandlerIndexTime;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.HandlerProperties;
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

public abstract class IndexingHandlers {

	/**
	 * This is not implemented, but we do need an API for extending the default chain.
	 * 
	 * For custom ordering there's {@link Group} and {@link IndexingHandlers#STANDARD}, but those are difficult to use.
	 * 
	 * The actual need for extending is to insert handlers at various points,
	 * and possibly rearrange in cases like moving Background marker a few steps down.
	 */
	static interface HandlerChain {
		
	}	
	
	// with this we can change definition of the different handler groups and rename the steps
	public enum Group {
		Unblock,
		Structure,
		Fast,
		Nice,
		Content,
		Final
	}
	
	@SuppressWarnings("serial")
	public static final Map<Group, Iterable<Class<? extends IndexingItemHandler>>> STANDARD = Collections.unmodifiableMap(
		new HashMap<IndexingHandlers.Group, Iterable<Class<? extends IndexingItemHandler>>>() {{
			//put(Group.X, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
			//}}));
			put(Group.Unblock, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(ScheduleBackground.class);
			}}));
			put(Group.Structure, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerIndexTime.class);
				// No longer need to update head flag of previous revision.
				/*add(HandlerHeadinfo.class);*/
				add(HandlerPathinfo.class);
			}}));
			put(Group.Fast, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(IndexingItemHandlerPropertiesEnable.class);
				add(HandlerProperties.class);
				add(IndexingItemHandlerPropertiesDisable.class); // the others can read from indexing doc instead
			}}));
			put(Group.Nice, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerSendIncrementalSolrjRepositem.class);
				add(ScheduleAwaitNewer.class);
				add(HandlerContentEnable.class);
			}}));
			put(Group.Content, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerChecksum.class);
			}}));
			put(Group.Final, Collections.unmodifiableList(new LinkedList<Class<? extends IndexingItemHandler>>() {{
				add(HandlerContentDisable.class);
				add(HandlerHeadClone.class); // Send head=true item, subsequent handlers processes the head=false item.
				add(HandlerSendSolrjRepositem.class);
				add(MarkerRevisionComplete.class);
				add(MarkerCommitSolrjRepositem.class);
				// do we need to optimize? we never delete from this core, except at clean/resync //add(MarkerOptimizeSolrjRepositem.class);
			}}));
		}});

}
