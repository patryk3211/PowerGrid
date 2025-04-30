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
package org.patryk3211.powergrid.electricity.creative;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PreciseNumberFormat;

import java.util.Collection;
import java.util.List;

public class CreativeResistorBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private ScrollValueBehaviour value;

    private FloatingNode terminal1;
    private FloatingNode terminal2;
    private ElectricWire wire;

    public CreativeResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        value = new CreativeResistorValueBehaviour(Lang.translateDirect("devices.creative.resistance"), this, new BoxTransform());
        value.value = 45;
        value.withCallback(i -> wire.setResistance(CreativeResistorValueBehaviour.exponentialValue(i)));
        behaviours.add(value);
    }

    @Override
    public void initializeNodes() {
        terminal1 = new FloatingNode();
        terminal2 = new FloatingNode();
        wire = new ElectricWire(100f, terminal1, terminal2);
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(terminal1);
        nodes.add(terminal2);
    }

    @Override
    public void addInternalWires(Collection<ElectricWire> wires) {
        wires.add(wire);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        wire.setResistance(tag.getFloat("Resistance"));
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("Resistance", wire.getResistance());
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.creative_resistor.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.creative_resistor.resistance")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var resistance = wire.getResistance();
        var resistanceText = PreciseNumberFormat.format(resistance);
        Lang.builder()
                .text(resistanceText)
                .add(Lang.unit("ohm"))
                .style(Formatting.BLUE)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_resistor.current")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        float current = wire.current();
        var currentText = PreciseNumberFormat.format(current);
        Lang.builder()
                .text(currentText)
                .add(Lang.unit("amp"))
                .style(Formatting.GREEN)
                .forGoggles(tooltip, 1);

        Lang.builder().translate("gui.creative_resistor.power")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var power = PreciseNumberFormat.format(current * current * resistance);
        Lang.builder()
                .text(power)
                .add(Lang.unit("watt"))
                .style(Formatting.YELLOW)
                .forGoggles(tooltip, 1);
        return true;
    }

    public static class BoxTransform extends CenteredSideValueBoxTransform {
        public BoxTransform() {
            super((state, dir) -> {
                if(dir.getAxis() == state.get(CreativeResistorBlock.HORIZONTAL_AXIS))
                    return false;
                return dir != Direction.DOWN;
            });
        }

        @Override
        protected Vec3d getSouthLocation() {
            if(direction != Direction.UP)
                return VecHelper.voxelSpace(8.0f, 6.0f, 10.5f);
            else
                return VecHelper.voxelSpace(8.0f, 8.0f, 8.5f);
        }
    }
}
