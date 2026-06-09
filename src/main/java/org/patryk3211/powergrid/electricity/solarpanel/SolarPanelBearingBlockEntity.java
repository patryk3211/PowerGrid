package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlockEntity.*;

public class SolarPanelBearingBlockEntity extends ElectricKineticBlockEntity implements IBearingBlockEntity, IDisplayAssemblyExceptions {
    protected ScrollOptionBehaviour<RotationMode> movementMode;
    protected ControlledContraptionEntity movedContraption;
    protected float angle;
    protected boolean running;
    protected boolean assembleNextTick;
    protected float clientAngleDiff;
    protected AssemblyException lastException;
    protected double sequencedAngleLimit;
    protected VoltageSourceCoupling sourceCoupling;
    SolarPanelBearingContraption contraption;
    private float prevAngle;
    private float cloudCover = 0;
    private boolean firstTick = true;
    private float AMBIENT_TEMP = -2000f;
    private int rayCastDelay = 0;
    private float sunVisibility = 0;
    private float startAngle = 0;
    private int temp = 0;
    protected SolarPanelBearingBlockScrollBehaviour parallelNumbers;
    private Vector3d panelNormal;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        parallelNumbers = new SolarPanelBearingBlockScrollBehaviour(this);
        behaviours.add(parallelNumbers);
    }

    public SolarPanelBearingBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(3);
        sequencedAngleLimit = -1;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        sourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), 0.01f);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (movedContraption != null && !level.isClientSide)
            sendData();
    }

    @Override
    public void tick() {
        super.tick();

        prevAngle = angle;
        if (level.isClientSide)
            clientAngleDiff /= 2;

        if (!level.isClientSide && assembleNextTick) {
            assembleNextTick = false;
            if (running) {
                if (speed == 0 && (movedContraption == null || movedContraption.getContraption().getBlocks().isEmpty())) {
                    if (movedContraption != null)
                        movedContraption.getContraption().stop(level);
                    disassemble();
                    return;
                }
            } else {
                assemble();
            }
        }

        if (!running)
            return;

        if (!(movedContraption != null && movedContraption.isStalled())) {
            float angularSpeed = getAngularSpeed();
            if (sequencedAngleLimit >= 0) {
                angularSpeed = (float) Mth.clamp(angularSpeed, -sequencedAngleLimit, sequencedAngleLimit);
                sequencedAngleLimit = Math.max(0, sequencedAngleLimit - Math.abs(angularSpeed));
            }
            float newAngle = angle + angularSpeed;
            angle = (float) (newAngle % 360);
        }

        applyRotation();
    }

    @Override
    public void electricalTick() {
        if (SableCompanion.INSTANCE.isInPlotGrid(level, this.getBlockPos().getCenter())){
            subWorldTick();
            return;
        }

        if (contraption == null) {
            if (movedContraption != null) {
                contraption = (SolarPanelBearingContraption) movedContraption.getContraption();
            } else return;
        }
        if (!running) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(10000);
            return;
        }
        var world = getLevel();
        if (world == null) return;
        if (sourceCoupling == null) return;
        if (firstTick) {
            AMBIENT_TEMP = ThermalBehaviour.getAmbientTemperature(world, this.getBlockPos());
            if (AMBIENT_TEMP <= ThermalBehaviour.ABSOLUTE_ZERO)
                AMBIENT_TEMP = 22f;
            firstTick = false;
        }

        if (world.isRaining() && world.isThundering()) {
            cloudCover = .925f;
        } else if (world.isRaining()) {
            cloudCover = .85f;
        } else {
            cloudCover = 0;
        }


        Direction bearingFacing = getBlockState().getValue(SolarPanelBearingBlock.FACING);
        Vector3d rotAxis = new Vector3d(
                bearingFacing.getNormal().getX(),
                bearingFacing.getNormal().getY(),
                bearingFacing.getNormal().getZ()
        );
        panelNormal = new Vector3d(contraption.panelNormal);
        new Quaterniond().fromAxisAngleRad(rotAxis, Math.toRadians(-angle)).transform(panelNormal);
        panelNormal.normalize();

        int stringsInParallel = parallelNumbers.getDivisor();
        var irradiance = getIrradiance(getAM(world), cloudCover, this.getBlockPos().getY(), world);
        var cellTemp = getCellTemp(irradiance);
        var Vt = 8.617e-5 * (cellTemp + 273.15);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt);
        double cellCurrent = adjusted[0];
        double Voc_t = adjusted[1];
        double Voc_panel = Voc_t * (CELLS_IN_SERIES * ((double) contraption.getPanelBlocks() / stringsInParallel));

        if (cellCurrent <= 0) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(1e6f);
            return;
        }

        double panelResistance = (cellCurrent > 0) ? Voc_panel / cellCurrent : 1e6;
        sourceCoupling.setVoltage((float) Voc_panel);
        sourceCoupling.setResistance((float) panelResistance);

        if (temp++ == 20){
            System.out.println("Cell Temp: " + cellTemp);
            System.out.println("Single cell voltage: " + Voc_t);
            System.out.println("Single cell current: " + cellCurrent);
            System.out.println("Vt: " + Vt);
            System.out.println("Current irradiance: " + irradiance);
            System.out.println("AM: " + getAM(world));
            System.out.println();
            temp = 0;
        }

        super.electricalTick();
    }

    private void subWorldTick() {
        var world = getLevel();
        if (world == null || world.isClientSide()) return;
        if (sourceCoupling == null) return;
        if (contraption == null) {
            if (movedContraption != null) {
                contraption = (SolarPanelBearingContraption) movedContraption.getContraption();
            } else return;
        }
        if (!running) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(10000);
            return;
        }

        var d = SableCompanion.INSTANCE.projectOutOfSubLevel(world, new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ()));
        var pos = new BlockPos((int)d.x, (int)d.y, (int)d.z);

        AMBIENT_TEMP = ThermalBehaviour.getAmbientTemperature(world, pos);
        if (AMBIENT_TEMP <= ThermalBehaviour.ABSOLUTE_ZERO)
            AMBIENT_TEMP = 22f;

        if (world.isRaining() && world.isThundering()) {
            cloudCover = .925f;
        } else if (world.isRaining()) {
            cloudCover = .85f;
        } else {
            cloudCover = 0;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel == null)
            return;

        Direction bearingFacing = getBlockState().getValue(SolarPanelBearingBlock.FACING);
        Vector3d rotAxis = new Vector3d(bearingFacing.getNormal().getX(), bearingFacing.getNormal().getY(),
                bearingFacing.getNormal().getZ());
        panelNormal = new Vector3d(contraption.panelNormal);
        new Quaterniond().fromAxisAngleRad(rotAxis, Math.toRadians(angle)).transform(panelNormal);
        subLevel.logicalPose().orientation().transform(panelNormal);
        panelNormal.normalize();

        var irradiance = getIrradiance(getAM(world), cloudCover, pos.getY(), world);
        var cellTemp = getCellTemp(irradiance);
        var Vt = 8.617e-5 * (cellTemp + 273.15);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt);
        double cellCurrent = adjusted[0];
        double Voc_t = adjusted[1];
        double Voc_panel = Voc_t * CELLS_IN_SERIES;

        if (cellCurrent <= 0) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(1e6f);
            return;
        }

        double panelResistance = (cellCurrent > 0) ? Voc_panel / cellCurrent : 1e6;
        sourceCoupling.setVoltage((float) Voc_panel);
        sourceCoupling.setResistance((float) panelResistance);

        if (temp++ == 20){
            System.out.println("Cell Temp: " + cellTemp);
            System.out.println("Single cell voltage: " + Voc_t);
            System.out.println("Single cell current: " + cellCurrent);
            System.out.println("Vt: " + Vt);
            System.out.println("Current irradiance: " + irradiance);
            System.out.println("AM: " + getAM(world));

            System.out.println();
            temp = 0;
        }
        super.electricalTick();
    }

    public double[] getTempAdjusted(double irradiance, double cellTemp, double Vt) {
        int stringsInParallel = parallelNumbers.getDivisor();
        var Isc_T = SolarPanelBlockEntity.SHORT_CURRENT * stringsInParallel * (irradiance / 1000) * (1 + ALPHAISC * (cellTemp - 25));
        if (Isc_T <= 0) return new double[]{0, 0};
        var Voc_base = IDEALITY * Vt * Math.log(Isc_T / (I_O * stringsInParallel) + 1);
        var Voc_T = Voc_base + BETAVOC * (cellTemp - 25);
        return new double[]{Isc_T, Voc_T};
    }

    public double getCellTemp(double Irradiance){
        return AMBIENT_TEMP + (NOCT - 20) * (Irradiance / 800);
    }

    public double getIrradiance(double AM, double cloudCover, int YPos, Level world) {
        if (AM == Double.POSITIVE_INFINITY) return 0;
        var transmisttance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level

        if (rayCastDelay-- == 0){
            sunVisibility = sunRaycast(world);
            rayCastDelay = world.random.nextInt(41) + 10;
        }

        double sunAngle = world.getSunAngle(0);
        Vector3d sunDir = new Vector3d(-Math.sin(sunAngle), Math.cos(sunAngle), 0);
        if (sunDir.y <= 0) return 0;

        double cosIncidence = Math.max(0, sunDir.dot(panelNormal));
        cosIncidence = Math.max(0, cosIncidence);
        double diffuseLight = 0.1 * irradiance * (1 + cloudCover) * ((1 + panelNormal.y()) / 2);
        double reflected = 0.15 * irradiance * ((1 - panelNormal.y()) / 2.0);

        return (irradiance * sunVisibility) * transmisttance * cosIncidence + diffuseLight +  reflected;
    }

    public double getAM(Level world){
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

    public float sunRaycast(Level world) {

        var blockPos = getBlockPos();
        int castLength = 0;
        ChunkAccess chunk;

        double sunAngle = world.getSunAngle(0);
        double sunX = -Math.sin(sunAngle);
        double sunY = Math.cos(sunAngle);
        boolean positiveX = -Math.sin(world.getSunAngle(0)) > 0;
        for (int i = 1; i <= 10; i++) {
            int xOffset = (positiveX ? i : -i) * 16;
            chunk = world.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(blockPos.getX() + xOffset),
                    SectionPos.blockToSectionCoord(blockPos.getZ()));
            if (chunk != null) {
                castLength = i * 16;
            } else {
                break;
            }
        }
        var centerPanelPos = getContraptionCenter(movedContraption);
        var end = centerPanelPos.add(new Vec3(sunX, sunY, 0).scale(castLength));
        var results = DDA(world, centerPanelPos, end);
        float returnValue = 1;
        for (BlockPos result : results) {
            var blockState = world.getBlockState(result);

            if (blockState.is(ModdedTags.Block.GLASS_BLOCK.tag) || blockState.is(ModdedTags.Block.GLASS_PANE.tag)) {
                returnValue *= .8f;
                continue;
            }
            if (blockState.is(Blocks.WATER)) {
                returnValue *= .5f;
                continue;
            }
            if (blockState.is(BlockTags.LEAVES)) {
                returnValue *= .2f;
                continue;
            }
            if (blockState.is(Blocks.IRON_BARS)) {
                returnValue *= 9f / 16;
                continue;
            }

            returnValue = 0;
            break;
        }
        return returnValue;
    }

    public static List<BlockPos> DDA(Level level, Vec3 start, Vec3 end) {
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

    public void getPlacedBlockRotation(){
        var face = this.getBlockState().getValue(Rotation4ElectricBlock.FACING).getOpposite();
        var n = face.getNormal();
        panelNormal = new Vector3d(n.getX(), n.getY(), n.getZ());
    }

    private float calculateStartAngle(BearingContraption contraption) {
        for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry
                : contraption.getBlocks().entrySet()) {

            BlockState state = entry.getValue().state();
            if (!state.is(ModdedBlocks.SOLAR_PANEL.get())) continue;

            Direction facing = state.getValue(SolarPanelBearingBlock.FACING).getOpposite();
            if (state.getValue(SolarPanelBearingBlock.FACING).getAxis() == Direction.Axis.X){
                return switch (facing) {
                    case UP    ->   0f;
                    case EAST ->  90f;
                    case DOWN  -> 180f;
                    case WEST -> 270f;
                    default    ->   0f;
                };
            }

            if (state.getValue(SolarPanelBearingBlock.FACING).getAxis() == Direction.Axis.Y){
                return switch (facing) {
                    case UP    ->   0f;
                    case EAST ->  90f;
                    case DOWN  -> 180f;
                    case WEST -> 270f;
                    case NORTH -> 90f;
                    case SOUTH -> 270f;
                    default    ->   0f;
                };
            }

            if (state.getValue(SolarPanelBearingBlock.FACING).getAxis() == Direction.Axis.Z){
                if (getBlockState().getValue(SolarPanelBearingBlock.FACING).getAxis() == Direction.Axis.Y) {
                    return switch (facing) {
                        case NORTH ->   0f;
                        case SOUTH -> 180f;
                        case UP    ->   0f;
                        case DOWN  -> 180f;
                        default    ->   0f;
                    };
                }
                return switch (facing) {
                    case UP    ->   0f;
                    case DOWN  -> 180f;
                    case NORTH -> 90f;
                    case SOUTH -> 270f;
                    default    ->   0f;
                };
            }

        }
        return 0f;
    }

    private Vec3 getContraptionCenter(AbstractContraptionEntity entity) {
        double x = 0, y = 0, z = 0;
        int count = 0;
        for (BlockPos local : entity.getContraption().getBlocks().keySet()) {
            x += local.getX() + 0.5;
            y += local.getY() + 0.5;
            z += local.getZ() + 0.5;
            count++;
        }
        if (count == 0) return entity.position();
        Vec3 localCenter = new Vec3(x / count, y / count, z / count);
        return entity.toGlobalVector(localCenter, 1.0f);
    }

    public void assemble() {
        if (level == null) return;
        if (!(level.getBlockState(worldPosition)
                .getBlock() instanceof SolarPanelBearingBlock))
            return;

        Direction direction = getBlockState().getValue(SolarPanelBearingBlock.FACING);
        contraption = new SolarPanelBearingContraption(direction);
        try {
            if (!contraption.assemble(level, worldPosition))
                return;

            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = ControlledContraptionEntity.create(level, this, contraption);
        BlockPos anchor = worldPosition.relative(direction);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        if (contraption.containsBlockBreakers())
            award(AllAdvancements.CONTRAPTION_ACTORS);

        startAngle = calculateStartAngle(contraption);

        running = true;
        angle = 0;

        if (contraption == null) {
            if (movedContraption != null) {
                contraption = (SolarPanelBearingContraption) movedContraption.getContraption();
            }
        }

        parallelNumbers.refreshDivisors(contraption.getPanelBlocks());
        sendData();
        getPlacedBlockRotation();
    }

    public void disassemble() {
        if (!running && movedContraption == null)
            return;
        angle = 0;
        sequencedAngleLimit = -1;
        if (movedContraption != null) {
            movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
        }

        movedContraption = null;
        running = false;
        assembleNextTick = false;
        sendData();
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return lastException;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return Mth.lerp(partialTicks + .5f, prevAngle, angle);
        if (movedContraption == null || movedContraption.isStalled() || !running)
            partialTicks = 0;
        float angularSpeed = getAngularSpeed();
        if (sequencedAngleLimit >= 0)
            angularSpeed = (float) Mth.clamp(angularSpeed, -sequencedAngleLimit, sequencedAngleLimit);
        return Mth.lerp(partialTicks, angle, angle + angularSpeed);
    }

    public float getAngularSpeed() {
        float speed = convertToAngular(getSpeed());
        if (getSpeed() == 0)
            speed = 0;
        if (level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed;
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = true;
        sequencedAngleLimit = -1;

        if (movedContraption != null && Math.signum(prevSpeed) != Math.signum(getSpeed()) && prevSpeed != 0) {
            if (!movedContraption.isStalled()) {
                angle = Math.round(angle);
                applyRotation();
            }
            movedContraption.getContraption()
                    .stop(level);
        }

        if (sequenceContext != null
                && sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE)
            sequencedAngleLimit = sequenceContext.getEffectiveValue(getTheoreticalSpeed());
    }

    protected void applyRotation() {
        if (movedContraption == null)
            return;
        movedContraption.setAngle(angle);
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(BlockStateProperties.FACING))
            movedContraption.setRotationAxis(blockState.getValue(BlockStateProperties.FACING)
                    .getAxis());
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putFloat("startingAngle", startAngle);
        compound.putBoolean("Running", running);
        compound.putFloat("Angle", angle);
        if (sequencedAngleLimit >= 0)
            compound.putDouble("SequencedAngleLimit", sequencedAngleLimit);
        compound.putInt("StringsInParallel", parallelNumbers.getDivisor());
        compound.putInt("PanelCount", parallelNumbers.getPanelCount());
        AssemblyException.write(compound, registries, lastException);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (wasMoved) {
            super.read(compound, registries, clientPacket);
            return;
        }

        float angleBefore = angle;
        running = compound.getBoolean("Running");
        angle = compound.getFloat("Angle");
        sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1;
        startAngle = compound.getFloat("startingAngle");
        lastException = AssemblyException.read(compound, registries);
        if (compound.contains("PanelCount"))
            parallelNumbers.refreshDivisors(compound.getInt("PanelCount"));
        super.read(compound, registries, clientPacket);
        if (compound.contains("StringsInParallel"))
            parallelNumbers.setByDivisor(compound.getInt("StringsInParallel"));
        if (!clientPacket)
            return;
        if (running) {
            if (movedContraption == null || !movedContraption.isStalled()) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore, angle);
                angle = angleBefore;
            }
        } else
            movedContraption = null;
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
        BlockState blockState = getBlockState();
        if (!(contraption.getContraption() instanceof SolarPanelBearingContraption))
            return;
        if (!blockState.hasProperty(SolarPanelBearingBlock.FACING))
            return;

        this.movedContraption = contraption;
        setChanged();
        BlockPos anchor = worldPosition.relative(blockState.getValue(SolarPanelBearingBlock.FACING));
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        if (!level.isClientSide) {
            this.running = true;
            sendData();
        }
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    @Override
    public void setAngle(float forcedAngle) {
        angle = forcedAngle;
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        return movedContraption == contraption;
    }

    @Override
    public void onStall() {
        if (!level.isClientSide)
            sendData();
    }

    @Override
    public void remove() {
        if (!level.isClientSide)
            disassemble();
        super.remove();
    }

    @Override
    public boolean isValid() {
        return !isRemoved();
    }

    @Override
    public BlockPos getBlockPosition() {
        return worldPosition;
    }

    @Override
    protected boolean syncSequenceContext() {
        return true;
    }
}
