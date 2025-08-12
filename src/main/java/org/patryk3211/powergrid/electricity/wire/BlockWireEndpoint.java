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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Objects;

;

public class BlockWireEndpoint implements IWireEndpoint {
    private BlockPos pos;
    private int terminal;

    public BlockWireEndpoint() {
        this(null, 0);
    }

    public BlockWireEndpoint(BlockPos pos, int terminal) {
        this.pos = pos;
        this.terminal = terminal;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.BLOCK;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getTerminal() {
        return terminal;
    }

    @Override
    public void read(CompoundTag nbt) {
        var posArr = nbt.getIntArray("Pos");
        pos = new BlockPos(posArr[0], posArr[1], posArr[2]);
        terminal = nbt.getInt("Terminal");
    }

    @Override
    public void write(CompoundTag nbt) {
        nbt.putIntArray("Pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        nbt.putInt("Terminal", terminal);
    }

    public IElectric getElectricBlock(Level world) {
        return IElectric.getAt(world, pos);
    }

    public ElectricBehaviour getElectricBehaviour(Level world) {
        var electric = getElectricBlock(world);
        if(electric == null)
            return null;
        var state = world.getBlockState(pos);
        return electric.getBehaviour(world, pos, state);
    }

    @Override
    @NotNull
    public Vec3 getExactPosition(Level world) {
        return IElectric.getTerminalPos(world, pos, this.terminal);
    }

    @Override
    public IElectricNode getNode(Level world) {
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return null;
        return behaviour.getTerminal(terminal);
    }

    @Override
    public void joinNetwork(Level world, ElectricalNetwork network) {
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return;
        behaviour.joinNetwork(network);
    }

    @Override
    public boolean isValid(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return false;
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return false;
        return behaviour.hasTerminal(terminal);
    }

    @Override
    public void assignWireEntity(WireEntity entity) {
        var behaviour = getElectricBehaviour(entity.level());
        if(behaviour == null)
            return;
        behaviour.addConnection(this, entity);
    }

    @Override
    public void removeWireEntity(WireEntity entity) {
        var behaviour = getElectricBehaviour(entity.level());
        if(behaviour == null)
            return;
        behaviour.removeConnection(this, entity);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj instanceof BlockWireEndpoint other) {
            return pos.equals(other.pos) && terminal == other.terminal;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, terminal);
    }

    @Override
    public String toString() {
        return String.format("Block(pos=%s, n=%d)", pos, terminal);
    }

    public ITerminalPlacement getTerminalPlacement(Level world) {
        var electric = getElectricBlock(world);
        var state = world.getBlockState(pos);
        return electric.terminal(state, terminal);
    }
}
