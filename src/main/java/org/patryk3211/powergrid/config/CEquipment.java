package org.patryk3211.powergrid.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CEquipment extends ConfigBase {
    public final ConfigInt electroZapperEnergyPerShot = i(500, 1, "electroZapperEnergyPerShot", Comments.electroZapperEnergyPerShot);
    public final ConfigInt electroBatonEnergyPerUse = i(250, 1, "electroBatonEnergyPerShot", Comments.electroBatonEnergyPerUSe);

    public final ConfigInt drillEnergyPerUse = i(25, 1, "portableDrillEnergyPerUse", Comments.toolEnergyPerUse);
    public final ConfigFloat drillMineSpeedBase = f(6.0f, 0, "drillMineSpeedBase", Comments.drillMineSpeedBase);
    public final ConfigFloat drillMineSpeedBulk = f(16.0f, 0, "drillMineSpeedBulk", Comments.drillMineSpeedBulk);

    public final ConfigInt sawEnergyPerUse = i(100, 1, "portableSawEnergyPerUse", Comments.toolEnergyPerUse);
    public final ConfigFloat sawMineSpeed = f(6.0f, 0, "sawMineSpeedBase", Comments.sawMineSpeed);

    public final ConfigInt portableBatteryBaseCapacity = i(20000, 1, "portableBatteryBaseCapacity", Comments.portableBatteryBaseCapacity);
    public final ConfigInt portableBatteryEnchantCapacity = i(20000, 1, "portableBatteryEnchantCapacity", Comments.portableBatteryEnchantCapacity);

    public final ConfigFloat multimeterDistance = f(5, 1, "multimeterDistance", Comments.multimeterDistance);
    public final ConfigFloat multimeterVoltage = f(500, 1, "multimeterVoltage", Comments.multimeterVoltage);
    public final ConfigFloat multimeterCurrent = f(50, 1, "multimeterCurrent", Comments.multimeterCurrent);

    @NotNull
    @Override
    public String getName() {
        return "equipment";
    }

    private static class Comments {
        public static final String electroZapperEnergyPerShot = "Energy used by Electro-Zapper per shot";
        public static final String electroBatonEnergyPerUSe = "Energy used by Electro-Baton per hit";

        public static final String toolEnergyPerUse = "Energy used by the tool for each block mined";
        public static final String drillMineSpeedBase = "Base mining speed of the Portable Drill";
        public static final String drillMineSpeedBulk = "Top mining speed of the Portable Drill when used continuously";

        public static final String sawMineSpeed = "Mining speed of the Portable Saw";

        public static final String portableBatteryBaseCapacity = "Portable Battery energy capacity before enchants";
        public static final String portableBatteryEnchantCapacity = "Portable Battery energy capacity increase per level of Capacity enchant";

        public static final String multimeterDistance = "Max multimeter distance";
        public static final String multimeterVoltage = "Max multimeter voltage";
        public static final String multimeterCurrent = "Max multimeter current";
    }
}
