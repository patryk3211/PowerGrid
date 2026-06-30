package org.patryk3211.powergrid.mixin;

import net.minecraft.world.entity.player.Player;
import org.patryk3211.powergrid.electricity.wire.IAlternatePlacementExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerMixin implements IAlternatePlacementExtension {
    @Unique
    private boolean powerGrid$alternatePlacement;

    @Override
    public boolean powerGrid$alternatePlacement() {
        return powerGrid$alternatePlacement;
    }

    @Override
    public void powerGrid$setAlternatePlacement(boolean value) {
        powerGrid$alternatePlacement = value;
    }
}
