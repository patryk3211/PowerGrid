package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.fuse.FuseState;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.special.FuseSwitchWire;

import java.util.Collection;
import java.util.List;

public class FuseHolderComponent extends OrientableComponent implements IInteractableComponent, IGoggleLabel {
    public static final EnumProperty<FuseState> STATE = new EnumProperty<>(PowerGrid.MOD_ID, "fuse_state", FuseState.class).hidden().cast();
    public static final EnumProperty<FuseState> PREV_STATE = new EnumProperty<>(PowerGrid.MOD_ID, "fuse_state_prev", FuseState.class).hidden().cast();
    public static final FloatProperty MAX_CURRENT = new FloatProperty(PowerGrid.MOD_ID, "current_fuse_max", 10, 1, 20);

    public FuseHolderComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(STATE, PREV_STATE, MAX_CURRENT);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
        var fuseWire = new FuseSwitchWire(0.05f, builder.terminalNode(0), builder.terminalNode(1), placed.get(STATE) == FuseState.CLOSED, placed.get(MAX_CURRENT));
        builder.add(fuseWire);
        placed.add(fuseWire);
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 3 / 16f);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty() || placed.isClient())
            return true;

        var fuseWire = (FuseSwitchWire) placed.wires.get(0);
        if (fuseWire.wasBlown()) {
            placed.set(STATE, FuseState.BLOWN);
            placed.notifyClients(STATE);
            stateUpdated(placed);
        }

        return true;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        super.stateUpdated(placed);
        if (placed.wires.isEmpty())
            return;
        ((FuseSwitchWire) placed.wires.get(0)).setState(placed.get(STATE) == FuseState.CLOSED);
        placed.onClientWorld(() -> world -> {
            modelChanged(placed.getPos());
            if (placed.get(STATE) == FuseState.BLOWN && placed.get(PREV_STATE) == FuseState.CLOSED) {
                var pos = placed.getPos();
                ModdedSoundEvents.FUSE_POPS.playAt(world, pos, 1.0f, 1.0f, false);
                var localPos = placed.getExactPos();
                SparkParticleData.explodeParticles(world, localPos.x, localPos.y, localPos.z, Direction.UP, 5);
            }
        });
        placed.set(PREV_STATE, placed.get(STATE));
    }

    public boolean resetFuse(PlacedComponent placed) {
        if (placed.get(STATE) == FuseState.OPEN) {
            placed.set(STATE, FuseState.CLOSED);
            placed.onServerWorld(() -> world -> {
                placed.notifyClients(STATE);
                stateUpdated(placed);
                ModdedSoundEvents.FUSE_INSTALL.playOnServer(world, placed.getPos());
            });
            return true;
        }
        return false;
    }

    public boolean removeBlown(PlacedComponent placed) {
        if(placed.get(STATE) == FuseState.BLOWN) {
            placed.set(STATE, FuseState.OPEN);
            placed.onServerWorld(() -> world -> {
                placed.notifyClients(STATE);
                stateUpdated(placed);
            });
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent component, Player player) {
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(stack.is(ModdedTags.Item.FUSE_RESETTING.tag)) {
            if(resetFuse(component)) {
                if(!player.isCreative())
                    stack.shrink(1);
                be.setChanged();
                return InteractionResult.SUCCESS;
            }
        } else if(stack.isEmpty()) {
            if(removeBlown(component)) {
                be.setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }
    
    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return switch (component.get(STATE)) {
            case OPEN -> PowerGrid.asResource("fuse");
            case BLOWN -> PowerGrid.asResource("fuse_blown");
            case CLOSED -> PowerGrid.asResource("fuse_on");
        };
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
                PowerGrid.asResource("fuse"),
                PowerGrid.asResource("fuse_blown"),
                PowerGrid.asResource("fuse_on")
        );
    }


}
