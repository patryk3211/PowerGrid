package org.patryk3211.powergrid.electricity.wire;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityWireInteraction {
    public static final float ENTITY_RESISTANCE = 10000;

    private static final Map<LivingEntity, CircuitData> DATA = new Reference2ReferenceOpenHashMap<>();
    private static final Multimap<WireEntity, LivingEntity> TOUCHING = HashMultimap.create();

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
            currentTouching.add(entity);
            var data = DATA.computeIfAbsent(entity, $ -> new CircuitData());
            data.add(global, wire);
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
            connection.wire1.remove();
            connection.wire2.remove();
            global.scheduleIslandDiscovery(data.node.getNetwork());
            if(data.wires.isEmpty()) {
                data.node.remove();
                DATA.remove(oldTouching);
            }
        }
    }

    private record WireConnection(WireEntity entity, ElectricWire wire1, ElectricWire wire2) {
    }

    private static class CircuitData {
        private final FloatingNode node = new FloatingNode();
        private final List<WireConnection> wires = new ArrayList<>();

        public WireConnection remove(WireEntity entity) {
            for(var wire : wires) {
                if(wire.entity == entity) {
                    wires.remove(wire);
                    return wire;
                }
            }
            return null;
        }

        public void add(WorldNetworks global, WireEntity entity) {
            var network = global.prepareForConnection(entity.endpoint1, node);
            if(network == null) {
                PowerGrid.LOGGER.error("Failed to unify networks for entity circuit");
                return;
            }
            var node1 = entity.endpoint1.getNode(global.world);
            var wire1 = new ElectricWire(ENTITY_RESISTANCE, node1, node);
            network.addWire(wire1);

            network = global.prepareForConnection(entity.endpoint2, node);
            if(network == null) {
                PowerGrid.LOGGER.error("Failed to unify networks for entity circuit");
                return;
            }
            var node2 = entity.endpoint2.getNode(global.world);
            var wire2 = new ElectricWire(ENTITY_RESISTANCE, node2, node);
            network.addWire(wire2);

            global.setDirty();
            wires.add(new WireConnection(entity, wire1, wire2));
        }
    }
}
