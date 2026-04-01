package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.*;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.modulardisplay.DisplayModuleType;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.network.packets.UpdateComponentBiPacket;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

public class ModularDisplayComponent extends OrientableComponent implements IRenderedComponent, IInteractableComponent{
    public static final IntProperty INDEX = new IntProperty(PowerGrid.MOD_ID, "modular_display_index", 1, 0, 30).hidden().cast();
    public static final BooleanProperty HALF_CLICK = new BooleanProperty(PowerGrid.MOD_ID, "modular_display_half_click").hidden().cast();
    public static final IntProperty CURRENT_MODULE = new IntProperty(PowerGrid.MOD_ID, "modular_display_module", 0, 0, 10).hidden().cast();
    public static final StringProperty DISPLAYED_TEXTURE = new StringProperty(PowerGrid.MOD_ID, "modular_display_texture", "zerotonine").hidden().cast();
    public static final FloatProperty SPRITE_WIDTH = new FloatProperty(PowerGrid.MOD_ID, "modular_display_sprite_width", 80, 16, 300).hidden().cast();
    public static final FloatProperty CHARACTER_COUNT = new FloatProperty(PowerGrid.MOD_ID, "modular_display_character_count", 9, 0, 50).hidden().cast();
    public static final BooleanProperty WIRE_RESET = new BooleanProperty(PowerGrid.MOD_ID, "modular_display_reset").hidden().cast();
    public static final StringProperty CURRENT_COLOR = new StringProperty(PowerGrid.MOD_ID, "modular_display_current_color","WHITE").hidden().cast();
    public static final ConstantProperty MIN_CURRENT = new ConstantProperty(PowerGrid.MOD_ID, "modular_display_current", Unit.CURRENT.formatWithPrefixes(.5f).component());
    public static final ConstantProperty RESISTANCE = new ConstantProperty(PowerGrid.MOD_ID, "modular_display_resistance", Unit.RESISTANCE.formatWithPrefixes(25).component());


    private ValueSettingsBoard board = null;

    public ModularDisplayComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    private static final float SHEET_HEIGHT = 16f;
    private static final float FRAME_WIDTH = 5f;
    private static final float FRAME_HEIGHT = 7f;
    private static final float FRAME_PADDING = 1f;
    private static final float PIXEL = 1f / 16f;
    private static final float INNER_OFFSET = 1f * PIXEL;
    private static final float INNER_UD_OFFSET = .75f * PIXEL;
    private static final float INNER_UD_SIZE = 2.5f * PIXEL;
    private static final float INNER_RL_SIZE = 2f * PIXEL;
    private static final float Y_NUDGE = 0.0001f;

    private void setCurrentModule(PlacedComponent component){
        var Module = component.get(CURRENT_MODULE);
        component.set(INDEX, 0);

        switch (Module){
            case 0:
                component.set(DISPLAYED_TEXTURE, "zerotonine");
                component.set(SPRITE_WIDTH, 80f);
                component.set(CHARACTER_COUNT, 9f);
                component.set(CURRENT_MODULE, 0);
                break;

            case 1:
                component.set(DISPLAYED_TEXTURE, "ninetozero");
                component.set(SPRITE_WIDTH, 80f);
                component.set(CHARACTER_COUNT, 9f);
                component.set(CURRENT_MODULE, 1);
                break;

            case 2:
                component.set(DISPLAYED_TEXTURE, "onetozero");
                component.set(SPRITE_WIDTH, 80f);
                component.set(CHARACTER_COUNT, 9f);
                component.set(CURRENT_MODULE, 2);
                break;

            case 3:
                component.set(DISPLAYED_TEXTURE,"zerotof");
                component.set(SPRITE_WIDTH, 112f);
                component.set(CHARACTER_COUNT, 15f);
                component.set(CURRENT_MODULE, 3);
                break;

            case 4:
                component.set(DISPLAYED_TEXTURE, "symbols");
                component.set(SPRITE_WIDTH, 80f);
                component.set(CHARACTER_COUNT, 8f);
                component.set(CURRENT_MODULE, 4);
                break;

            case 5:
                component.set(DISPLAYED_TEXTURE, "alphabet");
                component.set(SPRITE_WIDTH, 176f);
                component.set(CHARACTER_COUNT, 25f);
                component.set(CURRENT_MODULE, 5);
                break;
        }
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RESISTANCE, MIN_CURRENT, INDEX, HALF_CLICK, CURRENT_MODULE, DISPLAYED_TEXTURE, SPRITE_WIDTH,
                CHARACTER_COUNT, WIRE_RESET, CURRENT_COLOR, power(25));
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (placed.isClient()) return true;
        if(placed.wires.isEmpty())
            return true;
        var coilNodeToReset = (SwitchedWire) placed.wires.get(0);
        var coilNodeToNegative =  (SwitchedWire) placed.wires.get(2);

