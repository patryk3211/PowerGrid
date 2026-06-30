package org.patryk3211.powergrid.electricity.solarpanel;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlockEntity.*;

public class SolarHelper {

    public static double[] getTempAdjusted(double irradiance, double cellTemp, double Vt, int stringsInParallel) {
        var Isc_T = SHORT_CURRENT * stringsInParallel * (irradiance / 1000) * (1 + ALPHAISC * (cellTemp - 25));
        if (Isc_T <= 0) return new double[]{0, 0};
        var Voc_base = IDEALITY * Vt * Math.log(Isc_T / (I_O * stringsInParallel) + 1);
        var Voc_T = Voc_base + BETAVOC * (cellTemp - 25);
        return new double[]{Isc_T, Voc_T};
    }

//    public double getIrradiance(double AM, double cloudCover, int YPos, Level world) {
//        if (AM == Double.POSITIVE_INFINITY) return 0;
//        var transmisttance = 1 - cloudCover;
//        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
//        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level
//
//        double sunAngle = world.getSunAngle(0);
//        Vector3d sunDir = new Vector3d(-Math.sin(sunAngle), Math.cos(sunAngle), 0);
//        if (sunDir.y <= 0) return 0;
//
//        if (rayCastDelay-- == 0){
//            sunVisablity = sunRaycast(world);
//            rayCastDelay = world.random.nextInt(41) + 10;
//        }
//
//        double cosIncidence = Math.max(0, sunDir.dot(panelNormal));
//        cosIncidence = Math.max(0, cosIncidence);
//        double diffuseLight = 0.1 * irradiance * (1 + cloudCover) * ((1 + panelNormal.y()) / 2);
//        double reflected = 0.15 * irradiance * ((1 - panelNormal.y()) / 2.0);
//
//        return (irradiance * sunVisablity) * transmisttance * cosIncidence + diffuseLight +  reflected;
//    }

    public static double getCellTemp(double Irradiance, float AMBIENT_TEMP){
        return AMBIENT_TEMP + (NOCT - 20) * (Irradiance / 800);
    }

    public static double getAM(Level world){
        var sunAngle = Math.max(0, Math.cos(world.getSunAngle(0)));
        if (sunAngle <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double elevationRad = Math.asin(sunAngle);
        double elevationDeg = Math.toDegrees(elevationRad);

        // Kasten-Young formula
        var result = 1.0 / (sunAngle + 0.50572 * Math.pow(elevationDeg + 6.07995, -1.6364));
        return Math.max(1, result);
    }

    public static float getWeather(Level world){
        if (world.isRaining() && world.isThundering()) {
            return .925f;
        } else if (world.isRaining()) {
            return .85f;
        } else {
            return 0;
        }
    }

    public static List<BlockPos> DDA(Level level, Vec3 start, Vec3 end) {
        var subLevels = SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(start, end));
        List<BlockPos> blockHits = new ArrayList<>();
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        Vec3 norm = dir.normalize();

        int x = (int) Math.floor(start.x);
        int y = (int) Math.floor(start.y);
        int z = (int) Math.floor(start.z);

        int endX = (int) Math.floor(end.x);
        int endY = (int) Math.floor(end.y);
        int endZ = (int) Math.floor(end.z);

        int stepX = norm.x >= 0 ? 1 : -1;
        int stepY = norm.y >= 0 ? 1 : -1;
        int stepZ = norm.z >= 0 ? 1 : -1;

        double tDeltaX = norm.x == 0 ? Double.MAX_VALUE : Math.abs(1.0 / norm.x);
        double tDeltaY = norm.y == 0 ? Double.MAX_VALUE : Math.abs(1.0 / norm.y);
        double tDeltaZ = norm.z == 0 ? Double.MAX_VALUE : Math.abs(1.0 / norm.z);

        double tMaxX = norm.x == 0 ? Double.MAX_VALUE : (stepX > 0 ? Math.ceil(start.x) - start.x : start.x - Math.floor(start.x)) / Math.abs(norm.x);
        double tMaxY = norm.y == 0 ? Double.MAX_VALUE : (stepY > 0 ? Math.ceil(start.y) - start.y : start.y - Math.floor(start.y)) / Math.abs(norm.y);
        double tMaxZ = norm.z == 0 ? Double.MAX_VALUE : (stepZ > 0 ? Math.ceil(start.z) - start.z : start.z - Math.floor(start.z)) / Math.abs(norm.z);
        for (int i = 0; i < length; i++) {
            BlockPos pos = new BlockPos(x, y, z);
            Vec3 worldPos = new Vec3(x + 0.5, y + 0.5, z + 0.5);

            boolean handledBySublevel = false;
            for (SubLevelAccess subLevel : subLevels) {
                if (subLevel.boundingBox().contains(worldPos.x, worldPos.y, worldPos.z)) {
                    Vec3 local = subLevel.logicalPose().transformPositionInverse(worldPos);
                    BlockPos localPos = BlockPos.containing(local);
                    BlockState localState = level.getBlockState(localPos);
                    if (!localState.isAir()) {
                        if (!localState.isCollisionShapeFullBlock(level, localPos) && localState.getBlock() != Blocks.WATER) {
                            VoxelShape shape = localState.getShape(level, localPos);
                            if (shape.isEmpty()) continue;
                            BlockHitResult hit = shape.clip(start, end, pos);
                            if (hit != null) {
                                blockHits.add(localPos);
                            }
                        } else blockHits.add(localPos);
                    }
                    handledBySublevel = true;
                    break;
                }
            }

            if (!handledBySublevel) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    if (!state.isCollisionShapeFullBlock(level, pos) && state.getBlock() != Blocks.WATER) {
                        VoxelShape shape = state.getShape(level, pos);
                        if (shape.isEmpty()) continue;
                        BlockHitResult hit = shape.clip(start, end, pos);
                        if (hit != null) {
                            blockHits.add(pos);
                        }
                    } else blockHits.add(pos);
                }
            }

            if (x == endX && y == endY && z == endZ) break;

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
        return blockHits;
    }
}
