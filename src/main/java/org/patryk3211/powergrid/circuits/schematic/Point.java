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
package org.patryk3211.powergrid.circuits.schematic;

import org.jetbrains.annotations.NotNull;

public record Point(int x, int y) implements Comparable<Point> {
    @Override
    public int compareTo(@NotNull Point o) {
        var r = Integer.compare(y, o.y);
        return r == 0 ? Integer.compare(x, o.x) : r;
    }
}
