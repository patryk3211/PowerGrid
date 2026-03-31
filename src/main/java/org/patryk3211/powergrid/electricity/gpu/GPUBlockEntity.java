package org.patryk3211.powergrid.electricity.gpu;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class GPUBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private ElectricWire power;
    private ElectricWire[] ctrl;
    private boolean hasSoundSource = false;

    public float angle, anglePrev;
    private int honseDelay = 0;

    public GPUBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean enabled(int feature) {
        return ctrl[feature].potentialDifference() >= 5;
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        if(thermalBehaviour != null) {
            thermalBehaviour.applyWirePower(power);
        }
        if(fan() > 0.25f) {
            var random = level.random;
            if(enabled(0)) {
                float chance = 1.0f - fan() * 0.1f;
                if(random.nextFloat() > chance) {
                    int range = (int) (fan() * 32);
                    int x = worldPosition.getX() + random.nextIntBetweenInclusive(-range, range);
                    int y = worldPosition.getY() + random.nextIntBetweenInclusive(-range/8, range/8);
                    int z = worldPosition.getZ() + random.nextIntBetweenInclusive(-range, range);
                    if (worldPosition.getX() != x || worldPosition.getY() != y || worldPosition.getZ() != z) {
                        level.destroyBlock(new BlockPos(x, y, z), true);
                    }
                }
            }
            if(enabled(1)) {
                float chance = 1.0f - fan() * 0.05f;
                if(random.nextFloat() > chance) {
                    int count = BuiltInRegistries.ITEM.size();
                    var item = BuiltInRegistries.ITEM.byId(random.nextInt(count));
                    var player = level.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 16, false);
                    if(player != null) {
                        player.addItem(new ItemStack(item));
                    }
                }
            }
            if(enabled(2)) {
                var bb = new AABB(worldPosition).inflate(fan() * 16);
                var living = level.getEntitiesOfClass(LivingEntity.class, bb);
                for(var entity : living) {
                    entity.setSecondsOnFire(3);
                }
            }
            if(enabled(3)) {
                if(honseDelay > 40) {
                    var horse = EntityType.HORSE.create(level);
                    if(horse != null) {
                        horse.setPos(worldPosition.getX() + 0.5f, worldPosition.getY() + 1.5f, worldPosition.getZ() + 0.5f);
                        level.addFreshEntity(horse);
                    }
                } else {
                    ++honseDelay;
                }
            } else {
                honseDelay = 0;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(level.isClientSide)
            clientTick();
    }

    @Environment(EnvType.CLIENT)
    private void clientTick() {
        anglePrev = angle;
        angle += fan() * 4f;
        if(angle > Math.PI * 2 && anglePrev > Math.PI * 2) {
            angle -= Math.PI * 2;
            anglePrev -= Math.PI * 2;
        }
        if(fan() > 0.25f) {
            if(enabled(2)) {
                var range = fan() * 16f;
                var random = level.random;
                for(int i = 0; i < 4; ++i) {
                    float x = worldPosition.getX() + random.nextFloat() * range * 2 - range;
                    float y = worldPosition.getY() + random.nextFloat() * range * 2 - range;
                    float z = worldPosition.getZ() + random.nextFloat() * range * 2 - range;
                    level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this).overheatCallback(() -> {
            level.explode(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 20, Level.ExplosionInteraction.BLOCK);
        });
    }

    public float fan() {
        if(thermalBehaviour == null)
            return 0.5f;
        return Math.max((thermalBehaviour.getTemperature() - 25) / 100, 0);
    }

    @Override
    public void tickAudio() {
        super.tickAudio();
        if(!hasSoundSource && fan() > 0.05f) {
            Minecraft.getInstance().getSoundManager().play(new GPUSoundInstance(this));
            hasSoundSource = true;
        } else if(hasSoundSource && fan() < 0.05f) {
            hasSoundSource = false;
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(6);
        power = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
        ctrl = new ElectricWire[4];
        for(int i = 0; i < 4; ++i) {
            ctrl[i] = builder.connect(100f, builder.terminalNode(2 + i), builder.terminalNode(1));
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.text("Status: ")
                .add(fan() > 0.25f ? Component.literal("ON") : Component.literal("OFF"))
                .forGoggles(tooltip);
        Lang.text("Features: ")
                .forGoggles(tooltip);
        if(enabled(0)) {
            Lang.text("- Miner").forGoggles(tooltip, 1);
        }
        if(enabled(1)) {
            Lang.text("- Generator").forGoggles(tooltip, 1);
        }
        if(enabled(2)) {
            Lang.text("- Firewall").forGoggles(tooltip, 1);
        }
        if(enabled(3)) {
            Lang.text("- Trojan Horse").forGoggles(tooltip, 1);
        }
        return true;
    }
}
