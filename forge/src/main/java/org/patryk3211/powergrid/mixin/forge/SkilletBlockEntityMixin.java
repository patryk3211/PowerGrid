package org.patryk3211.powergrid.mixin.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

@Mixin(value = SkilletBlockEntity.class, remap = false)
public class SkilletBlockEntityMixin {

    @Redirect(method = "cookingTick", at = @At(value = "INVOKE",
        target = "Lvectorwing/farmersdelight/common/block/entity/SkilletBlockEntity;isHeated(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean powerGrid$redirectTickIsHeated(SkilletBlockEntity skilletBlockEntity, Level level, BlockPos pos) {
        return powerGrid$checkBasinHeater(level, pos) || skilletBlockEntity.isHeated(level, pos);
    }

    @Redirect(method = "isCooking", at = @At(value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/block/entity/SkilletBlockEntity;isHeated()Z"))
    private boolean powerGrid$redirectCookingIsHeated(SkilletBlockEntity skilletBlockEntity) {
        if (skilletBlockEntity.getLevel() != null && powerGrid$checkBasinHeater(skilletBlockEntity.getLevel(),
                skilletBlockEntity.getBlockPos())) {
            return true;
        }
        return skilletBlockEntity.isHeated();
    }

    @Redirect(method = "animationTick", at = @At(value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/block/entity/SkilletBlockEntity;isHeated(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean powerGrid$redirectAnimIsHeated(SkilletBlockEntity skilletBlockEntity, Level level, BlockPos pos) {
        return powerGrid$checkBasinHeater(level, pos) || skilletBlockEntity.isHeated(level, pos);
    }

    @Redirect(method = "addItemToCook", at = @At(value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/block/entity/SkilletBlockEntity;isHeated(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean powerGrid$redirectSoundIsHeated(SkilletBlockEntity skilletBlockEntity, Level level, BlockPos pos) {
        return powerGrid$checkBasinHeater(level, pos) || skilletBlockEntity.isHeated(level, pos);
    }

    private static boolean powerGrid$checkBasinHeater(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof BasinHeaterBlock && below.getValue(BasinHeaterBlock.HEAT_LEVEL).ordinal() >= 2;
    }
}
