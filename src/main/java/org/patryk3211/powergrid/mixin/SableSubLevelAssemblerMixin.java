package org.patryk3211.powergrid.mixin;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.compat.sable.SableUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubLevelAssemblyHelper.class)
public class SableSubLevelAssemblerMixin {
    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false, order = 1100)
    private static void powerGrid$moveWireStuff(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        var global = GlobalElectricNetworks.getWorldNetworks((LevelAccessor) level);
        if(global == null)
            return;
        var altered = new ReferenceOpenHashSet<BaseWireEntity>();
        for(var blockPos : blocks) {
            var behaviour = ElectricBehaviour.get(level, blockPos, ElectricBehaviour.TYPE);
            if(behaviour == null)
                continue;
            var wires = global.findConnectedWires(behaviour);
            for(TransmissionLinePart linePart : wires) {
                if(linePart.owner == null) {
                    // TODO: This line part might have to be deleted.
                    return;
                }
                IWireEndpoint endpoint1 = linePart.owner.getEndpoint1();
                IWireEndpoint endpoint2 = linePart.owner.getEndpoint2();
                // TODO: Trace block wire junctions
                if(endpoint1 instanceof BlockWireEndpoint endpoint) {
                    if(blockPos.equals(endpoint.getPos())) {
                        endpoint1 = new BlockWireEndpoint(transform.apply(blockPos), endpoint.getTerminal());
                    }
                }
                if(endpoint2 instanceof BlockWireEndpoint endpoint) {
                    if(blockPos.equals(endpoint.getPos())) {
                        endpoint2 = new BlockWireEndpoint(transform.apply(blockPos), endpoint.getTerminal());
                    }
                }
                linePart.owner.sublevelMove(endpoint1, endpoint2);
                altered.add(linePart.owner);
            }
        }
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
