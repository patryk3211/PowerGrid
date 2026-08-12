package org.patryk3211.powergrid.equipment;

import com.simibubi.create.AllItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public enum PGToolMaterials implements Tier {
    ZINC(150, 6.0f, 1.5f, BlockTags.INCORRECT_FOR_IRON_TOOL, 12, () -> Ingredient.of(AllItems.ZINC_INGOT.asItem())),
    ZINC_DRILL(250, 8.0f, 3.0f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 12, () -> Ingredient.of(AllItems.ZINC_INGOT.asItem()));

    private final int uses;
    private final float speed;
    private final float attackDamage;
    private final TagKey<Block> incorrectBlocksForDrops;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    PGToolMaterials(int uses, float speed, float attackDamage, TagKey<Block> incorrectBlocksForDrops, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocksForDrops;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }
}
