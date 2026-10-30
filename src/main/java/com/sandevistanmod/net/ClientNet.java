package com.sandevistanmod.net;

import com.sandevistanmod.client.ClientData;
import net.minecraft.core.RegistryAccess;

public final class ClientNet {
    private ClientNet() {}

    public static void openDoctor() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.sandevistanmod.client.DoctorScreen());
    }

    public static void handleSync(SyncPayload payload) {
        ClientData.DATA.deserializeNBT(RegistryAccess.EMPTY, payload.tag());
    }
}
