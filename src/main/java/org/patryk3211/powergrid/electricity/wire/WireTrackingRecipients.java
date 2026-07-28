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
package org.patryk3211.powergrid.electricity.wire;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

final class WireTrackingRecipients<T> {
    private final Set<T> recipients = Collections.newSetFromMap(new IdentityHashMap<>());

    void start(T recipient) {
        recipients.add(recipient);
    }

    void stop(T recipient) {
        recipients.remove(recipient);
    }

    void forEach(Consumer<T> action) {
        recipients.forEach(action);
    }
}
