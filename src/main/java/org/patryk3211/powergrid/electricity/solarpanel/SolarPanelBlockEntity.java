package org.patryk3211.powergrid.electricity.solarpanel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

public class SolarPanelBlockEntity extends ElectricBlockEntity {
    protected VoltageSourceCoupling sourceCoupling;

    private int temp =0;
    private float cloudCover = 0;

    private final int SOLAR_CONSTANT = 1361;
    private final int SHORT_CURRENT = 9;
    private final int CELLS_IN_SERIES = 48;
    private final int STRINGS_IN_PARALLEL = 1;
    private final float BETAVOC = -0.0023f;
    private final float ALPHAISC = 0.0005f;
    private final float NOCT = 45;
    private final double I_O = 1e-7;
    private final int AMBIENT_TEMP = 22; //todo This might be cool to change depending on biome
    private final double IDEALITY = 1.3;
    private final double THERMAL_VOLTAGE = 0.02585;
    private final float panelTiltDeg = 35;
    private final float panelAzimuthDeg = 90;

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        sourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), 0.01f);
    }

    @Override
    public void tick() {
        super.tick();
        var world = getLevel();
        if (world == null || world.isClientSide()) return;
        if (world.isRaining() && world.isThundering()) {
            cloudCover = .95f;
        } else if (world.isRaining()) {
            cloudCover = .875f;
        } else {
            cloudCover = 0;
        }

        var irradiance = getIrradiance(getAM(world), cloudCover, this.getBlockPos().getY(), panelTiltDeg, panelAzimuthDeg, world);
        var cellTemp = getCellTemp(irradiance);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp);
        double cellCurrent = adjusted[0];
        double Voc_t = adjusted[1];
        double Voc_panel = Voc_t * CELLS_IN_SERIES;

        if (cellCurrent <= 0) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(1e6f);
            return;
        }

        double terminalVoltage = sourceCoupling.getVoltage();
        double cellVoltage = Math.min(terminalVoltage / CELLS_IN_SERIES, 500 * IDEALITY * THERMAL_VOLTAGE);
        double expTerm = Math.exp(cellVoltage / (IDEALITY * THERMAL_VOLTAGE));

        double panelResistance = (cellCurrent > 0) ? Voc_panel / cellCurrent : 1e6;

        sourceCoupling.setVoltage((float) Voc_panel);
        sourceCoupling.setResistance((float) panelResistance);

        if (temp++ == 20){
            System.out.println("Cell Temp: " + cellTemp);
            System.out.println("Single cell voltage: " + cellVoltage);
            System.out.println("Single cell current: " + cellCurrent);
            System.out.println("Current irradiance: " + irradiance);
            System.out.println("AM: " + getAM(world));
            System.out.println("Cloud cover: " + cloudCover);

            System.out.println();
            System.out.println();

            temp = 0;
        }
    }

    private double[] getTempAdjusted(double irradiance, double cellTemp){
        var Isc_T = SHORT_CURRENT * (irradiance / 1000) * (1 + ALPHAISC * (cellTemp - 25));
        if (Isc_T <= 0) return new double[]{0, 0};
        var Voc_base = IDEALITY * THERMAL_VOLTAGE * Math.log(Isc_T / I_O + 1);
        var Voc_T = Voc_base + BETAVOC * (cellTemp - 25);
        return new double[]{Isc_T, Voc_T};
    }

    private double getCellTemp(double Irradiance){
        return AMBIENT_TEMP + (NOCT - 20) * (Irradiance / 800);
    }

    private double getIrradiance(double AM, double cloudCover, int YPos, float panelTiltDeg, float panelAzimuthDeg, Level world) {
        if (AM <= 0)
            return 0;
        var transmisttance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level

        double sunElevationRad = Math.asin(Math.max(0, Math.cos(world.getSunAngle(0))));
        double sunAzimuthRad = world.getSunAngle(0) < Math.PI
                ? Math.toRadians(90)
                : Math.toRadians(270);

        double tiltRad = Math.toRadians(panelTiltDeg);
        double panelAzimuthRad = Math.toRadians(panelAzimuthDeg);

        double cosIncidence = Math.sin(sunElevationRad) * Math.cos(tiltRad) + Math.cos(sunAzimuthRad)
                * Math.cos(sunElevationRad - panelAzimuthRad) * Math.sin(tiltRad);

        var diffuseLight = 0.1f * irradiance * (1 + cloudCover) * ((1 + Math.cos(tiltRad)) / 2);
        var reflected = 0.2 * irradiance * ((1 - Math.cos(tiltRad)) / 2.0);

        return irradiance * transmisttance * cosIncidence + diffuseLight +  reflected;
    }

    private double getAM(Level world){
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

    private double getCellCurrent(double V, double cellCurrent){
        return cellCurrent - I_O * (Math.exp(V / (IDEALITY * THERMAL_VOLTAGE)) - 1);
    }

    private double getCellPower(double V, double cellCurrent){
        return V * getCellCurrent(V, cellCurrent);
    }

}
