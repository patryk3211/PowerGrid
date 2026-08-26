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
package org.patryk3211.powergrid.kinetics.variac;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.kinetics.base.TunedBlock;
import org.patryk3211.powergrid.utility.Lang;

public class VariacBlock extends TunedBlock implements IBE<VariacBlockEntity> {
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(2, 2, 2, 14, 10, 14),
            box(5, 10, 5, 11, 16, 11)
    );
    private static final VoxelShape SHAPE_NORTH_BASELESS = Shapes.or(
            box(5, 0, 5, 11, 2, 11),
            box(2, 2, 2, 14, 10, 14),
            box(5, 10, 5, 11, 16, 11)
    );

    private static final Component PRIMARY = Lang.builder()
            .translate("variac.primary")
            .style(ChatFormatting.GRAY)
            .component();

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(PRIMARY, 10, 2, 0, 12, 4, 1),
            new TerminalBoundingBox(IDecoratedTerminal.COMMON, 7, 2, 0, 9, 4, 1)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.TAP, 4, 2, 0, 6, 4, 1)
    };

    public VariacBlock(Properties properties) {
        super(properties);
        setTerminalCollection(tunedNorthTerminals(this, TERMINALS_NORTH, SHAPE_NORTH, SHAPE_NORTH_BASELESS));
    }

    @Override
    public Class<VariacBlockEntity> getBlockEntityClass() {
        return VariacBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VariacBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.VARIAC.get();
    }
}
