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
package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import it.unimi.dsi.fastutil.objects.Object2ReferenceArrayMap;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedAdvancements;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.utility.Lang;

import java.util.Map;
import java.util.UUID;

import static org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock.HORIZONTAL_AXIS;
import static org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock.PART;

public class TransformerCoreBlock extends Block implements IWrenchable {
    private record TransformationData(Direction dir, Player player) {
        public void sendError(String translationKey, Object... args) {
            if(player == null)
                return;
            player.sendSystemMessage(Lang
                    .translate("message.nether.transformer_fail")
                    .style(ChatFormatting.GOLD)
                    .component());
            player.sendSystemMessage(Lang
                    .text(" - ").add(Lang
                            .translate(translationKey, args)
                            .color(16765876))
                    .component());
        }
    }
    private static final Map<BlockPos, TransformationData> SCHEDULED_TRANSFORMATIONS = new Object2ReferenceArrayMap<>();

    public TransformerCoreBlock(Properties settings) {
        super(settings);
    }

    private boolean locate2x2(Level level, BlockPos pos, Direction dir, Player player) {
        int[] blockMask = new int[3 * 3];

        for(int x = -1; x <= 1; ++x) {
            for(int y = -1; y <= 1; ++y) {
                var i = x + 1;
                var j = y + 1;
                var oPos = pos.relative(dir, x).relative(Direction.UP, y);
                var state = level.getBlockState(oPos);
                if(state.is(this)) {
                    blockMask[i + j * 3] = 1;
                } else if(state.is(Blocks.NETHER_PORTAL)) {
                    blockMask[i + j * 3] = 2;
                }
            }
        }

        for(int x = -1; x < 1; ++x) {
            for(int y = -1; y < 1; ++y) {
                var i = x + 1;
                var j = y + 1;
                if(blockMask[i + j * 3] != 0 &&
                   blockMask[i + 1 + j * 3] != 0 &&
                   blockMask[i + (j + 1) * 3] != 0 &&
                   blockMask[i + 1 + (j + 1) * 3] != 0) {
                    if(blockMask[i + j * 3] == 2 && blockMask[i + (j + 1) * 3] == 2) {
                        if(level instanceof ServerLevel serverLevel) {
                            var corePos = pos.relative(dir, i).above(j - 1);
                            SCHEDULED_TRANSFORMATIONS.put(corePos, new TransformationData(
                                    dir.getOpposite(),
                                    player
                            ));
                            level.scheduleTick(corePos, this, 1);
                        }
                        return true;
                    } else if(blockMask[i + 1 + j * 3] == 2 || blockMask[i + 1 + (j + 1) * 3] == 2) {
                        if(level instanceof ServerLevel serverLevel) {
                            var corePos = pos.relative(dir, i - 1).above(j - 1);
                            SCHEDULED_TRANSFORMATIONS.put(corePos, new TransformationData(
                                    dir,
                                    player
                            ));
                            level.scheduleTick(corePos, this, 1);
                        }
                        return true;
                    } else {
                        // 2x2 section of transformer core found.
                        if (!level.isClientSide) {
                            var state = ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                                    .setValue(TransformerMediumBlock.HORIZONTAL_AXIS, dir.getAxis());
                            level.setBlockAndUpdate(pos.relative(dir, x).relative(Direction.UP, y), state.setValue(PART, 0));
                            level.setBlockAndUpdate(pos.relative(dir, x + 1).relative(Direction.UP, y), state.setValue(PART, 1));
                            level.setBlockAndUpdate(pos.relative(dir, x).relative(Direction.UP, y + 1), state.setValue(PART, 2));
                            level.setBlockAndUpdate(pos.relative(dir, x + 1).relative(Direction.UP, y + 1), state.setValue(PART, 3));
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var data = SCHEDULED_TRANSFORMATIONS.remove(pos);
        if(data == null)
            return;
        var otherSide = PortalTrackProvider.getOtherSide(level, new BlockFace(pos, data.dir));
        if(otherSide == null) {
            data.sendError("message.nether.no_other_side");
            return;
        }
        var otherLevel = otherSide.level();

        var otherPos = otherSide.face().getPos();
        if(!otherLevel.getBlockState(otherPos).canBeReplaced()) {
            data.sendError("message.nether.blocked", otherPos.getX(), otherPos.getY(), otherPos.getZ());
            return;
        }
        if(!otherLevel.getBlockState(otherPos.above()).canBeReplaced()) {
            data.sendError("message.nether.blocked", otherPos.getX(), otherPos.getY() + 1, otherPos.getZ());
            return;
        }

        var setState = ModdedBlocks.NETHER_TRANSFORMER.getDefaultState()
                .setValue(HORIZONTAL_AXIS, data.dir.getAxis());
        var otherDir = otherSide.face().getFace();
        var otherSetState = ModdedBlocks.NETHER_TRANSFORMER.getDefaultState()
                .setValue(HORIZONTAL_AXIS, otherDir.getAxis());
        if(data.dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            level.setBlockAndUpdate(pos, setState.setValue(NetherTransformerBlock.PART, 0));
            level.setBlockAndUpdate(pos.above(), setState.setValue(NetherTransformerBlock.PART, 2));
        } else {
            level.setBlockAndUpdate(pos, setState.setValue(NetherTransformerBlock.PART, 1));
            level.setBlockAndUpdate(pos.above(), setState.setValue(NetherTransformerBlock.PART, 3));
        }
        if(otherDir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            otherLevel.setBlockAndUpdate(otherPos, otherSetState.setValue(NetherTransformerBlock.PART, 0));
            otherLevel.setBlockAndUpdate(otherPos.above(), otherSetState.setValue(NetherTransformerBlock.PART, 2));
        } else {
            otherLevel.setBlockAndUpdate(otherPos, otherSetState.setValue(NetherTransformerBlock.PART, 1));
            otherLevel.setBlockAndUpdate(otherPos.above(), otherSetState.setValue(NetherTransformerBlock.PART, 3));
        }
        if(data.player != null && !ModdedAdvancements.NETHER_TRANSFORMER.isAlreadyAwardedTo(data.player)) {
            ModdedAdvancements.NETHER_TRANSFORMER.awardTo(data.player);
        }
        var id = UUID.randomUUID();
        if(level.getBlockEntity(pos.above()) instanceof NetherTransformerBlockEntity be1 &&
           otherLevel.getBlockEntity(otherPos.above()) instanceof NetherTransformerBlockEntity be2) {
            be1.link(id, false);
            be2.link(id, true);
        } else {
            level.destroyBlock(pos, true);
            level.destroyBlock(pos.above(), true);
            otherLevel.destroyBlock(otherPos, false);
            otherLevel.destroyBlock(otherPos.above(), false);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var pos = context.getClickedPos();
        var world = context.getLevel();
        if (!locate2x2(world, pos, Direction.SOUTH, context.getPlayer()) && !locate2x2(world, pos, Direction.EAST, context.getPlayer())) {
            // 1x1 transformer
            if(!world.isClientSide) {
                world.setBlockAndUpdate(pos, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                        .setValue(TransformerSmallBlock.HORIZONTAL_AXIS, context.getHorizontalDirection().getClockWise().getAxis()));
            }
        }
        IWrenchable.playRotateSound(world, pos);
        return InteractionResult.SUCCESS;
    }
}
