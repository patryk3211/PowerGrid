package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3d;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedContraptions;
import org.patryk3211.powergrid.utility.Lang;

public class SolarPanelBearingContraption extends BearingContraption {
    protected int panelBlocks;
    protected Vector3d panelNormal;

    public SolarPanelBearingContraption() {
        super();
    }

    public SolarPanelBearingContraption(Direction facing) {
        this.facing = facing;
    }

    @Override
    public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
        BlockPos offset = pos.relative(facing);
        if (!searchMovedStructure(world, offset, null))
            return false;
        startMoving(world);
        expandBoundsAroundAxis(facing.getAxis());
        if (panelBlocks < 1)
            throw new AssemblyException(Lang.translateDirect(
                    "contraption.assembly.not_enough_panels"));
        for (StructureTemplate.StructureBlockInfo info : getBlocks().values()) {
            if (!(info.state().getBlock() instanceof SolarPanelBlock))
                continue;
            Direction panelFacing = info.state().getValue(SolarPanelBlock.FACING).getOpposite();
            var n = panelFacing.getNormal();
            if (panelNormal == null) {
                panelNormal = new Vector3d(n.getX(), n.getY(), n.getZ());
            } else if (!panelNormal.equals(new Vector3d(n.getX(), n.getY(), n.getZ()))) {
                throw new AssemblyException(Lang.translateDirect(
                        "contraption.assembly.mismatched_panel_facing"));
            }
        }
        if (!hasValidDivisor(panelBlocks)){
            throw new AssemblyException(Lang.translateDirect("contraption.assembly.invalid_panel_amount"));
        }

        return !blocks.isEmpty();
    }

    private static boolean hasValidDivisor(int panelCount) {
        for (int i = 1; i <= 9; i++) {
            if (panelCount % i == 0 && (panelCount / i) <= 25)
                return true;
        }
        return false;
    }

    @Override
    public void addBlock(Level level, BlockPos pos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> capture) {
        BlockPos localPos = pos.subtract(anchor);
        if (!getBlocks().containsKey(localPos) && ModdedBlockEntities.SOLAR_PANEL.is(capture.getRight())){
            panelBlocks++;
        }
        super.addBlock(level, pos, capture);
    }

    @Override
    public CompoundTag writeNBT(boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(spawnPacket);
        tag.putInt("Panels", panelBlocks);
        tag.putInt("Facing", facing.get3DDataValue());
        tag.putDouble("normalX", panelNormal.x);
        tag.putDouble("normalY", panelNormal.y);
        tag.putDouble("normalZ", panelNormal.z);
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
        panelBlocks = tag.getInt("Panels");
        facing = Direction.from3DDataValue(tag.getInt("Facing"));
        panelNormal = new Vector3d(tag.getDouble(("normalX")), tag.getDouble("normalY"), tag.getDouble("normalZ"));
        super.readNBT(world, tag, spawnData);
    }

    @Override
    public ContraptionType getType() {
        return ModdedContraptions.SOLAR_PANEL.value();
    }

    public int getPanelBlocks(){
        return panelBlocks;
    }
}

