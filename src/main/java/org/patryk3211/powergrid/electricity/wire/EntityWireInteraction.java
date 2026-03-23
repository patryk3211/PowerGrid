package org.patryk3211.powergrid.electricity.wire;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityWireInteraction {
    private static final Map<LivingEntity, CircuitData> DATA = new Reference2ReferenceOpenHashMap<>();
    private static final Multimap<WireEntity, LivingEntity> TOUCHING = HashMultimap.create();

    public static float entityResistance() {
        return ModdedConfigs.server().electricity.entityResistance.getF();
    }

    public static void wireTouching(WireEntity wire, List<LivingEntity> touchingEntities) {
        var level = wire.level();
        if(level.isClientSide)
            return;
        var global = GlobalElectricNetworks.getWorldNetworks(level);
        var currentTouching = TOUCHING.get(wire);
        for(var entity : touchingEntities) {
            if(currentTouching.contains(entity))
                continue;
            // Added
            TOUCHING.put(wire, entity);
            var data = DATA.computeIfAbsent(entity, $ -> new CircuitData());
            data.add(global, wire, entity.onGround());
        }
        var iter = currentTouching.iterator();
        while(iter.hasNext()) {
            var oldTouching = iter.next();
            if(touchingEntities.contains(oldTouching))
                continue;
            // Removed
            iter.remove();
            var data = DATA.get(oldTouching);
            if(data == null)
                continue;
            var connection = data.remove(wire);
            if(connection == null)
                continue;
            connection.remove();
            global.scheduleIslandDiscovery(data.node.getNetwork());
            if(data.wires.isEmpty()) {
                if(data.ground != null)
                    data.ground.remove();
                data.node.remove();
                DATA.remove(oldTouching);
            }
        }
    }

    public static void postTick(MinecraftServer server) {
        final var source = new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModdedDamageTypes.ELECTROCUTION));
        var iter = DATA.entrySet().iterator();
        while(iter.hasNext()) {
            var entityEntry = iter.next();
            var entity = entityEntry.getKey();
            var data = entityEntry.getValue();
            if(entity.isRemoved()) {
                for(var entry : data.wires) {
                    entry.remove();
                    TOUCHING.remove(entry.entity, entity);
                }
                if(data.ground != null)
                    data.ground.remove();
                var global = GlobalElectricNetworks.getWorldNetworks(entity.level());
                global.scheduleIslandDiscovery(data.node.getNetwork());
                data.node.remove();
                iter.remove();
                continue;
            }
            double I = data.totalCurrent();
            double threshold = ModdedConfigs.server().electricity.entityCurrentDamageThreshold.get();
            if(I >= threshold) {
                entity.hurt(source, (float) (I / threshold));
            }
            if(data.wires.isEmpty()) {
                if(data.ground != null)
                    data.ground.remove();
                data.node.remove();
                iter.remove();
            } else {
                data.updateGrounding(entity.onGround());
            }
        }
    }

    private static class WireConnection {
        public final WireEntity entity;
        @Nullable
        public final ElectricWire wire1;
        @Nullable
        public final ElectricWire wire2;
        public boolean justAdded = true;

        private WireConnection(WireEntity entity, @Nullable ElectricWire wire1, @Nullable ElectricWire wire2) {
            this.entity = entity;
            this.wire1 = wire1;
            this.wire2 = wire2;
        }

        public void remove() {
            if(wire1 != null)
                wire1.remove();
            if(wire2 != null)
                wire2.remove();
        }

        public double current() {
            return (wire1 != null ? Math.abs(wire1.current()) : 0) + (wire2 != null ? Math.abs(wire2.current()) : 0);
        }
    }

    private static class CircuitData {
        private final FloatingNode node = new FloatingNode();
        private final List<WireConnection> wires = new ArrayList<>();
        private boolean grounded = false;
        private ElectricWire ground = null;
        private boolean groundJustAdded = false;

        public WireConnection remove(WireEntity entity) {
            for(var wire : wires) {
                if(wire.entity == entity) {
                    wires.remove(wire);
                    return wire;
                }
            }
            return null;
        }

        public void add(WorldNetworks global, WireEntity entity, boolean grounded) {
            if(entity.endpoint1 == null && entity.endpoint2 == null)
                return;
            ElectricWire wire1 = null, wire2 = null;
            if(entity.endpoint1 != null) {
                var network = global.prepareForConnection(entity.endpoint1, node);
                if (network == null) {
                    PowerGrid.LOGGER.error("Failed to unify networks for entity circuit");
                    return;
                }
                var node1 = entity.endpoint1.getNode(global.world);
                wire1 = new ElectricWire(entityResistance(), node1, node);
                network.addWire(wire1);
            }

            if(entity.endpoint2 != null) {
                var network = global.prepareForConnection(entity.endpoint2, node);
                if (network == null) {
                    PowerGrid.LOGGER.error("Failed to unify networks for entity circuit");
                    return;
                }
                var node2 = entity.endpoint2.getNode(global.world);
                wire2 = new ElectricWire(entityResistance(), node2, node);
                network.addWire(wire2);
            }
            if(wire1 == null) {
                wire2.setResistance(entityResistance() * 0.5);
            } else if(wire2 == null) {
                wire1.setResistance(entityResistance() * 0.5);
            }

            global.setDirty();
            wires.add(new WireConnection(entity, wire1, wire2));
            updateGrounding(grounded);
        }

        private void updateGrounding(boolean grounded) {
            if(this.grounded == grounded)
                return;
            if(!grounded && ground != null) {
                ground.remove();
                ground = null;
            } else if(grounded && ground == null) {
                var network = node.getNetwork();
                if(network == null) {
                    this.grounded = false;
                    return;
                }
                ground = new ElectricWire(entityResistance(), node, null);
                network.addWire(ground);
                groundJustAdded = true;
            }
            this.grounded = grounded;
        }

        public double totalCurrent() {
            double sum = !groundJustAdded && ground != null ? Math.abs(ground.current()) : 0;
            groundJustAdded = false;
            var iter = wires.iterator();
            while(iter.hasNext()) {
                var entry = iter.next();
                if(entry.entity.isRemoved()) {
                    entry.remove();
                    GlobalElectricNetworks.getWorldNetworks(entry.entity.level()).scheduleIslandDiscovery(node.getNetwork());
                    iter.remove();
                    continue;
                }
                if(entry.justAdded) {
                    entry.justAdded = false;
                    continue;
                }
                sum += entry.current();
            }
            return sum;
        }
    }
}
