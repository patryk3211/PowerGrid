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
package org.patryk3211.powergrid.commands;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.catnip.data.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.ForgeConfigSpec;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.CServer;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.minecraft.commands.Commands.literal;

public class ConfigCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> reset() {
        return literal("reset_configs")
                .requires(cs -> cs.hasPermission(3))
                .executes(ctx -> {
                    resetConfig(ModdedConfigs.server().specification);
                    ctx.getSource().sendSystemMessage(Lang.translateDirect("message.config_reset_ok"));
                    return Command.SINGLE_SUCCESS;
                });
    }

    public static ArgumentBuilder<CommandSourceStack, ?> ignore() {
        return literal("ignore_configs")
                .requires(cs -> cs.hasPermission(3))
                .executes(ctx -> {
                    ModdedConfigs.server().version.set(CServer.CONFIG_VERSION);
                    ctx.getSource().sendSystemMessage(Lang.translateDirect("message.config_ignore_ok"));
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static void resetConfig(UnmodifiableConfig values) {
        values.valueMap().forEach((key, obj) -> {
            if (obj instanceof AbstractConfig) {
                resetConfig((UnmodifiableConfig) obj);
            } else if (obj instanceof ForgeConfigSpec.ConfigValue) {
                ForgeConfigSpec.ConfigValue<Object> configValue = (ForgeConfigSpec.ConfigValue<Object>) obj;
                ForgeConfigSpec.ValueSpec valueSpec = ModdedConfigs.server().specification.getRaw(configValue.getPath());

                List<String> comments = new ArrayList<>();

                if (valueSpec.getComment() != null)
                    comments.addAll(Arrays.asList(valueSpec.getComment().split("\n")));

                Pair<String, Map<String, String>> metadata = ConfigHelper.readMetadataFromComment(comments);

                ConfigHelper.setValue(String.join(".", configValue.getPath()), configValue, valueSpec.getDefault(), metadata.getSecond());
            }
        });
    }
}
