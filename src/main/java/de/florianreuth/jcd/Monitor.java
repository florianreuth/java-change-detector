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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Monitor(String name, String url, String interval, String path, List<String> execution) {

    private static final Pattern INTERVAL_PATTERN = Pattern.compile("^(\\d+)([smhd])$");
    private static final long MIN_INTERVAL_SECONDS = 10;

    public long intervalInSeconds() {
        final Matcher matcher = INTERVAL_PATTERN.matcher(interval);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid interval format: '" + interval + "'. Expected format: <number><unit> (e.g., 30s, 5m, 1h, 1d)");
        }

        final long value = Long.parseLong(matcher.group(1));
        final String unit = matcher.group(2);
        return getSeconds(unit, value);
    }

    private static long getSeconds(String unit, long value) {
        final long seconds = switch (unit) {
            case "s" -> value;
            case "m" -> TimeUnit.MINUTES.toSeconds(value);
            case "h" -> TimeUnit.HOURS.toSeconds(value);
            case "d" -> TimeUnit.DAYS.toSeconds(value);
            default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
        };

        if (seconds < MIN_INTERVAL_SECONDS) {
            throw new IllegalArgumentException("Interval too short: " + seconds + "s. Minimum interval is " + MIN_INTERVAL_SECONDS + "s");
        }
        return seconds;
    }

}
