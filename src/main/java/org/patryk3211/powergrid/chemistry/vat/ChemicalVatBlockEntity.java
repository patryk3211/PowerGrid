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
package org.patryk3211.powergrid.chemistry.vat;

import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.particle.FluidParticleData;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.patryk3211.powergrid.chemistry.GasConstants;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.reagent.mixture.ConstantReagentMixture;
import org.patryk3211.powergrid.chemistry.reagent.mixture.MixtureHelper;
import org.patryk3211.powergrid.chemistry.reagent.mixture.ReagentMixture;
import org.patryk3211.powergrid.chemistry.reagent.mixture.VolumeReagentInventory;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionGetter;
import org.patryk3211.powergrid.chemistry.recipe.RecipeProgressStore;
import org.patryk3211.powergrid.chemistry.vat.particles.BubbleParticleData;
import org.patryk3211.powergrid.chemistry.vat.particles.GasParticleData;
import org.patryk3211.powergrid.chemistry.vat.upgrade.ChemicalVatUpgrade;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PreciseNumberFormat;
import org.patryk3211.powergrid.utility.Unit;

import java.util.*;

import static org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock.*;

@SuppressWarnings("UnstableApiUsage")
public class ChemicalVatBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity, IHaveGoggleInformation {
    // TODO: Balance this value.
    public static final float DISSIPATION_FACTOR = 30f;
    public static final float LIQUID_STACK_PRESSURE_CONSTANT = 0.015f;

    private final VolumeReagentInventory reagentInventory;
    private final RecipeProgressStore progressStore;

    protected final Map<Direction, ItemStack> upgrades = new HashMap<>();

    private final Vector3d gasMomentum = new Vector3d();
    private int bubbles = 0;

    private StorageView<FluidVariant> maxFluid;

