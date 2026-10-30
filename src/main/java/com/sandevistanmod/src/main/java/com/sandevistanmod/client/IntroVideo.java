package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Video da primeira vez: 38 quadros (16:9) a 30 por segundo, desenhados em tela cheia por cima de tudo.
 * O video cobre a tela inteira em qualquer resolucao ou nivel de HUD (recorta so o excesso quando a janela nao e 16:9).
 */
public final class IntroVideo {
    private IntroVideo() {}

    private static final int FRAMES = 38;
    private static final double FPS = 30.0;
    private static final int TEX_W = 854;
    private static final int TEX_H = 480;
    private static final ResourceLocation[] TEX = new ResourceLocation[FRAMES];

    static {
        for (int i = 0; i < FRAMES; i++) {
            TEX[i] = ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID,
                    String.format("textures/gui/intro/f%02d.png", i));
        }
    }

    private static long startNanos = -1L;

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        try {
            for (ResourceLocation rl : TEX) mc.getTextureManager().getTexture(rl);
        } catch (RuntimeException ignored) {
            // se algum quadro falhar, o desenho abaixo simplesmente pula
        }
        startNanos = System.nanoTime();
    }

    public static void stop() {
        startNanos = -1L;
    }

    public static void render(GuiGraphics gg, DeltaTracker delta) {
        if (startNanos < 0L) return;
        double t = (System.nanoTime() - startNanos) / 1.0e9;
        int frame = (int) (t * FPS);
        if (frame >= FRAMES) {
            startNanos = -1L;
            return;
        }
        int sw = gg.guiWidth();
        int sh = gg.guiHeight();
        // "cover": preenche a tela toda; so recorta a sobra do lado maior
        float screenAspect = sw / (float) sh;
        float vidAspect = TEX_W / (float) TEX_H;
        float u = 0f;
        float v = 0f;
        float uw = TEX_W;
        float vh = TEX_H;
        if (screenAspect > vidAspect) {
            vh = TEX_W / screenAspect;
            v = (TEX_H - vh) / 2f;
        } else if (screenAspect < vidAspect) {
            uw = TEX_H * screenAspect;
            u = (TEX_W - uw) / 2f;
        }
        gg.blit(TEX[frame], 0, 0, sw, sh, u, v, Math.max(1, Math.round(uw)), Math.max(1, Math.round(vh)), TEX_W, TEX_H);
    }
}
