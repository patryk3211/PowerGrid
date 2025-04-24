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
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.wire.IWire;

public class TerminalHandler {
    private static final Object outlineSlot = new Object();

    public static void init() {
        ClientTickEvents.END_WORLD_TICK.register(TerminalHandler::tick);
    }

    private static void tick(ClientWorld world) {
        var client = MinecraftClient.getInstance();
        var target = client.crosshairTarget;
        if(target == null || target.getType() == HitResult.Type.MISS || client.player == null)
            return;


        var mainItem = client.player.getMainHandStack();
        var offItem = client.player.getOffHandStack();
        if(!(mainItem != null && !mainItem.isEmpty() && mainItem.getItem() instanceof IWire) &&
                !(offItem != null && !offItem.isEmpty() && offItem.getItem() instanceof IWire) &&
                !GogglesItem.isWearingGoggles(client.player))
            return;

        if(target instanceof BlockHitResult blockHit) {
            var state = world.getBlockState(blockHit.getBlockPos());
            if(!(state.getBlock() instanceof IElectric electric))
                return;
            var blockPos = blockHit.getBlockPos();
            var terminal = electric.terminalAt(state, blockHit.getPos().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            if(terminal == null)
                return;

            if(!(terminal instanceof IDecoratedTerminal decorated))
                return;

            CreateClient.OUTLINER.showAABB(outlineSlot, decorated.getOutline().offset(blockPos))
                    .colored(decorated.getColor())
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.020f);
        }
    }
}
