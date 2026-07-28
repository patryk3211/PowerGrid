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
package org.patryk3211.powergrid.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.TransmissionNetworkRebuildJob;
import org.patryk3211.powergrid.electricity.WorldNetworks;

import static net.minecraft.commands.Commands.literal;

public final class RebuildNetworkCommand {
    static final long COOLDOWN_TICKS = 20L * 60L;
    private static final RebuildCooldown<ServerLevel> COOLDOWN = new RebuildCooldown<>(COOLDOWN_TICKS);

    private RebuildNetworkCommand() {
    }

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("rebuild")
                .requires(source -> source.hasPermission(2))
                .executes(context -> execute(context.getSource()));
    }

    private static int execute(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if(level == null)
            level = source.getServer().overworld();
        if(level == null) {
            source.sendFailure(Component.translatable("powergrid.command.rebuild.no_level"));
            return 0;
        }

        var targetLevel = level;
        var networks = GlobalElectricNetworks.getWorldNetworks(targetLevel);
        if(networks.verifiedRebuildInProgress()) {
            source.sendFailure(Component.translatable("powergrid.command.rebuild.running"));
            return 0;
        }

        var currentTick = targetLevel.getGameTime();
        if(!COOLDOWN.tryAcquire(targetLevel, currentTick)) {
            var remainingTicks = COOLDOWN.remainingTicks(targetLevel, currentTick);
            var remainingSeconds = Math.max(1, (remainingTicks + 19) / 20);
            source.sendFailure(Component.translatable(
                    "powergrid.command.rebuild.cooldown",
                    remainingSeconds
            ));
            return 0;
        }

        try {
            var start = networks.startVerifiedRebuild(new TransmissionNetworkRebuildJob.Listener() {
                @Override
                public void completed(TransmissionNetworkRebuildJob.Outcome outcome) {
                    if(outcome.complete()) {
                        source.sendSystemMessage(Component.translatable(
                                "powergrid.command.rebuild.success",
                                outcome.rebuild().parts(),
                                outcome.uniqueChunks(),
                                outcome.refreshedRecords(),
                                outcome.recoveredRecords(),
                                outcome.removedRecords(),
                                outcome.rebuild().linesAfter(),
                                outcome.elapsedMillis()
                        ).withStyle(ChatFormatting.GREEN));
                        return;
                    }

                    source.sendFailure(Component.translatable(
                            "powergrid.command.rebuild.partial",
                            outcome.unresolvedParts(),
                            outcome.rebuild().retryingParts(),
                            outcome.rebuild().retryLimit()
                    ));
                }

                @Override
                public void failed(TransmissionNetworkRebuildJob.Failure failure) {
                    source.sendFailure(Component.translatable(
                            "powergrid.command.rebuild.failed",
                            failure.attempts()
                    ));
                }
            });

            if(start.status() == WorldNetworks.VerifiedRebuildStartStatus.ALREADY_RUNNING) {
                source.sendFailure(Component.translatable("powergrid.command.rebuild.running"));
                return 0;
            }
            if(start.status() == WorldNetworks.VerifiedRebuildStartStatus.TOO_MANY_CHUNKS) {
                source.sendFailure(Component.translatable(
                        "powergrid.command.rebuild.too_many_chunks",
                        start.requestedChunks(),
                        TransmissionNetworkRebuildJob.MAX_UNIQUE_CHUNKS
                ));
                return 0;
            }

            var info = start.info();
            source.sendSystemMessage(Component.translatable(
                    "powergrid.command.rebuild.start",
                    info.parts(),
                    info.uniqueChunks(),
                    info.batches()
            ).withStyle(ChatFormatting.GRAY));
            return Command.SINGLE_SUCCESS;
        } catch(RuntimeException exception) {
            PowerGrid.LOGGER.error(
                    "Transmission network rebuild requested by {} failed in {}",
                    source.getTextName(),
                    targetLevel.dimension().location(),
                    exception
            );
            source.sendFailure(Component.translatable(
                    "powergrid.command.rebuild.failed",
                    0
            ));
            return 0;
        }
    }
}
