package org.patryk3211.powergrid.electricity.solarpanel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

public abstract class SolarPanelBlockEntity extends ElectricBlockEntity {
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
    private float AMBIENT_TEMP = -2000f;
    private float panelTiltDeg = 0;
    private float panelAzimuthDeg = 90;
    private int rayCastDelay = 0;
    private float sunVisablity = 0;
    protected int totalCells = CELLS_IN_SERIES;


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

        if (firstTick) {
            AMBIENT_TEMP = ThermalBehaviour.getAmbientTemperature(world, this.getBlockPos());
            if (AMBIENT_TEMP <= ThermalBehaviour.ABSOLUTE_ZERO)
                AMBIENT_TEMP = 22f;
            getPlacedBlockRotation();
            firstTick = false;
        }

        if (world.isRaining() && world.isThundering()) {
            cloudCover = .925f;
        } else if (world.isRaining()) {
            cloudCover = .85f;
        } else {
            cloudCover = 0;
        }

        var irradiance = getIrradiance(getAM(world), cloudCover, this.getBlockPos().getY(), panelTiltDeg, panelAzimuthDeg, world);
        var cellTemp = getCellTemp(irradiance);
        var Vt = 8.617e-5 * (cellTemp + 273.15);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt);
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
//            System.out.println("Cell Temp: " + cellTemp);
//            System.out.println("Single cell voltage: " + Voc_t);
//            System.out.println("Single cell current: " + cellCurrent);
//            System.out.println("Vt: " + Vt);
//            System.out.println("Current irradiance: " + irradiance);
//            System.out.println("AM: " + getAM(world));
//            System.out.println("azimuth: " + panelAzimuthDeg);
//            System.out.println("tilt: " + panelTiltDeg);
//            System.out.println();
            temp = 0;
        }
        super.electricalTick();
    }

    public static double[] getTempAdjusted(double irradiance, double cellTemp, double Vt) {
        var Isc_T = SHORT_CURRENT * STRINGS_IN_PARALLEL * (irradiance / 1000) * (1 + ALPHAISC * (cellTemp - 25));
        if (Isc_T <= 0) return new double[]{0, 0};
        var Voc_base = IDEALITY * Vt * Math.log(Isc_T / I_O + 1);
        var Voc_T = Voc_base + BETAVOC * (cellTemp - 25);
        return new double[]{Isc_T, Voc_T};
    }

    public double getCellTemp(double Irradiance){
        return AMBIENT_TEMP + (NOCT - 20) * (Irradiance / 800);
    }

    public double getIrradiance(double AM, double cloudCover, int YPos, float panelTiltDeg, float panelAzimuthDeg, Level world) {
        if (AM == Double.POSITIVE_INFINITY) return 0;
        var transmisttance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level

        double dayAngle = world.getSunAngle(0);
        if (dayAngle > Math.PI) dayAngle -= 2 * Math.PI;
        double sunAzimuthRad = Math.PI + dayAngle;

        double sunElevationRad = Math.asin(Math.max(0, Math.cos(world.getSunAngle(0))));
        if (rayCastDelay-- == 0){
            sunVisablity = sunRaycast(world, sunAzimuthRad, sunElevationRad);
            rayCastDelay = world.random.nextInt(41) + 10;
        }

        double tiltRad = Math.toRadians(panelTiltDeg);
        double panelAzimuthRad = Math.toRadians(panelAzimuthDeg);

        double cosIncidence = Math.sin(sunElevationRad) * Math.cos(tiltRad) + Math.cos(sunElevationRad)
                * Math.cos(sunAzimuthRad - panelAzimuthRad) * Math.sin(tiltRad);

        cosIncidence = Math.max(0, cosIncidence);

        var diffuseLight = 0.1 * irradiance * (1 + cloudCover) * ((1 + Math.cos(tiltRad)) / 2);
        var reflected = 0.15 * irradiance * ((1 - Math.cos(tiltRad)) / 2.0);

        return (irradiance * sunVisablity) * transmisttance * cosIncidence + diffuseLight +  reflected;
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

    public float sunRaycast(Level world, double sunAzimuthRad, double sunElevationRad){
        var blockPos = getBlockPos();
        int castLength = 0;
        ChunkAccess chunk;
        var sunDir = new Vec3(Math.cos(sunElevationRad) * Math.sin(sunAzimuthRad),
                sunElevationRad, 0);
        var sunX = Math.cos(sunElevationRad) * (sunAzimuthRad < Math.PI ? 1 : -1);
        var sunY = Math.sin(sunElevationRad);
        boolean positiveX = sunDir.x > 0;
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
        var centerBlockPos = getBlockPos().getCenter().add(0, -3f/16, 0);
        var end = centerBlockPos.add(new Vec3(sunX, sunY, 0).scale(castLength));
        ClipContext clipContext = new ClipContext(centerBlockPos, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.WATER, null);
        BlockHitResult result = level.clip(clipContext);
        if (result.getType() == HitResult.Type.MISS){
            return 1F;
        }
        if (result.getType() == HitResult.Type.BLOCK){
            //todo think about adding blocks that can pass light (glass, water, ice, leaves, trapdoors, slime/honey) but this would require more raycasting
            return 0F;
        }
        return 0;
    }

    public void getPlacedBlockRotation(){
        var face = this.getBlockState().getValue(Rotation4ElectricBlock.FACING).getOpposite();

        switch (face){
            case NORTH:
                panelAzimuthDeg = 0;
                panelTiltDeg = 90;
                break;
            case SOUTH:
                panelAzimuthDeg = 180;
                panelTiltDeg = 90;
                break;
            case EAST:
                panelAzimuthDeg = 90;
                panelTiltDeg = 90;
                break;
            case WEST:
                panelAzimuthDeg = 270;
                panelTiltDeg = 90;
                break;
            case UP:
                panelAzimuthDeg = 0;
                panelTiltDeg = 0;
                break;
            case DOWN:
                panelAzimuthDeg = 0;
                panelTiltDeg = 180;
                break;
            default:
                break;
        }
    }

}
