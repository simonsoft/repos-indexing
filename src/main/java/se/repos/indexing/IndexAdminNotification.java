/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration of index admins is a bit bulky so it will help future refactorings if notification receivers extends this class.
 */
@Singleton
public abstract class IndexAdminNotification implements IndexAdmin {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Inject
	public final void setIndexAdminCentral(IndexAdmin cetnralIndexAdmin) {
		cetnralIndexAdmin.addPostAction(this);
		logger.info("Activated index admin {}", this);		
	}
	
	@Override
	public void addPostAction(IndexAdmin notificationReceiver) {
		throw new UnsupportedOperationException("Not supported for notification receivers");
	}

}