        var coilNodeToNegativeCurrent = Math.abs(coilNodeToNegative.current());
        var coilNodeToResetCurrent = Math.abs(coilNodeToReset.current());
        var charCount = placed.get(CHARACTER_COUNT);
        //every module display texture has the characters in the sprite plus a blank space and the first character again for smooth transition
        //but im only counting characters before the blank space and adding one for the blank space and two for the transition

        if (placed.get(WIRE_RESET) == true){
            coilNodeToReset.setState(false);
            coilNodeToNegative.setState(true);
            placed.set(WIRE_RESET, false);
        }

        if (coilNodeToNegative.isConverged()){

            if (coilNodeToNegativeCurrent >= .5 && placed.get(INDEX) != charCount+1 && !placed.get(HALF_CLICK)) {
                placed.set(INDEX, placed.get(INDEX) +1);
                placed.set(HALF_CLICK, true);
                placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(world, placed.getPos(), 0.75f, 2f));
                placed.notifyClients(INDEX);
                placed.notifyClients(HALF_CLICK);
            }

            if (coilNodeToNegativeCurrent < .5 && placed.get(INDEX) == charCount+1 && coilNodeToNegative.getState()){
                placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(world, placed.getPos(), 0.75f, 1.9f));
                coilNodeToNegative.setState(false);
                coilNodeToReset.setState(true);
                placed.set(HALF_CLICK, false);
                //placed.notifyClients(INDEX);
                placed.notifyClients(HALF_CLICK);
            }

            if (coilNodeToNegativeCurrent < .5 && coilNodeToNegative.getState() && placed.get(HALF_CLICK)) {
                placed.set(HALF_CLICK, false);
                placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(world, placed.getPos(), 0.75f, 1.9f));
                //placed.notifyClients(INDEX);
                placed.notifyClients(HALF_CLICK);
            }

            if (coilNodeToReset.getState() && coilNodeToResetCurrent >= .5 && placed.get(INDEX) == charCount+1) {
                placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(world, placed.getPos(), 0.75f, 2f));
                placed.set(INDEX, placed.get(INDEX) +1);
                placed.set(HALF_CLICK, true);
                coilNodeToNegative.setState(true);
                coilNodeToReset.setState(false);
                placed.notifyClients(INDEX);
                placed.notifyClients(HALF_CLICK);
            }

            if (placed.get(INDEX) >= charCount+2 && !placed.get(HALF_CLICK)){
                placed.set(INDEX, 0);
                placed.notifyClients(INDEX);
            }
        }
        return true;
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {

        var coilNode = builder.addInternalNode();
        var coil = builder.connect(25, builder.terminalNode(0), coilNode);
        var coilNodeToNegitive = builder.connectSwitch(0.1f, builder.terminalNode(1), coilNode, true);
        var coilNodeToReset = builder.connectSwitch(0.1f, builder.terminalNode(2), coilNode, false);
        placed.add(coilNodeToReset); placed.add(coil); placed.add(coilNodeToNegitive);

        thermals.builder()
                .setThermalMass(0.15f)
                .setMaxPower(30, 125f)
                .setDissipationFactor(.25f)
                .addHeatSource(coil);

    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack pStack,
                       MultiBufferSource buffer, int light, int overlay) {

        pStack.pushPose();
        pStack.translate(0.5, 8f/16f, 0.5);
        Boolean halfClick = false;

        pStack.translate(-0.5, 0, -0.5);
        pStack.translate(0f, Y_NUDGE, 0f);

        Matrix4f matrix = pStack.last().pose();
        if (placed.has(HALF_CLICK)) {
            halfClick = placed.get(HALF_CLICK);
        }

        float frameIndex = placed.get(INDEX);
        if (halfClick){
            frameIndex -= .5f;
        }
        var displayTexture = "block/modular_display/" + placed.get(DISPLAYED_TEXTURE);

        float innerX = 0 + INNER_OFFSET;
        float innerY = 0 + INNER_UD_OFFSET;

        float uMin = (frameIndex * (FRAME_WIDTH + FRAME_PADDING)) / placed.get(SPRITE_WIDTH);
        float uMax = (frameIndex * (FRAME_WIDTH + FRAME_PADDING) + FRAME_WIDTH) / placed.get(SPRITE_WIDTH);
        float vMin = 0f;
        float vMax = FRAME_HEIGHT / SHEET_HEIGHT;

        float[] rgb = DyeColor.byName(placed.get(CURRENT_COLOR), DyeColor.WHITE).getTextureDiffuseColors();

        renderQuad(matrix, buffer,
                PowerGrid.texture(displayTexture),
                innerX, innerY,
                INNER_RL_SIZE, INNER_UD_SIZE,
                uMin, vMin, uMax, vMax,
                light, overlay, rgb);

        pStack.popPose();
    }

    private void renderQuad(Matrix4f matrix, MultiBufferSource bufferSource, ResourceLocation texture,
                            float x, float z, float width, float height, float uMin, float vMin, float uMax, float vMax,
                            int packedLight, int packedOverlay, float[] rgb) {

        VertexConsumer vc = bufferSource.getBuffer(RenderType.text(texture));

        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];

        vc.vertex(matrix, x + width, 0f, z).color(r, g, b, 1)
                .uv(uMax, vMin).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(matrix, x, 0f, z).color(r, g, b, 1)
                .uv(uMin, vMin).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(matrix, x, 0f, z + height).color(r, g, b, 1)
                .uv(uMin, vMax).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(matrix, x + width, 0f, z + height).color(r, g, b, 1)
                .uv(uMax, vMax).overlayCoords(packedOverlay).uv2(packedLight)
                .normal(0f, 0f, 1f)
                .endVertex();
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 7 / 16f);
    }

    @Override
    public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent component, Player player) {

        if (player.getMainHandItem().getItem() instanceof DyeItem dye && !be.getLevel().isClientSide()){
            component.set(CURRENT_COLOR, dye.getDyeColor().getName());
            component.notifyClients(CURRENT_COLOR);
            if (!player.isCreative()) player.getMainHandItem().shrink(1);
            return InteractionResult.SUCCESS;
        }

        component.onClientWorld(() -> world -> {
            if (board == null) {
                board = new ValueSettingsBoard(
                        Lang.translateDirect("devices.display_module.module_type"),
                        DisplayModuleType.values().length - 1,
                        1,
                        ImmutableList.of(Component.literal("Index")),
                        new ValueSettingsFormatter.ScrollOptionSettingsFormatter(DisplayModuleType.values())
                );
            }

            var value = component.get(CURRENT_MODULE);

            CustomValueSettingsScreen.beginInteraction(() -> new CustomValueSettingsScreen(
                    be.getBlockPos(), board,
                    new ValueSettingsBehaviour.ValueSettings(0, value),
                    setting -> {
                        component.set(CURRENT_MODULE, setting.value());
                        setCurrentModule(component);
                        component.set(WIRE_RESET, true);
                        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, component, CURRENT_MODULE));
                        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, component, DISPLAYED_TEXTURE));
                        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, component, CHARACTER_COUNT));
                        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, component, SPRITE_WIDTH));
                        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, component, INDEX));
                        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, component, WIRE_RESET));
                    }
            ));
        });
        return InteractionResult.SUCCESS;
    }
}
