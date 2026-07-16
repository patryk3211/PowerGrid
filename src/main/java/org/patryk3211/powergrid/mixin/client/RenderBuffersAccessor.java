package org.patryk3211.powergrid.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.SortedMap;

@Mixin(RenderBuffers.class)
public interface RenderBuffersAccessor {
    @Accessor("fixedBuffers")
    SortedMap<RenderType, BufferBuilder> getFixedBuffers();
}
