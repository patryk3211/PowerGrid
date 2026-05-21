package org.patryk3211.powergrid.mixin;

import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlock;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChuteBlockEntity.class)
public abstract class ChuteBlockEntityMixin extends SmartBlockEntity {
    private ChuteBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "calculatePull", at = @At("HEAD"), cancellable = true, order = 1100, remap = false)
    private void powerGrid$electricFanPull(CallbackInfoReturnable<Float> cir) {
        if(level == null)
            return;
        BlockState blockStateAbove = level.getBlockState(worldPosition.above());
        if(ModdedBlocks.ELECTRIC_FAN.has(blockStateAbove)
                && blockStateAbove.getValue(ElectricFanBlock.FACING) == Direction.DOWN) {
            BlockEntity be = level.getBlockEntity(worldPosition.above());
            if(be instanceof ElectricFanBlockEntity fan && !be.isRemoved()) {
                cir.setReturnValue(fan.getSpeed());
            }
        }
    }

    @Inject(method = "calculatePush", at = @At("HEAD"), cancellable = true, order = 1100, remap = false)
    private void powerGrid$electricFanPush(int branchCount, CallbackInfoReturnable<Float> cir) {
        if(level == null)
            return;
        BlockState blockStateBelow = level.getBlockState(worldPosition.below());
        if(ModdedBlocks.ELECTRIC_FAN.has(blockStateBelow)
                && blockStateBelow.getValue(ElectricFanBlock.FACING) == Direction.UP) {
            BlockEntity be = level.getBlockEntity(worldPosition.below());
            if(be instanceof ElectricFanBlockEntity fan && !be.isRemoved()) {
                cir.setReturnValue(fan.getSpeed());
            }
        }
    }
}
