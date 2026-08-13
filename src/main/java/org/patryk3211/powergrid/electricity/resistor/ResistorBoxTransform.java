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
package org.patryk3211.powergrid.electricity.resistor;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.electricity.base.SurfaceElectricBlock;

public class ResistorBoxTransform extends CenteredSideValueBoxTransform {
    public ResistorBoxTransform() {
        super((state, dir) -> dir == state.getValue(SurfaceElectricBlock.FACING).getOpposite());
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8.0f, 8.0f, 7.6f);
    }
}
