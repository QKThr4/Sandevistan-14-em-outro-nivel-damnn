package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.SandevistanVariant;
import com.sandevistanmod.data.Tx;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ClientOverlay {
    private ClientOverlay() {}

    /** Tela com um tiquinho da cor do modelo ligado (verde, verde escuro, azul, neutra ou laranja). */
    public static void renderVision(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        SandevistanData d = ClientData.DATA;
        if (!d.implanted || !d.active) return;
        int w = gg.guiWidth();
        int h = gg.guiHeight();
        SandevistanVariant v = d.variant();
        gg.fill(0, 0, w, h, v.tint);
        if (v == SandevistanVariant.QIANT) {
            // escudo de energia termica: bordas de ambar nos cantos da tela
            int e = Math.max(6, w / 40);
            int c = 0x38FF8A1E;
            gg.fill(0, 0, w, e, c);
            gg.fill(0, h - e, w, h, c);
            gg.fill(0, e, e, h - e, c);
            gg.fill(w - e, e, w, h - e, c);
        }
    }

    private static String time(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    public static int mindColor(float mind) {
        if (mind > 40f) return 0xFF39FF14;
        if (mind > 20f) return 0xFFFFCC22;
        return 0xFFFF3333;
    }

    public static void bar(GuiGraphics gg, int x, int y, int w, int h, float frac, int color) {
        gg.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        gg.fill(x, y, x + w, y + h, 0xFF16201A);
        int fw = (int) (w * Math.max(0f, Math.min(1f, frac)));
        gg.fill(x, y, x + fw, y + h, color);
    }

    /** Marca em vermelho escuro, no fim da barra, a parte da Mente perdida de vez. */
    public static void lostMark(GuiGraphics gg, int x, int y, int w, int h, float lost) {
        int lw = (int) (w * Math.max(0f, Math.min(1f, lost / SandevistanData.MAX_MIND)));
        if (lw > 0) gg.fill(x + w - lw, y, x + w, y + h, 0xFF5A0A0A);
    }

    /** Marca em azul, antes da parte perdida, a Mente que os chips inseridos tiram (volta se o chip sair). */
    public static void chipMark(GuiGraphics gg, int x, int y, int w, int h, float lost, float chipMind) {
        int lw = (int) (w * Math.max(0f, Math.min(1f, lost / SandevistanData.MAX_MIND)));
        int cw = (int) (w * Math.max(0f, Math.min(1f, chipMind / SandevistanData.MAX_MIND)));
        if (cw > 0) gg.fill(x + w - lw - cw, y, x + w - lw, y + h, 0xFF2F6BFF);
    }

    /** Cor da Carga Cibernetica: verde ate 49%, laranja a partir de 50% (pode dar psicose), vermelho a partir de 80%. */
    public static int loadColor(float load) {
        if (load >= 80f) return 0xFFFF3333;
        if (load >= 50f) return 0xFFFF9A22;
        return 0xFF39FF14;
    }

    /** HUD: tres blocos independentes (Mente, ciberpsicose e avisos), cada um na posicao escolhida no editor. */
    public static void renderHud(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        SandevistanData d = ClientData.DATA;
        if (!d.hasAny()) return;
        int sw = gg.guiWidth();
        int sh = gg.guiHeight();
        int mh = ClientLayout.scaled(d.implanted ? ClientLayout.MIND_H : ClientLayout.MIND_H - 12);
        int[] mp = ClientLayout.mindPos(sw, sh, mh);
        block(gg, mp, () -> drawMind(gg, mc.font, d));
        int[] last = mp;
        int lastH = mh;
        if (d.psy > 0f) {
            int ph = ClientLayout.scaled(ClientLayout.PSY_H);
            int[] pp = ClientLayout.psyPos(sw, sh, mp, mh, ph);
            block(gg, pp, () -> drawPsy(gg, mc.font, d));
            last = pp;
            lastH = ph;
        }
        int lines = statusLines(d);
        if (lines > 0) {
            int[] sp = ClientLayout.statusPos(sw, sh, last, lastH, ClientLayout.scaled(lines * 10));
            block(gg, sp, () -> drawStatus(gg, mc.font, d));
        }
    }

    private static void block(GuiGraphics gg, int[] pos, Runnable draw) {
        gg.pose().pushPose();
        gg.pose().translate(pos[0], pos[1], 0);
        gg.pose().scale(ClientLayout.SCALE, ClientLayout.SCALE, 1f);
        draw.run();
        gg.pose().popPose();
    }

    private static void drawMind(GuiGraphics gg, Font f, SandevistanData d) {
        int y = 0;
        if (d.implanted) {
            MutableComponent title = Component.literal("[")
                    .append(ModKeys.TOGGLE.getTranslatedKeyMessage())
                    .append(Component.literal("] "))
                    .append(Tx.t("hud.title"))
                    .append(Component.literal(": "))
                    .append(d.active ? Tx.t("hud.on", time(d.secondsLeft())) : Tx.t("hud.off"));
            gg.drawString(f, title, 0, 0, d.active ? 0x39FF14 : 0xAAAAAA);
            y = 12;
        }
        bar(gg, 0, y, 110, 7, d.mind / SandevistanData.MAX_MIND, mindColor(d.mind));
        lostMark(gg, 0, y, 110, 7, SandevistanData.MAX_MIND - d.maxMind() - d.chipMind());
        chipMark(gg, 0, y, 110, 7, SandevistanData.MAX_MIND - d.maxMind() - d.chipMind(), d.chipMind());
        gg.drawString(f, Tx.t("hud.mind", (int) d.mind), 0, y + 10, d.mind < 20f ? 0xFF5555 : 0x88FFAA);
    }

    private static void drawPsy(GuiGraphics gg, Font f, SandevistanData d) {
        int col = d.psy >= 100f ? 0xFFFF2030 : d.psy >= 51f ? 0xFFFF6A20 : 0xFFD84AFF;
        bar(gg, 0, 0, 110, 5, d.psy / 100f, col);
        gg.drawString(f, Tx.t("hud.psy", (int) d.psy), 0, 8, d.psy >= 100f ? 0xFF4455 : 0xDD88FF);
    }

    private static int statusLines(SandevistanData d) {
        int n = d.goodMind > 0f ? 1 : 0;
        n += Math.min(2, d.doses.size());
        for (int i = 0; i < 2; i++) if (d.chip[i] != 0 && d.chipLoad[i] > 0f) n++;
        return n;
    }

    private static void drawStatus(GuiGraphics gg, Font f, SandevistanData d) {
        int y = 0;
        if (d.goodMind > 0f) {
            gg.drawString(f, Tx.t("hud.goodmind", time((int) Math.ceil(d.goodMind))), 0, y, 0x66FF99);
            y += 10;
        }
        // no maximo 2 Imunossupressores: o que esta para acabar e o proximo (cada um tem o seu tempo)
        for (int i = 0; i < Math.min(2, d.doses.size()); i++) {
            gg.drawString(f, Tx.t("hud.immuno", time((int) Math.ceil(d.doses.get(i)[0]))), 0, y, 0x66CCFF);
            y += 10;
        }
        for (int i = 0; i < 2; i++) {
            if (d.chip[i] != 0 && d.chipLoad[i] > 0f) {
                gg.drawString(f, Tx.t("chip.dl", (int) Math.ceil(d.chipLoad[i])), 0, y, 0x55DDFF);
                y += 10;
            }
        }
    }
}
