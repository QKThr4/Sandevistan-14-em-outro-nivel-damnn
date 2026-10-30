package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.SandevistanVariant;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
import com.sandevistanmod.net.DoctorActionPayload;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela do Ripperdoc: menu (Loja / Implantes), encaixe do Sandevistan na silhueta e loja de remedios. */
public class DoctorScreen extends Screen {
    private static final int MENU = 0;
    private static final int IMPLANTS = 1;
    private static final int SHOP = 2;

    private int mode = MENU;
    /** Implante encaixado no slot (ainda nao implantado) e encaixe selecionado (-1 nenhum). */
    private final boolean[] placed = new boolean[SandevistanData.SLOT_COUNT];
    private int sel = -1;
    /** Modelo do Sandevistan colocado no encaixe das costas (0 a 4). */
    private int placedVar;
    private ItemStack carried = ItemStack.EMPTY;
    private Button implantBtn;
    private Button removeBtn;
    private Button buy1;
    private Button buy2;

    public DoctorScreen() {
        super(Tx.t("doc.name"));
    }

    private static int count(net.minecraft.world.item.Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int n = 0;
        for (ItemStack s : mc.player.getInventory().items) if (s.is(item)) n += s.getCount();
        for (ItemStack s : mc.player.getInventory().offhand) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static boolean creative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getAbilities().instabuild;
    }

    private boolean hasItemFor(int slot) {
        return creative() || count(SandevistanLogic.implantItem(slot, slot == SandevistanData.SLOT_BACK ? sandVar() : 0)) > 0;
    }

    private int sandVar() {
        return placedVar;
    }

    private boolean fits(int slot, ItemStack st) {
        if (st.isEmpty()) return false;
        if (slot == SandevistanData.SLOT_BACK) return SandevistanLogic.variantOf(st) >= 0;
        return st.is(SandevistanLogic.implantItem(slot));
    }

    private boolean freeSlot(int slot) {
        return !ClientData.DATA.has(slot) && !placed[slot];
    }

