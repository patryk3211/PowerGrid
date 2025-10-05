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
package org.patryk3211.powergrid.electricity.grounding;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class GroundingRodBlock extends ElectricBlock implements IBE<GroundingRodBlockEntity> {
    private static final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 6, 10, 2, 10)
    };

    public GroundingRodBlock(Properties properties) {
        super(properties);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> TERMINALS_DOWN)
                .withShapeMapper(state -> Shapes.empty())
                .build());
    }

    @Override
    public Class<GroundingRodBlockEntity> getBlockEntityClass() {
        return GroundingRodBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GroundingRodBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GROUNDING_ROD.get();
    }
}
