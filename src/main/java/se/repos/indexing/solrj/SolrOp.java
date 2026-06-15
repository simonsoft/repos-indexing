/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.solrj;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared error handling for solr operations.
 */
public abstract class SolrOp<T> {

	protected SolrClient core;
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	public final Long retryPause = 10000L;

	public SolrOp(SolrClient core) {
		this.core = core;
	}

	protected void doLogSlowQuery(SolrClient core, String opName, String query, SolrResponseBase response) {
		long eTime = response.getElapsedTime();
		if (eTime > 1000) {
			String coreName = core.toString();
			if (core instanceof HttpSolrServerNamed) {
				coreName = ((HttpSolrServerNamed) core).getName();
			}
			logger.warn("Slow SolR operation {} [{}] (eTime: {} qTime: {}): {}", opName, coreName, eTime, response.getQTime(), query);
		}
	}
	
	public T run() {
		try {
			return runOp();
		} catch (RemoteSolrException e) {
			// #1358 Thrown when response != 200, unexpected content type, invalid response body.
			// #1358 This extends RuntimeException (via SolrException) so it was never handled here before.
			logger.warn("Solr first attempt failed with RemoteSolrException HTTP {}, retry in {} ms: {}", e.code(), retryPause, e.getMessage());
			logger.debug("Solr first attempt failed with RemoteSolrException HTTP {}: ", e.code(), e);
			return retry(e);
		} catch (SolrServerException e) {
			// Retry here as well, seems like IOExceptions sometimes are wrapped.
			// #1358 SolR 6.6.6 and SolR 8 seems to always wrap IOException in SolrServerException.
			// See: https://github.com/apache/lucene-solr/blame/branch_6_6/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpSolrClient.java
			logger.warn("Solr first attempt failed with SolrServerException, retry in {} ms: {}", retryPause, e.getMessage());
			logger.debug("Solr first attempt failed with SolrServerException: ", e);
			return retry(e);
		} catch (IOException e) {
			// #1358 Likely never happens.
			logger.warn("Solr first attempt failed with IOException, retry in {} ms: {}", retryPause, e.getMessage());
			logger.debug("Solr first attempt failed with IOException: ", e);
			return retry(e);
		}
	}
	
	private T retry(Exception exceptionFirst) {
		if (!isRetryAllowed()) {
			throw new RuntimeException("Solr exception (retry not allowed for this op)", exceptionFirst);
		}
		
		T result = null;
		try {
			Thread.sleep(retryPause);
		} catch (InterruptedException e) {
			throw new RuntimeException("Retry sleep interrupted: " +  e.getMessage());
		}
		
		logger.info("Solr second attempt...");
		try {
			result = runOp();
		} catch (RemoteSolrException e) {
			logger.warn("Solr second attempt failed with RemoteSolrException HTTP {}", e.code(), e.getMessage());
			logger.debug("Solr second attempt failed with RemoteSolrException HTTP {}: ", e.code(), e);
			throw new RuntimeException("Solr exception during retry", e);
		} catch (SolrServerException e) {
			logger.warn("Solr second attempt failed with SolrServerException: ", e);
			throw new RuntimeException("Solr exception during retry", e);
		} catch (IOException e) {
			// #1358 Likely never happens.
			logger.warn("Solr second attempt failed with IOException: ", e);
			throw new RuntimeException("Solr exception during retry", e);
		}
		logger.info("Solr second attempt successful");
		return result;
	}
	
	protected abstract T runOp() throws SolrServerException, IOException;
	
	protected abstract boolean isRetryAllowed();
	
}
