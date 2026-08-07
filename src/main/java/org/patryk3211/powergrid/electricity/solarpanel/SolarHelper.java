package org.patryk3211.powergrid.electricity.solarpanel;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
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
import org.joml.Vector3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SolarHelper {
    public static final int SOLAR_CONSTANT = 1361;
    public static final double IDEALITY = 1.5;

    // STC constants
    public static final double W_REF = 1000.0;
    public static final double T_REF = 25.0;

    public static final double ALPHA_ISC = 0.0005;
    public static final double BETA_VOC  = -0.0035;
    public static final double RS = 0.05;

    public static final double DIFFUSE_FRAC = .12;
    public static final double ALBEDO_FRAC = .08;

    private static boolean showDebugLines = true;

    public record DDAHit(BlockPos worldOrLocalPos, AbstractContraptionEntity contraption) {}

    public static double getCellTemp(double Irradiance, float AMBIENT_TEMP){
        return AMBIENT_TEMP + (noct() - 20) * (Irradiance / 800);
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
        var subLevels = SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(checkBox));
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
            Vec3 worldPos = new Vec3(x + 0.5, y + 0.5, z + 0.5);
            BlockState state = level.getBlockState(pos);
            if (showDebugLines) debugLines(serverWorld, pos, ParticleTypes.SOUL_FIRE_FLAME);

            var handledByContraption = false;
            for (AbstractContraptionEntity candidate : candidates) {
                Contraption contraption = candidate.getContraption();
                if (contraption == null) continue;

                Vec3 localStart = ContraptionCollider.worldToLocalPos(start, candidate);
                Vec3 localEnd = ContraptionCollider.worldToLocalPos(end, candidate);

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
                                hits.add(new DDAHit(localPos, null));
                                if (showDebugLines) debugLines(serverWorld, worldPos, ParticleTypes.FLAME);
                            }
                        } else {
                            hits.add(new DDAHit(localPos, null));
                            if (showDebugLines) debugLines(serverWorld, worldPos, ParticleTypes.FLAME);
                        }
                    }
                    handledBySublevel = true;
                    break;
                }
            }

            if (!handledBySublevel && !handledByContraption) {
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

    public static double[] IVCurve(double irradiance, double cellTempC, double v0, int panelsInSeries, int panelsInParallel) {
        double k = 1.380649e-23; // Boltzmann constant in J/K
        double q = 1.602176634e-19; // Elementary charge in C
        double dT = cellTempC - T_REF;
        double gRatio = Math.max(0, irradiance) / W_REF;
        if (gRatio <= 1e-6) return new double[] {0, 2e-2};
        double Isc = (gRatio * (iscRef() + ALPHA_ISC * iscRef() * dT)) * panelsInParallel;
        double logG = Math.log(Math.max(gRatio, 1e-6));
        double tV = k * (cellTempC + 273.15) / q;
        double delta = IDEALITY * cellCount() * tV;
        double Voc = (vocRef() + BETA_VOC * vocRef() * dT + delta * logG) * panelsInSeries;
        double Imp = (gRatio * (imp() + ALPHA_ISC * imp() * dT)) * panelsInParallel;
        double Vmp = (vmp() + BETA_VOC * vmp() * dT + delta * logG) * panelsInSeries;
        if (Voc <= 0 || Isc <= 0) return new double[] {0, 2e-2};

        double ratio = Math.min(0.999, Imp / Isc);
        double C2 = (Vmp / Voc - 1.0) / Math.log(1.0 - ratio);
        double C1 = (1.0 - ratio) * Math.exp(-Vmp / (C2 * Voc));

        double V = Math.max(0, Math.min(v0, Voc * 1.05));
        double expTerm = Math.exp(V / (C2 * Voc));
        double I = Isc * (1 - C1 * (expTerm - 1));
        double dIdV = -Isc * C1 / (C2 * Voc) * expTerm;

        double G = Math.max(1e-6, -dIdV);
        double G_floor = Isc / Voc;
        G = Math.max(G, G_floor);

        double Ieq = I + G * V;
        return new double[] {Ieq, G};
    }

    public static boolean skyCheck(Level world, BlockPos pos) {
        var subLevel = SableCompanion.INSTANCE.getContaining(world, new Vector3d(pos.getX(), pos.getY(), pos.getZ()));
        if (subLevel == null) {
            if (!world.canSeeSky(pos)) {
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
                return !hit;
            }
            return true;
        } else {
            var worldPos = BlockPos.containing(JOMLConversion.toMojang(SableCompanion.INSTANCE.projectOutOfSubLevel(world,
                    new Vector3d(pos.getX(), pos.getY(), pos.getZ()))));
            if (world.canSeeSky(worldPos) && world.canSeeSky(pos)) return true;
            var topYWorld = world.getHeight(Heightmap.Types.MOTION_BLOCKING, worldPos.getX(), worldPos.getZ());
            var topYSubLevel = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            boolean hit = false;
            if (topYSubLevel > pos.getY()) {
                var list = DDA(world, pos.getCenter(), pos.getCenter().add(0, topYSubLevel - pos.getY(), 0));
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
            }

            if (topYWorld > worldPos.getY()) {
                var list = DDA(world, worldPos.getCenter(), worldPos.getCenter().add(0, topYWorld - worldPos.getY(), 0));
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
            }
            return !hit;
        }
    }

    public static Vec3 getSolarPanelCenter(BlockPos self, Set<BlockPos> connectedPanels) {
        double sumX = self.getX();
        double sumY = self.getY();
        double sumZ = self.getZ();
        for (BlockPos pos : connectedPanels) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }
        int count = connectedPanels.size() + 1;
        double avgX = sumX / count;
        double avgY = sumY / count;
        double avgZ = sumZ / count;
        return new Vec3(avgX + 0.5, avgY + 0.5, avgZ + 0.5);
    }

    private static void debugLines(ServerLevel serverLevel, Vec3 pos, SimpleParticleType particle) {
        serverLevel.sendParticles(particle, pos.x, pos.y, pos.z, 0, 0, 0, 0, 0);
    }

    private static void debugLines(ServerLevel serverLevel, BlockPos pos, SimpleParticleType particle) {
        debugLines(serverLevel, new Vec3(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5), particle);
    }

    public static double vocRef() {
        return ModdedConfigs.server().electricity.solarPanelVoc.get();
    }
    public static double iscRef() {
        return ModdedConfigs.server().electricity.solarPanelIsc.get();
    }
    public static double vmp() {
        return ModdedConfigs.server().electricity.solarPanelVmp.get();
    }
    public static double imp() {
        return ModdedConfigs.server().electricity.solarPanelImp.get();
    }
    public static double cellCount() {
        return ModdedConfigs.server().electricity.solarPanelCellCount.get();
    }
    public static double noct() {
        return ModdedConfigs.server().electricity.solarPanelNOCT.get();
    }

}
