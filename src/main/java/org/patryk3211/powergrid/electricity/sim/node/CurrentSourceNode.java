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
package org.patryk3211.powergrid.electricity.sim.node;

/**
 * Warning! Current source nodes cannot be directly connected to transformer couplings.
 */
public class CurrentSourceNode extends ElectricNode {
    public CurrentSourceNode() {

    }

    public CurrentSourceNode(float current) {
        setCurrent(current);
    }

    @Override
    public void setCurrent(float current) {
        var old = this.current;
        super.setCurrent(current);
        if(network != null)
            network.updateCurrent(this, old);
    }
    @Override
    public void receiveResult(float value) {
        this.voltage = value;
    }
}
