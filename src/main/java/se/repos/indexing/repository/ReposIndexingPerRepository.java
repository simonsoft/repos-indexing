/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.repository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexAdmin;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.repos.indexing.scheduling.IndexingUnitRevision;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.repos.indexing.twophases.IndexingEventAware;
import se.repos.indexing.twophases.IndexingItemProgressPhases;
import se.repos.indexing.twophases.RepositoryIndexStatus;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.properties.CmsItemProperties;

@RequestScoped
public class ReposIndexingPerRepository implements ReposIndexing {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private CmsRepository repository;
	private IndexingSchedule schedule;
	private CmsChangesetReader changesetReader;
	private CmsContentsReader contentsReader;
	private CmsRepositoryLookup revisionLookup;
	private Set<IndexingItemHandler> handlers;

	private RepoRevision lock = null;

	private RepositoryIndexStatus repositoryStatus = null;
	
	private static final Semaphore statuscheck = new Semaphore(1);
	
	@Inject
	public ReposIndexingPerRepository(CmsRepository repository) {
		this.repository = repository;
	}
	
	@Inject
	public void setRepositoryStatus(RepositoryIndexStatus repositoryStatus) {
		this.repositoryStatus  = repositoryStatus;
	}
	
	@Inject
	public void setIndexingSchedule(IndexingSchedule schedule) {
		this.schedule = schedule;
	}
	
//	@Inject
//	public void setSolrRepositem(@Named("repositem") SolrClient repositem) {
//		this.repositem = repositem;
//	}
//	
	@Inject
	public void setCmsChangesetReader(CmsChangesetReader changesetReader) {
		this.changesetReader = changesetReader;
	}
	
	@Inject
	public void setCmsContentsReader(CmsContentsReader contentsReader) {
		this.contentsReader = contentsReader;
	}
	
	@Inject
	public void setHandlers(Set<IndexingItemHandler> handlers) {
		this.handlers = handlers;
		for (IndexingItemHandler h : handlers) {
			if (h instanceof IndexingEventAware) {
				throw new IllegalArgumentException("Handler " + h + " not accepted because this indexing impl does not support " + IndexingEventAware.class.getSimpleName());
			}
		}
		logger.debug("Handler chain for {} is: {}", repository, handlers);
	}

	@Inject
	public void setRevisionLookup(@Named("inspection") CmsRepositoryLookup lookup) {
		this.revisionLookup = lookup;
	}
	
	@Inject
	public void setIndexAdmin(IndexAdmin forAddingNotification) {
		forAddingNotification.addPostAction(new IndexAdminNotificationHandler());
	}
	
//
//	/**
//	 * Optional, adds event listeners that are not added through as other types of dependencies.
//	 * 
//	 * {@link IndexingEventAware} is detected from the following:
//	 * <ul>
//	 * <li>{@link #setItemBlocking(Set)}</li>
//	 * <li>{@link #setItemBackground(Set)}</li>
//	 * <li>{@link #setItemContentsBufferStrategy(ItemContentsBufferStrategy)}</li>
//	 * <li>{@link #setItemPropertiesBufferStrategy(ItemPropertiesBufferStrategy)}</li>
//	 */
//	@Inject
//	public void addEventListeners(Set<IndexingEventAware> other) {
//		if (other != null) {
//			this.eventHandlers.addAll(other);
//		}
//	}
//	
//	protected Collection<IndexingEventAware> getListerners() {
//		return eventHandlers;
//	}
//	

	@Override
	public void sync(CmsRepository repositoryParamDeprecated, RepoRevision revision) {
		throw new IllegalArgumentException("Repository parameter is deprecated");
	}
	
