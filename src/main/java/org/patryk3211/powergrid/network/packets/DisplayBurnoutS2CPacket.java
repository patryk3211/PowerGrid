package org.patryk3211.powergrid.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.modulardisplay.ModularDisplayBlock;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.network.S2CPacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

public class DisplayBurnoutS2CPacket implements S2CPacket {

    private final BlockPos pos;
    private final int slotIndex;

    public DisplayBurnoutS2CPacket(BlockPos pos, int slotIndex) {
        this.pos = pos;
        this.slotIndex = slotIndex;
    }

    public DisplayBurnoutS2CPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.slotIndex = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(slotIndex);
    }

    @Override
    public void handle(Minecraft mc) {
        var world = ClientSideAccess.world();
        if (world == null) return;

        int col = slotIndex % 2;
        int row = slotIndex / 2;

        float cellX = (col * 8 + 4) / 16f;
        float cellY = ((1 - row) * 8 + 4) / 16f;

        BlockState state = world.getBlockState(pos);
        Direction facing = state.getValue(ModularDisplayBlock.HORIZONTAL_FACING);

        float x, y, z;
        y = pos.getY() + cellY;

        switch (facing) {
            case NORTH -> { x = pos.getX() + cellX;        z = pos.getZ(); }
            case SOUTH -> { x = pos.getX() + (1 - cellX);  z = pos.getZ() + 1; }
            case WEST  -> { x = pos.getX();                 z = pos.getZ() + (1 - cellX); }
            case EAST  -> { x = pos.getX() + 1;             z = pos.getZ() + cellX; }
            default    -> { x = pos.getX() + 0.5f;          z = pos.getZ() + 0.5f; }
        }

        RandomSource random = world.random;
        SparkParticleData.explodeParticles(world, x, y, z, facing, 10);
        ModdedSoundEvents.COMPONENT_EXPLODE.playAt(world, pos, 1.0f, random.nextFloat() * 0.1f + 0.9f, true);
    }
}