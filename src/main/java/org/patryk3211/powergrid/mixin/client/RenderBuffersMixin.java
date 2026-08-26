package org.patryk3211.powergrid.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderBuffers;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;

@Mixin(RenderBuffers.class)
public abstract class RenderBuffersMixin {
    @Inject(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;immediateWithBuffers(Ljava/util/SequencedMap;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;)Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    void powerGrid$addLayer(int bufferCount, CallbackInfo ci, @Local SequencedMap sequencedMap) {
        var layer = ModdedRenderLayers.getAdditive();
        sequencedMap.put(layer, new ByteBufferBuilder(layer.bufferSize()));
    }
}
