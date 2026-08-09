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
package org.patryk3211.powergrid.equipment.portablebattery;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.List;

public class PortableBatteryBlock extends HorizontalElectricBlock implements IBE<PortableBatteryBlockEntity>, SpecialBlockItemRequirement, IAcceptCord {
    private static final VoxelShape SHAPE = Shapes.or(
            box(4, 0, 4, 12, 9, 12),
            box(5, 9, 5, 11, 12, 11)
    );

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
        new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4.5, 9.5, 6.5, 6.5, 12.5, 9.5),
        new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 9.5, 9.5, 6.5, 11.5, 12.5, 9.5)
    };

    public PortableBatteryBlock(Properties settings) {
        super(settings);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection
                        .each(TERMINALS_NORTH, terminal -> terminal
                                .rotateAroundY((int) (180 - state.getValue(HORIZONTAL_FACING).toYRot()))))
                .withShapeMapper(state -> SHAPE)
                .build());
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public int capacity() {
        return ModdedConfigs.server().equipment.portableBatteryBaseCapacity.get();
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
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pBuilder) {
        List<ItemStack> lootDrops = super.getDrops(pState, pBuilder);

        BlockEntity blockEntity = pBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof PortableBatteryBlockEntity be))
            return lootDrops;

        DataComponentPatch components = be.getDataPatch()
                .forget(c -> c.equals(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get()));
        if (components.isEmpty())
            return lootDrops;

        return lootDrops.stream()
                .peek(stack -> {
                    if (stack.getItem() instanceof PortableBatteryItem) {
                        stack.set(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get(), be.getCharge());
                        stack.applyComponents(components);
                    }
                })
                .toList();
    }

    /**
     * @see com.simibubi.create.content.equipment.armor.BacktankBlock#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)
     */
    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(player == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(PlayerUtilities.isFake(player))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var heldItem = player.getMainHandItem().getItem();
        if(heldItem instanceof BlockItem || IWire.isWire(world, heldItem))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!world.isClientSide) {
            world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .75f, 1);
            player.setItemSlot(EquipmentSlot.CHEST, getCloneItemStack(world, pos, state));
            world.destroyBlock(pos, false);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if(worldIn.isClientSide)
            return;
        if(stack == null)
            return;
        withBlockEntityDo(worldIn, pos, be -> {
            int level = EnchantmentHelper.getItemEnchantmentLevel(
                    worldIn.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(AllEnchantments.CAPACITY),
                    stack
            );
            be.setCapacityEnchantLevel(level);
            be.setCharge(BatteryUtils.getCurrentCharge(stack));
            if(stack.has(DataComponents.CUSTOM_NAME))
                be.setName(stack.getHoverName());
            be.setDataPatch(stack.getComponentsPatch());
            be.setChanged();
        });
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return getBlockEntityOptional(world, pos).map(PortableBatteryBlockEntity::getComparatorOutput).orElse(0);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        var item = asItem();
        if(item instanceof BacktankItem.BacktankBlockItem placeable)
            item = placeable.getActualItem();

        var be = getBlockEntityOptional(world, pos);
        var components = be.map(PortableBatteryBlockEntity::getDataPatch)
                .orElse(DataComponentPatch.EMPTY);
        var charge = be.map(PortableBatteryBlockEntity::getCharge)
                .orElse(0);

        ItemStack stack = new ItemStack(item, 1);
        stack.applyComponents(components);
        stack.set(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get(), charge);
        return stack;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        var item = asItem();
        if(item instanceof BacktankItem.BacktankBlockItem placeable)
            item = placeable.getActualItem();
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, item);
    }

    @Override
    public boolean renderPlug() {
        return true;
    }

    @Override
    public @Nullable AutoCordEndpoint getEndpoint(UseOnContext context) {
        var pos = context.getClickedPos();
        var center = Vec3.atCenterOf(pos);
        var point = center.add(0, 0.4375f, 0);
        return new AutoCordEndpoint(context.getClickedPos(), 0, 1, point, Direction.UP);
    }

    @Override
    public @Nullable ITerminalPlacement cordTerminal(BlockState state, Level level, BlockHitResult hit) {
        return new TerminalBoundingBox(IDecoratedTerminal.CORD,
                6, 12, 6,
                10, 14, 10);
    }
}
