package org.patryk3211.powergrid.electricity.modulardisplay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.patryk3211.powergrid.PowerGrid;

public class ModularDisplayBlockEntityRenderer extends SafeBlockEntityRenderer<ModularDisplayBlockEntity> {
    public ModularDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }
    private static final float SHEET_HEIGHT = 16f;

    private static final float FRAME_WIDTH = 5f;
    private static final float FRAME_HEIGHT = 7f;
    private static final float FRAME_PADDING = 1f;

    private static final int GRID_COLS = 2;
    private static final int GRID_ROWS = 2;

    private static final float PIXEL = 1f / 16f;
    private static final float INNER_UD_SIZE = 5f * PIXEL;
    private static final float INNER_RL_SIZE = 4.25f * PIXEL;

    private static final float Z_NUDGE = 0.001f;

    @Override
    protected void renderSafe(ModularDisplayBlockEntity be, float partialTicks, PoseStack pStack, MultiBufferSource buffer, int light, int overlay) {

        Direction facing = be.getBlockState().getValue(ModularDisplayBlock.HORIZONTAL_FACING);
        pStack.pushPose();
        pStack.translate(0.5, 0.5, 0.5);

        float yRot = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST  -> 90f;
            case EAST  -> 270f;
            default    -> 0f;
        };

        pStack.mulPose(Axis.YP.rotationDegrees(yRot));
        pStack.translate(-0.5, -0.5, -0.5);
        pStack.translate(0f, 0f, -Z_NUDGE);

        Matrix4f matrix = pStack.last().pose();

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {

                int slotIndex = row * GRID_COLS + col;
                SlotData slot = be.getSlot(slotIndex);

                if (slot.isEmpty()) continue;

                var halfClick = slot.getModule().getHalfClick();
                float innerX = 0, innerY = 0;
                switch (slotIndex){
                    case 0:innerX = 2 * PIXEL + (.375f/16); innerY = 9 * PIXEL;break;
                    case 1:innerX = 9 * PIXEL + (.375f/16); innerY = 9 * PIXEL;break;
                    case 2:innerX = 2 * PIXEL + (.375f/16); innerY = 2 * PIXEL;break;
                    case 3:innerX = 9 * PIXEL + (.375f/16); innerY = 2 * PIXEL;break;
                }

                float frameIndex = slot.getIndex();
                if (halfClick){
                    frameIndex -= .5f;
                }
                var SHEET_WIDTH = slot.getModule().getDisplayTextureSize();

                float uMin = (frameIndex * (FRAME_WIDTH + FRAME_PADDING)) / SHEET_WIDTH;
                float uMax = (frameIndex * (FRAME_WIDTH + FRAME_PADDING) + FRAME_WIDTH) / SHEET_WIDTH;
                float vMin = 0f;
                float vMax = FRAME_HEIGHT / SHEET_HEIGHT;

                float[] rgb = slot.getModule().getColor().getTextureDiffuseColors();
                var texture = (be.getRemovedBlankingPage(slotIndex) ?
                        slot.getModule().getDisplayTexture() + "noblank" : slot.getModule().getDisplayTexture());

                renderQuad(matrix, buffer,
                        PowerGrid.texture(texture),
                        innerX, innerY,
                        INNER_RL_SIZE, INNER_UD_SIZE,
                        uMin, vMin, uMax, vMax,
                        light, overlay, rgb);

            }
        }
        pStack.popPose();
    }

    private void renderQuad(Matrix4f matrix, MultiBufferSource bufferSource, ResourceLocation texture,
                            float x, float y, float width, float height, float uMin, float vMin, float uMax, float vMax,
                            int packedLight, int packedOverlay, float[] rgb) {

        VertexConsumer vc = bufferSource.getBuffer(RenderType.text(texture));

        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];

        vc.vertex(matrix, x + width, y, 0f).color(r, g, b, 1)
                .uv(uMin, vMax).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, -1f)
                .endVertex();

        vc.vertex(matrix, x, y, 0f).color(r, g, b, 1)
                .uv(uMax, vMax).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, -1f)
                .endVertex();

        vc.vertex(matrix, x, y + height, 0f).color(r, g, b, 1)
                .uv(uMax, vMin).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, -1f)
                .endVertex();

        vc.vertex(matrix,x + width, y + height, 0f).color(r, g, b, 1)
                .uv(uMin, vMin).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, -1f)
                .endVertex();

    }
}
