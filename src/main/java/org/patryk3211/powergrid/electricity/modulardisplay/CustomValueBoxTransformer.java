package org.patryk3211.powergrid.electricity.modulardisplay;


import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CustomValueBoxTransformer extends ValueBoxTransform {

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        int slot = blockEntity.lastHitSlot;
        int col = slot % 4;
        int row = slot / 4;

        double x = 1.0 - (col * 4 + 2) / 16.0;
        double y = ((3 - row) * 4 + 2) / 16.0;
        double z = 1.0;

        return rotateHorizontally(state, new Vec3(x, y, z-0.015));
    }
    private final ModularDisplayBlockEntity blockEntity;

    public CustomValueBoxTransformer(ModularDisplayBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        Direction facing = state.getValue(ModularDisplayBlock.HORIZONTAL_FACING);

        double bestDist = Double.MAX_VALUE;
        int bestSlot = 0;

        for (int i = 0; i < 16; i++) {
            int col = i % 4;
            int row = i / 4;

            double slotX, slotY, slotZ;
            double u = (col * 4 + 2) / 16.0;
            double v = ((3 - row) * 4 + 2) / 16.0;

            switch (facing) {
                case NORTH -> { slotX = u;        slotY = v; slotZ = 0.0;}
                case SOUTH -> { slotX = 1.0 - u;  slotY = v; slotZ = 1.0;}
                case WEST  -> { slotX = 0.0;      slotY = v; slotZ = 1.0 - u;}
                case EAST  -> { slotX = 1.0;      slotY = v; slotZ = u;}
                default    -> { slotX = 0.5;      slotY = v; slotZ = 0.5;}
            }

            double dist = localHit.distanceTo(new Vec3(slotX, slotY, slotZ));
            if (dist < bestDist) {
                bestDist = dist;
                bestSlot = i;
            }
        }

        blockEntity.lastHitSlot = bestSlot;
        blockEntity.syncBehaviourToSlot(bestSlot);

        if (level.isClientSide()){
            Minecraft mc = Minecraft.getInstance();
            ItemStack held = mc.player.getMainHandItem();
            if (held.getItem() instanceof DyeItem) return false;
        }

        if (blockEntity.modules[bestSlot] == null) return false;

        return bestDist < 0.2;
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        float yRot = AngleHelper.horizontalAngle(
                state.getValue(ModularDisplayBlock.HORIZONTAL_FACING)
        ) + 180;
        TransformStack.of(ms).rotateYDegrees(yRot);
    }

    @Override
    public float getScale() {
        return 0.35f;
    }
}