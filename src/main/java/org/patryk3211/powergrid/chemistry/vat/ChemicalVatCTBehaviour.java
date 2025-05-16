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
package org.patryk3211.powergrid.chemistry.vat;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;

public class ChemicalVatCTBehaviour extends ConnectedTextureBehaviour.Base {
    private static final CTSpriteShiftEntry SHIFT = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, PowerGrid.asResource("block/vat/side"), PowerGrid.asResource("block/vat/side_connected"));

    public ChemicalVatCTBehaviour() {

    }

    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable Sprite sprite) {
        if(direction.getAxis() == Direction.Axis.Y)
            return null;
        return SHIFT;
    }
}
