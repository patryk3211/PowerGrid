package org.patryk3211.powergrid.mixin.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.equipment.drill.DrillItem;
import org.patryk3211.powergrid.equipment.drill.PlayerDrillExtensions;
import org.patryk3211.powergrid.network.packets.DrillSpeedS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDrillMixin extends LivingEntity implements PlayerDrillExtensions {
    private PlayerDrillMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    @Unique
    public boolean powerGrid$mining = false;
    @Unique
    public int powerGrid$prevDrillSpeed = 0;
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
        return (float) (powerGrid$drillSpeed / DrillItem.TICKS_PER_SPEED_LEVEL) / (DrillItem.MAX_SPEED_TICKS / DrillItem.TICKS_PER_SPEED_LEVEL);
    }

    @Override
    public void powerGrid$receiveSpeed(int speed) {
        powerGrid$drillSpeed = speed * DrillItem.TICKS_PER_SPEED_LEVEL;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void powerGrid$tick(CallbackInfo ci) {
        if(level().isClientSide)
            return;
        if(!powerGrid$mining && powerGrid$drillSpeed > 0)
            --powerGrid$drillSpeed;
        int speedLevel = powerGrid$drillSpeed / DrillItem.TICKS_PER_SPEED_LEVEL;
        if((Object) this instanceof ServerPlayer player && speedLevel != powerGrid$prevDrillSpeed) {
            ModdedPackets.sendToClient(new DrillSpeedS2CPacket(speedLevel), player);
            powerGrid$prevDrillSpeed = speedLevel;
        }
    }

    @Override
    public void powerGrid$blockDrilled(float power) {
        powerGrid$drillSpeed += DrillItem.TICKS_PER_SPEED_LEVEL;
        int maxLevel;
        if(power < 0.3f) {
            maxLevel = 0;
        } else if(power < 0.4f) {
            maxLevel = DrillItem.MAX_SPEED_TICKS / 3;
        } else if(power < 0.5f) {
            maxLevel = DrillItem.MAX_SPEED_TICKS * 2 / 3;
        } else {
            maxLevel = DrillItem.MAX_SPEED_TICKS;
        }
        maxLevel += DrillItem.TICKS_PER_SPEED_LEVEL - 1;
        if(powerGrid$drillSpeed > maxLevel)
            powerGrid$drillSpeed = maxLevel;
    }

    @Inject(method = "getDigSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void powerGrid$adjustDrillDigSpeed(BlockState arg, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        var stack = getItemBySlot(EquipmentSlot.MAINHAND);
        if(!(stack.getItem() instanceof DrillItem))
            return;
        cir.setReturnValue(cir.getReturnValue() * (1 + powerGrid$drillSpeedMultiplier() * DrillItem.SPEED_BOOST));
    }
}
