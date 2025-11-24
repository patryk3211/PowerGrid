package org.patryk3211.powergrid.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.GraphedElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.NetworkGraph;
import org.patryk3211.powergrid.electricity.sim.PerformanceCounter;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import static net.minecraft.commands.Commands.literal;


public class DebugCommand {
    private record ProblematicWire(AbstractElectricWire wire, OwnedFloatingNode closestOwnedNode) { }
    private record ProblemEntry(Date timestamp, double residual, List<ProblematicWire> wires) { }
    private static final List<ProblemEntry> savedProblematicExecutions = new ArrayList<>();

    private static OwnedFloatingNode findClosestNode(NetworkGraph graph, AbstractElectricWire wire) {
        var checkNodes = new ArrayList<IElectricNode>();
        checkNodes.add(wire.getNode1());
        checkNodes.add(wire.getNode2());

        while(!checkNodes.isEmpty()) {
            var node = checkNodes.remove(0);
            var connected = graph.getConnectedNodes(node);
            for(var other : connected) {
                if(other instanceof OwnedFloatingNode owned)
                    return owned;
            }
            checkNodes.addAll(connected);
        }
        return null;
    }

    public static void pushProblems(GraphedElectricalNetwork network, double residual, Collection<AbstractElectricWire> wires) {
        var frame = wires.stream().map(wire -> new ProblematicWire(wire, findClosestNode(network.getGraph(), wire))).toList();
        savedProblematicExecutions.add(new ProblemEntry(new Date(), residual, frame));
        if(savedProblematicExecutions.size() > 5)
            savedProblematicExecutions.remove(0);
    }

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("debug")
                .then(literal("problems").executes(ctx -> {
                    var source = ctx.getSource();
                    source.sendSystemMessage(Component.literal("Showing problematic wires of " + savedProblematicExecutions.size() +
                            " latest non-converged networks:").withStyle(ChatFormatting.GRAY));
                    for(int i = 0; i < savedProblematicExecutions.size(); ++i) {
                        var frame = savedProblematicExecutions.get(i);
                        source.sendSystemMessage(Component.literal("[" + i + "] " + PerformanceCounter.FORMAT.format(frame.timestamp) + " final residual = " + frame.residual)
                                .withStyle(ChatFormatting.GRAY));
                        for(var entry : frame.wires) {
                            source.sendSystemMessage(Component.literal("  " + entry.wire()).withStyle(ChatFormatting.GOLD));
                            source.sendSystemMessage(Component.literal("  Closest owned node:").withStyle(ChatFormatting.DARK_GRAY));
                            source.sendSystemMessage(Component.literal("  " + entry.closestOwnedNode()).withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("status").executes(ctx -> {
                    var source = ctx.getSource();
                    var player = source.getPlayerOrException();
                    var global = GlobalElectricNetworks.getWorldNetworks((LevelAccessor) player.level());
                    if(global == null) {
                        source.sendSystemMessage(Component.literal("Current level doesn't have any electrical networks"));
                    } else {
                        var nets = global.subnetworks;
                        int notConverged = 0;
                        for(var net : nets) {
                            if(!net.isConverged())
                                ++notConverged;
                        }
                        source.sendSystemMessage(Component.literal("Current level has " + global.subnetworks.size() + " networks, " + notConverged + " are not converged."));
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
