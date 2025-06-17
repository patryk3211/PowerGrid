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
package org.patryk3211.powergrid.electricity.portablebattery;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;
import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;
import java.util.Optional;

public class PortableBatteryBlock extends HorizontalElectricBlock implements IBE<PortableBatteryBlockEntity>, IHaveElectricProperties, ISpecialBlockItemRequirement {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            createCuboidShape(4, 0, 4, 12, 9, 12),
            createCuboidShape(5, 9, 5, 11, 12, 11)
    );

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
        new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4.5, 9.5, 6.5, 6.5, 12.5, 9.5),
        new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 9.5, 9.5, 6.5, 11.5, 12.5, 9.5)
    };

    public PortableBatteryBlock(Settings settings) {
        super(settings);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection
                        .each(TERMINALS_NORTH, terminal -> terminal
                                .rotateAroundY((int) (180 - state.get(HORIZONTAL_FACING).asRotation()))))
                .withShapeMapper(state -> SHAPE)
                .build());
    }

    public int capacity() {
        return ModdedConfigs.server().electricity.portableBatteryBaseCapacity.get();
    }

    @Override
    public Class<PortableBatteryBlockEntity> getBlockEntityClass() {
        return PortableBatteryBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PortableBatteryBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.PORTABLE_BATTERY.get();
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var stacks = super.getDroppedStacks(state, builder);
        return stacks;
    }

    /**
     * @see com.simibubi.create.content.equipment.armor.BacktankBlock#onUse(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult)
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(player == null)
            return ActionResult.PASS;
        if(player instanceof FakePlayer)
            return ActionResult.PASS;
        if(player.isSneaking())
            return ActionResult.PASS;
        if(player.getMainHandStack().getItem() instanceof BlockItem)
            return ActionResult.PASS;
        if(!player.getEquippedStack(EquipmentSlot.CHEST).isEmpty())
            return ActionResult.PASS;
        if(!world.isClient) {
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .75f, 1);
            player.equipStack(EquipmentSlot.CHEST, getPickStack(world, pos, state));
            world.breakBlock(pos, false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(worldIn, pos, state, placer, stack);
        if(worldIn.isClient)
            return;
        if(stack == null)
            return;
        withBlockEntityDo(worldIn, pos, be -> {
            int level = EnchantmentHelper.getLevel(AllEnchantments.CAPACITY.get(), stack);
            be.setCapacityEnchantLevel(level);
            be.setCharge(BatteryUtils.getCurrentCharge(stack));

            var vanillaTag = stack.getOrCreateNbt();
            if(stack.hasCustomName())
                be.setName(stack.getName());

            be.setTags(vanillaTag);
            be.markDirty();
        });
    }

    public static float resistance() {
        return ModdedConfigs.server().electricity.portableBatteryResistance.getF();
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return getBlockEntityOptional(world, pos).map(PortableBatteryBlockEntity::getComparatorOutput).orElse(0);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        var item = asItem();
        if(item instanceof BacktankItem.BacktankBlockItem placeable)
            item = placeable.getActualItem();

        var be = getBlockEntityOptional(world, pos);
        var vanillaTag = be.map(PortableBatteryBlockEntity::getVanillaTag)
                .orElse(new NbtCompound());
        var charge = be.map(PortableBatteryBlockEntity::getCharge)
                .orElse(0);

        ItemStack stack = new ItemStack(item, 1);
        vanillaTag.putInt("Charge", charge);
        stack.setNbt(vanillaTag);
        return stack;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        var item = asItem();
        if(item instanceof BacktankItem.BacktankBlockItem placeable)
            item = placeable.getActualItem();
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, item);
    }
}
