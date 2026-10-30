package com.sandevistanmod.logic;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.net.Net;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SandevistanMod.MODID)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            SandevistanLogic.tick(sp);
            // recargas de itens (perola, escudo...) tambem 85% mais rapidas
            if (SandevistanData.get(sp).active) {
                for (int i = 0; i < SandevistanLogic.extraTicks(sp); i++) sp.getCooldowns().tick();
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post e) {
        SandevistanLogic.updateTickRate(e.getServer());
    }

    /**
     * Com o Sandevistan o jogo roda a 2 ticks e o "tempo de invulnerabilidade" do alvo duraria varios segundos:
     * zera, para bater rapido continuar funcionando como no tempo normal.
     */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer sp
                && e.getEntity() != sp
                && SandevistanData.get(sp).active) {
            e.getEntity().invulnerableTime = 0;
        }
    }

    /** Durabilidade dos chips: Artes Marciais perde 1 por golpe, Supressao de Dor 1 por dano recebido. */
    @SubscribeEvent
    public static void chipDamage(LivingIncomingDamageEvent e) {
        if (e.getAmount() <= 0f) return;
        if (e.getSource().getEntity() instanceof ServerPlayer atk && e.getEntity() != atk
                && e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && SandevistanData.get(atk).chipActive(SandevistanData.CHIP_MARTIAL)) {
            SandevistanLogic.useChip(atk, SandevistanData.CHIP_MARTIAL);
        }
        if (e.getEntity() instanceof ServerPlayer sp && SandevistanData.get(sp).chipActive(SandevistanData.CHIP_PAIN)) {
            SandevistanLogic.useChip(sp, SandevistanData.CHIP_PAIN);
        }
    }

    /** Chip de Mineracao: cada bloco quebrado gasta 1 de durabilidade. */
    @SubscribeEvent
    public static void chipMining(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer sp && SandevistanData.get(sp).chipActive(SandevistanData.CHIP_MINING)) {
            SandevistanLogic.useChip(sp, SandevistanData.CHIP_MINING);
        }
    }

    /** Chip de Andarilho: sem dano de queda; cada queda evitada gasta 1 de durabilidade. */
    @SubscribeEvent
    public static void chipFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !SandevistanData.get(sp).chipActive(SandevistanData.CHIP_WALKER)) {
            return;
        }
        if (e.getDistance() > 3f) SandevistanLogic.useChip(sp, SandevistanData.CHIP_WALKER);
        e.setDamageMultiplier(0f);
    }

    /** Zetatech dobra (e no ar aumenta) o dano causado; QianT reduz o dano recebido pela metade alem da Resistencia. */
    @SubscribeEvent
    public static void variantDamage(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer atk && e.getEntity() != atk && SandevistanData.get(atk).active) {
            com.sandevistanmod.data.SandevistanVariant v = SandevistanData.get(atk).variant();
            float mult = v.damage;
            if (v.airDamage > 1f && !atk.onGround() && !atk.isInWater() && !atk.onClimbable()) mult = v.damage * v.airDamage;
            if (mult != 1f) e.setAmount(e.getAmount() * mult);
        }
        if (e.getEntity() instanceof ServerPlayer sp && SandevistanData.get(sp).active
                && SandevistanData.get(sp).variant().taken < 1f
                && !e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setAmount(e.getAmount() * SandevistanData.get(sp).variant().taken);
        }
    }

    /** Desvio: com o Sandevistan ligado, 50% dos ataques (de mao ou projeteis) nao causam dano. */
    @SubscribeEvent
    public static void dodge(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !SandevistanData.get(sp).active) return;
        net.minecraft.world.damagesource.DamageSource src = e.getSource();
        if (src.getEntity() == null && src.getDirectEntity() == null) return;
        boolean projectile = src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
        if (!projectile) {
            if (src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                    || src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                    || src.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                    || src.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                    || src.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)
                    || src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                return;
            }
        }
        if (SandevistanData.get(sp).variant().dodge > 0f && sp.getRandom().nextFloat() < SandevistanData.get(sp).variant().dodge) {
            e.setCanceled(true);
            SandevistanLogic.dodged(sp);
        }
    }

    /** Knockback maior para quem bate com o Sandevistan ligado (sem exagero). */
    @SubscribeEvent
    public static void extraKnockback(LivingDamageEvent.Post e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer sp) || e.getEntity() == sp
                || !SandevistanData.get(sp).active || e.getNewDamage() <= 0f) {
            return;
        }
        net.minecraft.world.entity.LivingEntity v = e.getEntity();
        double dx = v.getX() - sp.getX();
        double dz = v.getZ() - sp.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) return;
        double k = SandevistanData.get(sp).variant().knockback / (1.0 + v.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 4.0);
        v.setDeltaMovement(v.getDeltaMovement().add(dx / len * k, 0.1, dz / len * k));
        v.hurtMarked = true;
    }

    /** Com ciberpsicose (de 30% a 100%) o jogador perde o knockback que recebe, ate nao receber nenhum. */
    @SubscribeEvent
    public static void knockbackTaken(LivingKnockBackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        SandevistanData d = SandevistanData.get(sp);
        float f = SandevistanLogic.psyResist(d);
        if (f <= 0f) return;
        if (f >= 1f) e.setCanceled(true);
        else e.setStrength(e.getStrength() * (1f - f));
    }

    /** Comer, beber, arcos, bestas, tridente: 85% mais rapidos junto com o jogador (o jogo roda a 3 ticks). */
    @SubscribeEvent
    public static void useTick(LivingEntityUseItemEvent.Tick e) {
        if (e.getEntity() instanceof ServerPlayer sp && SandevistanData.get(sp).active) {
            e.setDuration(Math.max(1, e.getDuration() - SandevistanLogic.extraTicks(sp)));
        }
    }

    /** Segurando o Imunossupressor e agachado, bater em outro jogador injeta o remedio nele. */
    @SubscribeEvent
    public static void inject(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !(e.getTarget() instanceof ServerPlayer target)) return;
        net.minecraft.world.item.ItemStack held = sp.getMainHandItem();
        if (!held.is(com.sandevistanmod.registry.ModRegistries.IMUNOSSUPRESSOR.get()) || !sp.isShiftKeyDown()) return;
        e.setCanceled(true);
        if (SandevistanLogic.locked(SandevistanData.get(sp))) return;
        if (!SandevistanLogic.applyImmuno(target, sp)) return;
        sp.level().playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
        sp.displayClientMessage(com.sandevistanmod.data.Tx.t(net.minecraft.ChatFormatting.AQUA, "immuno.injected",
                target.getDisplayName()), true);
        if (!sp.getAbilities().instabuild) {
            held.shrink(1);
            net.minecraft.world.item.ItemStack bottle = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
            if (!sp.getInventory().add(bottle)) sp.drop(bottle, false);
        }
    }

    // ---- acima de 100% de ciberpsicose: nao usa itens, nao dropa, nao abre inventario ----
    private static boolean locked(net.minecraft.world.entity.player.Player p) {
        return p instanceof ServerPlayer sp && SandevistanLogic.locked(SandevistanData.get(sp));
    }

    @SubscribeEvent
    public static void lockUse(LivingEntityUseItemEvent.Start e) {
        if (locked(e.getEntity() instanceof net.minecraft.world.entity.player.Player pl ? pl : null)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void lockRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (locked(e.getEntity())) e.setCanceled(true);
    }

    /** Portas e portoes abertos a mao continuam valendo sem controle (o jogador sai andando sozinho). */
    public static boolean isHandDoor(net.minecraft.world.level.block.state.BlockState s) {
        return (s.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                || s.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
                && !s.is(net.minecraft.world.level.block.Blocks.IRON_DOOR);
    }

    @SubscribeEvent
    public static void lockRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (locked(e.getEntity()) && !isHandDoor(e.getLevel().getBlockState(e.getPos()))) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void lockEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (locked(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void lockToss(ItemTossEvent e) {
        if (locked(e.getPlayer())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.resetOnDeath(sp);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.onLogin(sp);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.onLogout(sp);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.onRespawn(sp);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Net.sync(sp);
    }

    /** Comando de teste (OP): /sandevistan psicose <0-300> define a ciberpsicose. */
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("sandevistan")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("mente")
                        .then(mindMode("add", 0))
                        .then(mindMode("remove", 1))
                        .then(mindMode("set", 2))
                        .then(Commands.literal("atual")
                                .then(mindMode("add", 0, true))
                                .then(mindMode("remove", 1, true))
                                .then(mindMode("set", 2, true))))
                .then(Commands.literal("psicose")
                        .then(Commands.argument("valor", FloatArgumentType.floatArg(0f, 300f))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    SandevistanLogic.setPsy(p, FloatArgumentType.getFloat(c, "valor"));
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }

    /** /sandevistan mente <add|remove|set> <0-100> [jogadores]: muda a Mente permanente. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mindMode(String name, int mode) {
        return mindMode(name, mode, false);
    }

    /** current = true muda a Mente ATUAL (a que regenera) em vez da permanente. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mindMode(String name, int mode,
                                                                                                    boolean current) {
        return Commands.literal(name).then(Commands.argument("valor", IntegerArgumentType.integer(0, 100))
                .executes(c -> {
                    adjust(c.getSource().getPlayerOrException(), mode, IntegerArgumentType.getInteger(c, "valor"), current);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("alvos", EntityArgument.players()).executes(c -> {
                    int v = IntegerArgumentType.getInteger(c, "valor");
                    int n = 0;
                    for (ServerPlayer sp : EntityArgument.getPlayers(c, "alvos")) {
                        adjust(sp, mode, v, current);
                        n++;
                    }
                    return n;
                })));
    }

    private static void adjust(ServerPlayer p, int mode, int value, boolean current) {
        if (current) SandevistanLogic.adjustCurrentMind(p, mode, value);
        else SandevistanLogic.adjustMind(p, mode, value);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent e) {
        SandevistanLogic.clearAll();
    }
}
