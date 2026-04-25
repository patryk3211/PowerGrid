package org.patryk3211.powergrid.mixin.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.compat.sable.SableUtils;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.WorldNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.JunctionWireEndpoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblerMixin {
    @Unique
    private static void powerGrid$handleJunction(ServerLevel level, WorldNetworks global, BlockPos offset, JunctionWireEndpoint junction, Set<TransmissionLinePart> checked, Set<BaseWireEntity> altered, Set<TransmissionLinePart> abandonedLines) {
        var connected = global.findConnectedWires(junction);
        if(connected == null)
            return;
        for(TransmissionLinePart linePart : connected) {
            if(!checked.add(linePart))
                continue;
            if(linePart.getEndpoint1() instanceof JunctionWireEndpoint e1 && linePart.getEndpoint2() instanceof JunctionWireEndpoint e2) {
                BaseWireEntity owner = linePart.owner;
                if(owner == null) {
                    // Owner might have been moved already (block wires which were kicked out).
                    owner = linePart.persistentOwnerId.getEntity(level);
                    if(owner == null)
                        return;
                    abandonedLines.add(linePart);
                }

                var movedE1 = e1.makeOffset(offset);
                var movedE2 = e2.makeOffset(offset);
                owner.sublevelMove(movedE1, movedE2);
                if(linePart.owner != null)
                    altered.add(linePart.owner);

                if(linePart.getEndpoint1() == junction) {
                    powerGrid$handleJunction(level, global, offset, e2, checked, altered, abandonedLines);
                } else if(linePart.getEndpoint2() == junction) {
                    powerGrid$handleJunction(level, global, offset, e1, checked, altered, abandonedLines);
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
        var offset = transform.apply(BlockPos.ZERO);
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
                if(endpoint1 != null) {
                    var pos = endpoint1.getExactPosition(level);
                    if(blockPos.getX() == Mth.floor(pos.x) && blockPos.getY() == Mth.floor(pos.y) && blockPos.getZ() == Mth.floor(pos.z)) {
                        endpoint1 = endpoint1.makeOffset(offset);
                    }
                    if(endpoint1 instanceof JunctionWireEndpoint junction) {
                        if(checked.add(linePart)) {
                            powerGrid$handleJunction(level, global, offset, junction, checked, altered, abandonedLines);
                        }
                    }
                }
                if(endpoint2 != null) {
                    var pos = endpoint2.getExactPosition(level);
                    if(blockPos.getX() == Mth.floor(pos.x) && blockPos.getY() == Mth.floor(pos.y) && blockPos.getZ() == Mth.floor(pos.z)) {
                        endpoint2 = endpoint2.makeOffset(offset);
                    }
                    if(endpoint2 instanceof JunctionWireEndpoint junction) {
                        if(checked.add(linePart)) {
                            powerGrid$handleJunction(level, global, offset, junction, checked, altered, abandonedLines);
                        }
                    }
                }
                owner.sublevelMove(endpoint1, endpoint2);
                if(linePart.owner != null)
                    altered.add(linePart.owner);
            }
        }
        abandonedLines.forEach(TransmissionLinePart::remove);
        for(var entity : altered) {
            entity.dropWire();
            if(entity instanceof BlockWireEntity blockWire) {
                if(!SableUtils.sameSubLevel(level, blockWire.getEndpoint1().getExactPosition(level), blockWire.getEndpoint2().getExactPosition(level))) {
                    entity.kill();
                } else {
                    blockWire.setPos(transform.apply(blockWire.position()));
                }
            }
        }
    }
}
