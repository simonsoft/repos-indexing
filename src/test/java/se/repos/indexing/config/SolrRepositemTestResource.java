/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SolrRepositemTestResource implements QuarkusTestResourceLifecycleManager {

	private static final DockerImageName SOLR_IMAGE = DockerImageName.parse("solr:9.6.1");
	private static final int SOLR_PORT = 8983;

	private GenericContainer<?> solr;

	@Override
	public Map<String, String> start() {
		Path configset = Path.of("src/main/resources/se/repos/indexing/solr/repositem").toAbsolutePath();
		solr = new GenericContainer<>(SOLR_IMAGE)
				.withExposedPorts(SOLR_PORT)
				.withCopyFileToContainer(MountableFile.forHostPath(configset), "/repositem-config")
				.withCommand("solr-precreate", "repositem", "/repositem-config")
				.waitingFor(Wait.forHttp("/solr/repositem/select?q=*:*")
						.forPort(SOLR_PORT)
						.forStatusCode(200)
						.withStartupTimeout(Duration.ofMinutes(2)));
		solr.start();
		return Map.of(
				"quarkus.solr.devservices.enabled", "false",
				"quarkus.solr.enabled", "true",
				"quarkus.solr.url", "http://" + solr.getHost() + ":" + solr.getMappedPort(SOLR_PORT) + "/solr/repositem");
	}

	@Override
	public void stop() {
		if (solr != null) {
			solr.stop();
		}
	}
}
