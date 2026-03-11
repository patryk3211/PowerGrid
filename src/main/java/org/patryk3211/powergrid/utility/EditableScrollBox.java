package org.patryk3211.powergrid.utility;

import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.createmod.catnip.gui.widget.AbstractSimiWidget.HEADER_RGB;

public class EditableScrollBox extends EditBox {
    protected final List<String> options = new ArrayList<>();
    protected int selectedIndex = -1;
    private boolean soundPlayed = false;
    private final List<Component> toolTip = new ArrayList<>();

    private final Component title;
    private final Component scrollToSelect = Lang.translateDirect("gui.edit_scroll_box.scroll");

    private Consumer<Integer> optionCallback;

    public EditableScrollBox(Font font, int x, int y, int width, int height, Component message, Component title) {
        super(font, x, y, width, height, message);
        this.title = title;

        setTextColor(-1);
        setBordered(false);
        setEditable(true);
        onChanged();
    }

    @Override
    public void tick() {
        super.tick();
        soundPlayed = false;
    }

    public void setOptions(List<String> options) {
        this.options.clear();
        this.options.addAll(options);
        int prev = selectedIndex;
        clamp();
        if(prev != selectedIndex)
            onChanged();
        updateTooltip();
    }

    protected void onChanged() {
        if(selectedIndex >= 0 && selectedIndex < options.size()) {
            setValue(options.get(selectedIndex));
            if(optionCallback != null)
                optionCallback.accept(selectedIndex);
        }
        updateTooltip();
    }

    public void setState(int index) {
        if(selectedIndex != index) {
            selectedIndex = index;
            onChanged();
        }
    }

    public void calling(Consumer<Integer> optionSelected, Consumer<String> customValue) {
        this.optionCallback = optionSelected;
        setResponder(customValue);
    }

    @Override
    public void setResponder(Consumer<String> responder) {
        super.setResponder(responder);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = (int) -Math.signum(delta) * (AllKeys.shiftDown() ? 5 : 1);

        int priorState = selectedIndex;
        boolean shifted = AllKeys.shiftDown();
        selectedIndex += step;
        if(shifted)
            selectedIndex -= selectedIndex % 5;

        clamp();

        if(priorState != selectedIndex) {
            if (!soundPlayed)
                Minecraft.getInstance()
                        .getSoundManager()
                        .play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.getMainEvent(),
                                1.5f + 0.1f * (selectedIndex) / (options.size() - 1)));
            soundPlayed = true;
            onChanged();
        }

        return priorState != selectedIndex;
    }

    protected void clamp() {
        if(selectedIndex < 0)
            selectedIndex = 0;
        if(selectedIndex >= options.size())
            selectedIndex = options.size() - 1;
    }

    protected void updateTooltip() {
        toolTip.clear();
        toolTip.add(title.plainCopy()
                .withStyle(s -> s.withColor(HEADER_RGB.getRGB())));
        int min = Math.min(this.options.size() - 16, selectedIndex - 7);
        int max = Math.max(16, selectedIndex + 8);
        min = Math.max(min, 0);
        max = Math.min(max, this.options.size());
        if (1 == min)
            min--;
        if (min > 0) {
            toolTip.add(Component.literal("> ...")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (this.options.size() - 1 == max)
            max++;
        for (int i = min; i < max; i++) {
            if (i == selectedIndex)
                toolTip.add(Component.empty()
                        .append("-> ")
                        .append(options.get(i))
                        .withStyle(ChatFormatting.WHITE));
            else
                toolTip.add(Component.empty()
                        .append("> ")
                        .append(options.get(i))
                        .withStyle(ChatFormatting.GRAY));
        }
        if (max < this.options.size()) {
            toolTip.add(Component.literal("> ...")
                    .withStyle(ChatFormatting.GRAY));
        }

        toolTip.add(scrollToSelect.plainCopy()
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public List<Component> getToolTip() {
        return toolTip;
    }
}
