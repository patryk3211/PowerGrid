package org.patryk3211.powergrid.mixin.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.equipment.drill.DrillItem;
import org.patryk3211.powergrid.equipment.drill.PlayerDrillExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDrillMixin implements PlayerDrillExtensions {
    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    @Unique
    public boolean powerGrid$mining = false;
    @Unique
    public int powerGrid$drillSpeed = 0;

    @Override
    public void powerGrid$setMining(boolean value) {
        powerGrid$mining = value;
    }

    @Override
    public boolean powerGrid$isMining() {
        return powerGrid$mining;
    }

    @Override
    public float powerGrid$drillSpeedMultiplier() {
        return (float) (powerGrid$drillSpeed / 20) / (DrillItem.MAX_SPEED_TICKS / 20);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if(!powerGrid$mining && powerGrid$drillSpeed > 0)
            --powerGrid$drillSpeed;
    }

    @Override
    public void powerGrid$blockDrilled() {
        powerGrid$drillSpeed += 20;
        if(powerGrid$drillSpeed > DrillItem.MAX_SPEED_TICKS + 15)
            powerGrid$drillSpeed = DrillItem.MAX_SPEED_TICKS + 15;
    }

    @Inject(method = "getDigSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void adjustDrillDigSpeed(BlockState arg, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        var stack = getItemBySlot(EquipmentSlot.MAINHAND);
        if(!(stack.getItem() instanceof DrillItem))
            return;
        PowerGrid.LOGGER.info("{}", powerGrid$drillSpeed);
        cir.setReturnValue(cir.getReturnValue() * (1 + powerGrid$drillSpeedMultiplier() * DrillItem.SPEED_BOOST));
    }
}
