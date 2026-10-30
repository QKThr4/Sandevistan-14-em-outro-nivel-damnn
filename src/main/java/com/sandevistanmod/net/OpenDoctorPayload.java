package com.sandevistanmod.net;

import com.sandevistanmod.SandevistanMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor para o cliente: abre a tela do Ripperdoc (Loja / Implantes). */
public record OpenDoctorPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenDoctorPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "open_doctor"));
    public static final StreamCodec<ByteBuf, OpenDoctorPayload> CODEC = StreamCodec.unit(new OpenDoctorPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
