package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;

public class SolarPanelCTBehaviour extends ConnectedTextureBehaviour.Base {
    private static final CTSpriteShiftEntry TOP = CTSpriteShifter.getCT(
            AllCTTypes.OMNIDIRECTIONAL,
            PowerGrid.asResource("block/solar_panel/solar_panel"),
            PowerGrid.asResource("block/solar_panel/solar_panel_connected")
    );
    private static final CTSpriteShiftEntry BOTTOM = CTSpriteShifter.getCT(
            AllCTTypes.OMNIDIRECTIONAL,
            PowerGrid.asResource("block/solar_panel/copper_plating"),
            PowerGrid.asResource("block/solar_panel/copper_plating_connected")
    );

    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState blockState, Direction direction, @Nullable TextureAtlasSprite textureAtlasSprite) {
        var facing = blockState.getValue(SolarPanelBlock.FACING);
        if(facing == direction)
            return BOTTOM;
        if(facing == direction.getOpposite())
            return TOP;
        return null;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        if(state != other)
            return false;
        var be1 = reader.getBlockEntity(pos);
        var be2 = reader.getBlockEntity(otherPos);
        if(be1 instanceof SolarPanelBlockEntity sbe1 && be2 instanceof SolarPanelBlockEntity sbe2) {
            return SolarPanelBlockEntity.areConnected(sbe1, sbe2);
        }
        return false;
    }
}
