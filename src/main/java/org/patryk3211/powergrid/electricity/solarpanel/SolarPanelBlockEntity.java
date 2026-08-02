package org.patryk3211.powergrid.electricity.solarpanel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.CurrentSourceWire;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;

import java.util.*;

import static org.patryk3211.powergrid.electricity.solarpanel.SolarHelper.*;

public class SolarPanelBlockEntity extends ElectricBlockEntity implements ISolarPropertyConsumer {
    protected CurrentSourceWire currentSource;
    protected ElectricWire seriesResistor;

    private boolean firstTick = true;
    private float ambientTemp = -2000f;
    private int rayCastDelay = 0;
    private float sunVisibility = 0;
    private boolean skyVisible = false;
    private Vector3d panelNormal;
    private double irradiance;

    private final Map<BlockPos, SolarPanelBlockEntity> connectedPanelBEs = new HashMap<>();
    private final Set<BlockPos> connectedPanels = new HashSet<>();
    private BlockPos controller;
    private BlockPos lastKnownPos;

    private double Rs, I;
    private int panelCount;
    private boolean valid = true;

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        var node = builder.addInternalNode();
        currentSource = new CurrentSourceWire(node, builder.terminalNode(1), 0.00001f);
        seriesResistor = builder.connect((float) 1, node, builder.terminalNode(0));
        builder.add(currentSource);
        builder.add(seriesResistor);
    }

    private void makeProxy() {
        assert controller != null;
        if(electricBehaviour instanceof ProxyElectricBehaviour)
            return;
        List<TransmissionLinePart> wires = null;
        if(level != null)
            wires = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(electricBehaviour);
        var old = electricBehaviour;
        electricBehaviour = new ProxyElectricBehaviour(this, () -> controller);
        electricBehaviour.inheritConnections(old);
        old.pause();
        attachBehaviourLate(electricBehaviour);
        currentSource = null;
        seriesResistor = null;
        if(wires != null)
            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
    }

    private void makeMain() {
        assert controller == null;
        if(!(electricBehaviour instanceof ProxyElectricBehaviour proxy))
            return;
        List<TransmissionLinePart> wires = null;
        if(level != null)
            wires = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(electricBehaviour);
        electricBehaviour = new ElectricBehaviour(this);
        electricBehaviour.inheritConnections(proxy);
        attachBehaviourLate(electricBehaviour);
        if(wires != null)
            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
    }

    private void electricalProperties(SolarPanelBlockEntity controller) {
        float cloudCover = getWeather(level);

        getPlacedBlockRotation();
        irradiance = getIrradiance(getAM(level), cloudCover, this.getBlockPos().getY(), level);
        SolarHelper.electricalProperties(controller);
    }

    @Override
    public void accept(double Rs) {
        this.Rs += Rs;
        ++panelCount;
    }

    @Override
    public void electricalTick() {
        var world = getLevel();
        if (world == null || world.isClientSide()) return;
        if (currentSource == null) return;

        if (firstTick) {
            ambientTemp = ThermalBehaviour.getAmbientTemperature(world, this.getBlockPos());
            if (ambientTemp <= ThermalBehaviour.ABSOLUTE_ZERO)
                ambientTemp = 22f;
            firstTick = false;
        }

        if (currentSource == null) return;

        Rs = 0; panelCount = 0;
        electricalProperties(this);
        var iter = connectedPanelBEs.values().iterator();
        while(iter.hasNext()) {
            var panel = iter.next();
            if(panel.valid) {
                panel.electricalProperties(this);
            } else {
                connectedPanels.remove(panel.getBlockPos());
                iter.remove();
                notifyUpdate();
            }
        }

        double v0 = currentSource.potentialDifference();
        var currentCellTemp = SolarHelper.getCellTemp(irradiance, ambientTemp);
        double[] results = IVCurve(irradiance, currentCellTemp, v0, panelCount, 1);
        currentSource.setCurrent(results[0]);
        currentSource.setConductance(results[1]);
        seriesResistor.setResistance(Rs);
        super.electricalTick();
    }

    public double getIrradiance(double AM, double cloudCover, int YPos, Level world) {
        if (AM == Double.POSITIVE_INFINITY) return 0;
        var transmittance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level
        if (irradiance > SOLAR_CONSTANT) irradiance = SOLAR_CONSTANT;

        double sunAngle = world.getSunAngle(0);
        Vector3d sunDir = new Vector3d(-Math.sin(sunAngle), Math.cos(sunAngle), 0);
        if (sunDir.y <= 0) return 0;

        if (rayCastDelay-- == 0){
            sunVisibility = sunRaycast(world);
            rayCastDelay = world.random.nextInt(41) + 10;
            skyVisible = skyCheck(world, this.getBlockPos());
        }

        double cosIncidence = Math.max(0, sunDir.dot(panelNormal));
        cosIncidence = Math.max(0, cosIncidence);
        double diffuseLight = DIFFUSE_FRAC * (Math.max(0, sunDir.y) * irradiance * transmittance)
                * (1 + cloudCover) * ((1 + panelNormal.y()) / 2);
        double reflected = ALBEDO_FRAC * (Math.max(0, sunDir.y) * irradiance * transmittance) * ((1 - panelNormal.y()) / 2.0);

        if (!skyVisible) {
            diffuseLight = 0;
            reflected = 0;
        }
        return (irradiance * sunVisibility) * transmittance * cosIncidence + diffuseLight + reflected;
    }

    public float sunRaycast(Level world) {
        var blockPos = getBlockPos();
        int castLength = 0;
        ChunkAccess chunk;
        double sunAngle = world.getSunAngle(0);
        double sunX = -Math.sin(sunAngle);
        double sunY = Math.cos(sunAngle);
        boolean positiveX = sunX > 0;
        for (int i = 1; i <= 10; i++) {
            int xOffset = (positiveX ? i : -i) * 16;
            chunk = world.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(blockPos.getX() + xOffset),
                    SectionPos.blockToSectionCoord(blockPos.getZ()));
            if (chunk != null) {
                castLength = i * 16;
            } else {
                break;
            }
        }
        var centerBlockPos = getBlockPos().getCenter().add(0, 0, 0);
        var end = centerBlockPos.add(new Vec3(sunX, sunY, 0).scale(castLength));
        var results = DDA(world, centerBlockPos, end);
        float returnValue = 1;
        for (DDAHit result : results) {
            BlockState blockState;
            if (result.contraption() != null) {
                blockState = result.contraption().getContraption().getBlocks().get(result.worldOrLocalPos()).state();
            } else {
                blockState = world.getBlockState(result.worldOrLocalPos());
            }

            if (blockState.is(ModdedBlocks.SOLAR_PANEL.get())) {
                if (result.worldOrLocalPos().equals(this.getBlockPos())) {
                    continue;
                } else if (world.getBlockEntity(result.worldOrLocalPos()) instanceof SolarPanelBlockEntity be) {
                    if (SolarPanelBlockEntity.areConnected(this, be)){
                        continue;
                    } else {
                        returnValue = 0;
                        break;
                    }
                } else {
                    returnValue = 0;
                    break;
                }
            }
            if (blockState.is(ModdedTags.Block.SOLAR_QUARTER_LIGHT.tag)) {
                returnValue *= .25f;
                continue;
            }
            if (blockState.is(ModdedTags.Block.SOLAR_HALF_LIGHT.tag)) {
                returnValue *= .5f;
                continue;
            }
            if (blockState.is(ModdedTags.Block.SOLAR_3QUARTER_LIGHT.tag)) {
                returnValue *= .75f;
                continue;
            }
            if (blockState.is(ModdedTags.Block.SOLAR_FULL_LIGHT.tag)){
                returnValue *= 1f;
                continue;
            }
            returnValue = 0;
            break;
        }
        return returnValue;
    }

    public void getPlacedBlockRotation() {
        var face = this.getBlockState().getValue(Rotation4ElectricBlock.FACING).getOpposite();
        var n = face.getNormal();
        panelNormal = new Vector3d(n.getX(), n.getY(), n.getZ());
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(controller != null) {
            tag.put("Controller", NbtUtils.writeBlockPos(controller));
        } else {
            if(!connectedPanels.isEmpty()) {
                var list = new ListTag();
                for (var pos : connectedPanels) {
                    list.add(NbtUtils.writeBlockPos(pos));
                }
                tag.put("Connected", list);
            }
        }
        if(lastKnownPos != null)
            tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        if(lastKnownPos != null) {
            if (controller != null) {
                tag.put("Controller", NbtUtils.writeBlockPos(controller));
            } else {
                if (!connectedPanels.isEmpty()) {
                    var list = new ListTag();
                    for (var pos : connectedPanels) {
                        list.add(NbtUtils.writeBlockPos(pos));
                    }
                    tag.put("Connected", list);
                }
            }
            tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        Vec3i offset = null;
        if(tag.contains("LastKnownPos")) {
            lastKnownPos = NbtUtils.readBlockPos(tag.getCompound("LastKnownPos"));
            if(!worldPosition.equals(lastKnownPos)) {
                offset = worldPosition.subtract(lastKnownPos);
            }
            lastKnownPos = worldPosition;
        } else {
            lastKnownPos = null;
        }
        connectedPanels.clear();
        if(tag.contains("Controller")) {
            controller = NbtUtils.readBlockPos(tag.getCompound("Controller"));
            if(offset != null)
                controller = controller.offset(offset);
            makeProxy();
        } else {
            controller = null;
            if(tag.contains("Connected", ListTag.TAG_LIST)) {
                var list = tag.getList("Connected", ListTag.TAG_COMPOUND);
                for(int i = 0; i < list.size(); ++i) {
                    var pos = NbtUtils.readBlockPos(list.getCompound(i));
                    if(offset != null)
                        pos = pos.offset(offset);
                    connectedPanels.add(pos);
                }
                if(level != null)
                    discoverPanels();
            }
            makeMain();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if(lastKnownPos == null)
            lastKnownPos = worldPosition;
        if(controller == null) {
            discoverPanels();
        } else {
            if(!level.isLoaded(controller))
                return;
            var controllerBE = level.getBlockEntity(controller);
            if(controllerBE instanceof SolarPanelBlockEntity solar) {
                solar.connectedPanelBEs.put(worldPosition, this);
            } else {
                // Controller is gone.
                controller = null;
            }
        }
    }

    private void discoverPanels() {
        assert level != null;
        connectedPanelBEs.clear();
        for(var pos : connectedPanels) {
            if(!level.isLoaded(pos))
                continue;
            var be = level.getBlockEntity(pos);
            if(be instanceof SolarPanelBlockEntity solarBE) {
                connectedPanelBEs.put(pos, solarBE);
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        valid = false;
    }

    @Override
    public void destroy() {
        super.destroy();
        var axis = getBlockState().getValue(SolarPanelBlock.FACING).getAxis();
        getController().ifPresent(controller -> {
            // Put all panels into the pool
            var toCheck = new ArrayList<>(controller.connectedPanels);
            toCheck.add(controller.worldPosition);
            toCheck.remove(worldPosition);
            while(!toCheck.isEmpty()) {
                // Trace through blocks to find all connected panels.
                var island = new ArrayList<BlockPos>(25);
                var queue = new ArrayList<BlockPos>(25);
                var startingPos = toCheck.remove(0);
                queue.add(startingPos);
                island.add(startingPos);
                toCheck.remove(startingPos);
                while(!queue.isEmpty()) {
                    var pos = queue.remove(0);
                    for(var dir : Direction.values()) {
                        if(dir.getAxis() == axis)
                            continue;
                        var neighbor = pos.relative(dir);
                        if(toCheck.contains(neighbor)) {
                            toCheck.remove(neighbor);
                            island.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                // Connect the discovered panels together.
                SolarPanelBlockEntity newController = null;
                for(var pos : island) {
                    if(!(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be))
                        continue;
                    newController = be;
                    newController.connectedPanels.clear();
                    newController.controller = null;
                    island.remove(pos);
                    break;
                }
                if(newController == null) {
                    // Empty island?
                    continue;
                }
                newController.connectedPanels.addAll(island);
                newController.makeMain();
                for(var pos : island) {
                    if(!(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be))
                        continue;
                    be.controller = newController.getBlockPos();
                    be.makeProxy();
                    be.notifyUpdate();
                }
                newController.discoverPanels();
                newController.notifyUpdate();
            }
        });
    }

    public static boolean areConnected(SolarPanelBlockEntity be1, SolarPanelBlockEntity be2) {
        if(be1.controller == null && be2.controller == null) {
            // Two separate controllers, they cannot be connected.
            return false;
        }
        if(be1.controller != null && be2.controller != null) {
            // Same controller, so they are connected.
            return be1.controller.equals(be2.controller);
        }
        if(be1.controller != null && be1.controller.equals(be2.worldPosition)) {
            // be2 is controller of be1, connected.
            return true;
        }
        if(be2.controller != null && be2.controller.equals(be1.worldPosition)) {
            // be1 is controller of be2, connected.
            return true;
        }
        return false;
    }

    public Optional<SolarPanelBlockEntity> getController() {
        if(controller == null)
            return Optional.of(this);
        if(!level.isLoaded(controller))
            return Optional.empty();
        if(level.getBlockEntity(controller) instanceof SolarPanelBlockEntity be)
            return Optional.of(be);
        return Optional.empty();
    }

    public static int maxPanels() {
        return ModdedConfigs.server().electricity.solarPanelMaxSize.get();
    }

    public boolean canAccept() {
        return getController()
                .map(be -> be.connectedPanels.size() < maxPanels() - 1)
                .orElse(false);
    }

    public void connect(SolarPanelBlockEntity panel) {
        assert controller == null;
        connectedPanels.add(panel.getBlockPos());
        connectedPanelBEs.put(panel.getBlockPos(), panel);
        panel.controller = worldPosition;
        panel.notifyUpdate();
        panel.makeProxy();
        notifyUpdate();
    }

    public static boolean isPast(BlockPos pos, int splittingCord, Direction splittingPlane) {
        var cord = pos.get(splittingPlane.getAxis());
        if(splittingPlane.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            return cord > splittingCord;
        } else {
            return cord < splittingCord;
        }
    }

    public static void splitMultiblock(SolarPanelBlockEntity controller, int splittingCoordinate, Direction splittingPlane) {
        if(isPast(controller.worldPosition, splittingCoordinate, splittingPlane)) {
            splittingCoordinate += splittingPlane.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
            splittingPlane = splittingPlane.getOpposite();
        }
        var level = controller.level;
        assert level != null;
        var wires = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(controller.electricBehaviour);

        var iter = controller.connectedPanels.iterator();
        SolarPanelBlockEntity secondController = null;
        while(iter.hasNext()) {
            var pos = iter.next();
            if(isPast(pos, splittingCoordinate, splittingPlane)) {
                var be = level.getBlockEntity(pos);
                if(!(be instanceof SolarPanelBlockEntity solar))
                    continue;
                if(secondController == null) {
                    secondController = solar;
                    secondController.controller = null;
                    secondController.connectedPanels.clear();
                    secondController.connectedPanelBEs.clear();
                    secondController.makeMain();
                } else {
                    secondController.connect(solar);
                }
                iter.remove();
            }
        }
        controller.notifyUpdate();
        wires.forEach(TransmissionLinePart::refreshEndpointNodes);
    }

    public static void mergeMultiblock(SolarPanelBlockEntity controller1, SolarPanelBlockEntity controller2) {
        assert controller1.level != null && controller1.level == controller2.level;
        if(controller1.connectedPanels.size() + controller2.connectedPanels.size() + 2 > maxPanels())
            return;
        var level = controller1.level;
        var wires1 = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(controller1.electricBehaviour);
        var wires2 = GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(controller2.electricBehaviour);

        for(var pos : controller2.connectedPanels) {
            var be = level.getBlockEntity(pos);
            if(!(be instanceof SolarPanelBlockEntity solar))
                continue;
            controller1.connect(solar);
        }
        controller2.connectedPanels.clear();
        controller2.connectedPanelBEs.clear();
        controller1.connect(controller2);

        wires1.forEach(TransmissionLinePart::refreshEndpointNodes);
        wires2.forEach(TransmissionLinePart::refreshEndpointNodes);
    }
}
