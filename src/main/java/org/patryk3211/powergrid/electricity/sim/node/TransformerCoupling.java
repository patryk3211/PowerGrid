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

import org.ejml.data.DMatrixRMaj;

public abstract class TransformerCoupling extends CouplingNode {
    protected float ratio;
    protected float resistance;

    protected TransformerCoupling(float ratio, float resistance) {
        this.ratio = ratio;
        this.resistance = resistance;
    }

    @Override
    public void couple(DMatrixRMaj conductance) {
        // Unlike other fields, this one holds resistance instead of conductance.
        conductance.set(this.index, this.index, resistance);
    }

    public void setResistance(float resistance) {
        if(network != null) {
            network.alterConductanceMatrix(this.index, this.index, resistance - this.resistance);
            this.resistance = resistance;
        }
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
            super(ratio, resistance);
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public void couple(DMatrixRMaj conductance) {
            super.couple(conductance);
            conductance.set(this.index, primary.getIndex(), ratio);
            conductance.set(this.index, secondary.getIndex(), -1);
            conductance.set(secondary.getIndex(), this.index, 1);
            conductance.set(primary.getIndex(), this.index, -ratio);
        }
    }

    private static class Tr1P2S extends TransformerCoupling {
        private final IElectricNode primary;
        private final IElectricNode secondary1;
        private final IElectricNode secondary2;

        protected Tr1P2S(float ratio, float resistance, IElectricNode primary, IElectricNode secondary1, IElectricNode secondary2) {
            super(ratio, resistance);
            this.primary = primary;
            this.secondary1 = secondary1;
            this.secondary2 = secondary2;
        }

        @Override
        public void couple(DMatrixRMaj conductance) {
            super.couple(conductance);
            conductance.set(this.index, primary.getIndex(), ratio);
            conductance.set(this.index, secondary1.getIndex(), -1.0);
            conductance.set(this.index, secondary2.getIndex(),  1.0);
            conductance.set(secondary1.getIndex(), this.index,  1.0);
            conductance.set(secondary2.getIndex(), this.index, -1.0);
            conductance.set(primary.getIndex(), this.index, -ratio);
        }
    }

    private static class Tr2P2S extends TransformerCoupling {
        private final IElectricNode primary1;
        private final IElectricNode primary2;
        private final IElectricNode secondary1;
        private final IElectricNode secondary2;

        protected Tr2P2S(float ratio, float resistance, IElectricNode primary1, IElectricNode primary2, IElectricNode secondary1, IElectricNode secondary2) {
            super(ratio, resistance);
            this.primary1 = primary1;
            this.primary2 = primary2;
            this.secondary1 = secondary1;
            this.secondary2 = secondary2;
        }

        @Override
        public void couple(DMatrixRMaj conductance) {
            super.couple(conductance);
            conductance.set(this.index, primary1.getIndex(),  ratio);
            conductance.set(this.index, primary2.getIndex(), -ratio);
            conductance.set(this.index, secondary1.getIndex(), -1.0);
            conductance.set(this.index, secondary2.getIndex(),  1.0);
            conductance.set(secondary1.getIndex(), this.index,  1.0);
            conductance.set(secondary2.getIndex(), this.index, -1.0);
            conductance.set(primary1.getIndex(), this.index, -ratio);
            conductance.set(primary2.getIndex(), this.index,  ratio);
        }
    }
}
