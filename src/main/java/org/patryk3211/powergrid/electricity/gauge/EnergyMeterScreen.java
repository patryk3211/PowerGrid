package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.EnergyMeterInteractionC2SPacket;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

import static org.patryk3211.powergrid.network.packets.EnergyMeterInteractionC2SPacket.Action.*;

public class EnergyMeterScreen extends AbstractSimiContainerScreen<EnergyMeterMenu> {
    private static final ResourceLocation BACKGROUND = PowerGrid.texture("gui/energy_meter");
    private static final int WIDTH = 246;
    private static final int HEIGHT = 98;

    private TooltipArea valueHover;

    private final ItemStack stack;

    public EnergyMeterScreen(EnergyMeterMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        stack = new ItemStack(menu.contentHolder.getBlockState().getBlock());
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);
        setWindowOffset(0, 0);

        super.init();

        IconButton wattHours = new IconButton(leftPos + 7, topPos + 73, ModIcons.I_Wh)
                .withCallback(() -> ModdedPackets.sendToServer(new EnergyMeterInteractionC2SPacket(PRECISION_WH, menu.contentHolder.getBlockPos())));
        wattHours.setToolTip(Lang.translateDirect("gui.energy_meter.wh"));
        IconButton kiloWattHours = new IconButton(leftPos + 25, topPos + 73, ModIcons.I_kWh)
                .withCallback(() -> ModdedPackets.sendToServer(new EnergyMeterInteractionC2SPacket(PRECISION_KWH, menu.contentHolder.getBlockPos())));
        kiloWattHours.setToolTip(Lang.translateDirect("gui.energy_meter.kwh"));
        IconButton reset = new IconButton(leftPos + 185, topPos + 73, AllIcons.I_ROTATE_CCW)
                .withCallback(() -> ModdedPackets.sendToServer(new EnergyMeterInteractionC2SPacket(ZERO, menu.contentHolder.getBlockPos())));
        reset.setToolTip(Lang.translateDirect("gui.energy_meter.reset"));
        valueHover = new TooltipArea(leftPos + 11, topPos + 21, 218, 43);

        IconButton close = new IconButton(leftPos + 213, topPos + 73, AllIcons.I_CONFIRM)
                .withCallback(this::onClose);


        addRenderableWidgets(wattHours, kiloWattHours, reset, valueHover, close);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        double processedEnergy = Math.round(menu.contentHolder.energy * 10) / 10.0;
        processedEnergy = processedEnergy % 100000;
        valueHover.withTooltip(List.of(
                Lang.translate("gui.energy_meter.measured")
                        .style(ChatFormatting.GRAY)
                        .component(),
                Lang.text(" ")
                        .add(Lang.numberConstant(processedEnergy))
                        .add(Lang.text(menu.contentHolder.measurementPrecision ? " " : " k"))
                        .add(Unit.ENERGY.get())
                        .style(ChatFormatting.AQUA)
                        .component()
        ));
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float partialTick, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        ctx.blit(BACKGROUND, bgX, topPos, 0, 0, WIDTH, HEIGHT);

        ctx.drawCenteredString(font, title, leftPos + (WIDTH - 8) / 2, topPos + 3, 0xFFFFFF);

        var ms = ctx.pose();
        int x = 0;
        for(int i = 10000; i >= 1; i /= 10) {
            float angle = getDialAngle(menu.contentHolder.energy, menu.contentHolder.lastEnergy, i, partialTick);
            ms.pushPose();
            ms.translate(leftPos + 31 + x, topPos + 35, 0);
            ms.rotateAround(new Quaternionf().rotateZ(angle), 1.5f, 5.5f, 0);
            ctx.blit(BACKGROUND, 0, 0, 21, 101, 3, 8);
            ms.popPose();
            x += 40;
        }

        ms.pushPose();
        float angle = getDialAngle(menu.contentHolder.energy, menu.contentHolder.lastEnergy, 0.1, partialTick);
        ms.translate(leftPos + 217, topPos + 53, 0);
        ms.rotateAround(new Quaternionf().rotateZ(angle), 0.5f, 3.5f, 0);
        ctx.blit(BACKGROUND, 0, 0, 25, 103, 1, 4);
        ms.popPose();

        int indicatorOffset = menu.contentHolder.measurementPrecision ? 9 : 27;
        ctx.blit(BACKGROUND, leftPos + indicatorOffset, topPos + 69, 4, 104, 15, 4);

        var pose = ctx.pose();
        pose.pushPose();
        pose.scale(2, 2, 1);
        ctx.renderItem(stack, (bgX + 244) / 2, (topPos + 65) / 2);
        pose.popPose();
    }

    public static float getDialAngle(double energy, double lastEnergy, double multiplier, double partialTick) {
        float rotation = (float) ((energy / multiplier / 10) % 1);
        float prevRotation = (float) ((lastEnergy / multiplier / 10) % 1);
        if(energy > lastEnergy && rotation < prevRotation) {
            rotation += 1;
        }
        if(energy < lastEnergy && rotation > prevRotation) {
            rotation -= 1;
        }
        return  (float) Mth.lerp(partialTick, prevRotation * Math.PI * 2, rotation * Math.PI * 2);
    }
}
