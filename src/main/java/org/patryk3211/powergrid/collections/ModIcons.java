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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.patryk3211.powergrid.PowerGrid;

/**
 * Mostly a copy of Create's AllIcons class
 * @see AllIcons
 */
public class ModIcons extends AllIcons {
    public static final ResourceLocation ICON_ATLAS = new ResourceLocation(PowerGrid.MOD_ID, "textures/gui/icons.png");
    public static final int ATLAS_SIZE = 64;

    private static int x = 0, y = -1;

    public static final ModIcons I_SERIES = newRow();
    public static final ModIcons I_PARALLEL = next();
    public static final ModIcons I_CONNECT = next();
    public static final ModIcons I_CANCEL = next();

    public static final ModIcons I_LAYER_FRONT = newRow();
    public static final ModIcons I_LAYER_BACK = next();
    public static final ModIcons I_RIGHT = next();
    public static final ModIcons I_TOGGLE = next();

    // Unfortunately we need these since AllIcons has them private, and we need them in render function.
    private final int iconX;
    private final int iconY;

    public ModIcons(int x, int y) {
        super(0, 0);
        iconX = x * 16;
        iconY = y * 16;
    }

    private static ModIcons next() {
        return new ModIcons(++x, y);
    }

    private static ModIcons newRow() {
        x = 0;
        return new ModIcons(x, ++y);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, ICON_ATLAS);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS, x, y, 0, (float)this.iconX, (float)this.iconY, 16, 16, ATLAS_SIZE, ATLAS_SIZE);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void render(PoseStack ms, MultiBufferSource buffer, int color) {
        var builder = buffer.getBuffer(RenderType.text(ICON_ATLAS));
        var matrix = ms.last().pose();
        var rgb = new Color(color);
        int light = 15728880;
        var vec1 = new Vec3(0, 0, 0);
        var vec2 = new Vec3(0, 1, 0);
        var vec3 = new Vec3(1, 1, 0);
        var vec4 = new Vec3(1, 0, 0);
        float u1 = (float) this.iconX / ATLAS_SIZE;
        float u2 = (float) (this.iconX + 16) / ATLAS_SIZE;
        float v1 = (float) this.iconY / ATLAS_SIZE;
        float v2 = (float) (this.iconY + 16) / ATLAS_SIZE;
        this.vertex(builder, matrix, vec1, rgb, u1, v1, light);
        this.vertex(builder, matrix, vec2, rgb, u1, v2, light);
        this.vertex(builder, matrix, vec3, rgb, u2, v2, light);
        this.vertex(builder, matrix, vec4, rgb, u2, v1, light);
    }

    @Environment(EnvType.CLIENT)
    private void vertex(VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
        builder.vertex(matrix, (float)vec.x, (float)vec.y, (float)vec.z)
                .color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
                .uv(u, v)
                .uv2(light)
                .endVertex();
    }
}
