package com.sandevistanmod.net;

import com.sandevistanmod.SandevistanMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor para o cliente: toca o video da primeira vez que o jogador usa o Sandevistan depois de implantar. */
public record IntroPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<IntroPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "intro"));
    public static final StreamCodec<ByteBuf, IntroPayload> CODEC = StreamCodec.unit(new IntroPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
