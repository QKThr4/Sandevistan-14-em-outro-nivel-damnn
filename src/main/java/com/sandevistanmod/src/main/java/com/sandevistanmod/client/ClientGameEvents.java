package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    /** Queda (Apogee): velocidade vertical nova = velocidade * FALL_MULT - FALL_ADD, limitada a FALL_CAP; os outros modelos escalam. */
    private static final double FALL_MULT = 2.4;
    private static final double FALL_ADD = 0.7;
    private static final double FALL_CAP = 16.0;

    private static boolean wasActive;
    /** Ticks extras por tick ao usar itens e nas recargas, pelo modelo ligado. */
    private static int extraTicks() {
        return ClientData.DATA.variant().extraTicks();
    }

    /** Soma na velocidade vertical por tick para cancelar a gravidade extra dentro da agua (gravidade extra / 16). */
    private static double waterGravityFix() {
        return ClientData.DATA.variant().gravityAdd() / 16.0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            wasActive = false;
            return;
        }
        softenHurtTilt(mc);
        PlayerGlitch.tickLocal();
        boolean on = ClientState.active(mc.player.getUUID());
        if (on) {
            // cair bem mais rapido (a gravidade do jogo ja esta no maximo, entao aceleramos a descida)
            Vec3 m = mc.player.getDeltaMovement();
            double sc = ClientData.DATA.variant().scale();
            if (!mc.player.onGround() && !mc.player.getAbilities().flying && !mc.player.isFallFlying()
                    && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.onClimbable() && m.y < 0) {
                mc.player.setDeltaMovement(m.x,
                        Math.max(-FALL_CAP, m.y * (1.0 + (FALL_MULT - 1.0) * sc) - FALL_ADD * sc), m.z);
            }
            // escadas e trepadeiras, teia, arbusto de frutas e neve em po: o movimento do tick e multiplicado
            // (o jogo e lento, entao sem isso esses lugares seriam bem mais lentos que o resto)
            boolean stuck = inStuckBlock(mc);
            boolean climb = mc.player.onClimbable();
            if (!mc.isPaused() && (stuck || climb)) {
                Vec3 d = new Vec3(mc.player.getX() - mc.player.xo, mc.player.getY() - mc.player.yo,
                        mc.player.getZ() - mc.player.zo);
                if (d.lengthSqr() > 1.0E-8) {
                    double mh = stuck ? 3.0 * sc : 0.0;
                    double mv = (stuck ? 9.0 : 5.0) * sc;
                    mc.player.move(MoverType.SELF, new Vec3(d.x * mh, d.y * mv, d.z * mh));
                }
            }
        } else if (wasActive) {
            // desligou: para no lugar
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        wasActive = on;
        while (ModKeys.SCREEN.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new SandevistanScreen());
        }
        while (ModKeys.TOGGLE.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(0));
        }
    }

    /** Dentro da agua (inclusive cachoeira) da para subir segurando pular e descer agachado, rapido. */
    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e.getEntity() != mc.player || !ClientState.active(mc.player.getUUID())) return;
        for (int i = 0; i < extraTicks(); i++) mc.player.getCooldowns().tick();
        if (mc.player.isInWater() && !mc.player.getAbilities().flying) {
            Vec3 m = mc.player.getDeltaMovement();
            double y = m.y + waterGravityFix();
            if (mc.options.keyJump.isDown()) y = Math.min(0.6, Math.max(y, 0.0) + 0.3);
            else if (mc.options.keyShift.isDown()) y = Math.max(-0.6, y - 0.2);
            mc.player.setDeltaMovement(m.x, y, m.z);
        }
    }

    /** Comer, beber, arcos, bestas e tridente 85% mais rapidos (o jogo roda a 3 ticks). */
    @SubscribeEvent
    public static void useTick(LivingEntityUseItemEvent.Tick e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && e.getEntity() == mc.player && ClientState.active(mc.player.getUUID())) {
            e.setDuration(Math.max(1, e.getDuration() - extraTicks()));
        }
    }

    // ---- acima de 100% de ciberpsicose: sem usar itens nem abrir inventarios (so o menu) ----
    private static boolean locked() {
        var d = ClientData.DATA;
        return d.implanted && d.psychotic && d.psy >= 100f;
    }

    @SubscribeEvent
    public static void lockScreen(ScreenEvent.Opening e) {
        if (locked() && e.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void lockUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() == Minecraft.getInstance().player && locked()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void lockRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() == Minecraft.getInstance().player && locked()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void lockRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity() == Minecraft.getInstance().player && locked()
                && !ClientPath.isHandDoor(e.getLevel().getBlockState(e.getPos()))) {
            e.setCanceled(true);
        }
    }

    /** Com ciberpsicose (30% a 100%) a tela nao balanca ao tomar dano: some aos poucos ate nao balancar nada. */
    private static void softenHurtTilt(Minecraft mc) {
        var d = ClientData.DATA;
        if (!d.implanted || !d.psychotic || d.psy < 30f) return;
        float f = Math.min(1f, (d.psy - 30f) / 70f);
        int cap = Math.round(mc.player.hurtDuration * (1f - f));
        if (mc.player.hurtTime > cap) mc.player.hurtTime = cap;
    }

    private static boolean inStuckBlock(Minecraft mc) {
        AABB box = mc.player.getBoundingBox().deflate(0.001);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var s = mc.level.getBlockState(pos);
            if (s.is(Blocks.COBWEB) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW)) return true;
        }
        return false;
    }

    /**
     * Com a velocidade absurda o campo de visao abriria ao maximo o tempo todo: com o Sandevistan ligado
     * ele fica num valor fixo e suave.
     */
    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent e) {
        if (ClientState.active(e.getPlayer().getUUID())) {
            e.setNewFovModifier(1.12f);
        }
    }

    /** Com a ciberpsicose a camera treme. */
    @SubscribeEvent
    public static void cameraShake(ViewportEvent.ComputeCameraAngles e) {
        if (!PsychosisFx.shaking()) return;
        float psy = PsychosisFx.psy();
        e.setRoll(e.getRoll() + PsychosisFx.shake(psy, 90.0) * 0.8f);
        e.setPitch(e.getPitch() + PsychosisFx.shake(psy, 57.0) * 0.4f);
        e.setYaw(e.getYaw() + PsychosisFx.shake(psy, 73.0) * 0.4f);
    }

    /** Ao sair do mundo, limpa o estado do cliente para nao vazar para o proximo mundo. */
    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientState.clear();
        Afterimages.clear();
        ClientSounds.clear();
        IntroVideo.stop();
        PlayerGlitch.clear();
        ClientData.DATA.deserializeNBT(RegistryAccess.EMPTY, new CompoundTag());
    }
}