	@Override
	public void sync(RepoRevision revision) {
		long start;
		
		if (revision.getDate() == null) {
			logger.warn("Sync revision {} lacks timestamp. Retrieving from repsitory.");
			revision = new RepoRevision(revision.getNumber(), revisionLookup.getRevisionTimestamp(repository, revision.getNumber()));
		}
		
		statuscheck.acquireUninterruptibly();
		try { // to be sure we release the semaphore
		
		logger.info("Sync requested {} rev {}", repository, revision);
		if (revision.getDate() == null) {
			throw new IllegalArgumentException("Revision must be qualified with timestamp, got " + revision);
		}
		
		/*
		At large sync operations, do we run all blocking indexing first and then all background, or do we need more sophistication?
		Do we completely rule out other ongoing tasks than those executed by this instance?
		How do we handle indexing errors so we don't index that revision again and again?
		
		 */
		
		// #1358 Perform a SolR ping to ensure connectivity.
		// Uses SolrOp to ensure retry logic which slows things down when SolR is not ready or under heavy load.
		// Getting index status below now also has retry but will not be performed after startup.
		repositoryStatus.indexPing();
		
		if (lock == null) {
			logger.info("Unknown index completion status for repository {}. Polling.", repository);
			RepoRevision completedHighest = repositoryStatus.getIndexedRevisionHighestCompleted(repository);
			RepoRevision scheduledLowest = repositoryStatus.getIndexedRevisionLowestStarted(repository);
			RepoRevision scheduledHighest = repositoryStatus.getIndexedRevisionHighestStarted(repository);
			if (scheduledLowest == null) {
				if (scheduledHighest != null) {
					throw new IllegalStateException("Failed to query for index status. Inconsistent result " + scheduledHighest + ".");
				}
				if (completedHighest != null) {
					logger.info("Indexing has completed revision {}, no indexing in progress", completedHighest);
					lock = completedHighest;
				} else {
					lock = indexFirst();
				}
			} else {
				if (scheduledHighest == null) {
					throw new IllegalStateException("Failed to query for index status. Inconsistent result " + scheduledLowest + ".");
				}
				if (completedHighest != null && completedHighest.isNewerOrEqual(scheduledLowest)) {
					throw new IllegalStateException("Inconsistent index contents. Highest completed revision is "
							+ completedHighest + " but in progress at " + scheduledLowest + " to " + scheduledHighest);
				}
				if (schedule.isComplete()) {
					logger.warn("Index has incomplete revisions " + scheduledLowest + " to " + scheduledHighest + " but schedule is empty."
							+ " Assuming aborted indexing. Restarting " + (completedHighest == null ? "from scratch." : "after " + completedHighest + "."));
					if (completedHighest == null) {
						lock = indexFirst();
					} else {
						lock = completedHighest;
					}
				} else {
					logger.info("Indexing has completed revision {}, in progress from {} to {}", completedHighest, scheduledLowest, scheduledHighest);
					lock = scheduledHighest;
				}
			}
		}
		
		if (lock == null) {
			throw new AssertionError("Failed to calculate index status.");
		}
		
		if (lock.isNewerOrEqual(revision)) {
			logger.debug("Nothing to do at revision {}", revision);
			return;
		}
		
		// flag that this sync run is responsible for revisions up to the given one
		onSync(lock, revision);
		start = lock.getNumber() + 1;
		lock = revision;
		
		} finally { // everything since semaphore acquire
		statuscheck.release();
		}
		
		// Trusting onSync to take care of blocking we can run one revision at a time without storing traces of them first.
		// Scheduling will returng quickly to next revision if implemented with background worker. 
		for (long i = start; i <= lock.getNumber(); i++) {
			Date revt = revisionLookup.getRevisionTimestamp(repository, i);
			RepoRevision next = new RepoRevision(i, revt);
			logger.debug("Creating indexing unit {}", next);
			IndexingUnitRevision read = getIndexingUnit(next, lock);
			schedule.add(read);
		}

	}

	/**
	 * Start indexing of a repository
	 * @return the initial revision
	 */
	protected RepoRevision indexFirst() {
		logger.debug("No revision status in index. Starting from 0.");
		RepoRevision first = new RepoRevision(0, revisionLookup.getRevisionTimestamp(repository, 0L));
		CmsItemProperties revprops0 = null;
		repositoryStatus.indexRevEmpty(repository, first, revprops0, null);
		return first;
	}

	/**
	 * Ensure that
	 *  - No other sync operation is invoked on this repository for same or lower revision
	 *  - Sync operations on this repository to higher revision is allowed to schedule but runs after
	 * @param lock the revision we had covered before the sync call
	 * @param revision the revision we've been asked to run to
	 */
	protected void onSync(RepoRevision lock, RepoRevision revision) {
		// quick fix for subsequent sync, i.e. hook call if current indexing has not completed
		logger.info("Immediately marking {} as started, previous completed {}, current sync HEAD for {}", revision, lock, repository);
		// maybe index some kind of lock using IdStrategy#getIdEntry(CmsRepository, String) and https://wiki.apache.org/solr/RealTimeGet		
		repositoryStatus.indexRevStartAndCommit(repository, revision, null);
	}

