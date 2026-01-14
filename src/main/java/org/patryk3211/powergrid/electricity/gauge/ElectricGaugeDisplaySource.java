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
package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.utility.Lang;

public class ElectricGaugeDisplaySource extends PercentOrProgressBarDisplaySource {
    @Override
    protected @Nullable Float getProgress(DisplayLinkContext context) {
        if(context.getSourceBlockEntity() instanceof GaugeBlockEntity gauge) {
            return gauge.getProgress();
        }
        return 0f;
    }

    @Override
    protected MutableComponent formatNumeric(DisplayLinkContext context, Float currentLevel) {
        if(context.getSourceBlockEntity() instanceof GaugeBlockEntity gauge) {
            if(getMode(context) == 3) {
                return gauge.getCustomFormatted();
            } else {
                var value = gauge.getValue();
                if (getMode(context) == 1) {
                    value = Math.abs(value);
                }
                return Lang.numberConstant(value)
                        .add(Component.literal(" "))
                        .add(gauge.getUnit().get())
                        .component();
            }
        }
        return super.formatNumeric(context, currentLevel);
    }

    private int getMode(DisplayLinkContext context) {
        return context.sourceConfig().getInt("Mode");
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
        return "electric_gauge";
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (isFirstLine)
            return;

        builder.addSelectionScrollInput(0, 120, (si, l) -> si
                        .forOptions(Lang.translatedOptions("display_source.electric_gauge",
                                "progress_bar", "absolute", "polarized", "custom"))
                        .titled(Lang.translateDirect("display_source.display_information")),
                "Mode");
    }

}
