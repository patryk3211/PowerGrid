/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.sim;

import org.patryk3211.powergrid.PowerGrid;

import java.util.ArrayList;
import java.util.List;

public class PerformanceCounter {
    public static final List<PerformanceCounter> COUNTERS = new ArrayList<>();

    private final String name;

    private long microsTotal;
    private long minTime;
    private long maxTime;
    private long epochCount;

    private long start;

    public PerformanceCounter(String name) {
        this.name = name;
        COUNTERS.add(this);
    }

    public void start() {
        start = System.nanoTime();
    }

    public void end() {
        var duration = System.nanoTime() - start;
        if(minTime == 0) {
            minTime = duration;
        } else if(minTime > duration) {
            minTime = duration;
        }
        if(maxTime < duration) {
            maxTime = duration;
        }
        ++epochCount;
        microsTotal += duration / epochCount;

        if(epochCount >= 1000) {
//            log();
            reset();
        }
    }

    public void reset() {
        microsTotal = 0;
        epochCount = 0;
        maxTime = 0;
        minTime = 0;
    }

    public void log() {
        PowerGrid.LOGGER.info("Performance counter '{}':", name);
        PowerGrid.LOGGER.info("  Min / Max / Avg");
        PowerGrid.LOGGER.info("  {}µs / {}µs / {}µs", minTime / 1000f, maxTime / 1000f, (float) microsTotal / epochCount);
    }
}
