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
package org.patryk3211.powergrid.fabric;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;

public class ComponentItemEntryProviderImpl extends AbstractPowerGridRegistrate.ComponentItemEntryProvider {
    public ComponentItemEntryProviderImpl(AbstractRegistrate<?> owner, RegistrateDataProvider.DataInfo dataInfo) {
        super(owner, dataInfo.output());
    }

    @NotNull
    @Override
    public EnvType getSide() {
        return EnvType.SERVER;
    }
}
