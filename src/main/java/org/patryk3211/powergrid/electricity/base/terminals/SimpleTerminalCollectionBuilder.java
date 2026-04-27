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
package org.patryk3211.powergrid.electricity.base.terminals;

import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SimpleTerminalCollectionBuilder<B extends SimpleTerminalCollectionBuilder<?, C>, C> implements TerminalCollectionBuilder<C> {
    protected final List<TerminalBoundingBox> terminals = new ArrayList<>();

    public B add(TerminalBoundingBox terminal) {
        terminals.add(terminal);
        return (B) this;
    }

    public B add(TerminalBoundingBox... terminals) {
        Collections.addAll(this.terminals, terminals);
        return (B) this;
    }
}
