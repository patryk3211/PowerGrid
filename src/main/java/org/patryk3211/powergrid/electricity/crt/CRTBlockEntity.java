/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.crt;

import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class CRTBlockEntity extends ElectricBlockEntity {
    private ElectricWire xDeflect, yDeflect;
    private ElectricWire heater, gridCathode;
    private SwitchedWire anodeCathode;

    // These are only needed on the client. CRT state is not persistent nor synchronized.
    protected float[] xPoints = new float[sampleCount()];
    protected float[] yPoints = new float[sampleCount()];
    protected float[] brightness = new float[sampleCount()];
    protected int head;

    public CRTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int sampleCount() {
        return EnvExecutor.getEnvSpecific(() -> () -> ModdedConfigs.client().crtPointCount.get(), () -> () -> 1);
    }

    @Override
    public float resistance(String suffix) {
        return ResistanceValues.get(ModdedBlocks.CRT.get(), suffix);
    }

    @Override
    public void tick() {
        applyPower(xDeflect);
        applyPower(yDeflect);
        applyPower(heater);
        super.tick();
        // Simplified electron tube model
        var Vgk = gridCathode.potentialDifference();
        var ival = Math.min(Vgk, 0) + anodeCathode.potentialDifference() / 100;

        // 1 amp is the target current for normal operation.
        // 0.8 amps is the minimum current.
        var heaterPower = Math.abs(heater.current());
        heaterPower = heaterPower < 0.8 ? 0 : Math.min(heaterPower * heaterPower, 1.2f);

        var I = anodeCathode.current();
        // Heater power affects the electron gun current.
        var R = (ival <= 0 || heaterPower <= 0) ? 0 : (anodeCathode.potentialDifference() / (ival * 0.01f * heaterPower));
        if(R <= 0) {
            anodeCathode.setState(false);
        } else {
            anodeCathode.setState(true);
            anodeCathode.setResistance(R);
        }

        if(level.isClientSide) {
            // 100 mA is needed for max brightness.
            // 50 mA is the minimum current.
            brightness[head] = Mth.clamp((I - 0.05f) / 0.05f, 0, 1.5f);
            xPoints[head] = Mth.clamp(xDeflect.current() / 0.5f, -1, 1);
            yPoints[head] = Mth.clamp(yDeflect.current() / 0.5f, -1, 1);
            head = (head + 1) % brightness.length;
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(7);
        xDeflect = builder.connect(resistance("coils"), builder.terminalNode(4), builder.terminalNode(6));
        yDeflect = builder.connect(resistance("coils"), builder.terminalNode(5), builder.terminalNode(6));

        gridCathode = builder.connect(1e+6f, builder.terminalNode(2), builder.terminalNode(0));
        heater = builder.connect(resistance("heater"), builder.terminalNode(1), builder.terminalNode(0));
        anodeCathode = builder.connectSwitch(1, builder.terminalNode(3), builder.terminalNode(0), false);
    }
}
