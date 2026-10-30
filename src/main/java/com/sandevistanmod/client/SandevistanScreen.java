package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
import com.sandevistanmod.net.ActionPayload;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Aba J: implantes do jogador (so visual), Carga Cibernetica, chips, botoes do HUD e a aba de efeitos de cada implante. */
public class SandevistanScreen extends Screen {
    private Button armBtn;
    private Button tab0;
    private Button tab1;
    private final java.util.List<Button> page0 = new java.util.ArrayList<>();
    /** 0 implantes, 1 efeitos. */
    private static int tab;

    public SandevistanScreen() {
        super(Tx.t("implants.title"));
    }

    private int cx() {
        return this.width / 2;
    }

    private int y0() {
        return this.height / 2 - 102;
    }

    private int lx() {
        return cx() - 100;
    }

    @Override
    protected void init() {
        page0.clear();
        int bx = cx() + 16;
        int y = y0();
        tab0 = addRenderableWidget(Button.builder(Tx.t("tab.implants"), b -> setTab(0))
                .bounds(cx() - 92, y - 22, 90, 18).build());
        tab1 = addRenderableWidget(Button.builder(Tx.t("tab.effects"), b -> setTab(1))
                .bounds(cx() + 2, y - 22, 90, 18).build());
        armBtn = addRenderableWidget(Button.builder(armLabel(), b -> PacketDistributor.sendToServer(new ActionPayload(4)))
                .bounds(bx, y + 104, 176, 20).build());
        page0.add(armBtn);
        page0.add(addRenderableWidget(Button.builder(Tx.t("btn.layout"),
                        b -> Minecraft.getInstance().setScreen(new HudLayoutScreen(this)))
                .bounds(bx, y + 126, 176, 20).build()));
        page0.add(addRenderableWidget(Button.builder(musicLabel(), b -> {
                    ClientLayout.nextMusic();
                    b.setMessage(musicLabel());
                })
                .bounds(bx, y + 148, 176, 20).build()));
        page0.add(addRenderableWidget(Button.builder(Tx.t("btn.close"), b -> onClose())
                .bounds(bx, y + 170, 176, 20).build()));
        updateWidgets();
    }

    private void setTab(int t) {
        tab = t;
        updateWidgets();
    }

    private void updateWidgets() {
        for (Button b : page0) b.visible = tab == 0;
        if (armBtn != null) armBtn.visible = tab == 0 && ClientData.DATA.arms() > 0;
        if (tab0 != null) tab0.active = tab != 0;
        if (tab1 != null) tab1.active = tab != 1;
    }

    private static Component armLabel() {
        return ClientData.DATA.armsVisible ? Tx.t("arms.visible") : Tx.t("arms.hidden");
    }

    private static Component musicLabel() {
        return Tx.t("btn.music", Tx.t("music." + ClientLayout.music));
    }

