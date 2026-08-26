package org.patryk3211.powergrid.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.patryk3211.powergrid.collections.ModdedAdvancements;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public abstract class SheepMixin extends Animal implements Shearable {
    @Shadow
    public abstract void setSheared(boolean sheared);

    protected SheepMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void powerGrid$extendedShearing(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = player.getItemInHand(hand);
        if(itemStack.is(ModdedTags.Item.WIRE_CUTTERS.tag)) {
            Level level = level();
            if(!level.isClientSide && readyForShearing()) {
                level.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
                setSheared(true);

                if(!ModdedAdvancements.SHEEP_CUT.isAlreadyAwardedTo(player)) {
                    ModdedAdvancements.SHEEP_CUT.awardTo(player);
                }

                hurt(level.damageSources().playerAttack(player), 1);
                int count = random.nextInt(3);
                for(int j = 0; j < count; ++j) {
                    ItemEntity itemEntity = spawnAtLocation(Items.STRING, 1);
                    if (itemEntity != null) {
                        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement()
                                .add((random.nextFloat() - random.nextFloat()) * 0.1,
                                        random.nextFloat() * 0.05,
                                        (random.nextFloat() - random.nextFloat()) * 0.1));
                    }
                }
                gameEvent(GameEvent.SHEAR, player);
                itemStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                cir.setReturnValue(InteractionResult.SUCCESS);
            } else {
                cir.setReturnValue(InteractionResult.CONSUME);
            }
        }
    }
}
