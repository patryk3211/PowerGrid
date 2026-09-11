package org.patryk3211.powergrid.mixin.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.HeatableBlockEntity;

@Mixin(value = CookingPotBlockEntity.class, remap = false)
public abstract class CookingPotBlockEntityMixin implements HeatableBlockEntity {

    @Redirect(method = "cookingTick", at = @At(value = "INVOKE",
    target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;isHeated(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean powerGrid$redirectTickIsHeated(CookingPotBlockEntity cookingPotBlockEntity, Level level, BlockPos pos) {
        return powerGrid$checkBasinHeater(level, pos) || cookingPotBlockEntity.isHeated();
    }

    @Inject(method = "isHeated()Z", at = @At("HEAD"), cancellable = true)
    private void powerGrid$fixIsHeated(CallbackInfoReturnable<Boolean> cir) {
        CookingPotBlockEntity self = (CookingPotBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null && powerGrid$checkBasinHeater(level, self.getBlockPos())) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "animationTick", at = @At(value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;isHeated(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean powerGrid$redirectAnimIsHeated(CookingPotBlockEntity cookingPotBlockEntity, Level level, BlockPos pos) {
        return powerGrid$checkBasinHeater(level, pos) || cookingPotBlockEntity.isHeated();
    }

    private static boolean powerGrid$checkBasinHeater(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof BasinHeaterBlock && below.getValue(BasinHeaterBlock.HEAT_LEVEL).ordinal() >= 2;
    }
}
