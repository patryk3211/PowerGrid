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
package org.patryk3211.powergrid.electricity.zapper;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.collections.ModdedItems;

@Environment(EnvType.CLIENT)
public class ElectroZapperRenderHandler extends ShootableGadgetRenderHandler {
    @Override
    protected void playSound(Hand hand, Vec3d position) {
        ZapProjectileEntity.playLaunchSound(MinecraftClient.getInstance().world, position, 1);
    }

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return ModdedItems.ELECTROZAPPER.get().isZapper(stack);
    }

    @Override
    protected void transformTool(MatrixStack ms, float flip, float equipProgress, float recoil, float pt) {
        ms.translate(flip * -.1f, 0, .14f);
        ms.scale(.75f, .75f, .75f);
        TransformStack.cast(ms)
                .rotateX(recoil * 80);
    }

    @Override
    protected void transformHand(MatrixStack ms, float flip, float equipProgress, float recoil, float pt) {
        ms.translate(flip * -.09, -.275, -.25);
        TransformStack.cast(ms)
                .rotateZ(flip * -10);
    }
}
