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
package org.patryk3211.powergrid.electricity.base.terminals;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class BlockStateTerminalCollection {
    private final Map<PartialState, TerminalBoundingBox[]> terminals;
    private final List<Property<?>> checkedProperties;
    private Function<BlockState, VoxelShape> shapeMapper;
    private final int count;

    private BlockStateTerminalCollection(Map<PartialState, TerminalBoundingBox[]> terminals, List<Property<?>> checkedProperties, int count) {
        this.terminals = terminals;
        this.checkedProperties = checkedProperties;
        this.count = count;
    }

    public TerminalBoundingBox get(BlockState state, int index) {
        var value = terminals.get(PartialState.of(state, checkedProperties));
        if(value == null)
            return null;
        if(index < 0 || index >= value.length)
            return null;
        return value[index];
    }

    public int count() {
        return count;
    }

    public Function<BlockState, VoxelShape> shapeMapper() {
        if(shapeMapper == null)
            return null;
        return state -> {
            var baseShape = shapeMapper.apply(state);
            var terminals = this.terminals.get(PartialState.of(state, checkedProperties));
            for(var terminal : terminals) {
                if(terminal == null)
                    continue;
                baseShape = VoxelShapes.union(baseShape, terminal.getShape());
            }
            return baseShape;
        };
    }

    public static TerminalBoundingBox[] each(TerminalBoundingBox[] input, UnaryOperator<TerminalBoundingBox> func) {
        var copy = new TerminalBoundingBox[input.length];
        for(var i = 0; i < input.length; ++i)
            copy[i] = func.apply(input[i]);
        return copy;
    }

    public static class Builder implements TerminalCollectionBuilder<BlockStateTerminalCollection> {
        private final Block block;
        private final Map<PartialState, TerminalBoundingBox[]> terminals;
        private Function<BlockState, VoxelShape> shapeMapper = null;

        private Builder(Block block) {
            this.block = block;
            this.terminals = new HashMap<>();
        }

        public Builder forAllStates(Function<BlockState, TerminalBoundingBox[]> mapper) {
            return forAllStatesExcept(mapper);
        }

        public Builder forAllStatesExcept(Function<BlockState, TerminalBoundingBox[]> mapper, Property<?>... ignored) {
            var seen = new HashSet<PartialState>();

            var states = block.getStateManager().getStates();
            for(var state : states) {
                var properties = Maps.newLinkedHashMap(state.getEntries());
                for(var prop : ignored)
                    properties.remove(prop);

                var partial = new PartialState(block, properties);
                if(seen.add(partial))
                    terminals.put(partial, mapper.apply(state));
            }
            return this;
        }

        public Builder withShapeMapper(Function<BlockState, VoxelShape> mapper) {
            this.shapeMapper = mapper;
            return this;
        }

        @Override
        public BlockStateTerminalCollection build() {
            Set<Property<?>> checkedProperties = null;
            int count = 0;
            for(var state : terminals.keySet()) {
                if(checkedProperties == null) {
                    checkedProperties = state.states.keySet();
                    count = terminals.get(state).length;
                    continue;
                }
                if(!checkedProperties.equals(state.states.keySet()))
                    throw new IllegalStateException("All partial states must check the same property set");
                if(terminals.get(state).length != count)
                    throw new IllegalStateException("All states must map the same number of terminals");
            }
            BlockStateTerminalCollection collection;
            if(checkedProperties != null) {
                collection = new BlockStateTerminalCollection(terminals, Lists.newArrayList(checkedProperties), count);
            } else {
                collection = new BlockStateTerminalCollection(terminals, List.of(), count);
            }
            collection.shapeMapper = shapeMapper;
            return collection;
        }
    }

    public static Builder builder(Block block) {
        return new Builder(block);
    }

    public static class PartialState implements Predicate<BlockState> {
        private final Block block;
        private final Map<Property<?>, Comparable<?>> states;

        public PartialState(Block block, Map<Property<?>, Comparable<?>> states) {
            this.block = block;
            this.states = states;
        }

        @Override
        public boolean test(BlockState state) {
            if(!state.isOf(block))
                return false;

            for(var property : states.entrySet()) {
                if(state.get(property.getKey()) != property.getValue())
                    return false;
            }
            return true;
        }

        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj instanceof PartialState state)
                return block == state.block && states.equals(state.states);
            return false;
        }

        public int hashCode() {
            return Objects.hash(block, states);
        }

        public static PartialState of(BlockState state, Collection<Property<?>> properties) {
            var map = new HashMap<Property<?>, Comparable<?>>();
            for(var property : properties) {
                map.put(property, state.get(property));
            }
            return new PartialState(state.getBlock(), map);
        }
    }
}
