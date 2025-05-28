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
package org.patryk3211.powergrid.chemistry.reagent.mixture;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;

import java.util.HashSet;
import java.util.Set;

public class MixtureHelper {
    public static void diffuse(ReagentMixture mixture1, ReagentMixture mixture2, Set<Reagent> thisReagents, ReagentState state, int amount) {
        if(thisReagents.isEmpty())
            return;
        var otherReagents = new HashSet<Reagent>();
        for(var reagent : mixture1.getReagents()) {
            if(mixture2.getState(reagent) == state)
                otherReagents.add(reagent);
        }

        while(amount > 0) {
            try(var transaction = Transaction.openOuter()) {
                var mix1 = mixture1.remove(amount, thisReagents, transaction);
                var mix2 = mixture2.remove(mix1.getTotalAmount(), otherReagents, transaction);

                int added = mixture1.add(mix2, transaction);
                if(added != mix2.getTotalAmount()) {
                    amount = added;
                    transaction.abort();
                    continue;
                }
                added = mixture2.add(mix1, transaction);
                if(added != mix1.getTotalAmount()) {
                    amount = added;
                    transaction.abort();
                    continue;
                }
                transaction.commit();
                break;
            }
        }
    }

    public static int moveReagents(ReagentMixture source, Set<Reagent> reagents, ReagentMixture target, int amount) {
        if(reagents.isEmpty())
            return 0;
        if(amount <= 0)
            return 0;
        while(amount > 0) {
            try(var transaction = Transaction.openOuter()) {
                var mix = source.remove(amount, reagents, transaction);
                amount = mix.getTotalAmount();
                int added = target.add(mix, transaction);
                if(added == amount) {
                    transaction.commit();
                    break;
                }
                transaction.abort();
                amount = added;
            }
        }
        return amount;
    }
}
