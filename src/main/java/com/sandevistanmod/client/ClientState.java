package com.sandevistanmod.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Estado visivel de cada jogador (implante nas costas, Sandevistan ligado), recebido do servidor. */
public final class ClientState {
    private ClientState() {}

    private static final Map<UUID, Integer> STATES = new HashMap<>();

    public static void set(UUID id, int code) {
        int before = STATES.getOrDefault(id, 0);
        if (code <= 0) STATES.remove(id);
        else STATES.put(id, code);
        boolean was = (before & 2) != 0;
        boolean now = (code & 2) != 0;
        if (was != now) ClientSounds.onToggle(id, now, (code >> 14) & 7);
    }

    /** Ciberpsicose visivel do jogador (0 a 100; 100 = 100% ou mais). 0 se nao ha sintomas. */
    public static int psy(UUID id) {
        int c = get(id);
        return (c & 4) != 0 ? (c >> 3) & 127 : 0;
    }

    public static int get(UUID id) {
        return STATES.getOrDefault(id, 0);
    }

    public static boolean implanted(UUID id) {
        return (get(id) & 1) != 0;
    }

    /** Modelo do Sandevistan do jogador (0 Apogee, 1 Falcon, 2 Dynalar, 3 Zetatech, 4 QianT). */
    public static int variant(UUID id) {
        return (get(id) >> 14) & 7;
    }

    public static boolean armR(UUID id) {
        return (get(id) & (1 << 10)) != 0;
    }

    public static boolean armL(UUID id) {
        return (get(id) & (1 << 11)) != 0;
    }

    public static boolean neural(UUID id) {
        return (get(id) & (1 << 12)) != 0;
    }

    public static boolean armsVisible(UUID id) {
        return (get(id) & (1 << 13)) != 0;
    }

    public static boolean active(UUID id) {
        return (get(id) & 2) != 0;
    }

    public static Set<UUID> activeIds() {
        Set<UUID> out = new HashSet<>();
        for (Map.Entry<UUID, Integer> en : STATES.entrySet()) {
            if ((en.getValue() & 2) != 0) out.add(en.getKey());
        }
        return out;
    }

    public static void clear() {
        STATES.clear();
    }
}
