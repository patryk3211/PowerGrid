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
package org.patryk3211.powergrid.electricity.info;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;

public class TerminalHandler {
    private static final Object outlineSlot = new Object();
    private static IDecoratedTerminal targetTerminal = null;

    public static void tick(ClientLevel world) {
        targetTerminal = null;
        var client = Minecraft.getInstance();
        var target = client.hitResult;
        if(target == null || target.getType() == HitResult.Type.MISS || client.player == null)
            return;

        var mainItem = client.player.getMainHandItem();
        var offItem = client.player.getOffhandItem();
        if(!(mainItem != null && !mainItem.isEmpty() && IWire.isWire(world, mainItem.getItem())) &&
                !(offItem != null && !offItem.isEmpty() && IWire.isWire(world, offItem.getItem())) &&
                !GogglesItem.isWearingGoggles(client.player))
            return;

        if(target instanceof BlockHitResult blockHit) {
            ITerminalPlacement terminal = null;
            var blockPos = blockHit.getBlockPos();
            var state = world.getBlockState(blockPos);
            var electric = IElectric.getAt(world, blockPos);
            if(electric != null) {
                terminal = electric.terminalAt(state, blockHit.getLocation().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            }
            if(terminal == null &&
                    ((mainItem != null && IWire.isCord(world, mainItem.getItem())) ||
                     (offItem != null && IWire.isCord(world, offItem.getItem())))) {
                for(var handler : CordItem.PLACEMENT_HANDLERS) {
                    terminal = handler.terminal(state, world, blockHit);
                    if(terminal != null)
                        break;
                }
            }

            if(!(terminal instanceof IDecoratedTerminal decorated))
                return;
            targetTerminal = decorated;

            Outliner.getInstance().chaseAABB(outlineSlot, decorated.getOutline().move(blockPos))
                    .colored(decorated.getColor())
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.020f);
        }
    }

    @Nullable
    public static Component overlayText(Player player) {
        if(targetTerminal == null)
            return null;
        return targetTerminal.getName();
    }
}
