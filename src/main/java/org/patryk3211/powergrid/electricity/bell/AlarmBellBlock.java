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
package org.patryk3211.powergrid.electricity.bell;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.shape.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class AlarmBellBlock extends HorizontalElectricBlock implements IBE<AlarmBellBlockEntity>, IHaveElectricProperties {
    private static final VoxelShape NORTH_SHAPE = createCuboidShape(4, 4, 0, 12, 12, 6);

    private static final TerminalBoundingBox[] NORTH_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 11, 0, 7, 13, 1),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 9, 11, 0, 10, 13, 1)
    };

    public AlarmBellBlock(Settings settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, NORTH_TERMINALS, NORTH_SHAPE));
    }

    public static float resistance() {
        return 10f;
    }

    @Override
    public Class<AlarmBellBlockEntity> getBlockEntityClass() {
        return AlarmBellBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AlarmBellBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ALARM_BELL.get();
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }
}
