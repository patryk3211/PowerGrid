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
package org.patryk3211.powergrid.electricity.battery;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.redstoneconverter.IRedstoneConverterBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

@MethodsReturnNonnullByDefault
public class BatteryBlock extends AbstractBatteryBlock<MultiBlockBatteryEntity> implements IAcceptConnector, IHaveElectricProperties, IRedstoneConverterBehaviour {
    protected BatterySpec spec;

    public BatteryBlock(Properties settings) {
        super(settings);
    }

    public static <T extends BatteryBlock, P> NonNullUnaryOperator<BlockBuilder<T, P>> setSpec(BatterySpec spec) {
        return b -> b.onRegister(block -> block.spec = spec);
    }

    public BatterySpec getSpec() {
        return spec;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if(oldState.getBlock() == state.getBlock())
            return;
        if(moved)
            return;
        // fabric: see comment in FluidTankItem
//        Consumer<FluidTankBlockEntity> consumer = FluidTankItem.IS_PLACING_NBT
//                ? FluidTankBlockEntity::queueConnectivityUpdate
//                : FluidTankBlockEntity::updateConnectivity;
        withBlockEntityDo(world, pos, MultiBlockBatteryEntity::queueConnectivityUpdate);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            var be = world.getBlockEntity(pos);
            if (!(be instanceof MultiBlockBatteryEntity battery))
                return;
            var wires = GlobalElectricNetworks.getWorldNetworks(world).findConnectedWires(battery.getElectricBehaviour());
            super.onRemove(state, world, pos, newState, moved);
            CustomConnectivityHandler.splitMulti(battery);

            // Rewire all wires that still target the stale behaviour
            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var stacks = super.getDrops(state, builder);
        var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(be instanceof MultiBlockBatteryEntity battery) {
            for(var stack : stacks) {
                if(stack.is(this.asItem())) {
                    var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    tag.putDouble("Energy", Math.floor(battery.getIndividualEnergy()));
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    break;
                }
            }
        }
        return stacks;
    }

    @Override
    public Class<MultiBlockBatteryEntity> getBlockEntityClass() {
        return MultiBlockBatteryEntity.class;
    }

    @Override
    public BlockEntityType<? extends MultiBlockBatteryEntity> getBlockEntityType() {
        return ModdedBlockEntities.MULTIBLOCK_BATTERY.get();
    }

    @Override
    public boolean isPolarized() {
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        var be = getBlockEntity(level, pos);
        if(be == null)
            return 0;
        double fill = be.getEnergy() / be.getCapacity();
        return Mth.floor(fill * 14.0f) + (fill > 0.001 ? 1 : 0);
    }

    @Override
    public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
        var be = getBlockEntity(level, pos);
        if(be == null)
            return 0;
        return (float) (be.getEnergy() / be.getCapacity());
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Voltage.max(spec.calculateVoltage(1), player, tooltip);
        Power.max(stack, player, tooltip);
        float charge;
        if(!stack.has(DataComponents.CUSTOM_DATA) || !stack.get(DataComponents.CUSTOM_DATA).contains("Energy")) {
            charge = getSpec().getInitialCharge() / getSpec().getMaxCharge();
        } else {
            charge = (float) (stack.get(DataComponents.CUSTOM_DATA).copyTag().getDouble("Energy") / getSpec().getMaxCharge());
        }
        Lang.translate("tooltip.charge.current")
                .style(ChatFormatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Component.literal(" ")).add(Lang.numberConstant(charge * 100))
                .add(Component.literal("%"))
                .style(ChatFormatting.AQUA).addTo(tooltip);
    }
}
