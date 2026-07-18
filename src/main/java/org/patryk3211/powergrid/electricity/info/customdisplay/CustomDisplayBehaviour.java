/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity.info.customdisplay;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.architectury.registry.menu.MenuRegistry;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedMenus;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.function.Function;
import java.util.function.Supplier;

public class CustomDisplayBehaviour extends BlockEntityBehaviour implements MenuProvider {
    public static final BehaviourType<CustomDisplayBehaviour> TYPE = new BehaviourType<>("custom_display");

    protected String equationStr;
    protected Expression expr;
    protected Unit unit;
    public String unitStr;
    protected boolean prefixes;

    @Nullable
    private final Supplier<Float> maxValue;
    private final Function<Float, ChatFormatting> color;

    public CustomDisplayBehaviour(SmartBlockEntity be, Unit unit,
                                  boolean prefixes, @Nullable Supplier<Float> maxValue, Function<Float, ChatFormatting> color,
                                  String defaultEquation) {
        super(be);
        this.unit = unit;
        this.prefixes = prefixes;
        this.maxValue = maxValue;
        this.color = color;
        this.equationStr = defaultEquation;
        this.expr = Expression.parse(equationStr);
    }

    public static boolean use(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if(!player.getItemInHand(hand).is(Items.NAME_TAG))
            return false;
        var behaviour = BlockEntityBehaviour.get(level, pos, TYPE);
        if(behaviour == null)
            return false;
        if(!level.isClientSide && player instanceof ServerPlayer serverPlayer)
            MenuRegistry.openExtendedMenu(serverPlayer, behaviour, behaviour.blockEntity::sendToMenu);
        return true;
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return blockEntity.getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new CustomDisplayMenu(ModdedMenus.CUSTOM_DISPLAY.get(), i, inventory, blockEntity);
    }

    public LangBuilder format(float baseValue, @Nullable IVarSet variables) {
        if(expr == null) {
            return Lang.translate("gui.invalid_equation")
                    .style(ChatFormatting.RED);
        }
        String line = "";
        if(maxValue != null) {
            if (Math.abs(baseValue) > maxValue.get()) {
                line += baseValue >= 0 ? "> " : "< ";
                if (baseValue < 0)
                    baseValue = -maxValue.get();
                else
                    baseValue = maxValue.get();
            }
        }
        var evaluatedValue = variables != null ? expr.eval(variables) : expr.eval(baseValue);
        if(evaluatedValue >= 0)
            line += " ";
        String prefix;
        if(prefixes) {
            var abs = Math.abs(evaluatedValue);
            if (abs < 1) {
                // Milli
                evaluatedValue *= 1000;
                prefix = "m";
            } else if (abs < 1000) {
                prefix = "";
            } else if (abs < 1000000) {
                // Kilo
                evaluatedValue /= 1000;
                prefix = "k";
            } else {
                // Mega
                evaluatedValue /= 1000000;
                prefix = "M";
            }
        } else {
            prefix = "";
        }
        line += String.format("%.2f %s", evaluatedValue, prefix);
        var component = Lang.text(line)
                .style(color.apply(Math.abs(baseValue)));
        if(unit != null) {
            component.add(unit.get());
        } else if(unitStr != null) {
            component.add(Component.literal(unitStr));
        }
        return component;
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if(nbt.contains("Fmt", CompoundTag.TAG_COMPOUND)) {
            var tag = nbt.getCompound("Fmt");
            equationStr = tag.getString("Eq");
            if(equationStr.isEmpty())
                equationStr = "x";
            expr = Expression.tryParse(equationStr).orElse(null);
            if(tag.contains("Unit")) {
                unit = Unit.values()[tag.getByte("Unit")];
            } else if(tag.contains("UnitS")) {
                unit = null;
                unitStr = tag.getString("UnitS");
            }
            prefixes = tag.getBoolean("Prefix");
        }
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        var tag = new CompoundTag();
        if(unit == null && unitStr != null) {
            tag.putString("UnitS", unitStr);
        } else if(unit != null) {
            tag.putByte("Unit", (byte) unit.ordinal());
        }
        tag.putString("Eq", equationStr);
        tag.putBoolean("Prefix", prefixes);
        nbt.put("Fmt", tag);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public void set(String equation, Unit unit, String unitStr, boolean usePrefixes) {
        this.equationStr = equation;
        this.expr = Expression.tryParse(equationStr).orElse(null);
        this.unit = unit;
        this.unitStr = unitStr;
        this.prefixes = usePrefixes;
        blockEntity.notifyUpdate();
    }
}
