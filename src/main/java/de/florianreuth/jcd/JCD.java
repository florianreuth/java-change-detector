/*
 * This file is part of java-change-detector - https://github.com/florianreuth/java-change-detector
 * Copyright (C) 2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.florianreuth.jcd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JCD {

    private static final Logger LOGGER = LogManager.getLogger(JCD.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final HttpClient HTTP = HttpClient.newBuilder().build();
    private static final Path CACHE = Path.of(".cache");

    private final Config config;
    private ScheduledExecutorService executor;
    private CountDownLatch shutdownLatch;

    public JCD(final Config config) {
        this.config = config;
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("logs"));

        final Path path = Path.of("config.json");
        if (!Files.exists(path)) {
            Files.writeString(path, GSON.toJson(createDefaultConfig()));
        }

        final Config config = GSON.fromJson(Files.readString(path), Config.class);
        if (config.monitors().isEmpty()) {
            LOGGER.warn("No monitors configured.");
            return;
        }

        Files.createDirectories(CACHE);
        final JCD service = new JCD(config);
        service.start();
    }

    public void start() {
        executor = Executors.newScheduledThreadPool(Math.min(config.monitors().size(), 5));
        shutdownLatch = new CountDownLatch(1);

        for (final Monitor monitor : config.monitors()) {
            final long intervalSeconds = monitor.intervalInSeconds();
            LOGGER.info("[{}] Monitoring {} every {} ({}s){}", monitor.name(), monitor.url(), monitor.interval(), intervalSeconds, monitor.path() != null ? " (path: " + monitor.path() + ")" : "");
            executor.scheduleAtFixedRate(() -> this.check(monitor), 0, intervalSeconds, TimeUnit.SECONDS);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try {
            shutdownLatch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stop() {
        LOGGER.info("Stopping...");

        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                executor.shutdownNow();
            }
        }
        shutdownLatch.countDown();
        LOGGER.info("Stopped.");
    }

    private void check(final Monitor monitor) {
        try {
            final String content = fetch(monitor.url());
            final String newValue = extractValue(content, monitor.path());
            final Path cacheFile = CACHE.resolve(monitor.name() + ".txt");

            String oldValue = null;
            if (Files.exists(cacheFile)) {
                oldValue = Files.readString(cacheFile).trim();
            }

            if (oldValue == null) {
                LOGGER.info("[{}] Initial value: {}", monitor.name(), newValue);
                Files.writeString(cacheFile, newValue);
            } else if (!oldValue.equals(newValue)) {
                LOGGER.warn("[{}] Change detected: {} -> {}", monitor.name(), oldValue, newValue);
                Files.writeString(cacheFile, newValue);
                executeCommands(monitor, oldValue, newValue);
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("[{}] Error: {}", monitor.name(), e.getMessage());
        }
    }

    private String extractValue(final String json, final String path) {
        if (path == null || path.isEmpty()) {
            return json;
        }

        final Object result = JsonPath.read(json, path);
        return result != null ? result.toString() : null;
    }

    private void executeCommands(final Monitor monitor, final String oldValue, final String newValue) {
        if (monitor.execution() == null || monitor.execution().isEmpty()) {
            return;
        }

        for (final String command : monitor.execution()) {
            final String resolvedCommand = command.replace("${OLD_VALUE}", oldValue).replace("${NEW_VALUE}", newValue);

            try {
                LOGGER.info("[{}] Executing: {}", monitor.name(), resolvedCommand);
                final ProcessBuilder pb = new ProcessBuilder("sh", "-c", resolvedCommand);
                pb.inheritIO();
                final Process process = pb.start();
                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    LOGGER.warn("[{}] Command exited with code: {}", monitor.name(), exitCode);
                } else {
                    LOGGER.info("[{}] Command completed successfully", monitor.name());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("[{}] Command error: {}", monitor.name(), e.getMessage(), e);
            }
        }
    }

    private String fetch(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "JCD").GET().build();
        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static Config createDefaultConfig() {
        final Monitor monitor = new Monitor(
            "minecraft-versions",
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",
            "1d",
            "$.latest.release",
            List.of(
                "echo 'New Minecraft Version: ${NEW_VALUE}'",
                "echo 'Previous: ${OLD_VALUE}'"
            )
        );
        return new Config(List.of(monitor));
    }

}
