package org.patryk3211.powergrid.mixin.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import vectorwing.farmersdelight.common.block.entity.HeatableBlockEntity;
@Mixin(value = HeatableBlockEntity.class, remap = false)
public interface HeatableBlockEntityMixin {

    @Inject(method = "isHeated", at = @At("HEAD"), cancellable = true)
    private void powerGrid$testHeatable(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockState(pos.below()).getBlock() instanceof BasinHeaterBlock) {
            if (level.getBlockState(pos.below()).getValue(BasinHeaterBlock.HEAT_LEVEL).ordinal() >= 2){
                cir.setReturnValue(true);
                cir.cancel();
            } else {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

}
