package org.patryk3211.powergrid.electricity.solarpanel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
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
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import java.util.*;

import static org.patryk3211.powergrid.electricity.solarpanel.SolarHelper.*;

public class SolarPanelBlockEntity extends ElectricBlockEntity {
    protected VoltageSourceCoupling sourceCoupling;

    protected static final int SOLAR_CONSTANT = 1361;
    protected static final float SHORT_CURRENT = 9.2f;
    protected static final int CELLS_IN_SERIES = 48;
    protected static final int STRINGS_IN_PARALLEL = 1;
    protected static final float BETAVOC = -0.0023f;
    protected static final float ALPHAISC = 0.0005f;
    protected static final float NOCT = 52;
    protected static final double I_O = 1.11e-4;
    protected static final double IDEALITY = 1.8;
    protected static final double DIFFUSE_FRAC = .12;
    protected static final double ALBEDO_FRAC = .08;

    private boolean firstTick = true;
    private float ambientTemp = -2000f;
    private int rayCastDelay = 0;
    private float sunVisibility = 0;
    private boolean skyVisible = false;
    private Vector3d panelNormal;

    private final Map<BlockPos, SolarPanelBlockEntity> connectedPanelBEs = new HashMap<>();
    private final Set<BlockPos> connectedPanels = new HashSet<>();
    private BlockPos controller;

    private double panelVoltage, panelResistance;
    private boolean valid = true;

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        sourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), 0.01f);
    }

    private void electricalProperties(SolarPanelBlockEntity controller) {
        var world = getLevel();
        float cloudCover = getWeather(world);

        getPlacedBlockRotation();
        var irradiance = getIrradiance(getAM(world), cloudCover, this.getBlockPos().getY(), world);
        var cellTemp = getCellTemp(irradiance, ambientTemp);
        var Vt = 8.617e-5 * (cellTemp + 273.15);
        double[] adjusted = getTempAdjusted(irradiance, cellTemp, Vt, STRINGS_IN_PARALLEL);
        double cellCurrent = adjusted[0];
        double Voc_t = adjusted[1];
        double Voc_panel = Voc_t * CELLS_IN_SERIES;

        if (cellCurrent <= 0) {
            controller.panelResistance += 1e6f;
            return;
        }

        double panelResistance = (cellCurrent > 0) ? Voc_panel / cellCurrent : 1e6;
        controller.panelResistance += panelResistance;
        controller.panelVoltage += Voc_panel;
    }

    @Override
    public void electricalTick() {
        var world = getLevel();
        if (sourceCoupling == null) return;

        if (firstTick) {
            ambientTemp = ThermalBehaviour.getAmbientTemperature(world, this.getBlockPos());
            if (ambientTemp <= ThermalBehaviour.ABSOLUTE_ZERO)
                ambientTemp = 22f;
            firstTick = false;
        }

        panelVoltage = 0; panelResistance = 0;
        electricalProperties(this);
        var iter = connectedPanelBEs.values().iterator();
        while(iter.hasNext()) {
            var panel = iter.next();
            if(panel.valid) {
                panel.electricalProperties(this);
            } else {
                connectedPanels.remove(panel.getBlockPos());
                iter.remove();
                setChanged();
            }
        }

        sourceCoupling.setVoltage((float) panelVoltage);
        sourceCoupling.setResistance((float) panelResistance);

        super.electricalTick();
    }

    public double getIrradiance(double AM, double cloudCover, int YPos, Level world) {
        if (AM == Double.POSITIVE_INFINITY) return 0;
        var transmittance = 1 - cloudCover;
        var irradiance = SOLAR_CONSTANT * Math.pow(0.7,Math.pow(AM, 0.678));
        irradiance = irradiance * ((((YPos - 70) / 250f) * 0.04f) + 1); //70 is around average world height, but it could also be put to sea level

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
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        connectedPanels.clear();
        if(tag.contains("Controller")) {
            controller = NbtUtils.readBlockPos(tag.getCompound("Controller"));
        } else {
            controller = null;
            if(tag.contains("Connected", ListTag.TAG_COMPOUND)) {
                var list = tag.getList("Connected", ListTag.TAG_COMPOUND);
                for(int i = 0; i < list.size(); ++i) {
                    connectedPanels.add(NbtUtils.readBlockPos(list.getCompound(i)));
                }
                if(level != null)
                    discoverPanels();
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
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
                for(var pos : island) {
                    if(!(level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be))
                        continue;
                    be.controller = newController.getBlockPos();
                    be.setChanged();
                }
                newController.discoverPanels();
                newController.setChanged();
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

    public boolean canAccept() {
        return getController()
                .map(be -> be.connectedPanels.size() < 24)
                .orElse(false);
    }

    public void connect(SolarPanelBlockEntity panel) {
        assert controller == null;
        connectedPanels.add(panel.getBlockPos());
        connectedPanelBEs.put(panel.getBlockPos(), panel);
        panel.controller = worldPosition;
        setChanged();
        panel.setChanged();
    }
}
