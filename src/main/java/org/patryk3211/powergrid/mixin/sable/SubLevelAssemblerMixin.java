package org.patryk3211.powergrid.mixin.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Contract;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.compat.sable.SableUtils;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblerMixin {
    @Unique
    @Contract("_, null, _ -> null")
    private static IWireEndpoint powerGrid$offsetEndpoint(Level level, IWireEndpoint endpoint, SubLevelAssemblyHelper.AssemblyTransform transform) {
        if(endpoint == null)
            return null;
        return endpoint.makeOffset(level, transform);
    }

    @Unique
    private static void powerGrid$handleJunction(ServerLevel level, WorldNetworks global, SubLevelAssemblyHelper.AssemblyTransform transform, JunctionWireEndpoint junction, Set<TransmissionLinePart> checked, Set<BaseWireEntity> altered, Set<TransmissionLinePart> abandonedLines) {
        var connected = global.findConnectedWires(junction);
        if(connected == null)
            return;
        for(TransmissionLinePart linePart : connected) {
            if(linePart.getEndpoint1() instanceof JunctionWireEndpoint e1 && linePart.getEndpoint2() instanceof JunctionWireEndpoint e2) {
                if(!checked.add(linePart))
                    continue;
                BaseWireEntity owner = linePart.owner;
                if(owner == null) {
                    // Owner might have been moved already (block wires which were kicked out).
                    owner = linePart.persistentOwnerId.getEntity(level);
                    if(owner == null)
                        return;
                    abandonedLines.add(linePart);
                }
                // Needs to be dropped here or else the wires will be killed once junction is empty.
                owner.dropWire();
                linePart.remove();

                var movedE1 = powerGrid$offsetEndpoint(level, e1, transform);
                var movedE2 = powerGrid$offsetEndpoint(level, e2, transform);
                owner.sublevelMove(movedE1, movedE2);
                if(transform.getRotation() != Rotation.NONE) {
                    owner.sublevelRotate(transform.getRotation());
                }
                if(linePart.owner != null)
                    altered.add(linePart.owner);

                if(linePart.getEndpoint1() == junction) {
                    powerGrid$handleJunction(level, global, transform, e2, checked, altered, abandonedLines);
                } else if(linePart.getEndpoint2() == junction) {
                    powerGrid$handleJunction(level, global, transform, e1, checked, altered, abandonedLines);
                } else {
                    PowerGrid.LOGGER.warn("Bad wire tree structure");
                }
            }
        }
    }

    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false, order = 1100, require = 0)
    private static void powerGrid$moveWireStuff(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        var global = GlobalElectricNetworks.getWorldNetworks((LevelAccessor) level);
        if(global == null)
            return;
        var altered = new ReferenceOpenHashSet<BaseWireEntity>();
        var abandonedLines = new ReferenceOpenHashSet<TransmissionLinePart>();
        var checked = new ReferenceOpenHashSet<TransmissionLinePart>();
        for(var blockPos : blocks) {
            var behaviour = ElectricBehaviour.get(level, blockPos, ElectricBehaviour.TYPE);
            if(behaviour == null)
                continue;
            var wires = global.findConnectedWires(behaviour);
            for(TransmissionLinePart linePart : wires) {
                BaseWireEntity owner = linePart.owner;
                if(owner == null) {
                    // Owner might have been moved already (block wires which were kicked out).
                    owner = linePart.persistentOwnerId.getEntity(level);
                    if(owner == null)
                        return;
                    abandonedLines.add(linePart);
                }
                IWireEndpoint endpoint1 = owner.getEndpoint1();
                IWireEndpoint endpoint2 = owner.getEndpoint2();
                if(checked.add(linePart) && !altered.contains(owner)) {
                    if(endpoint1 instanceof JunctionWireEndpoint junction) {
                        endpoint1 = powerGrid$offsetEndpoint(level, endpoint1, transform);
                        endpoint2 = powerGrid$offsetEndpoint(level, endpoint2, transform);
                        // Needs to be dropped here or else the wires will be killed once junction is empty.
                        owner.dropWire();
                        linePart.remove();
                        powerGrid$handleJunction(level, global, transform, junction, checked, altered, abandonedLines);
                    } else if(endpoint2 instanceof JunctionWireEndpoint junction) {
                        endpoint1 = powerGrid$offsetEndpoint(level, endpoint1, transform);
                        endpoint2 = powerGrid$offsetEndpoint(level, endpoint2, transform);
                        // Needs to be dropped here or else the wires will be killed once junction is empty.
                        owner.dropWire();
                        linePart.remove();
                        powerGrid$handleJunction(level, global, transform, junction, checked, altered, abandonedLines);
                    } else {
                        endpoint1 = powerGrid$offsetEndpoint(level, endpoint1, transform);
                        endpoint2 = powerGrid$offsetEndpoint(level, endpoint2, transform);
                    }
                    owner.sublevelMove(endpoint1, endpoint2);
                    if(transform.getRotation() != Rotation.NONE) {
                        owner.sublevelRotate(transform.getRotation());
                    }
                }
                if(linePart.owner != null)
                    altered.add(linePart.owner);
            }
        }
        abandonedLines.forEach(TransmissionLinePart::remove);
        for(var entity : altered) {
            entity.dropWire();
            if(entity instanceof BlockWireEntity blockWire) {
                if(blockWire.getEndpoint1() != null &&
                        blockWire.getEndpoint2() != null &&
                        !SableUtils.sameSubLevel(level, blockWire.getEndpoint1().getExactPosition(level), blockWire.getEndpoint2().getExactPosition(level))) {
                    entity.kill();
                } else {
                    blockWire.setPos(transform.apply(blockWire.position()));
                }
            }
        }
    }
}
