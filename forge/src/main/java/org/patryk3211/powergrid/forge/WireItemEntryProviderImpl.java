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
package org.patryk3211.powergrid.forge;

import com.tterrag.registrate.AbstractRegistrate;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;

public class WireItemEntryProviderImpl extends AbstractPowerGridRegistrate.WireItemEntryProvider {
    public WireItemEntryProviderImpl(AbstractRegistrate<?> owner, GatherDataEvent data) {
        super(owner, data.getGenerator().getPackOutput());
    }

    @NotNull
    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }
}
