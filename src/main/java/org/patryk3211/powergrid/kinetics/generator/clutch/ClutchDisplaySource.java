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
package org.patryk3211.powergrid.kinetics.generator.clutch;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.utility.Lang;

import java.util.Optional;

public class ClutchDisplaySource extends PercentOrProgressBarDisplaySource {
    private Optional<GeneratorClutchBlockEntity> get(DisplayLinkContext ctx) {
        if(ctx.getSourceBlockEntity() instanceof GeneratorClutchBlockEntity clutch)
            return Optional.of(clutch);
        return Optional.empty();
    }

    @Override
    protected @Nullable Float getProgress(DisplayLinkContext context) {
        return get(context).map(be -> be.load).orElse(0f);
    }

    @Override
    protected MutableComponent formatNumeric(DisplayLinkContext context, Float currentLevel) {
        if(getMode(context) != 2)
            return super.formatNumeric(context, currentLevel);
        return get(context).map(be ->
                Lang.numberConstant(Math.abs(be.rotorBehaviour.getAngularVelocity()))
                        .add(Component.literal(" "))
                        .add(CreateLang.translate("generic.unit.rpm"))
                        .component())
                .orElseGet(() -> super.formatNumeric(context, currentLevel));
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 40;
    }

    @Override
    protected boolean progressBarActive(DisplayLinkContext context) {
        return getMode(context) == 0;
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    @Override
    protected String getTranslationKey() {
        return "generator_clutch";
    }

    private int getMode(DisplayLinkContext context) {
        return context.sourceConfig().getInt("Mode");
    }

    @Override
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if(isFirstLine)
            return;

        builder.addSelectionScrollInput(0, 120, (si, l) -> si
                        .forOptions(Lang.translatedOptions("display_source.generator_clutch",
                                "load_bar", "load_value", "rpm"))
                        .titled(Lang.translateDirect("display_source.display_information")),
                "Mode");
    }
}
