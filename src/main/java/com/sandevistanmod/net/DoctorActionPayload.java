package com.sandevistanmod.net;

import com.sandevistanmod.SandevistanMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente para o servidor: 1 implantar, 2 remover implante, 3 comprar Estabilizador, 4 comprar Imunossupressor. */
public record DoctorActionPayload(int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DoctorActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "doctor_action"));
    public static final StreamCodec<ByteBuf, DoctorActionPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(DoctorActionPayload::new, DoctorActionPayload::action);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