    @Override
    public void tick() {
        if (armBtn != null) armBtn.setMessage(armLabel());
        updateWidgets();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.SCREEN.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- posicoes dos encaixes ----
    private int slotX(int slot) {
        return switch (slot) {
            case SandevistanData.SLOT_BACK, SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_BONE -> lx() - 80;
            default -> lx() + 62;
        };
    }

    private int slotY(int slot) {
        return y0() + switch (slot) {
            case SandevistanData.SLOT_BACK, SandevistanData.SLOT_NEURAL -> 26;
            case SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_ARM_L -> 56;
            default -> 86;
        };
    }

    private int chipX() {
        return cx() + 16;
    }

    private int chipY(int i) {
        return y0() + 58 + i * 22;
    }

    private static boolean in(double mx, double my, int x, int y) {
        return mx >= x && mx < x + 18 && my >= y && my < y + 18;
    }

    private static ItemStack slotStack(int slot) {
        return new ItemStack(SandevistanLogic.implantItem(slot, ClientData.DATA.variant));
    }

    private static ItemStack chipStack(SandevistanData d, int i) {
        if (d.chip[i] == 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(ModRegistries.CHIPS.get(d.chip[i] - 1).get());
        s.setDamageValue(d.chipDmg[i]);
        return s;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && tab == 0) {
            for (int i = 0; i < 2; i++) {
                if (ClientData.DATA.chip[i] != 0 && in(mx, my, chipX(), chipY(i))) {
                    PacketDistributor.sendToServer(new ActionPayload(2 + i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static final String[] SLOT_KEYS = {"slot.back", "slot.armr", "slot.arml", "slot.neural", "slot.bone", "slot.nerve"};

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = cx();
        int y0 = y0();
        gg.fill(cx - 200, y0, cx + 200, y0 + 204, 0xC0101410);
        gg.fill(cx - 200, y0, cx + 200, y0 + 2, 0xFF39FF14);
        if (tab == 1) {
            drawEffects(gg, cx, y0);
            return;
        }
        SandevistanData d = ClientData.DATA;
        gg.fill(cx + 5, y0 + 8, cx + 6, y0 + 196, 0x40FFFFFF);
        gg.drawCenteredString(this.font, Tx.t("implants.title"), lx(), y0 + 7, 0x39FF14);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gg, lx() - 45, y0 + 18, lx() + 45, y0 + 112, 40,
                    0.0625f, mouseX, mouseY, mc.player);
        }
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            int x = slotX(s);
            int y = slotY(s);
            boolean on = d.has(s);
            gg.fill(x - 1, y - 1, x + 19, y + 19, on ? 0xFF39FF14 : 0xFF666666);
            gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
            if (on) gg.renderItem(slotStack(s), x + 1, y + 1);
            gg.drawCenteredString(this.font, Tx.t(SLOT_KEYS[s]), x + 9, y + 22, on ? 0xCCCCCC : 0x888888);
        }

        // Carga Cibernetica
        float cap = d.capacity();
        int bx = cx + 16;
        gg.drawCenteredString(this.font, Tx.t("load.title", Math.round(cap)), cx + 104, y0 + 7,
                cap >= 50f ? 0xFFAA33 : 0x88FFAA);
        ClientOverlay.bar(gg, bx, y0 + 20, 176, 9, cap / 100f, ClientOverlay.loadColor(cap));
        gg.fill(bx + 88, y0 + 18, bx + 89, y0 + 31, 0xFFFFFFFF);
        gg.drawWordWrap(this.font, Tx.t("load.hint"), bx, y0 + 34, 176, 0x888888);

        // chips
        for (int i = 0; i < 2; i++) {
            int x = chipX();
            int y = chipY(i);
            boolean has = d.chip[i] != 0;
            gg.fill(x - 1, y - 1, x + 19, y + 19, has ? 0xFF55DDFF : 0xFF666666);
            gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
            if (has) {
                ItemStack st = chipStack(d, i);
                gg.renderItem(st, x + 1, y + 1);
                gg.renderItemDecorations(this.font, st, x + 1, y + 1);
                gg.drawString(this.font, st.getHoverName(), x + 24, y + 1, 0xFFFFFF);
                if (d.chipLoad[i] > 0f) {
                    gg.drawString(this.font, Tx.t("chip.dl", (int) Math.ceil(d.chipLoad[i])), x + 24, y + 10, 0x55DDFF);
                } else if (d.chipDmg[i] >= SandevistanData.chipMax(d.chip[i])) {
                    gg.drawString(this.font, Tx.t("chip.brokenshort"), x + 24, y + 10, 0xFF5555);
                } else {
                    gg.drawString(this.font, Tx.t("chip.working"), x + 24, y + 10, 0x55FF55);
                }
            } else {
                gg.drawString(this.font, d.neural ? Tx.t("chip.none") : Tx.t("chip.noneural"), x + 24, y + 5, 0x888888);
            }
        }
    }

    /** Aba de efeitos: o que cada implante instalado esta causando no corpo do jogador. */
    private void drawEffects(GuiGraphics gg, int cx, int y0) {
        SandevistanData d = ClientData.DATA;
        int x = cx - 190;
        int y = y0 + 10;
        gg.drawCenteredString(this.font, Tx.t("tab.effects.title"), cx, y, 0x39FF14);
        y += 16;
        if (!d.hasAny()) {
            gg.drawCenteredString(this.font, Tx.t("eff.none"), cx, y + 30, 0x999999);
            return;
        }
        if (d.implanted) {
            var v = d.variant();
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_BACK, d.variant).getDescription(),
                    Tx.t("eff.sandevistan", v.slow, (int) v.seconds, Math.round(v.dodge * 100f)));
            y += 11;
            if (d.variant == 3) {
                effect(gg, x + 8, y, Component.empty(), Tx.t("variant.tip.zetatech"));
                y += 11;
            } else if (d.variant == 4) {
                effect(gg, x + 8, y, Component.empty(), Tx.t("variant.tip.qiant"));
                y += 11;
            }
        }
        if (d.arms() > 0) {
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_ARM_R, 0).getDescription(),
                    Tx.t("eff.arm", d.arms(), 3 * d.arms()));
            y += 11;
        }
        if (d.bone) {
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_BONE, 0).getDescription(), Tx.t("eff.bone"));
            y += 11;
        }
        if (d.nerve) {
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_NERVE, 0).getDescription(), Tx.t("eff.nerve"));
            y += 11;
        }
        if (d.neural) {
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_NEURAL, 0).getDescription(), Tx.t("eff.neural"));
            y += 11;
        }
        for (int i = 0; i < 2; i++) {
            if (d.chip[i] == 0) continue;
            String[] keys = {"tooltip.chip.martial", "tooltip.chip.pain", "tooltip.chip.mining", "tooltip.chip.walker"};
            Component status = d.chipLoad[i] > 0f ? Tx.t("chip.dl", (int) Math.ceil(d.chipLoad[i]))
                    : d.chipDmg[i] >= SandevistanData.chipMax(d.chip[i]) ? Tx.t("chip.brokenshort") : Tx.t("chip.working");
            effect(gg, x, y, chipStack(d, i).getHoverName(),
                    Component.empty().append(Tx.t(keys[d.chip[i] - 1])).append(Component.literal(" (")).append(status)
                            .append(Component.literal(")")));
            y += 11;
        }
        y = y0 + 168;
        float cap = d.capacity();
        gg.drawString(this.font, Tx.t("load.title", Math.round(cap)), x, y, cap >= 50f ? 0xFFAA33 : 0x88FFAA);
        if (d.chipMind() > 0f) {
            gg.drawString(this.font, Tx.t("eff.mind", (int) d.chipMind()), x, y + 11, 0x55AAFF);
        }
    }

    private void effect(GuiGraphics gg, int x, int y, Component name, Component desc) {
        gg.drawString(this.font, name, x, y, 0x55FF99);
        int w = this.font.width(name);
        gg.drawString(this.font, desc, x + w + (w > 0 ? 6 : 0), y, 0xCCCCCC);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        if (tab != 0) return;
        SandevistanData d = ClientData.DATA;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if (d.has(s) && in(mouseX, mouseY, slotX(s), slotY(s))) {
                gg.renderTooltip(this.font, slotStack(s), mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (d.chip[i] != 0 && in(mouseX, mouseY, chipX(), chipY(i))) {
                gg.renderTooltip(this.font, chipStack(d, i), mouseX, mouseY);
                return;
            }
        }
    }
}