	/**
	 * @param revision The revision to index
	 * @param referenceRevision Reference revision, for checking head status
	 * (whether item is overwritten before/at reference revision)
	 */
	protected IndexingUnitRevision getIndexingUnit(RepoRevision revision, RepoRevision referenceRevision) {
		
		CmsChangeset changeset = null;
		CmsItemProperties revprops = null;
		StringWriter err = new StringWriter();
		
		try {
			logger.debug("Reading revision properties for {}", revision);
			revprops = contentsReader.getRevisionProperties(revision);
			logger.debug("Read revision properties: {}", revprops.getKeySet());
		} catch (Exception e) {
			logger.error("Failed to read revision properties for revision: {}" , revision, e);
			e.printStackTrace(new PrintWriter(err));
		}
		
		if (revprops != null && revprops.getString("indexing:mode") != null && "none".equals(revprops.getString("indexing:mode").trim())) {
			logger.debug("Got indexing:mode = none for revision {}, marking as complete.", revision);
			repositoryStatus.indexRevEmpty(repository, revision, revprops, err.toString());
			return new IndexingUnitRevision(new LinkedList<IndexingItemProgress>(), handlers);
		}
		
		try {
			logger.info("Reading changeset {}{}", revision, referenceRevision == null ? "" : " with reference revision " + referenceRevision);
			if (revision.equals(referenceRevision)) {
				changeset = changesetReader.read(revision);
			} else {
				changeset = changesetReader.read(revision, referenceRevision);
			}
		} catch (Exception e) {
			logger.error("Failed to read changeset for revision: {}", revision, e);
			e.printStackTrace(new PrintWriter(err));
		}

		List<CmsChangesetItem> changesetItems = new LinkedList<CmsChangesetItem>();
		if (changeset != null) {
			changesetItems = changeset.getItems(); 
			logger.info("Read changeset {} - items: {}", revision, changesetItems.size());
			if (!changeset.isDeriveEnabled()) {
				logger.warn("Derived (copy/move) paths are not indexed.");
			}
		}
		
		String commitId;
		if (changesetItems.isEmpty()) {
			commitId = repositoryStatus.indexRevEmpty(repository, revision, revprops, err.toString());
		} else {
			commitId = repositoryStatus.indexRevStart(repository, revision, revprops, err.toString());
		}
		
		List<IndexingItemProgress> items = new LinkedList<IndexingItemProgress>();
		for (CmsChangesetItem item : changesetItems) {
			
			if (item.getPath() == null) {
				// TODO: Need to ensure that repository is indexed at r0 before allowing prop changes to index repository.
				// #896 Difficult to prevent indexing of repository root.
				logger.info("Repository root is likely not consistently indexed until property set on root: {} {}", repository, revision);
			}
			
			IndexingDocIncrementalSolrj doc = new IndexingDocIncrementalSolrj();
			doc.addField("revid", commitId);
			
			if (revprops != null && (item.isFile() || item.isFolder())) {
				// Add all revprops to proprev_* with transformation of ':'
				for (String key : revprops.getKeySet()) {
					doc.addField(getPropRevKey(key), revprops.getString(key));
				}
				
				doc.addField("revauthor", revprops.getString("svn:author"));
				doc.addField("revcomment", revprops.getString("svn:log"));
				if (item.isDerived()) {
					// TODO can we get commit revision without retrieving previous indexed entry?
				} else {
					doc.addField("revcauthor", revprops.getString("svn:author"));
					doc.addField("revccomment", revprops.getString("svn:log"));
				}
			}
			
			IndexingItemProgressPhases progress = new IndexingItemProgressPhases(repository, revision, item, doc);
			
			items.add(progress);
		}

		return new IndexingUnitRevision(items, handlers);	
	}

	@Override
	public RepoRevision getRevComplete(CmsRepository repository) {
		if (repository != null) {
			throw new IllegalArgumentException("Repository parameter is deprecated");
		}
		throw new UnsupportedOperationException("Scheduler status has superceded indexing status for completed revision");
	}

	@Override
	public RepoRevision getRevProgress(CmsRepository repository) {
		if (repository != null) {
			throw new IllegalArgumentException("Repository parameter is deprecated");
		}
		throw new UnsupportedOperationException("Use getRevision because this service can not see the difference between completed and in progress revisions");
	}
	
	
	protected String getPropRevKey(String key) {
		return "proprev_".concat(key.replace(':', '.'));
	}
	
	
	/**
	 * This is pretty internal.
	 * You need the schedule or the index to see the actual progress.
	 * Used for some unit testing, thus kept.
	 */
	@Override
	public RepoRevision getRevision() {
		if (lock == null) {
			throw new IllegalStateException("No indexing operation has been executed yet");
		}
		return lock;
	}
	
	private class IndexAdminNotificationHandler implements IndexAdmin {
		@Override
		public void clear() {
			logger.info("Got notification that index was cleared for repository {}, resetting internal state", repository);
			lock = null;
			if (schedule.isComplete()) {
				// probably we'll overwrite anything committed after clear, or should we throw exception and change IndexAdmin so that clear is not done in this case?
				logger.warn("Schedul is still running at index clear. May produce inconsistent index.");
			}
		}
		@Override
		public void addPostAction(IndexAdmin notificationReceiver) {
			throw new UnsupportedOperationException("Should not be called for receivers");
		}
	}
	
}
