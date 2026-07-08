/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.repos.indexing.config;

import java.nio.ByteBuffer;
import java.util.Base64;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

import se.simonsoft.svn.runtime.RepoId;

/**
 * Test alternative for the production repo id producer.
 * Generates a fresh random repository id per request.
 */
@Alternative
@Priority(1)
// String is final and cannot be proxied, so the request scope is held by the producer class instead.
@RequestScoped
public class RandomRepoIdProducer {

	private final String repoId = "test-" + Base64.getUrlEncoder().withoutPadding()
			.encodeToString(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array());

	@Produces
	@RepoId
	public String produceRepoId() {
		return repoId;
	}
}
