package com.sandevistanmod.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Preferencias do jogador: onde fica cada bloco do HUD (Mente, ciberpsicose e avisos) e a musica da ciberpsicose.
 * As posicoes sao fracoes (0 a 1) da area livre da tela; -1 no Y significa "empilhar embaixo do bloco anterior".
 */
public final class ClientLayout {
    private ClientLayout() {}

    /** Escala dos blocos do HUD (a mesma no jogo e no editor). */
    public static final float SCALE = 0.8f;
    public static final int W = 120;
    public static final int MIND_H = 34;
    public static final int PSY_H = 19;
    public static final int STATUS_H = 32;
    private static final int MARGIN = 6;

    public static float mindX = 0f;
    public static float mindY = 0f;
    public static float psyX = 0f;
    public static float psyY = -1f;
    public static float statusX = 0f;
    public static float statusY = -1f;
    /** Musica da ciberpsicose: 0 nenhuma, 1 e 2 as duas musicas. */
    public static int music = 1;

    static {
        load();
    }

    public static int scaled(int v) {
        return Math.round(v * SCALE);
    }

    public static int place(float frac, int screen, int size) {
        int free = Math.max(0, screen - size - MARGIN * 2);
        return MARGIN + Math.round(Math.max(0f, Math.min(1f, frac)) * free);
    }

    public static float frac(double px, int screen, int size) {
        int free = Math.max(1, screen - size - MARGIN * 2);
        return (float) Math.max(0.0, Math.min(1.0, (px - MARGIN) / free));
    }

    /** Posicao (x, y) do bloco da Mente; h = altura ja com a escala. */
    public static int[] mindPos(int sw, int sh, int h) {
        return new int[] {place(mindX, sw, scaled(W)), place(mindY, sh, h)};
    }

    /** Posicao do bloco da ciberpsicose; 'mind' e a posicao do bloco da Mente e mh a altura dele. */
    public static int[] psyPos(int sw, int sh, int[] mind, int mh, int h) {
        if (psyY < 0f) return new int[] {mind[0], mind[1] + mh + 2};
        return new int[] {place(psyX, sw, scaled(W)), place(psyY, sh, h)};
    }

    /** Posicao do bloco de avisos (Mente Boa, Imunossupressor, chips); 'prev' e o bloco visivel logo acima dele. */
    public static int[] statusPos(int sw, int sh, int[] prev, int prevH, int h) {
        if (statusY < 0f) return new int[] {prev[0], prev[1] + prevH + 2};
        return new int[] {place(statusX, sw, scaled(W)), place(statusY, sh, h)};
    }

    public static void nextMusic() {
        music = (music + 1) % 3;
        save();
    }

    public static void reset() {
        mindX = 0f;
        mindY = 0f;
        psyX = 0f;
        psyY = -1f;
        statusX = 0f;
        statusY = -1f;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("sandevistan-client.properties");
    }

    private static float num(Properties p, String key, float def) {
        try {
            return Float.parseFloat(p.getProperty(key, Float.toString(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            }
            music = Math.max(0, Math.min(2, (int) num(p, "music", 1f)));
            mindX = num(p, "mindX", 0f);
            mindY = num(p, "mindY", 0f);
            psyX = num(p, "psyX", 0f);
            psyY = num(p, "psyY", -1f);
            statusX = num(p, "statusX", 0f);
            statusY = num(p, "statusY", -1f);
        } catch (Exception ignored) {
            // sem arquivo ou arquivo invalido: fica com o padrao
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("music", Integer.toString(music));
            p.setProperty("mindX", Float.toString(mindX));
            p.setProperty("mindY", Float.toString(mindY));
            p.setProperty("psyX", Float.toString(psyX));
            p.setProperty("psyY", Float.toString(psyY));
            p.setProperty("statusX", Float.toString(statusX));
            p.setProperty("statusY", Float.toString(statusY));
            try (OutputStream out = Files.newOutputStream(file())) {
                p.store(out, "Sandevistan client settings");
            }
        } catch (Exception ignored) {
            // nao foi possivel salvar: vale so ate fechar o jogo
        }
    }
}
