package org.patryk3211.powergrid.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;

@Mixin(RenderBuffers.class)
public abstract class RenderBuffersAccessor {
    @Shadow
    private static void put(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> mapBuilders, RenderType renderType) {
    }

    @Inject(method="Lnet/minecraft/client/renderer/RenderBuffers;<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;immediateWithBuffers(Ljava/util/SequencedMap;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;)Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    void getBufferSource(int bufferCount, CallbackInfo ci, @Local(name = "sequencedMap") SequencedMap<RenderType, ByteBufferBuilder> sequencedMap) {
        var layer = ModdedRenderLayers.getAdditive();
        sequencedMap.put(layer, new ByteBufferBuilder(layer.bufferSize()));
    }
}
