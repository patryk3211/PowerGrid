package org.patryk3211.powergrid.electricity.modulardisplay;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.modulardisplay.modules.*;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.network.packets.DisplayBurnoutS2CPacket;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.List;

public class ModularDisplayBlockEntity extends ElectricBlockEntity{
    private AbstractElectricWire[] wires;
    public static final int SLOT_COUNT = 4;
    private ScrollOptionBehaviour<DisplayModuleType> moduleTypeBehaviour;
    public int lastHitSlot = 0;
    public final IDisplayModule[] modules = new IDisplayModule[SLOT_COUNT];
    private DisplaySlotThermal[] slotThermals;
    private boolean[] removeBlankingPage = new boolean[SLOT_COUNT];

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        slotThermals = new DisplaySlotThermal[SLOT_COUNT];
        moduleTypeBehaviour = new ScrollOptionBehaviour<>(
                DisplayModuleType.class,
                Lang.translateDirect("devices.modular_display.module_type"),
                this,
                new CustomValueBoxTransformer(this)
        ) {
            @Override
            public boolean bypassesInput(ItemStack mainhandItem) {
                return mainhandItem.getItem() instanceof DyeItem || AllItems.WRENCH.isIn(mainhandItem);
            }
        };

        moduleTypeBehaviour.setValue(0);
        moduleTypeBehaviour.withCallback(value -> onSlotTypeChanged(lastHitSlot, value));
        behaviours.add(moduleTypeBehaviour);
        int w1 = 0, w2 = 1, w3 = 2;
        for (int i = 0; i < SLOT_COUNT; i++) {
            final int slot = i;
            slotThermals[i] = new DisplaySlotThermal(
                    this,
                    slot,
                    .15f,
                    25 / (125 - 22f), //~.24
                    () -> {
                        modules[slot] = null;
                        emptySlotWires(slot);
                        markUpdated();
                        if (level instanceof ServerLevel serverLevel) {
                            ModdedPackets.sendToClientsAround(
                                    new DisplayBurnoutS2CPacket(worldPosition, slot),
                                    serverLevel,
                                    Vec3.atCenterOf(worldPosition),
                                    64.0
                            );
                        }

                    },
                    List.of(wires[w1], wires[w2], wires[w3])
            );
            w1+=3; w2+=3; w3+=3;
            behaviours.add(slotThermals[i]);
        }
    }

    private void onSlotTypeChanged(int slot, int value) {
        DisplayModuleType type = DisplayModuleType.values()[value];
        if (modules[slot] == null) return;
        var lastColor = modules[slot].getColor();
        switch (type) {
            case ZERO_TO_NINE -> modules[slot] = new ZeroToNineNumberModule(0, false, lastColor);
            case NINE_TO_ZERO -> modules[slot] = new NineToZeroNumberModule(0, false, lastColor);
            case ONE_TO_ZERO -> modules[slot] = new OneToZeroNumberModule(0, false, lastColor);
            case HEXADECIMAL -> modules[slot] = new HexadecimalAlphanumericModule(0, false, lastColor);
            case SYMBOLS -> modules[slot] = new SymbolLetterModule(0, false, lastColor);
            case ALPHABET -> modules[slot] = new AlphabetLetterModule(0, false, lastColor);
        }
        markUpdated();
        defaultSlotWires(slot);
    }

    public void syncBehaviourToSlot(int slot) {
        if (modules[slot] == null) return;
        moduleTypeBehaviour.value = modules[slot].getDisplayModuleType().ordinal();
    }

    public SlotData getSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) return SlotData.empty();
        return new SlotData(modules[index]);
    }

    public void interact(int slotIndex, Player player){
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return;

        ItemStack held = player.getMainHandItem();
        IDisplayModule heldModule = resolveModule(held);
        IDisplayModule current = modules[slotIndex];
        if (held.isEmpty() && player.isCrouching()) {
            if (current == null) return;
            if (!player.getInventory().add(new ItemStack(ModdedItems.DISPLAY_MODULE.get()))) {
                player.drop(new ItemStack(ModdedItems.DISPLAY_MODULE.get()), false);
            }
            modules[slotIndex] = null;
            slotThermals[slotIndex].resetTemperature();
            emptySlotWires(slotIndex);
            markUpdated();
            return;
        }

        if (heldModule == null) {
            if (AllItems.WRENCH.isIn(player.getItemInHand(InteractionHand.MAIN_HAND))) {
                removeBlankingPage[slotIndex] = !removeBlankingPage[slotIndex];
                setIndex(slotIndex, 0);
                setHalfClick(slotIndex, false);
                defaultSlotWires(slotIndex);
                markUpdated();
                return;
            }
            return;
        }

        modules[slotIndex] = heldModule;
        if (!player.isCreative()) held.shrink(1);
        defaultSlotWires(slotIndex);
        markUpdated();
    }

    public void setColor(int slotIndex, DyeColor color) {
        modules[slotIndex] = modules[slotIndex].withColor(color);
        markUpdated();
    }

    @Nullable
    private IDisplayModule resolveModule(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ModdedItems.DISPLAY_MODULE.get())) {
            return new ZeroToNineNumberModule(0, false, DyeColor.WHITE);
        }
        return null;
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void defaultSlotWires(int slotIndex) {
        var negative = (SwitchedWire) wires[slotIndex * 3+1];
        var reset = (SwitchedWire) wires[slotIndex * 3+2];
        reset.setState(false);
        negative.setState(true);
    }

    private void emptySlotWires(int slotIndex) {
        var negative = (SwitchedWire) wires[slotIndex * 3+1];
        var reset = (SwitchedWire) wires[slotIndex * 3+2];
        reset.setState(false);
        negative.setState(false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        ListTag slotList = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            IDisplayModule module = modules[i];
            slotList.add(StringTag.valueOf(module != null ? module.serialize() : ""));
        }
        var rbp = new ArrayList<Integer>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            rbp.add(removeBlankingPage[i] ? 1 : 0);
        }
        tag.put("slots", slotList);
        tag.putIntArray("rbp", rbp);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);

        ListTag slotList = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            IDisplayModule module = modules[i];
            slotList.add(StringTag.valueOf(module != null ? module.serialize() : ""));
        }
        var rbp = new ArrayList<Integer>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            rbp.add(removeBlankingPage[i] ? 1 : 0);
        }
        tag.put("slots", slotList);
        tag.putIntArray("rbp", rbp);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        if (tag.contains("slots", Tag.TAG_LIST)) {
            ListTag slotList = tag.getList("slots", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(slotList.size(), SLOT_COUNT); i++) {
                modules[i] = DisplayModuleRegistry.deserialize(slotList.getString(i));
                defaultSlotWires(i);
            }
        }
        if (tag.contains("rbp", Tag.TAG_INT_ARRAY)) {
            var rbp = tag.getIntArray("rbp");
            for (int i = 0; i < Math.min(rbp.length, SLOT_COUNT); i++) {
                removeBlankingPage[i] = rbp[i] == 1;
            }
        }
    }

    public void setIndex(int slotIndex, int digit) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return;
        if (modules[slotIndex] != null) {
            modules[slotIndex] = modules[slotIndex].withIndex(digit);
        }
    }

    public void add1ToIndex(int slotIndex){
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return;
        if (modules[slotIndex] != null) {
            int newVal = modules[slotIndex].getIndex() + 1;
            modules[slotIndex] = modules[slotIndex].withIndex(newVal);
        }
    }

    public void setHalfClick(int slotIndex, boolean halfClick) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return;
        if (modules[slotIndex] != null) {
            modules[slotIndex] = modules[slotIndex].withHalfClick(halfClick);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(AThermalBehaviour.shouldOverheat()) {
            int w1 = 0, w2 = 1, w3 = 2;
            for (int i = 0; i < SLOT_COUNT; i++) {
                var slot = getSlot(i);
                if (slot.isEmpty() || slotThermals[i] == null){
                    w1+=3; w2+=3; w3+=3;
                    continue;
                }

                slotThermals[i].tick();
                w1+=3; w2+=3; w3+=3;
            }
        }
    }

    @Override
    public void electricalTick() {
        for (AbstractElectricWire wire : wires) applyPower(wire);
        int w1 = 0, w2 = 1, w3 = 2;
        boolean updated = false;
        boolean playSound = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            var coilNodeToNegative = (SwitchedWire) wires[w2];
            var coilNodeToReset = (SwitchedWire) wires[w3];
            var coilNodeToNegativeCurrent = Math.abs(coilNodeToNegative.current());
            var coilNodeToResetCurrent = Math.abs(coilNodeToReset.current());
            var slot = getSlot(i);
            if (!slot.isEmpty()) {
                var charCount = removeBlankingPage[i] ? slot.getModule().getDisplayTextureCharacterCount() - 1 :
                        slot.getModule().getDisplayTextureCharacterCount();
                //every module display texture has the characters in the sprite plus a blank space and the first character again for smooth transition
                //but im only counting characters before the blank space and adding one for the blank space and two for the transition
                if (coilNodeToNegativeCurrent >= .5 && slot.getIndex() != charCount+1 && !slot.getHalfClick()) {
                    add1ToIndex(i);
                    setHalfClick(i, true);
                    playSound = true;
                    updated = true;
                }

                if (coilNodeToNegativeCurrent < .5 && slot.getIndex() == charCount+1 && coilNodeToNegative.getState()){
                    playSound = true;
                    coilNodeToNegative.setState(false);
                    coilNodeToReset.setState(true);
                    setHalfClick(i, false);
                    updated = true;
                }

                if (coilNodeToNegativeCurrent < .5 && coilNodeToNegative.getState() && slot.getHalfClick()) {
                    playSound = true;
                    setHalfClick(i, false);
                    updated = true;
                }

                if (coilNodeToReset.getState() && coilNodeToResetCurrent >= .5 && slot.getIndex() == charCount+1) {
                    playSound = true;
                    add1ToIndex(i);
                    setHalfClick(i, true);
                    coilNodeToNegative.setState(true);
                    coilNodeToReset.setState(false);
                    updated = true;
                }

                if (slot.getIndex() >= charCount+2 && !slot.getHalfClick()){
                    setIndex(i, 0);
                    updated = true;
                }
            }
            w1+=3; w2+=3; w3+=3;
        }

        if (playSound) {
            ModdedSoundEvents.RELAY_CLICK.playOnServer(level, worldPosition, .75f, 2f);
        }

        if (updated) {
            markUpdated();
        }

    }

    public ModularDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2 * SLOT_COUNT + 1);
        wires = new AbstractElectricWire[3 * SLOT_COUNT];
        var negative = builder.terminalNode(8);
        int p = 0, r = 1, w1 = 0, w2 = 1, w3 = 2;
        for (int s = 0; s < SLOT_COUNT; s++) {
            var coilNode = builder.addInternalNode();

            wires[w1] = builder.connect(25, builder.terminalNode(p), coilNode);
            wires[w2] = builder.connectSwitch(0.1f, negative, coilNode, false);
            wires[w3] = builder.connectSwitch(0.1f, builder.terminalNode(r), coilNode, false);
            p += 2; r += 2; w1 += 3; w2 += 3; w3 += 3;
        }
    }

    public boolean getRemovedBlankingPage(int index){
        return removeBlankingPage[index];
    }

    @Nullable
    @Environment(EnvType.CLIENT)
    public static Component overlayText(Player player) {
        if(!AllItems.WRENCH.isIn(player.getItemInHand(InteractionHand.MAIN_HAND)))
            return null;
        HitResult hit = Minecraft.getInstance().hitResult;
        if(!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS)
            return null;
        var state = Minecraft.getInstance().level.getBlockState(blockHit.getBlockPos());
        if(!(state.getBlock() instanceof ModularDisplayBlock displayBlock))
            return null;

        Direction facing = state.getValue(ModularDisplayBlock.HORIZONTAL_FACING);
        var bhit = (BlockHitResult) hit;
        Vec3 localHit = hit.getLocation().subtract(bhit.getBlockPos().getX(), bhit.getBlockPos().getY(), bhit.getBlockPos().getZ());
        double bestDist = Double.MAX_VALUE;
        int bestSlot = 0;

        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;

            double slotX, slotY, slotZ;
            double u = (col * 8 + 4) / 16f;
            double v = ((1 - row) * 8 + 4) / 16f;

            switch (facing) {
                case NORTH -> { slotX = u;        slotY = v; slotZ = 0.0;}
                case SOUTH -> { slotX = 1.0 - u;  slotY = v; slotZ = 1.0;}
                case WEST  -> { slotX = 0.0;      slotY = v; slotZ = 1.0 - u;}
                case EAST  -> { slotX = 1.0;      slotY = v; slotZ = u;}
                default    -> { slotX = 0.5;      slotY = v; slotZ = 0.5;}
            }

            double dist = localHit.distanceTo(new Vec3(slotX, slotY, slotZ));
            if (dist < bestDist) {
                bestDist = dist;
                bestSlot = i;
            }
        }
        var be = displayBlock.getBlockEntity(Minecraft.getInstance().level, blockHit.getBlockPos());
        if (be == null) return null;
        var removed = be.getRemovedBlankingPage(bestSlot);

        return Lang.translate("gui.modular_display.blank")
                .add(Lang.translate("generic." + (removed ? "disabled" : "enabled"))
                        .style(ChatFormatting.BLUE))
                .style(ChatFormatting.GRAY)
                .component();
    }
}
