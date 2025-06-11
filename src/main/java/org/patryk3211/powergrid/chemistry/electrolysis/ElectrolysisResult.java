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
package org.patryk3211.powergrid.chemistry.electrolysis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;

import java.util.Optional;

public record ElectrolysisResult(boolean negative, ReagentStack reagent) {
    public static final Codec<ElectrolysisResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("negative").forGetter(ElectrolysisResult::negative),
            ReagentRegistry.REGISTRY.getCodec().fieldOf("reagent").forGetter(obj -> obj.reagent.getReagent()),
            Codec.optionalField("amount", Codec.INT).forGetter(obj -> {
                var amount = obj.reagent.getAmount();
                return amount == 1 ? Optional.empty() : Optional.of(amount);
            })
    ).apply(instance, (negative, reagent, amount) -> new ElectrolysisResult(negative, new ReagentStack(reagent, amount.orElse(1)))));
}
