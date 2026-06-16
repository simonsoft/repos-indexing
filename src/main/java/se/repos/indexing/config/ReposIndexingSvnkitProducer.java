/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Provider;

import org.tmatesoft.svn.core.io.SVNRepository;

import se.simonsoft.cms.backend.svnkit.info.change.CmsContentsReaderSvnkit;
import se.simonsoft.cms.item.inspection.CmsContentsReader;

public class ReposIndexingSvnkitProducer {

	@Produces
	@RequestScoped
	public CmsContentsReader createCmsContentsReader(Provider<SVNRepository> svnkitProvider) {
		CmsContentsReaderSvnkit contentsReader = new CmsContentsReaderSvnkit();
		contentsReader.setClientProvider(svnkitProvider);
		return contentsReader;
	}
}
