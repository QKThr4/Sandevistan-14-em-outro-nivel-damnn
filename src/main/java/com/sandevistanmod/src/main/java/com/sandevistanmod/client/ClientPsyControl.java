package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * Ciberpsicose acima de 50%: o corpo age sozinho. A visao vira para o lado, o jogador anda ou pula para direcoes
 * aleatorias, de vez em quando olha a entidade mais proxima por 3 s e, no apagao (tela preta, a partir de 70%),
 * caminha ate a entidade mais proxima (jogador ou mob) contornando paredes, abrindo portas e subindo escadas e agua, e bate nela.
 */
@EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public final class ClientPsyControl {
    private ClientPsyControl() {}

    private static final Random R = new Random();
    private static int walkT;
    private static float walkF;
    private static float walkL;
    private static boolean walkJump;
    private static int driftT;
    private static float driftV;
    private static int hitCd;
    private static int retarget;
    private static LivingEntity target;
    // navegacao do apagao
    private static java.util.List<BlockPos> path = java.util.List.of();
    private static int pathIdx;
    private static int pathAge;
    private static int doorCd;
    private static int stuckT;
    private static int jumpT;
    private static double lastX;
    private static double lastZ;
    private static float navForward;
    private static boolean navJump;
    // olhar para a entidade mais proxima (3 s, de vez em quando)
    private static int lookT;
    private static int lookCd;

    private static boolean sick(SandevistanData d) {
        return d.implanted && d.psychotic && d.psy > 50f;
    }

    private static void reset() {
        walkT = 0;
        driftT = 0;
        hitCd = 0;
        retarget = 0;
        target = null;
        path = java.util.List.of();
        pathIdx = 0;
        pathAge = 0;
        stuckT = 0;
        jumpT = 0;
        navForward = 0f;
        navJump = false;
        lookT = 0;
        lookCd = 0;
    }

    private static LivingEntity nearest(Minecraft mc, LocalPlayer p, double range) {
        LivingEntity best = null;
        double bd = range * range;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == p || !(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator()) continue;
            double dd = le.distanceToSqr(p);
            if (dd < bd) {
                bd = dd;
                best = le;
            }
        }
        return best;
    }

    /** Vira a cabeca aos poucos para um ponto. */
    private static void aim(LocalPlayer p, double tx, double ty, double tz, float maxYaw, float maxPitch) {
        double dx = tx - p.getX();
        double dz = tz - p.getZ();
        double dy = ty - p.getEyeY();
        float wantYaw = (float) (Mth.atan2(dz, dx) * 57.2957795 - 90.0);
        float wantPitch = (float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.2957795));
        p.setYRot(p.getYRot() + Mth.clamp(Mth.wrapDegrees(wantYaw - p.getYRot()), -maxYaw, maxYaw));
        p.setXRot(p.getXRot() + Mth.clamp(wantPitch - p.getXRot(), -maxPitch, maxPitch));
        p.setYHeadRot(p.getYRot());
    }

    private static boolean nodeReached(LocalPlayer p, BlockPos n) {
        double dx = n.getX() + 0.5 - p.getX();
        double dz = n.getZ() + 0.5 - p.getZ();
        return dx * dx + dz * dz < 0.25 && Math.abs(p.getY() - n.getY()) < 1.0;
    }

    private static boolean closedDoor(Minecraft mc, BlockPos pos) {
        var st = mc.level.getBlockState(pos);
        return ClientPath.isHandDoor(st) && st.hasProperty(BlockStateProperties.OPEN) && !st.getValue(BlockStateProperties.OPEN);
    }

    private static void openDoor(Minecraft mc, LocalPlayer p, BlockPos pos) {
        if (mc.gameMode == null) return;
        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        p.swing(InteractionHand.MAIN_HAND);
        doorCd = 8;
    }

    /** Apagao: segue o caminho ate a entidade mais proxima, abre portas, sobe escadas e agua, e bate nela de perto. */
    private static void blindTick(Minecraft mc, LocalPlayer p) {
        if (target == null || !target.isAlive() || retarget-- <= 0) {
            target = nearest(mc, p, 24.0);
            retarget = 10;
        }
        navForward = 0f;
        navJump = false;
        if (doorCd > 0) doorCd--;
        if (jumpT > 0) jumpT--;
        if (target == null) {
            navForward = 0.6f;
            if (R.nextFloat() < 0.1f) p.setYRot(p.getYRot() + (R.nextFloat() * 2f - 1f) * 40f);
            return;
        }
        double dist = p.distanceTo(target);
        double hx = p.getX() - lastX;
        double hz = p.getZ() - lastZ;
        lastX = p.getX();
        lastZ = p.getZ();
        BlockPos wp = null;
        if (dist > 2.5) {
            if (pathAge-- <= 0 || path.isEmpty() || pathIdx >= path.size()) {
                path = ClientPath.find(mc.level, p.blockPosition(), target.blockPosition(), 2500);
                pathIdx = 0;
                pathAge = 20;
            }
            while (pathIdx < path.size() && nodeReached(p, path.get(pathIdx))) pathIdx++;
            if (pathIdx < path.size()) wp = path.get(pathIdx);
            // preso (parede sem caminho): refaz o caminho e pula
            if (hx * hx + hz * hz < 0.0004) {
                if (++stuckT > 12) {
                    stuckT = 0;
                    pathAge = 0;
                    jumpT = 6;
                }
            } else {
                stuckT = 0;
            }
        }
        double tx = wp != null ? wp.getX() + 0.5 : target.getX();
        double tz = wp != null ? wp.getZ() + 0.5 : target.getZ();
        double hd = Math.sqrt((tx - p.getX()) * (tx - p.getX()) + (tz - p.getZ()) * (tz - p.getZ()));
        if (dist < 3.2 || wp == null) {
            aim(p, target.getX(), target.getEyeY(), target.getZ(), 20f, 12f);
        } else if (hd > 0.3) {
            aim(p, tx, p.getEyeY(), tz, 25f, 12f);
        }
        // portas e portoes fechados no caminho (a frente e no proximo ponto)
        if (doorCd <= 0 && dist > 1.5) {
            double len = Math.max(0.001, hd);
            double ux = (tx - p.getX()) / len;
            double uz = (tz - p.getZ()) / len;
            BlockPos feet = BlockPos.containing(p.getX() + ux * 0.9, p.getY(), p.getZ() + uz * 0.9);
            for (BlockPos c : new BlockPos[] {feet, feet.above(), wp != null ? wp : feet, wp != null ? wp.above() : feet}) {
                if (closedDoor(mc, c)) {
                    openDoor(mc, p, c);
                    break;
                }
            }
        }
        boolean up = wp != null && wp.getY() > p.blockPosition().getY();
        navJump = jumpT > 0
                || (p.onGround() && (up || p.horizontalCollision))
                || (p.isInWater() && (wp == null ? target.getY() > p.getY() - 0.5 : wp.getY() >= p.blockPosition().getY()))
                || (p.onClimbable() && up);
        navForward = dist > 2.0 && hd > 0.3 ? 1f : 0f;
        if (dist <= 2.0) navForward = 0f;
        if (dist < 3.2 && --hitCd <= 0 && mc.gameMode != null) {
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
            hitCd = 8 + R.nextInt(6);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        SandevistanData d = ClientData.DATA;
        if (!sick(d)) {
            reset();
            return;
        }
        float k = Mth.clamp((d.psy - 50f) / 100f, 0f, 2f);

        if (d.blind) {
            lookT = 0;
            blindTick(mc, p);
            return;
        }
        target = null;
        path = java.util.List.of();

        // de vez em quando vira a cabeca e olha a entidade mais proxima por 3 segundos, depois para
        if (lookT > 0) {
            lookT--;
            LivingEntity t = nearest(mc, p, 16.0);
            if (t != null) aim(p, t.getX(), t.getEyeY(), t.getZ(), 9f, 6f);
            if (lookT == 0) lookCd = 160 + R.nextInt(240);
        } else if (lookCd > 0) {
            lookCd--;
        } else if (R.nextFloat() < 0.0015f + 0.002f * k && nearest(mc, p, 16.0) != null) {
            lookT = 60;
        }

        // a visao vira para o lado aos poucos
        if (driftT > 0) {
            driftT--;
            p.setYRot(p.getYRot() + driftV);
        } else if (lookT == 0 && R.nextFloat() < 0.012f + 0.01f * k) {
            driftT = 8 + R.nextInt(14);
            driftV = (R.nextBoolean() ? 1f : -1f) * (0.6f + 1.2f * k + R.nextFloat());
        }
        // anda ou pula para uma direcao aleatoria por um instante
        if (walkT > 0) {
            walkT--;
        } else if (R.nextFloat() < 0.006f + 0.006f * k) {
            walkT = 10 + R.nextInt(25);
            walkF = R.nextInt(3) - 1;
            walkL = R.nextInt(3) - 1;
            if (walkF == 0f && walkL == 0f) walkF = 1f;
            walkJump = R.nextFloat() < 0.3f + 0.2f * k;
        }
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        SandevistanData d = ClientData.DATA;
        if (p == null || e.getEntity() != p || !sick(d)) return;
        if (d.blind) {
            e.getInput().forwardImpulse = navForward;
            e.getInput().leftImpulse = 0f;
            e.getInput().jumping = navJump;
        } else if (walkT > 0) {
            e.getInput().forwardImpulse = walkF;
            e.getInput().leftImpulse = walkL;
            if (walkJump && p.onGround()) e.getInput().jumping = true;
        }
    }
}
