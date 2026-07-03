package org.patryk3211.powergrid.electricity.solarpanel;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import static org.patryk3211.powergrid.electricity.solarpanel.SolarHelper.*;

public class SolarPanelBlockEntity extends ElectricBlockEntity {
    protected VoltageSourceCoupling sourceCoupling;

    protected static final int SOLAR_CONSTANT = 1361;
    protected static final float SHORT_CURRENT = 9.2f;
    protected static final int CELLS_IN_SERIES = 48;
    protected static final int STRINGS_IN_PARALLEL = 1;
    protected static final float BETAVOC = -0.0023f;
    protected static final float ALPHAISC = 0.0005f;
    protected static final float NOCT = 52;
    protected static final double I_O = 1.11e-4;
    protected static final double IDEALITY = 1.8;

    private int temp = 0;
    private float cloudCover = 0;
    private boolean firstTick = true;
    private float ambientTemp = -2000f;
    private int rayCastDelay = 0;
    private float sunVisablity = 0;
    protected int totalCells = CELLS_IN_SERIES;
    private Vector3d panelNormal;

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        sourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), 0.01f);
    }

    @Override
    public void electricalTick() {
        var world = getLevel();
        if (world == null || world.isClientSide()) return;
        if (sourceCoupling == null) return;

        var d = SableCompanion.INSTANCE.projectOutOfSubLevel(world, new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ()));
        var pos = new BlockPos((int)d.x, (int)d.y, (int)d.z);

        if (firstTick) {
            ambientTemp = ThermalBehaviour.getAmbientTemperature(world, this.getBlockPos());
            if (ambientTemp <= ThermalBehaviour.ABSOLUTE_ZERO)
                ambientTemp = 22f;
            firstTick = false;
        }
        float cloudCover = getWeather(world);

        getPlacedBlockRotation();
        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel != null){
            subLevel.logicalPose().orientation().transform(panelNormal);
            panelNormal.normalize();
        }

        var irradiance = getIrradiance(getAM(world), cloudCover, this.getBlockPos().getY(), world);
        var cellTemp = getCellTemp(irradiance, ambientTemp);
        var Vt = 8.617e-5 * (cellTemp + 273.15);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt, STRINGS_IN_PARALLEL);
        double cellCurrent = adjusted[0];
        double Voc_t = adjusted[1];
        double Voc_panel = Voc_t * totalCells;

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
            //System.out.println("Single cell voltage: " + Voc_t);
            //System.out.println("Single cell current: " + cellCurrent);
            //System.out.println("Vt: " + Vt);
            System.out.println("Current irradiance: " + irradiance);
            System.out.println("AM: " + getAM(world));
            System.out.println("normal: " + panelNormal);
            //System.out.println("tilt: " + panelTiltDeg);
            System.out.println();
            temp = 0;
        }
        super.electricalTick();
    }

    public double getIrradiance(double AM, double cloudCover, int YPos, Level world) {
        if (AM == Double.POSITIVE_INFINITY) return 0;
        var transmisttance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level

        double sunAngle = world.getSunAngle(0);
        Vector3d sunDir = new Vector3d(-Math.sin(sunAngle), Math.cos(sunAngle), 0);
        if (sunDir.y <= 0) return 0;

        if (rayCastDelay-- == 0){
            sunVisablity = sunRaycast(world);
            rayCastDelay = world.random.nextInt(41) + 10;
        }

        double cosIncidence = Math.max(0, sunDir.dot(panelNormal));
        cosIncidence = Math.max(0, cosIncidence);
        double diffuseLight = 0.1 * (Math.max(0, sunDir.y) * irradiance * transmisttance)
                * (1 + cloudCover) * ((1 + panelNormal.y()) / 2);
        double reflected = 0.125 * (Math.max(0, sunDir.y) * irradiance * transmisttance) * ((1 - panelNormal.y()) / 2.0);
        if (!world.canSeeSky(getBlockPos())){
            var bp = getBlockPos();
            var topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, bp.getX(), bp.getZ());
            var list = DDA(world, bp.getCenter(), bp.getCenter().add(0, topY, 0));
            boolean hit = false;
            for (BlockPos result : list) {
                var blockState = world.getBlockState(result);
                if (blockState.is(ModdedBlocks.SOLAR_PANEL.get())) {
                    if (result.equals(this.getBlockPos())) {
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
                diffuseLight = 0;
                reflected = 0;
            }
        }
        return (irradiance * sunVisablity) * transmisttance * cosIncidence + diffuseLight + reflected;
    }

    public float sunRaycast(Level world){
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

        var centerBlockPos = blockPos.getCenter().add(0, 0, 0);
        var end = centerBlockPos.add(new Vec3(sunX, sunY, 0).scale(castLength));
        var results = DDA(world, centerBlockPos, end);
        float returnValue = 1;
        for (BlockPos result : results) {
            var blockState = world.getBlockState(result);

            if (blockState.is(ModdedBlocks.SOLAR_PANEL)) {
                if (result.equals(this.getBlockPos())) {
                    continue;
                } else {
                    returnValue = 0;
                    break;
                }
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
}
