package org.patryk3211.powergrid.advancements;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PGAdvancementBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<PGAdvancementBehaviour> TYPE = new BehaviourType<>();

    private UUID playerId;
    private final Set<PowerGridAdvancement> advancements = new HashSet<>();

    public PGAdvancementBehaviour(SmartBlockEntity be, PowerGridAdvancement... advancements) {
        super(be);
        add(advancements);
    }

    public void add(PowerGridAdvancement... advancements) {
        for(var advancement : advancements)
            this.advancements.add(advancement);
    }

    public boolean isOwnerPresent() {
        return playerId != null;
    }

    public void setPlayer(UUID id) {
        Player player = getWorld().getPlayerByUUID(id);
        if (player == null)
            return;
        playerId = id;
        removeAwarded();
        blockEntity.setChanged();
    }

    @Override
    public void initialize() {
        super.initialize();
        removeAwarded();
    }

    private void removeAwarded() {
        Player player = getPlayer();
        if (player == null)
            return;
        advancements.removeIf(c -> c.isAlreadyAwardedTo(player));
        if (advancements.isEmpty()) {
            playerId = null;
            blockEntity.setChanged();
        }
    }

    public void awardPlayerIfNear(PowerGridAdvancement advancement, int maxDistance) {
        Player player = getPlayer();
        if (player == null)
            return;
        if (player.distanceToSqr(Vec3.atCenterOf(getPos())) > maxDistance * maxDistance)
            return;
        award(advancement, player);
    }

    public void awardPlayer(PowerGridAdvancement advancement) {
        Player player = getPlayer();
        if (player == null)
            return;
        award(advancement, player);
    }

    private void award(PowerGridAdvancement advancement, Player player) {
        if (advancements.contains(advancement))
            advancement.awardTo(player);
        removeAwarded();
    }

    private Player getPlayer() {
        if (playerId == null)
            return null;
        return getWorld().getPlayerByUUID(playerId);
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if (playerId != null)
            nbt.putUUID("Owner", playerId);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.contains("Owner"))
            playerId = nbt.getUUID("Owner");
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public static void tryAward(BlockGetter reader, BlockPos pos, PowerGridAdvancement advancement) {
        var behaviour = BlockEntityBehaviour.get(reader, pos, TYPE);
        if(behaviour != null)
            behaviour.awardPlayer(advancement);
    }

    public static void setPlacedBy(Level worldIn, BlockPos pos, LivingEntity placer) {
        var behaviour = BlockEntityBehaviour.get(worldIn, pos, TYPE);
        if(behaviour == null)
            return;
        if(placer instanceof ServerPlayer player && !(player instanceof FakePlayer))
            behaviour.setPlayer(placer.getUUID());
    }

}
