/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.ponder.base;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;

public class VoltageSource extends PonderElementBase {
    private final IWireEndpoint target;
    private final float voltage;

    private ElectricWire wire;
    private VoltageSourceNode source;

    public VoltageSource(IWireEndpoint target, float voltage) {
        this.target = target;
        this.voltage = voltage;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        if(wire == null) {
            var node = target.getNode(scene.getWorld());
            if(node.getNetwork() == null) {
                var global = GlobalElectricNetworks.getWorldNetworks(scene.getWorld());
                target.joinNetwork(scene.getWorld(), global.newNetwork());
            }
            source = new VoltageSourceNode(voltage);
            var network = node.getNetwork();
            network.addNode(source);
            wire = new ElectricWire(1e-5, source, node);
            network.addWire(wire);
        }
    }

    @Override
    public void reset(PonderScene scene) {
        if(wire != null) {
            wire.remove();
            wire = null;
            source.getNetwork().removeNode(source);
            source = null;
        }
    }
}
