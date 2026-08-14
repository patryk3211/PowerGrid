package org.patryk3211.powergrid.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidPropagator.class, priority = 1100)
public class FluidPropagatorMixin {
    @Inject(method = "propagateChangedPipe",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 1))
    private static void powerGrid$notifyElectricPump(LevelAccessor world, BlockPos pipePos, BlockState pipeState, CallbackInfo ci,
                                                     @Local Direction direction, @Local(ordinal = 2) BlockPos target) {
        if(world.getBlockEntity(target) instanceof ElectricPumpBlockEntity pump) {
            pump.updatePipesOnSide(direction.getOpposite());
        }
    }
}