    private boolean fitsAnyFree(ItemStack st) {
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) if (fits(s, st) && freeSlot(s)) return true;
        return false;
    }

    private int px() {
        return this.width / 2 - 135;
    }

    private int panelH() {
        return mode == IMPLANTS ? 270 : 200;
    }

    private int py() {
        return Math.max(2, (this.height - panelH()) / 2);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = py();
        implantBtn = null;
        removeBtn = null;
        buy1 = null;
        buy2 = null;
        if (mode == MENU) {
            addRenderableWidget(Button.builder(Tx.t("doc.menu.shop"), b -> setMode(SHOP))
                    .bounds(cx - 80, top + 70, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("doc.menu.implants"), b -> setMode(IMPLANTS))
                    .bounds(cx - 80, top + 98, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("btn.close"), b -> onClose())
                    .bounds(cx - 80, top + 140, 160, 20).build());
        } else if (mode == IMPLANTS) {
            implantBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.implant"), b -> {
                        if (sel == SandevistanData.SLOT_BACK) PacketDistributor.sendToServer(new DoctorActionPayload(30 + placedVar));
                        else if (sel >= 0) PacketDistributor.sendToServer(new DoctorActionPayload(10 + sel));
                    })
                    .bounds(cx - 37, top + 156, 74, 20).build());
            removeBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.remove"), b -> {
                        if (sel >= 0) PacketDistributor.sendToServer(new DoctorActionPayload(20 + sel));
                    })
                    .bounds(cx - 50, top + 156, 100, 20).build());
            addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> setMode(MENU))
                    .bounds(px() + 6, top + 6, 50, 16).build());
        } else {
            buy1 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(3)))
                    .bounds(cx + 50, top + 62, 60, 20).build());
            buy2 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(4)))
                    .bounds(cx + 50, top + 98, 60, 20).build());
            addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> setMode(MENU))
                    .bounds(px() + 6, top + 6, 50, 16).build());
        }
        refresh();
    }

    private void setMode(int m) {
        mode = m;
        carrying = false;
        carried = ItemStack.EMPTY;
        rebuildWidgets();
    }

    private void refresh() {
        SandevistanData d = ClientData.DATA;
        if (implantBtn != null) {
            implantBtn.visible = sel >= 0 && placed[sel] && !d.has(sel);
            implantBtn.active = creative() || count(Items.EMERALD) >= SandevistanLogic.CLINIC_EMERALDS;
        }
        if (removeBtn != null) {
            removeBtn.visible = sel >= 0 && d.has(sel);
            removeBtn.active = sel < 0 || (sel == SandevistanData.SLOT_BACK ? !d.active
                    : sel != SandevistanData.SLOT_NEURAL || d.chips() == 0);
        }
        if (buy1 != null) buy1.active = creative() || count(Items.EMERALD) >= SandevistanLogic.STABILIZER_PRICE;
        if (buy2 != null) buy2.active = creative() || count(Items.EMERALD) >= SandevistanLogic.IMMUNO_PRICE;
    }

    @Override
    public void tick() {
        SandevistanData d = ClientData.DATA;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if (placed[s] && (d.has(s) || !hasItemFor(s))) placed[s] = false;
        }
        // dois bracos encaixados precisam de dois itens
        if (placed[SandevistanData.SLOT_ARM_R] && placed[SandevistanData.SLOT_ARM_L] && !creative()
                && count(SandevistanLogic.implantItem(SandevistanData.SLOT_ARM_R)) < 2) {
            placed[SandevistanData.SLOT_ARM_L] = false;
        }
        if (sel >= 0 && !placed[sel] && !d.has(sel)) sel = -1;
        if (!carried.isEmpty() && !fitsAnyFree(carried)) {
            carrying = false;
            carried = ItemStack.EMPTY;
        }
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- encaixes e inventario do jogador (arrastar o implante ate o encaixe) ----
    private int slotX(int slot) {
        int cx = this.width / 2;
        return slot == SandevistanData.SLOT_BACK || slot == SandevistanData.SLOT_ARM_R || slot == SandevistanData.SLOT_BONE
                ? cx - 112 : cx + 94;
    }

    private int slotY(int slot) {
        return py() + switch (slot) {
            case SandevistanData.SLOT_BACK, SandevistanData.SLOT_NEURAL -> 28;
            case SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_ARM_L -> 60;
            default -> 92;
        };
    }

    private int slotAt(double mx, double my) {
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if (mx >= slotX(s) && mx < slotX(s) + 18 && my >= slotY(s) && my < slotY(s) + 18) return s;
        }
        return -1;
    }

    private int invX() {
        return this.width / 2 - 81;
    }

    private int invY() {
        return py() + 188;
    }

    /** Indice do inventario (0 a 8 hotbar, 9 a 35 principal) sob o mouse, ou -1. */
    private int invIndexAt(double mx, double my) {
        int ix = (int) Math.floor((mx - invX()) / 18.0);
        if (ix < 0 || ix > 8 || mx < invX()) return -1;
        double ry = my - invY();
        if (ry >= 0 && ry < 54) return 9 + (int) (ry / 18) * 9 + ix;
        if (ry >= 58 && ry < 76) return ix;
        return -1;
    }

    private ItemStack invStack(int idx) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null || idx < 0 ? ItemStack.EMPTY : mc.player.getInventory().getItem(idx);
    }

    private boolean carrying;
    private double pressX;
    private double pressY;
    private boolean dragged;

    /** Solta o item carregado no encaixe sob o mouse, se ele couber e estiver livre. */
    private void dropOn(double mx, double my) {
        int s = slotAt(mx, my);
        if (s >= 0 && fits(s, carried) && freeSlot(s)) {
            if (s == SandevistanData.SLOT_BACK) placedVar = SandevistanLogic.variantOf(carried);
            placed[s] = true;
            sel = s;
        }
        carrying = false;
        carried = ItemStack.EMPTY;
        refresh();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mode == IMPLANTS && button == 0) {
            if (carrying) {
                dropOn(mx, my);
                return true;
            }
            int s = slotAt(mx, my);
            if (s >= 0) {
                if (placed[s]) {
                    placed[s] = false;
                    if (sel == s) sel = -1;
                } else if (ClientData.DATA.has(s)) {
                    sel = s;
                }
                refresh();
                return true;
            }
            int idx = invIndexAt(mx, my);
            if (idx >= 0 && fitsAnyFree(invStack(idx))) {
                carrying = true;
                carried = invStack(idx).copy();
                pressX = mx;
                pressY = my;
                dragged = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (carrying && Math.abs(mx - pressX) + Math.abs(my - pressY) > 4) dragged = true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (mode == IMPLANTS && carrying && button == 0 && dragged) {
            dropOn(mx, my);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int x0 = px();
        int top = py();
        int h = panelH();
        gg.fill(x0, top, x0 + 270, top + h, 0xD0101214);
        gg.fill(x0, top, x0 + 270, top + 2, 0xFFFF2040);
        gg.drawCenteredString(this.font, Tx.t("doc.name"), cx, top + 8, 0xFF5566);

        if (mode == MENU) {
            gg.drawCenteredString(this.font, Tx.t("doc.menu.title"), cx, top + 40, 0xDDDDDD);
        } else if (mode == IMPLANTS) {
            drawImplants(gg, cx, top, mouseX, mouseY);
        } else {
            drawShop(gg, cx, top);
        }
    }

    private static final String[] SLOT_KEYS = {"slot.back", "slot.armr", "slot.arml", "slot.neural", "slot.bone", "slot.nerve"};

    private int shownVar(int slot) {
        if (slot != SandevistanData.SLOT_BACK) return 0;
        return ClientData.DATA.has(slot) ? ClientData.DATA.variant : placedVar;
    }

    private void drawImplants(GuiGraphics gg, int cx, int top, int mouseX, int mouseY) {
        SandevistanData d = ClientData.DATA;
        Minecraft mc = Minecraft.getInstance();
        // o proprio jogador (o mesmo modelo do inventario)
        if (mc.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gg, cx - 40, top + 24, cx + 40, top + 104, 34, 0.0625f,
                    mouseX, mouseY, mc.player);
        }
        int hover = carrying ? slotAt(mouseX, mouseY) : -1;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            int x = slotX(s);
            int y = slotY(s);
            boolean on = d.has(s) || placed[s];
            boolean hot = hover == s && fits(s, carried) && freeSlot(s);
            int border = s == sel ? 0xFFFFFFFF : on || hot ? 0xFF39FF14 : 0xFF777777;
            gg.fill(x - 1, y - 1, x + 19, y + 19, border);
            gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
            if (on) gg.renderItem(new ItemStack(SandevistanLogic.implantItem(s, shownVar(s))), x + 1, y + 1);
            gg.drawCenteredString(this.font, Tx.t(SLOT_KEYS[s]), x + 9, y + 22, 0xAAAAAA);
        }
        boolean preview = sel >= 0 && placed[sel] && !d.has(sel);
        if (sel >= 0 && d.has(sel)) {
            gg.drawCenteredString(this.font, Tx.t("screen.implanted"), cx, top + 120, 0x55FF55);
        } else if (!preview) {
            gg.drawCenteredString(this.font, Tx.t("doc.place"), cx, top + 120, 0xDDDDDD);
        } else {
            gg.drawCenteredString(this.font, Tx.t("doc.hurt", SandevistanLogic.IMPLANT_HEARTS), cx, top + 120, 0xFF8888);
        }
        // Carga Cibernetica, com quanto vai subir antes de implantar
        float cap = d.capacity();
        float add = !preview ? 0f : sel == SandevistanData.SLOT_BACK ? SandevistanVariant.of(placedVar).load : SandevistanData.loadOf(sel);
        gg.drawCenteredString(this.font, add > 0f ? Tx.t("load.preview", Math.round(cap), Math.round(cap + add))
                : Tx.t("load.title", Math.round(cap)), cx, top + 132, cap + add >= 50f ? 0xFFAA33 : 0x88FFAA);
        ClientOverlay.bar(gg, cx - 100, top + 143, 200, 9, cap / 100f, ClientOverlay.loadColor(cap));
        if (add > 0f) {
            int x1 = cx - 100 + (int) (200 * Math.min(1f, cap / 100f));
            int x2 = cx - 100 + (int) (200 * Math.min(1f, (cap + add) / 100f));
            gg.fill(x1, top + 143, x2, top + 152, 0xCCFFFFFF);
        }
        gg.fill(cx, top + 141, cx + 1, top + 154, 0xFFFFFFFF);
        if (preview) {
            gg.renderItem(new ItemStack(Items.EMERALD), cx + 42, top + 158);
            boolean ok = creative() || count(Items.EMERALD) >= SandevistanLogic.CLINIC_EMERALDS;
            gg.drawString(this.font, "x" + SandevistanLogic.CLINIC_EMERALDS, cx + 62, top + 163, ok ? 0x55FF55 : 0xFF5555);
        }
        // inventario do jogador: 3 linhas + hotbar
        int ix = invX();
        int iy = invY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) drawInvSlot(gg, ix + c * 18, iy + r * 18, 9 + r * 9 + c);
        }
        for (int c = 0; c < 9; c++) drawInvSlot(gg, ix + c * 18, iy + 58, c);
    }

    private void drawInvSlot(GuiGraphics gg, int x, int y, int idx) {
        ItemStack st = invStack(idx);
        boolean glow = fitsAnyFree(st);
        gg.fill(x, y, x + 18, y + 18, glow ? 0xFF39FF14 : 0xFF3A3F44);
        gg.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1A1E22);
        if (!st.isEmpty()) {
            gg.renderItem(st, x + 1, y + 1);
            gg.renderItemDecorations(this.font, st, x + 1, y + 1);
        }
    }

    private void drawShop(GuiGraphics gg, int cx, int top) {
        int have = count(Items.EMERALD);
        gg.renderItem(new ItemStack(Items.EMERALD), cx - 120, top + 28);
        gg.drawString(this.font, Tx.t("doc.shop.have", have), cx - 98, top + 33, 0x55FF55);
        shopRow(gg, cx, top + 60, new ItemStack(ModRegistries.ESTABILIZADOR.get()), SandevistanLogic.STABILIZER_PRICE);
        shopRow(gg, cx, top + 96, new ItemStack(ModRegistries.IMUNOSSUPRESSOR.get()), SandevistanLogic.IMMUNO_PRICE);
    }

    private void shopRow(GuiGraphics gg, int cx, int y, ItemStack stack, int price) {
        gg.fill(cx - 124, y - 4, cx + 124, y + 28, 0x60000000);
        gg.renderItem(stack, cx - 118, y + 2);
        gg.drawString(this.font, stack.getHoverName(), cx - 96, y + 2, 0xFFFFFF);
        gg.renderItem(new ItemStack(Items.EMERALD), cx - 96, y + 12);
        gg.drawString(this.font, "x" + price, cx - 77, y + 17, count(Items.EMERALD) >= price || creative() ? 0x55FF55 : 0xFF5555);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        if (mode != IMPLANTS) return;
        if (carrying) {
            gg.renderItem(carried, mouseX - 8, mouseY - 8);
            return;
        }
        // informacoes do item: so aparecem com o implante no encaixe (ou ao passar o mouse num item do inventario)
        int s = slotAt(mouseX, mouseY);
        if (s >= 0 && (placed[s] || ClientData.DATA.has(s))) {
            gg.renderTooltip(this.font, new ItemStack(SandevistanLogic.implantItem(s, shownVar(s))), mouseX, mouseY);
            return;
        }
        int idx = invIndexAt(mouseX, mouseY);
        ItemStack st = invStack(idx);
        if (!st.isEmpty()) gg.renderTooltip(this.font, st, mouseX, mouseY);
    }
}
