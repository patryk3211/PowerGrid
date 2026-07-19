package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedTags;

import java.util.ArrayList;
import java.util.List;

public class SolarHelper {
    public static final int SOLAR_CONSTANT = 1361;
    public static final int CELLS_IN_SERIES = 48;
    protected static final float NOCT = 52;
    public static final double DIFFUSE_FRAC = .12;
    public static final double ALBEDO_FRAC = .08;
    public static final double IO_REF = 4.07e-6;
    // Diode ideality factor (per cell)
    public static final double IDEALITY = 1.5;

    // All values below are per panel, not per cell
    public static final double IPV_REF = 3.5;
    public static final double RS = 0.05;
    public static final double RSH_REF = 600.0;
    public static final boolean RSH_SCALES_WITH_IRRADIANCE = true;

    private static boolean showDebugLines = true;

    public record DDAHit(BlockPos worldOrLocalPos, AbstractContraptionEntity contraption) {}

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

    public static List<DDAHit> DDA(Level level, Vec3 start, Vec3 end) {
        if (level.isClientSide()) return new ArrayList<>();
        ServerLevel serverWorld = (ServerLevel) level;
        var checkBox = new AABB(start, end);
        List<AbstractContraptionEntity> candidates = level.getEntitiesOfClass(AbstractContraptionEntity.class, checkBox);
        List<DDAHit> hits = new ArrayList<>();
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        Vec3 norm = dir.normalize();

        int x = Mth.floor(start.x);
        int y = Mth.floor(start.y);
        int z = Mth.floor(start.z);

        int endX = Mth.floor(end.x);
        int endY = Mth.floor(end.y);
        int endZ = Mth.floor(end.z);

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
            BlockState state = level.getBlockState(pos);
            var handledByContraption = false;
            if (showDebugLines) debugLines(serverWorld, pos, ParticleTypes.SOUL_FIRE_FLAME);
            for (AbstractContraptionEntity candidate : candidates) {
                Contraption contraption = candidate.getContraption();
                if (contraption == null) continue;

                Vec3 localStart = ContraptionCollider.worldToLocalPos(start, candidate);
                Vec3 localEnd = ContraptionCollider.worldToLocalPos(end, candidate);

                Vec3 worldPos = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                Vec3 local = ContraptionCollider.worldToLocalPos(worldPos, candidate.getAnchorVec(), candidate.getContraption().entity.getRotationState());
                BlockPos localPos = BlockPos.containing(local);

                StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(localPos);
                if (info != null && !info.state().isAir()) {
                    VoxelShape shape = info.state().getShape(level, localPos);
                    if (!shape.isEmpty()) {
                        BlockHitResult localHit = shape.clip(localStart, localEnd, localPos);
                        if (localHit != null) {
                            hits.add(new DDAHit(localPos, candidate));
                            handledByContraption = true;
                            if (showDebugLines) debugLines(serverWorld, worldPos, ParticleTypes.FLAME);
                            break;
                        }
                    }
                }
            }

            if (!handledByContraption){
                if (!state.isAir()) {
                    if (!state.isCollisionShapeFullBlock(level, pos) && state.getBlock() != Blocks.WATER) {
                        VoxelShape shape = state.getShape(level, pos);
                        if (shape.isEmpty()) continue;
                        BlockHitResult hit = shape.clip(start, end, pos);
                        if (hit != null) {
                            hits.add(new DDAHit(pos, null));
                            if (showDebugLines) debugLines(serverWorld, pos, ParticleTypes.FLAME);
                        }
                    } else {
                        hits.add(new DDAHit(pos, null));
                        if (showDebugLines) debugLines(serverWorld, pos, ParticleTypes.FLAME);
                    }
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
        return hits;
    }

    public static boolean skyCheck(Level world, BlockPos pos) {
        if (!world.canSeeSky(pos)){
            var topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            var list = DDA(world, pos.getCenter(), pos.getCenter().add(0, topY - pos.getY(), 0));
            boolean hit = false;
            for (DDAHit result : list) {
                BlockState blockState;
                if (result.contraption() != null) {
                    blockState = result.contraption().getContraption().getBlocks().get(result.worldOrLocalPos()).state();
                } else {
                    blockState = world.getBlockState(result.worldOrLocalPos());
                }

                if (blockState.is(ModdedBlocks.SOLAR_PANEL.get())) {
                    if (result.worldOrLocalPos().equals(pos)) {
                        continue;
                    } else {
                        hit = true;
                        break;
                    }
                }

                if (blockState.is(ModdedTags.Block.SOLAR_QUARTER_LIGHT.tag)) continue;
                if (blockState.is(ModdedTags.Block.SOLAR_HALF_LIGHT.tag)) continue;
                if (blockState.is(ModdedTags.Block.SOLAR_3QUARTER_LIGHT.tag)) continue;
                if (blockState.is(ModdedTags.Block.SOLAR_FULL_LIGHT.tag)) continue;
                hit = true;
                break;
            }
            if (hit){
                return false;
            } else return true;
        }
        return true;
    }

    public static void electricalProperties(double irradiance, ISolarPropertyConsumer controller) {
        double Ipv = IPV_REF * (irradiance / 1000.0);

        double Rsh;
        if (RSH_SCALES_WITH_IRRADIANCE && irradiance > 1.0) {
            Rsh = RSH_REF * (1000.0 / irradiance);
        } else {
            Rsh = RSH_REF;
        }

        controller.accept(RS, Rsh, Ipv);
    }

//    public static void electricalProperties(double irradiance, float ambientTemp, ISolarPropertyConsumer controller) {
//        var cellTemp = getCellTemp(irradiance, ambientTemp);
//        var Vt = 8.617e-5 * (cellTemp + 273.15);
//        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt, STRINGS_IN_PARALLEL);
//        double Isc_t = adjusted[0];
//        double Voc_t = adjusted[1] * CELLS_IN_SERIES;
//
//        double Vmp_T = Vmp * (1 - BETAVOC * (cellTemp - 25));
//        double Imp_T = Imp * (1 - ALPHAISC * (cellTemp - 25));
//
////        double Rs = .5;
////        double Rsh = 350;
//        double Rs = (Voc_t - Vmp_T) / (16 * Imp_T);
//        double Rsh = 4 * Vmp_T / (Isc_t - Imp_T);
//        double Ipv = Isc_t * (Rsh + Rs) / Rsh;
//
//        controller.accept(Rs, Rsh, Ipv);
//    }

    private static void debugLines(ServerLevel serverLevel, Vec3 pos, SimpleParticleType particle) {
        serverLevel.sendParticles(particle, pos.x, pos.y, pos.z, 0, 0, 0, 0, 0);
    }

    private static void debugLines(ServerLevel serverLevel, BlockPos pos, SimpleParticleType particle) {
        debugLines(serverLevel, new Vec3(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5), particle);
    }
}
