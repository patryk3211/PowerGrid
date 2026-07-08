package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

import java.util.List;
import java.util.Map;

import static org.patryk3211.powergrid.electricity.solarpanel.SolarHelper.*;
import static org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlockEntity.*;

public class SolarPanelBearingBlockEntity extends ElectricKineticBlockEntity implements IBearingBlockEntity, IDisplayAssemblyExceptions {
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
    private boolean firstTick = true;
    private float ambientTemp = -2000f;
    private int rayCastDelay = 0;
    private float sunVisibility = 0;
    private boolean skyVisible = false;
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
            angle = newAngle % 360;
        }

        applyRotation();
    }

    @Override
    public void electricalTick() {
        var world = getLevel();
        if (world == null) return;
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

        if (sourceCoupling == null) return;
        if (firstTick) {
            ambientTemp = ThermalBehaviour.getAmbientTemperature(world, this.getBlockPos());
            if (ambientTemp <= ThermalBehaviour.ABSOLUTE_ZERO)
                ambientTemp = 22f;
            firstTick = false;
        }
        float cloudCover = getWeather(world);

        Vec3 localDir = new Vec3(contraption.panelNormal.x, contraption.panelNormal.y, contraption.panelNormal.z);
        Vec3 worldTip = movedContraption.toGlobalVector(localDir, 1.0f);
        Vec3 worldOrigin = movedContraption.toGlobalVector(Vec3.ZERO, 1.0f);
        Vec3 worldDir = worldTip.subtract(worldOrigin).normalize();
        panelNormal = new Vector3d(worldDir.x, worldDir.y, worldDir.z);

        var subLevel = SableCompanion.INSTANCE.getContaining(this);
        if (subLevel != null) {
            subLevel.logicalPose().orientation().transform(panelNormal);
            panelNormal.normalize();
        }

        var irradiance = getIrradiance(getAM(world), cloudCover, this.getBlockPos().getY(), world);
        var cellTemp = getCellTemp(irradiance, ambientTemp);
        var Vt = 8.617e-5 * (cellTemp + 273.15);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt, parallelNumbers.getDivisor());
        double cellCurrent = adjusted[0];
        double Voc_t = adjusted[1];
        double Voc_panel = Voc_t * (CELLS_IN_SERIES * ((double) contraption.getPanelBlocks() / parallelNumbers.getDivisor()));

        if (cellCurrent <= 0) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(1e6f);
            return;
        }

        double panelResistance = (cellCurrent > 0) ? Voc_panel / cellCurrent : 1e6;
        sourceCoupling.setVoltage(Voc_panel);
        sourceCoupling.setResistance((float) panelResistance);

        super.electricalTick();
    }

    public double getIrradiance(double AM, double cloudCover, int YPos, Level world) {
        if (AM == Double.POSITIVE_INFINITY) return 0;
        var transmittance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level

        if (rayCastDelay-- == 0){
            sunVisibility = sunRaycast(world);
            rayCastDelay = world.random.nextInt(41) + 10;
            skyVisible = skyCheck(world, BlockPos.containing(JOMLConversion.toMojang(getContraptionCenter(movedContraption)
                    .add(panelNormal.x, panelNormal.y, panelNormal.z))));
        }

        double sunAngle = world.getSunAngle(0);
        Vector3d sunDir = new Vector3d(-Math.sin(sunAngle), Math.cos(sunAngle), 0);
        if (sunDir.y <= 0) return 0;

        double cosIncidence = Math.max(0, sunDir.dot(panelNormal));
        cosIncidence = Math.max(0, cosIncidence);
        double diffuseLight = DIFFUSE_FRAC * (Math.max(0, sunDir.y) * irradiance * transmittance)
                * (1 + cloudCover) * ((1 + panelNormal.y()) / 2);
        double reflected = ALBEDO_FRAC * (Math.max(0, sunDir.y) * irradiance * transmittance) * ((1 - panelNormal.y()) / 2.0);

        if (!skyVisible){
            diffuseLight = 0;
            reflected = 0;
        }
        return (irradiance * sunVisibility) * transmittance * cosIncidence + diffuseLight +  reflected;
    }

    public float sunRaycast(Level world) {
        var d = SableCompanion.INSTANCE.projectOutOfSubLevel(world, new Vector3d(this.getBlockPos().getX() + .5,
                this.getBlockPos().getY() + .5, this.getBlockPos().getZ() + .5));

        var blockPos = BlockPos.containing(d.x, d.y, d.z);
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
        var centerPanelPos = JOMLConversion.toMojang(SableCompanion.INSTANCE.projectOutOfSubLevel(world, getContraptionCenter(movedContraption)));
        var end = centerPanelPos.add(new Vec3(sunX, sunY, 0).scale(castLength));
        var results = DDA(world, centerPanelPos.add(new Vec3(panelNormal.x, panelNormal.y, panelNormal.z)), end);
        float returnValue = 1;
        for (DDAHit result : results) {
            BlockState blockState;
            if (result.contraption() != null) {
                blockState = result.contraption().getContraption().getBlocks().get(result.worldOrLocalPos()).state();
            } else {
                blockState = world.getBlockState(result.worldOrLocalPos());
            }

            if (blockState.is(ModdedTags.Block.SOLAR_QUARTER_LIGHT.tag)) {
                returnValue *= .25f;
                continue;
            }
            if (blockState.is(ModdedTags.Block.SOLAR_HALF_LIGHT.tag)) {
                returnValue *= .5f;
                continue;
            }
            if (blockState.is(ModdedTags.Block.SOLAR_3QUARTER_LIGHT.tag)) {
                returnValue *= .75f;
                continue;
            }
            if (blockState.is(ModdedTags.Block.SOLAR_FULL_LIGHT.tag)){
                returnValue *= 1f;
                continue;
            }
            returnValue = 0;
            break;
        }
        return returnValue;
    }

    public void getPlacedBlockRotation(){
        var face = this.getBlockState().getValue(Rotation4ElectricBlock.FACING).getOpposite();
        var n = face.getNormal();
        panelNormal = new Vector3d(n.getX(), n.getY(), n.getZ());
    }

    private Vector3d getContraptionCenter(AbstractContraptionEntity entity) {
        double x = 0, y = 0, z = 0;
        int count = 0;
        for (BlockPos local : entity.getContraption().getBlocks().keySet()) {
            x += local.getX() + 0.5;
            y += local.getY() + 0.5;
            z += local.getZ() + 0.5;
            count++;
        }
        if (count == 0){
            return JOMLConversion.toJOML(entity.position());
        }
        Vec3 localCenter = new Vec3(x / count, y / count, z / count);
        return JOMLConversion.toJOML(entity.toGlobalVector(localCenter, 1.0f));
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
