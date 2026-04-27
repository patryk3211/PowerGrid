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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.patryk3211.powergrid.electricity.sim.PerformanceCounter;
import org.patryk3211.powergrid.utility.NumberFormats;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.StringReader.isAllowedInUnquotedString;
import static net.minecraft.commands.Commands.literal;

public class PerformanceCommand {
    public static PerformanceCounterArgument argument() {
        return PerformanceCounterArgument.INSTANCE;
    }

    public static class PerformanceCounterArgument implements ArgumentType<PerformanceCounter> {
        public static final PerformanceCounterArgument INSTANCE = new PerformanceCounterArgument();

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return CompletableFuture.supplyAsync(() -> {
                for (var counter : PerformanceCounter.COUNTERS) {
                    builder.suggest(counter.getName());
                }
                return builder.build();
            });
        }

        @Override
        public PerformanceCounter parse(StringReader reader) throws CommandSyntaxException {
            final int start = reader.getCursor();
            while (reader.canRead() && (isAllowedInUnquotedString(reader.peek()) || reader.peek() == ':')) {
                reader.skip();
            }
            var name = reader.getString().substring(start, reader.getCursor());
            PerformanceCounter prev = null;
            boolean cleanup = false;
            for (var counter : PerformanceCounter.COUNTERS) {
                if (name.equals(counter.getName())) {
                    if(prev != null) {
                        cleanup = true;
                    }
                    prev = counter;
                }
            }
            if(cleanup) {
                var iter = PerformanceCounter.COUNTERS.iterator();
                while(iter.hasNext()) {
                    var counter = iter.next();
                    if(name.equals(counter.getName()) && counter != prev) {
                        iter.remove();
                    }
                }
            }
            if(prev != null)
                return prev;
            throw new SimpleCommandExceptionType(Component
                    .literal("Performance counter '" + name + "' doesn't exist!")
                    .withStyle(ChatFormatting.RED)).create();
        }
    }

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("performance")
                .then(literal("get")
                        .then(Commands.argument("counter_name", argument())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    var counter = ctx.getArgument("counter_name", PerformanceCounter.class);
                                    source.sendSystemMessage(Component.literal("Performance counter '" + counter.getName() + "':").withStyle(ChatFormatting.GRAY));
                                    source.sendSystemMessage(Component.literal("  Last measurement: ")
                                            .append(Component.literal(counter.getTimestamp()).withStyle(ChatFormatting.AQUA))
                                            .withStyle(ChatFormatting.GRAY));
                                    source.sendSystemMessage(Component.literal("  ")
                                            .append(Component.literal("Min").withStyle(ChatFormatting.GRAY))
                                            .append(" / ")
                                            .append(Component.literal("Max").withStyle(ChatFormatting.GRAY))
                                            .append(" / ")
                                            .append(Component.literal("Avg").withStyle(ChatFormatting.GRAY))
                                            .withStyle(ChatFormatting.DARK_GRAY));
                                    source.sendSystemMessage(Component.literal("  ")
                                            .append(Component.literal(NumberFormats.formatConstant(counter.getMin()) + "µs")
                                                    .withStyle(ChatFormatting.AQUA))
                                            .append(" / ")
                                            .append(Component.literal(NumberFormats.formatConstant(counter.getMax()) + "µs")
                                                    .withStyle(ChatFormatting.AQUA))
                                            .append(" / ")
                                            .append(Component.literal(NumberFormats.formatConstant(counter.getAvg()) + "µs")
                                                    .withStyle(ChatFormatting.AQUA))
                                            .withStyle(ChatFormatting.DARK_GRAY));

                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(literal("period")
                        .then(Commands.argument("counter_name", argument())
                                .then(Commands.argument("ms_period", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            var counter = ctx.getArgument("counter_name", PerformanceCounter.class);
                                            var period = ctx.getArgument("ms_period", Integer.class);
                                            source.sendSystemMessage(Component.literal("Performance counter '" + counter.getName() + "' measurement period set to " + period + "ms").withStyle(ChatFormatting.GRAY));
                                            counter.setMeasurementTime(period);
                                            return Command.SINGLE_SUCCESS;
                                        }))));
    }
}