    public ChemicalVatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        reagentInventory = new VolumeReagentInventory(Reagent.BLOCK_MOLE_AMOUNT * 8);
        progressStore = new RecipeProgressStore();
        setLazyTickRate(20);
    }

    private float diffusionRate() {
        return (reagentInventory.temperature() + 273.15f) * 0.000025f;
    }

    @Override
    public void tick() {
        super.tick();

        boolean stillBurning = false;
        var recipes = ReactionGetter.getValidRecipes(world.getRecipeManager(), reagentInventory);
        if(!recipes.isEmpty()) {
            Collections.shuffle(recipes);
            for(int i = 0; i < recipes.size(); ++i) {
                var reaction = recipes.get(i);
                // Test if the reaction is still valid.
                if(reaction.test(reagentInventory)) {
                    reagentInventory.applyReaction(reaction, progressStore);
                    if(reaction.hasFlag(ReactionFlag.COMBUSTION)) {
                        stillBurning = true;
                    }
                }
            }
        }
        progressStore.filter(recipes);
        if(!stillBurning) {
            reagentInventory.setBurning(false);
        }

        // Dampen momentum
        gasMomentum.mul(0.95f);
        if(!Double.isFinite(gasMomentum.x))
            gasMomentum.x = 0;
        if(!Double.isFinite(gasMomentum.y))
            gasMomentum.y = 0;
        if(!Double.isFinite(gasMomentum.z))
            gasMomentum.z = 0;

        // Moving has to occur after recipe processing so that the burning flag is valid.
        moveReagents();

        if(getCachedState().get(ChemicalVatBlock.OPEN)) {
            // Allow gasses in and out
            reagentInventory.setOpen(true);

            var volume = reagentInventory.getFreeVolume() + 2000;
            var vatPressure = reagentInventory.staticPressure();

            var moveFraction = GasConstants.ATMOSPHERIC_PRESSURE - vatPressure;
            var moveAmount = GasConstants.calculateMoveAmount(GasConstants.ATMOSPHERIC_PRESSURE, vatPressure, volume,
                    GasConstants.ATMOSPHERE_ABSOLUTE_TEMPERATURE, (float) reagentInventory.getAbsoluteTemperature());
            var startAmount = reagentInventory.getGasAmount();
            if(moveAmount < 0) {
                moveAmount = (int) -Math.min(-moveAmount, startAmount * 0.9f);
            }

            int diffuseAmount = (int) (diffusionRate() * reagentInventory.getGasAmount()) - Math.abs(moveAmount);

            ReagentMixture diffused = null, moved = null;
            try(var transaction = Transaction.openOuter()) {
                if(diffuseAmount > 0) {
                    diffused = reagentInventory.remove(diffuseAmount, ReagentState.GAS, transaction);
                    reagentInventory.add(ConstantReagentMixture.ATMOSPHERE.scaledTo(diffused.getTotalAmount()), transaction);
                }
                if(moveAmount < 0) {
                    if(moveAmount < -100000)
                        moveAmount = -100000;
                    moved = reagentInventory.remove(-moveAmount, ReagentState.GAS, transaction);
                } else {
                    if(moveAmount > 100000)
                        moveAmount = 100000;
                    reagentInventory.add(ConstantReagentMixture.ATMOSPHERE.scaledTo(moveAmount), transaction);
                }
                transaction.commit();

                if(moveAmount > 0) {
                    var targetFractionVelocity = Math.min(moveAmount * 0.001f / 0.05f, speedOfSound());
                    gasMomentum.add(0, -targetFractionVelocity * moveAmount * 0.001f, 0);
                } else if(moveAmount < 0) {
                    moveFraction = (float) moveAmount / startAmount;
                    processGasMovement(Direction.UP, (float) -moveFraction, -moveAmount, null);
                }
            }

            if(world.isClient) {
                if(diffused != null && moved != null) {
                    try(var transaction = Transaction.openOuter()) {
                        diffused.add(moved, transaction);
                        transaction.commit();
                    }
                } else if(diffused == null) {
                    diffused = moved;
                }

                if(diffused != null) {
                    createGasParticles(diffused);
                }
            }

            // Spill liquids
            var liquidLevel = reagentInventory.getFillLevel();
            if(liquidLevel > 1) {
                float spillVolume = liquidLevel - 1;
                int spillAmount = (int) (spillVolume * reagentInventory.getVolume());
                try(var transaction = Transaction.openOuter()) {
                    var removed = reagentInventory.remove(spillAmount, ReagentState.LIQUID, transaction);
                    if(world.isClient) {
                        float particles = removed.getTotalAmount() / 100f;
                        var r = world.random;

                        while(r.nextFloat() < particles) {
                            var cPos = pos.toCenterPos();
                            var x = cPos.x + r.nextFloat() - 0.5f;
                            var y = cPos.y + 0.5f + r.nextFloat() * 0.1f;
                            var z = cPos.z + r.nextFloat() - 0.5f;

                            world.addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0, 0);
                            particles -= 1;
                        }
                    }
                    transaction.commit();
                }
            }
        } else {
            reagentInventory.setOpen(false);

            var tempDiff = reagentInventory.temperature() - GasConstants.ATMOSPHERE_TEMPERATURE;
            reagentInventory.removeEnergy(tempDiff * DISSIPATION_FACTOR * 0.05f);
        }

        var heat = BasinBlockEntity.getHeatLevelOf(world.getBlockState(pos.down()));
        if(heat.isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING)) {
            applyHeater(90000, 1300);
        } else if(heat.isAtLeast(BlazeBurnerBlock.HeatLevel.KINDLED)) {
            applyHeater(30000, 500);
        } else if(heat.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) {
            applyHeater(10000, 150);
        }

        if(world.isClient) {
            if(maxFluid != null && maxFluid.getAmount() == 0)
                maxFluid = null;
            for(var fluid : getFluidStorage(null)) {
                if(fluid.isResourceBlank())
                    continue;
                if(fluid.getAmount() <= 0)
                    continue;
                if(maxFluid != null) {
                    // Allow 5% difference to prevent flickering.
                    if(maxFluid.getAmount() < fluid.getAmount() * 0.95f) {
                        maxFluid = fluid;
                    }
                } else {
                    maxFluid = fluid;
                }
            }
            createFluidParticles();
        }

        markDirty();
        if(reagentInventory.wasAltered())
            sendData();
    }

    public void addBubbles(int amount) {
        if(world.isClient)
            bubbles += amount;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        sendData();
    }

    public void applyHeater(float power, float temperature) {
        float diff = (float) (temperature - reagentInventory.getTemperaturePrecise());
        float energyDiff = (float) (diff * reagentInventory.heatMass());
        if(energyDiff < 0)
            energyDiff = 0;
        var maxEnergyChange = power * 0.05f;
        reagentInventory.addEnergy(Math.min(maxEnergyChange, energyDiff));
    }

    public void moveReagents() {
        var gasses = new HashSet<Reagent>();
        var liquids = new HashSet<Reagent>();
        var solids = new HashSet<Reagent>();

        reagentInventory.getReagents().forEach(reagent -> {
            switch(reagentInventory.getState(reagent)) {
                case GAS -> gasses.add(reagent);
                case LIQUID -> liquids.add(reagent);
                case SOLID -> solids.add(reagent);
            }
        });

        for(var dir : Direction.values()) {
            var vat = getVat(pos.offset(dir));
            if(vat == null)
                continue;
            if(dir == Direction.DOWN && !solids.isEmpty()) {
                // Solids can only go down.
                MixtureHelper.moveReagents(reagentInventory, solids, vat.reagentInventory, reagentInventory.getTotalAmount());
            }
            if(!liquids.isEmpty()) {
                var liquidPressure1 = reagentInventory.getFillLevel();
                var liquidPressure2 = vat.reagentInventory.getFillLevel();

                float moveFraction;
                if(dir.getAxis() != Direction.Axis.Y) {
                    // Equalize the levels.
                    var targetLevel = (liquidPressure1 + liquidPressure2) * 0.5f;
                    moveFraction = liquidPressure1 - targetLevel;
                } else if(dir == Direction.UP) {
                    // Move fluid above the max standard pressure.
                    var expectedAdditionalPressure = Math.min(liquidPressure2, 1) * LIQUID_STACK_PRESSURE_CONSTANT;
                    expectedAdditionalPressure += liquidPressure2 - 1;
                    expectedAdditionalPressure = Math.max(expectedAdditionalPressure, 0);
                    moveFraction = Math.max(liquidPressure1 - 1.0f - expectedAdditionalPressure, 0) * 0.5f;
                } else {
                    // Move as much liquid down as possible.
                    var additionalPressure = Math.min(liquidPressure1, 1) * LIQUID_STACK_PRESSURE_CONSTANT;
                    additionalPressure += liquidPressure1 - 1;
                    additionalPressure = Math.max(additionalPressure, 0);
                    var missingLevel = 1.0f - liquidPressure2 + additionalPressure;
                    moveFraction = Math.min(missingLevel * 0.5f, liquidPressure1);
                }

                int moveAmount = (int) (moveFraction * reagentInventory.getVolume());
                int diffuseAmount = (int) (reagentInventory.getLiquidAmount() * diffusionRate()) - Math.abs(moveAmount);
                MixtureHelper.forceMoveReagents(reagentInventory, liquids, vat.reagentInventory, moveAmount);
                if(diffuseAmount > 0) {
                    MixtureHelper.diffuse(reagentInventory, vat.reagentInventory, liquids, ReagentState.LIQUID, diffuseAmount);
                }
            }
            if(!gasses.isEmpty()) {
                if(reagentInventory.getFreeVolume() == 0 && vat.reagentInventory.getFreeVolume() == 0) {
                    // No free volume so no gas movement can occur.
                    continue;
                } else if(reagentInventory.getFreeVolume() == 0) {
                    // Must move all gas from this inventory.
                    MixtureHelper.moveReagents(reagentInventory, gasses, vat.reagentInventory, reagentInventory.getGasAmount());
                } else if(vat.reagentInventory.getFreeVolume() == 0) {
                    // Cannot move gas into a full inventory.
                    continue;
                } else {
                    // Gasses can go anywhere.
                    var gasPressure1 = pressure(dir);
                    var gasPressure2 = vat.pressure(dir.getOpposite());

                    var targetPressure = (gasPressure1 + gasPressure2) * 0.5f;

                    float moveFraction = (float) (gasPressure1 - targetPressure);
                    int moveAmount = GasConstants.calculateMoveAmount(gasPressure1, targetPressure, reagentInventory.getFreeVolume(),
                            (float) reagentInventory.getAbsoluteTemperature(), (float) vat.reagentInventory.getAbsoluteTemperature());
                    moveAmount = (int) Math.min(moveAmount, reagentInventory.getGasAmount() * 0.9f);

                    int diffuseAmount = (int) (reagentInventory.getGasAmount() * diffusionRate()) - Math.abs(moveAmount);
                    moveAmount = MixtureHelper.moveReagents(reagentInventory, gasses, vat.reagentInventory, moveAmount);
                    if (diffuseAmount > 0) {
                        MixtureHelper.diffuse(reagentInventory, vat.reagentInventory, gasses, ReagentState.GAS, diffuseAmount);
                    }

                    if(moveAmount > 0) {
                        processGasMovement(dir, moveFraction, moveAmount, vat);
                    }
                }
            }

            if(reagentInventory.isBurning()) {
                // Propagate burning effects.
                vat.reagentInventory.setBurning(true);
            }
        }
    }

    private void processGasMovement(Direction dir, float moveFraction, float moveAmount, @Nullable ChemicalVatBlockEntity target) {
        if(moveFraction == 0)
            return;
        moveAmount *= 0.001f;
        var fractionVolume = moveFraction * reagentInventory.getFreeVolume() * 0.001f;

        var deltaMX = gasMomentum.x * moveFraction;
        var deltaMY = gasMomentum.y * moveFraction;
        var deltaMZ = gasMomentum.z * moveFraction;

        gasMomentum.sub(deltaMX, deltaMY, deltaMZ);
        if(target != null)
            target.gasMomentum.add(deltaMX, deltaMY, deltaMZ);

        var dirVect = dir.getVector();
        var sourceFractionVelocity = Math.min(fractionVolume / 0.05f, speedOfSound());
        gasMomentum.add(
                dirVect.getX() * sourceFractionVelocity * moveAmount,
                dirVect.getY() * sourceFractionVelocity * moveAmount,
                dirVect.getZ() * sourceFractionVelocity * moveAmount
        );

        if(target != null) {
            var targetFractionVelocity = Math.min(fractionVolume / 0.05f, speedOfSound());
            target.gasMomentum.add(
                    dirVect.getX() * targetFractionVelocity * moveAmount,
                    dirVect.getY() * targetFractionVelocity * moveAmount,
                    dirVect.getZ() * targetFractionVelocity * moveAmount
            );
        }
    }

    public double pressure(Direction direction) {
        var dirVect = direction.getVector();
        var mX = dirVect.getX() * gasMomentum.x;
        var mY = dirVect.getY() * gasMomentum.y;
        var mZ = dirVect.getZ() * gasMomentum.z;
        // Dynamic pressure calculation is an interpretation of the compressible flow dynamic pressure equation:
        // https://en.wikipedia.org/wiki/Dynamic_pressure#Compressible_flow

        // Technically this is not mass, but it's good enough for our purposes.
        var mass = reagentInventory.getGasAmount() * 0.001f;
        var volume = reagentInventory.getFreeVolume() * 0.001f;
        if(mass == 0)
            return 0;
        var velocity = (mX + mY + mZ) / mass;
        var pressure = reagentInventory.staticPressure();
        var speedOfSoundSqr = pressure * volume * GasConstants.HEAT_CAPACITY_RATIO / mass;
        var machNumberSqr = velocity * velocity / speedOfSoundSqr;
        var x = (1 + GasConstants.PRESSURE_HCR_CONST * machNumberSqr);

        var vat = getVat(pos.up());
        double deltaP = 0;
        if(direction == Direction.UP && (vat != null || getCachedState().get(ChemicalVatBlock.OPEN))) {
            // Calculate stack effect pressure gradient
            double temperatureDifference, outsidePressure;
            if(vat != null) {
                double T_o = vat.reagentInventory.getAbsoluteTemperature();
                double T_i = reagentInventory.getAbsoluteTemperature();
                temperatureDifference = (T_o == 0 ? 0 : 1 / T_o) - (T_i == 0 ? 0 : 1 / T_i);
                outsidePressure = vat.reagentInventory.staticPressure();
            } else {
                double T_i = reagentInventory.getAbsoluteTemperature();
                temperatureDifference = 1 / GasConstants.ATMOSPHERE_ABSOLUTE_TEMPERATURE - (T_i == 0 ? 0 : 1 / T_i);
                outsidePressure = GasConstants.ATMOSPHERIC_PRESSURE;
            }
            deltaP = GasConstants.STACK_EFFECT_CONST * outsidePressure * temperatureDifference;
        }

        return pressure * Math.sqrt(x * x * x * x * x) + deltaP;
    }

    public float speedOfSound() {
        return (float) Math.sqrt(reagentInventory.staticPressure() * reagentInventory.getFreeVolume() * 0.001f * GasConstants.HEAT_CAPACITY_RATIO / (reagentInventory.getGasAmount() * 0.001f));
    }

    public double dynamicPressure(Direction direction) {
        return pressure(direction) - reagentInventory.staticPressure();
    }

    @Environment(EnvType.CLIENT)
    private void createFluidParticles() {
        if(!getCachedState().get(OPEN))
            return;
        var r = world.random;
        float fluidLevel = getFluidLevel();

        var bubbleChance = bubbles * 0.1f;
        while(r.nextFloat() < bubbleChance) {
            float bx = pos.getX() + CORNER + SIDE * r.nextFloat();
            float bz = pos.getZ() + CORNER + SIDE * r.nextFloat();
            float by = pos.getY() + CORNER + FLUID_SPAN * fluidLevel * r.nextFloat() * 0.5f;
            world.addParticle(new BubbleParticleData(pos), bx, by, bz, 0, 0, 0);
            bubbleChance -= 1;
        }
        bubbles = 0;

        if(r.nextFloat() > 1 / 12f || maxFluid == null)
            return;

        float surface = pos.getY() + CORNER + FLUID_SPAN * fluidLevel + 1 / 32f;

        for(var fluid : getFluidStorage(null)) {
            if(fluid.isResourceBlank())
                continue;
            if(fluid.getAmount() <= 0)
                continue;
            if(fluid.getResource() == maxFluid.getResource())
                continue;

            float x = pos.getX() + CORNER + SIDE * r.nextFloat();
            float z = pos.getZ() + CORNER + SIDE * r.nextFloat();
            world.addImportantParticle(
                    new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), new FluidStack(fluid)),
                    x, surface, z, 0, 0, 0);
        }
    }

    public StorageView<FluidVariant> getRenderedFluid() {
        return maxFluid;
    }

    private void createGasParticles(ReagentMixture mixture) {
        var r = world.random;
        var x = pos.getX() + 2 / 16f;
        var surface = pos.getY() + 14 / 16f; //+ reagentInventory.getFillLevel();
        var z = pos.getZ() + 2 / 16f;

        for(var reagent : mixture.getReagents()) {
            var color = reagent.getParticleColor();
            if(color == 0)
//                color = 0xFFFFFF;
                continue;
            var amount = mixture.getAmount(reagent);

            float chance = amount / 10f;
            if(chance > 5)
                chance = 5;
            while(r.nextFloat() < chance) {
                float red = ((color >> 16) & 0xFF) / 255f;
                float green = ((color >> 8) & 0xFF) / 255f;
                float blue = (color & 0xFF) / 255f;

                double vX = 0, vY = 0.1f, vZ = 0;
                float mass = reagentInventory.getGasAmount() * 0.001f;
                if(mass > 0) {
                    vX = gasMomentum.x / mass;
                    vY = gasMomentum.y / mass;
                    vZ = gasMomentum.z / mass;
                }
                world.addParticle(new GasParticleData(red, green, blue),
                        x + r.nextFloat() * 12 / 16f, surface, z + r.nextFloat() * 12 / 16f, vX, vY, vZ);
                chance -= 1;
            }
        }
    }

    @Nullable
    public ChemicalVatBlockEntity getVat(BlockPos pos) {
        if(world == null)
            return null;
        if(world.getBlockEntity(pos) instanceof ChemicalVatBlockEntity vat)
            return vat;
        return null;
    }

    public ActionResult use(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(hand == Hand.OFF_HAND)
            return ActionResult.PASS;

        assert world != null;
        var stack = player.getStackInHand(hand);

        // Do fluid actions.
        var direction = hit.getSide();
        if(FluidHelper.tryEmptyItemIntoBE(world, player, hand, stack, this, direction))
            return ActionResult.SUCCESS;
        if(FluidHelper.tryFillItemFromBE(world, player, hand, stack, this, direction))
            return ActionResult.SUCCESS;
        if (GenericItemEmptying.canItemBeEmptied(world, stack) || GenericItemFilling.canItemBeFilled(world, stack))
            return ActionResult.SUCCESS;

        if(stack.isOf(Items.FLINT_AND_STEEL)) {
            if(!world.isClient) {
                if (!player.isCreative())
                    stack.damage(1, player, v -> {});
                light();
            }
            return ActionResult.SUCCESS;
        }
        if(stack.getItem() instanceof ChemicalVatUpgrade upgrade) {
            var upgradeSide = upgrade.isSided() ? upgrade.getSide(hit) : null;
            if(upgrades.containsKey(upgradeSide) || !upgrade.canApply(this, upgradeSide)) {
                // Upgrade slot taken.
                return ActionResult.FAIL;
            }
            if(!world.isClient) {
                var upgradeStack = stack.copyWithCount(1);
                upgrade.applyUpgrade(this, upgradeStack, upgradeSide);
                upgrades.put(upgradeSide, upgradeStack);
                stack.decrement(1);
                sendData();
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    public ActionResult removeUpgrade(@Nullable PlayerEntity player, @Nullable Direction side) {
        var stack = upgrades.remove(side);
        if(stack == null) {
            if(side != Direction.UP)
                stack = upgrades.remove(null);
            if(stack == null)
                return ActionResult.PASS;
        }
        var upgrade = (ChemicalVatUpgrade) stack.getItem();
        upgrade.removeUpgrade(this, stack, side);

        if(world.isClient)
            return ActionResult.SUCCESS;
        sendData();
        if(player != null && player.giveItemStack(stack))
            return ActionResult.SUCCESS;
        // Couldn't give to player so we spawn it in the world.
        var itemPos = pos.toCenterPos().offset(side != null ? side : Direction.UP, 0.6f);
        world.spawnEntity(new ItemEntity(world, itemPos.x, itemPos.y, itemPos.z, stack));
        return ActionResult.SUCCESS;
    }

    public void light() {
        reagentInventory.setBurning(true);
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        if(reagentInventory.getReagents().isEmpty())
            return false;

        Lang.translate("gui.chemical_vat.info_header").forGoggles(tooltip);

        Lang.builder().translate("gui.chemical_vat.temperature")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var temperatureText = String.format("%.2f", reagentInventory.temperature());
        Lang.builder()
                .text(temperatureText)
                .add(Text.of(" "))
                .add(Unit.TEMPERATURE.get())
                .style(Formatting.YELLOW)
                .forGoggles(tooltip, 1);

        tooltip.add(Text.empty());
        Lang.builder().translate("gui.chemical_vat.reagents").forGoggles(tooltip);
        for (var reagent : reagentInventory.getReagents()) {
            var state = reagentInventory.getState(reagent);
            var amount = reagentInventory.getAmount(reagent);
            Text amountText;
            if(state != ReagentState.SOLID) {
                var mb = (amount * Reagent.FLUID_MOLE_RATIO / 81);
                String str;
                if(mb < 1) {
                    str = "<1mB";
                } else {
                    str = String.format("%dmB", (int) mb);
                }
                amountText = Text.literal(str).formatted(Formatting.BLUE);
            } else {
                String str;
                if(reagent.asItem() != null) {
                    var count = (float) amount / reagent.getItemAmount();
                    str = "x" + PreciseNumberFormat.format(count);
                } else {
                    str = String.format("%.3f", (float) amount / 1000);
                }
                amountText = Text.literal(str).formatted(Formatting.GREEN);
            }
            Lang.builder()
                    .add(Text.translatable(reagent.getTranslationKey()).formatted(Formatting.GRAY))
                    .add(Text.of(" "))
                    .add(amountText)
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        reagentInventory.read(tag);
        progressStore.read(tag);
        if(tag.contains("Upgrades")) {
            upgrades.clear();
            var upgradeTag = tag.getCompound("Upgrades");
            for(var indexStr : upgradeTag.getKeys()) {
                var index = Integer.parseInt(indexStr);
                Direction dir = index < 0 ? null : Direction.values()[index];
                var stack = ItemStack.fromNbt(upgradeTag.getCompound(indexStr));
                var upgrade = (ChemicalVatUpgrade) stack.getItem();
                upgrade.readUpgrade(this, stack, dir);
                upgrades.put(dir, stack);
            }
        } else if(!upgrades.isEmpty()) {
            // All upgrades removed
            upgrades.forEach((dir, stack) -> {
                var upgrade = (ChemicalVatUpgrade) stack.getItem();
                upgrade.readRemovedUpgrade(this, stack, dir);
            });
        }
        if(tag.contains("Momentum")) {
            var momentum = tag.getCompound("Momentum");
            gasMomentum.x = momentum.getDouble("X");
            gasMomentum.y = momentum.getDouble("Y");
            gasMomentum.z = momentum.getDouble("Z");
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        reagentInventory.write(tag);
        progressStore.write(tag);

        if(!upgrades.isEmpty()) {
            var upgradeTag = new NbtCompound();
            for (var upgrade : upgrades.entrySet()) {
                var dir = upgrade.getKey();
                var index = dir == null ? -1 : dir.ordinal();
                upgradeTag.put(Integer.toString(index), upgrade.getValue().serializeNBT());
            }
            tag.put("Upgrades", upgradeTag);
        }

        var momentum = new NbtCompound();
        momentum.putDouble("X", gasMomentum.x);
        momentum.putDouble("Y", gasMomentum.y);
        momentum.putDouble("Z", gasMomentum.z);
        tag.put("Momentum", momentum);
    }

    @Nullable
    @Override
    public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        return reagentInventory.getItemView();
    }

    @Nullable
    @Override
    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        return reagentInventory.getFluidView();
    }

    public ReagentMixture getReagentMixture() {
        return reagentInventory;
    }

    public float getFluidLevel() {
        return reagentInventory.getFillLevel();
    }
}
