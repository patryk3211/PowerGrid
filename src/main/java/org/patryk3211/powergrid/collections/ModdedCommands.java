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
package org.patryk3211.powergrid.collections;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.patryk3211.powergrid.commands.ConfigCommand;
import org.patryk3211.powergrid.commands.DebugCommand;
import org.patryk3211.powergrid.commands.PerformanceCommand;
import org.patryk3211.powergrid.commands.RebuildNetworkCommand;
import org.patryk3211.powergrid.commands.SourceCommand;

public class ModdedCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("powergrid")
                .then(RebuildNetworkCommand.register())
                .then(PerformanceCommand.register().requires(cs -> cs.hasPermission(3)))
                .then(DebugCommand.register().requires(cs -> cs.hasPermission(3)))
                .then(SourceCommand.register().requires(cs -> cs.hasPermission(3)))
                .then(ConfigCommand.reset().requires(cs -> cs.hasPermission(3)))
                .then(ConfigCommand.ignore().requires(cs -> cs.hasPermission(3)));

        dispatcher.register(root);
    }
}
