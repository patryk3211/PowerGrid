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

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;

import java.util.Collection;
import java.util.List;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public abstract class TransformerCoupling extends CouplingNode {
    protected float ratio;
    protected float resistance;
    protected final Collection<IElectricNode> coupledNodes;

    protected TransformerCoupling(float ratio, float resistance, Collection<IElectricNode> coupledNodes) {
        if(resistance == 0 && ElectricalNetwork.LOGGER != null)
            ElectricalNetwork.LOGGER.warn("Zero-resistance transformer couplings can cause unstable behaviour.");
        this.ratio = ratio;
        this.resistance = resistance;
        this.coupledNodes = coupledNodes;
    }

    @Override
    public void couple(IAdmittanceAdder admittance) {
        // Unlike other fields, this one holds resistance instead of conductance.
        admittance.add(this.index, this.index, resistance);
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return coupledNodes;
    }

    public abstract void setRatio(float ratio);

    public void setResistance(float resistance) {
        if(network != null) {
            network.alterConductanceMatrix(this.index, this.index, resistance - this.resistance);
        }
        this.resistance = resistance;
    }

    public float getResistance() {
        return resistance;
    }

    public static TransformerCoupling create(float ratio, IElectricNode primary, IElectricNode secondary) {
        return new Tr1P1S(ratio, 0, primary, secondary);
    }

    public static TransformerCoupling create(float ratio, float resistance, IElectricNode primary, IElectricNode secondary) {
        return new Tr1P1S(ratio, resistance, primary, secondary);
    }

    public static TransformerCoupling create(float ratio, IElectricNode primary, IElectricNode secondary1, IElectricNode secondary2) {
        return new Tr1P2S(ratio, 0, primary, secondary1, secondary2);
    }

    public static TransformerCoupling create(float ratio, float resistance, IElectricNode primary, IElectricNode secondary1, IElectricNode secondary2) {
        return new Tr1P2S(ratio, resistance, primary, secondary1, secondary2);
    }

    public static TransformerCoupling create(float ratio, IElectricNode primary1, IElectricNode primary2, IElectricNode secondary1, IElectricNode secondary2) {
        return new Tr2P2S(ratio, 0, primary1, primary2, secondary1, secondary2);
    }

    public static TransformerCoupling create(float ratio, float resistance, IElectricNode primary1, IElectricNode primary2, IElectricNode secondary1, IElectricNode secondary2) {
        return new Tr2P2S(ratio, resistance, primary1, primary2, secondary1, secondary2);
    }

    private static class Tr1P1S extends TransformerCoupling {
        private final IElectricNode primary;
        private final IElectricNode secondary;

        protected Tr1P1S(float ratio, float resistance, IElectricNode primary, IElectricNode secondary) {
            super(ratio, resistance, List.of(primary, secondary));
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public void couple(IAdmittanceAdder admittance) {
            super.couple(admittance);
            admittance.add(this.index, primary.getIndex(), ratio);
            admittance.add(this.index, secondary.getIndex(), -1);
            admittance.add(secondary.getIndex(), this.index, 1);
            admittance.add(primary.getIndex(), this.index, -ratio);
        }

        @Override
        public void setRatio(float ratio) {
            if(network != null) {
                float change = ratio - this.ratio;
                network.alterConductanceMatrix(this.index, primary.getIndex(), change);
                network.alterConductanceMatrix(primary.getIndex(), this.index, -change);
            }
            this.ratio = ratio;
        }
    }

    private static class Tr1P2S extends TransformerCoupling {
        private final IElectricNode primary;
        private final IElectricNode secondary1;
        private final IElectricNode secondary2;

        protected Tr1P2S(float ratio, float resistance, IElectricNode primary, IElectricNode secondary1, IElectricNode secondary2) {
            super(ratio, resistance, List.of(primary, secondary1, secondary2));
            this.primary = primary;
            this.secondary1 = secondary1;
            this.secondary2 = secondary2;
        }

        @Override
        public void couple(IAdmittanceAdder admittance) {
            super.couple(admittance);
            admittance.add(this.index, primary.getIndex(), ratio);
            admittance.add(this.index, secondary1.getIndex(), -1.0);
            admittance.add(this.index, secondary2.getIndex(),  1.0);
            admittance.add(secondary1.getIndex(), this.index,  1.0);
            admittance.add(secondary2.getIndex(), this.index, -1.0);
            admittance.add(primary.getIndex(), this.index, -ratio);

            admittance.add(primary.getIndex(),   primary.getIndex(),    G_MIN/2);
            admittance.add(secondary1.getIndex(), secondary1.getIndex(),  G_MIN/2);
            admittance.add(primary.getIndex(),   secondary1.getIndex(), -G_MIN/2);
            admittance.add(secondary1.getIndex(), primary.getIndex(),   -G_MIN/2);
        }

        @Override
        public void setRatio(float ratio) {
            if(network != null) {
                float change = ratio - this.ratio;
                network.alterConductanceMatrix(this.index, primary.getIndex(), change);
                network.alterConductanceMatrix(primary.getIndex(), this.index, -change);
            }
            this.ratio = ratio;
        }
    }

    private static class Tr2P2S extends TransformerCoupling {
        private final IElectricNode primary1;
        private final IElectricNode primary2;
        private final IElectricNode secondary1;
        private final IElectricNode secondary2;

        protected Tr2P2S(float ratio, float resistance, IElectricNode primary1, IElectricNode primary2, IElectricNode secondary1, IElectricNode secondary2) {
            super(ratio, resistance, List.of(primary1, primary2, secondary1, secondary2));
            this.primary1 = primary1;
            this.primary2 = primary2;
            this.secondary1 = secondary1;
            this.secondary2 = secondary2;
        }

        @Override
        public void couple(IAdmittanceAdder admittance) {
            admittance.add(this.index, this.index, -resistance);

            admittance.add(this.index, primary1.getIndex(),  ratio);
            admittance.add(this.index, primary2.getIndex(), -ratio);
            admittance.add(this.index, secondary1.getIndex(),  1.0);
            admittance.add(this.index, secondary2.getIndex(), -1.0);
            admittance.add(secondary1.getIndex(), this.index,  1.0);
            admittance.add(secondary2.getIndex(), this.index, -1.0);
            admittance.add(primary1.getIndex(), this.index,  ratio);
            admittance.add(primary2.getIndex(), this.index, -ratio);

            // Put a tiny connection between the sides to stabilize the simulation
            admittance.add(primary2.getIndex(),   primary2.getIndex(),    G_MIN/2);
            admittance.add(secondary2.getIndex(), secondary2.getIndex(),  G_MIN/2);
            admittance.add(primary2.getIndex(),   secondary2.getIndex(), -G_MIN/2);
            admittance.add(secondary2.getIndex(), primary2.getIndex(),   -G_MIN/2);

            admittance.add(primary1.getIndex(),   primary1.getIndex(),    G_MIN/2);
            admittance.add(secondary1.getIndex(), secondary1.getIndex(),  G_MIN/2);
            admittance.add(primary1.getIndex(),   secondary1.getIndex(), -G_MIN/2);
            admittance.add(secondary1.getIndex(), primary1.getIndex(),   -G_MIN/2);
        }

        @Override
        public void setRatio(float ratio) {
            if(network != null) {
                float change = ratio - this.ratio;
                network.alterConductanceMatrix(this.index, primary1.getIndex(),  change);
                network.alterConductanceMatrix(this.index, primary2.getIndex(), -change);
                network.alterConductanceMatrix(primary1.getIndex(), this.index, -change);
                network.alterConductanceMatrix(primary2.getIndex(), this.index,  change);
            }
            this.ratio = ratio;
        }
    }
}
