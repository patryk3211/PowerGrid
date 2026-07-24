package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.patryk3211.powergrid.PowerGrid;

public class EnergyMeterScreen extends AbstractSimiContainerScreen<EnergyMeterMenu> {
    private static final ResourceLocation BACKGROUND = PowerGrid.texture("gui/energy_meter");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 60;

    public EnergyMeterScreen(EnergyMeterMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);
        setWindowOffset(0, 0);

        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float partialTick, int mouseX, int mouseY) {
        int bgX = getLeftOfCentered(WIDTH);
        ctx.blit(BACKGROUND, bgX, topPos, 0, 0, WIDTH, HEIGHT);

        ctx.drawCenteredString(font, title, leftPos + (WIDTH - 8) / 2, topPos + 3, 0xFFFFFF);

        var ms = ctx.pose();
        int x = 0;
        for(int i = 10000; i >= 1; i /= 10) {
            float rotation = getDialValue(menu.contentHolder.energy, i);
            float prevRotation = getDialValue(menu.contentHolder.lastEnergy, i);
            if(prevRotation > rotation) {
                rotation += 1;
            }
            float angle = (float) Mth.lerp(partialTick, prevRotation * Math.PI * 2, rotation * Math.PI * 2);

            ms.pushPose();
            ms.translate(leftPos + 18-2 + x, topPos + 37-6, 0);
            ms.rotateAround(new Quaternionf().rotateZ(angle), 2.5f, 6.5f, 0);
            ctx.blit(BACKGROUND, 0, 0, 183, 3, 5, 9);
            ms.popPose();
            x += 32;
        }

        ms.pushPose();
        float rotation = getDialValue(menu.contentHolder.energy, 0.1);
        float prevRotation = getDialValue(menu.contentHolder.lastEnergy, 0.1);
        if(prevRotation > rotation) {
            rotation += 1;
        }
        float angle = AngleHelper.angleLerp(partialTick, prevRotation * Math.PI * 2, rotation * Math.PI * 2);
        ms.translate(leftPos + 168, topPos + 41, 0);
        ms.rotateAround(new Quaternionf().rotateZ(angle), 1.5f, 3.5f, 0);
        ctx.blit(BACKGROUND, 0, 0, 184, 14, 3, 5);
        ms.popPose();

    }

    public static float getDialValue(double energy, double multiplier) {
        return (float) ((energy / multiplier / 10.0) % 1);
    }
}
