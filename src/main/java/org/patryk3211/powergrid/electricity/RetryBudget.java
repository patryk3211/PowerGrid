/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity;

final class RetryBudget {
    private final int maxAttempts;
    private int attempts;

    RetryBudget(int maxAttempts) {
        if(maxAttempts <= 0)
            throw new IllegalArgumentException("Maximum attempts must be positive");
        this.maxAttempts = maxAttempts;
    }

    boolean tryAcquire() {
        if(exhausted())
            return false;
        ++attempts;
        return true;
    }

    boolean exhausted() {
        return attempts >= maxAttempts;
    }

    int attempts() {
        return attempts;
    }

    int remaining() {
        return maxAttempts - attempts;
    }
}
