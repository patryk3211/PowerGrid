package org.patryk3211.powergrid.utility;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;

public class ShaperUtils extends VoxelShaper {
    private static final Function<Direction, Vec3> DIRECTION_VALUES = direction ->
            new Vec3(direction == Direction.UP ? 0.0 : (Direction.Plane.VERTICAL.test(direction) ? 180 : 90), -VoxelShaper.horizontalAngleFromDirection(direction), 0.0);

    public static VoxelShape rotate(VoxelShape shape, Direction from, Direction to) {
        return rotate(shape, from, to, DIRECTION_VALUES);
    }
}
