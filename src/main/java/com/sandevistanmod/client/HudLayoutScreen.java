package com.sandevistanmod.client;

import com.sandevistanmod.data.Tx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Editor do HUD: arraste cada bloco (Mente, ciberpsicose e avisos) para onde quiser na tela. */
public class HudLayoutScreen extends Screen {
    private final Screen parent;
    /** Bloco arrastado: 1 Mente, 2 ciberpsicose, 3 avisos (0 nenhum). */
    private int dragging;
    private double offX;
    private double offY;

    public HudLayoutScreen(Screen parent) {
        super(Tx.t("layout.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Tx.t("layout.reset"), b -> ClientLayout.reset())
                .bounds(cx - 100, this.height - 28, 98, 20).build());
        addRenderableWidget(Button.builder(Tx.t("layout.save"), b -> onClose())
                .bounds(cx + 2, this.height - 28, 98, 20).build());
    }

    private int mh() {
        return ClientLayout.scaled(ClientLayout.MIND_H);
    }

    private int ph() {
        return ClientLayout.scaled(ClientLayout.PSY_H);
    }

    private int sh() {
        return ClientLayout.scaled(ClientLayout.STATUS_H);
    }

    private int w() {
        return ClientLayout.scaled(ClientLayout.W);
    }

    private int[] mind() {
        return ClientLayout.mindPos(this.width, this.height, mh());
    }

    private int[] psy() {
        return ClientLayout.psyPos(this.width, this.height, mind(), mh(), ph());
    }

    private int[] status() {
        return ClientLayout.statusPos(this.width, this.height, psy(), ph(), sh());
    }

    private static boolean inside(double mx, double my, int[] p, int w, int h) {
        return mx >= p[0] && mx < p[0] + w && my >= p[1] && my < p[1] + h;
    }

    private void box(GuiGraphics gg, int[] p, int h, Component label, int color) {
        int x = p[0];
        int y = p[1];
        int w = w();
        gg.fill(x, y, x + w, y + h, 0x80000000 | (color & 0xFFFFFF));
        gg.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        gg.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        gg.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        gg.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);
        gg.drawCenteredString(this.font, label, x + w / 2, y + h / 2 - 4, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        gg.drawCenteredString(this.font, Tx.t("layout.title"), cx, this.height / 2 - 14, 0xFFFFFF);
        gg.drawCenteredString(this.font, Tx.t("layout.hint"), cx, this.height / 2 - 2, 0xCCCCCC);
        box(gg, mind(), mh(), Tx.t("layout.box.mind"), 0x22AA55);
        box(gg, psy(), ph(), Tx.t("layout.box.psy"), 0xAA33DD);
        box(gg, status(), sh(), Tx.t("layout.box.status"), 0x2288DD);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        int[] s = status();
        int[] p = psy();
        int[] m = mind();
        if (inside(mx, my, s, w(), sh())) {
            // passa para posicao propria (sai do empilhamento automatico)
            ClientLayout.statusX = ClientLayout.frac(s[0], this.width, w());
            ClientLayout.statusY = ClientLayout.frac(s[1], this.height, sh());
            dragging = 3;
            offX = mx - s[0];
            offY = my - s[1];
            return true;
        }
        if (inside(mx, my, p, w(), ph())) {
            ClientLayout.psyX = ClientLayout.frac(p[0], this.width, w());
            ClientLayout.psyY = ClientLayout.frac(p[1], this.height, ph());
            dragging = 2;
            offX = mx - p[0];
            offY = my - p[1];
            return true;
        }
        if (inside(mx, my, m, w(), mh())) {
            dragging = 1;
            offX = mx - m[0];
            offY = my - m[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == 1) {
            ClientLayout.mindX = ClientLayout.frac(mx - offX, this.width, w());
            ClientLayout.mindY = ClientLayout.frac(my - offY, this.height, mh());
            return true;
        }
        if (dragging == 2) {
            ClientLayout.psyX = ClientLayout.frac(mx - offX, this.width, w());
            ClientLayout.psyY = ClientLayout.frac(my - offY, this.height, ph());
            return true;
        }
        if (dragging == 3) {
            ClientLayout.statusX = ClientLayout.frac(mx - offX, this.width, w());
            ClientLayout.statusY = ClientLayout.frac(my - offY, this.height, sh());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClientLayout.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
