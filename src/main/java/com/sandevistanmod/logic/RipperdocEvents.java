package com.sandevistanmod.logic;

import com.mojang.brigadier.Command;
import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.registry.ModRegistries;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Field;
import net.minecraft.ChatFormatting;
import com.sandevistanmod.net.OpenDoctorPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.pathfinder.Path;

/** O Ripperdoc: aldeao doutor da clinica da vila. Implanta e remove o Sandevistan. */
@EventBusSubscriber(modid = SandevistanMod.MODID)
public final class RipperdocEvents {
    private RipperdocEvents() {}

    public static final ResourceLocation CLINIC_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "village/ripperdoc_clinic");
    /** Posicao (x, y, z) do bloco jigsaw dentro da estrutura (entrada, virada para o norte). */
    private static final int ENTRANCE_X = 5;
    private static final int ENTRANCE_Y = 1;

    private static final String[] VILLAGE_TYPES = {"plains", "desert", "savanna", "snowy", "taiga"};

    // ---------- conversa com o doutor ----------
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof Villager v)) return;
        if (v.getVillagerData().getProfession() != ModRegistries.RIPPERDOC.get()) return;
        // o doutor nao faz troca normal: so conversa
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(e.getLevel().isClientSide()));
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (e.getEntity() instanceof ServerPlayer sp) talk(sp, v);
    }

    /** De tempo em tempo o doutor anda de volta para o bloco da clinica (o posto de trabalho dele). */
    @SubscribeEvent
    public static void walkBack(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Villager v) || v.level().isClientSide || v.tickCount % 400 != 0) return;
        if (v.getVillagerData().getProfession() != ModRegistries.RIPPERDOC.get()) return;
        BlockPos target = v.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(GlobalPos::pos).orElse(null);
        if (target == null) target = findClinic(v);
        if (target == null || v.blockPosition().distSqr(target) <= 9.0) return;
        Path path = v.getNavigation().createPath(target, 1);
        if (path != null) v.getNavigation().moveTo(path, 0.5);
    }

    private static BlockPos findClinic(Villager v) {
        BlockPos c = v.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 4, 12))) {
            if (v.level().getBlockState(pos).is(ModRegistries.CLINICA.get())) return pos.immutable();
        }
        return null;
    }

    /** Abre a tela do Ripperdoc (Loja / Implantes) no cliente do jogador. Nada e dito no chat. */
    public static void talk(ServerPlayer p, Villager v) {
        if (v != null) {
            v.getNavigation().stop();
            v.getLookControl().setLookAt(p, 30f, 30f);
        }
        PacketDistributor.sendToPlayer(p, new OpenDoctorPayload());
    }

    // ---------- comandos ----------
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        // OP: constroi a clinica na frente do jogador (para testar sem procurar vila)
        e.getDispatcher().register(Commands.literal("sandevistan")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("clinica").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean ok = placeClinic(p);
                    p.sendSystemMessage(Tx.t(ok ? ChatFormatting.GREEN : ChatFormatting.RED, ok ? "clinic.built" : "clinic.fail"));
                    return ok ? Command.SINGLE_SUCCESS : 0;
                })));
    }

    /** Coloca a clinica ao sul do jogador, com a entrada virada para o norte (para o jogador). */
    private static boolean placeClinic(ServerPlayer p) {
        try {
            ServerLevel level = p.serverLevel();
            StructureTemplate t = level.getStructureManager().get(CLINIC_STRUCTURE).orElse(null);
            if (t == null) return false;
            BlockPos origin = new BlockPos(p.getBlockX() - ENTRANCE_X, p.getBlockY() - ENTRANCE_Y, p.getBlockZ() + 2);
            StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
            boolean ok = t.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
            if (ok) {
                // o jigsaw so serve para a vila: troca por um bloco de calcada
                BlockPos jig = origin.offset(ENTRANCE_X, ENTRANCE_Y, 0);
                if (level.getBlockState(jig).is(Blocks.JIGSAW)) level.setBlock(jig, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
            }
            return ok;
        } catch (Exception ex) {
            return false;
        }
    }

    // ---------- a clinica nas vilas ----------
    /** Coloca a clinica na lista de casas das vilas (as 5 variantes), sem substituir nada do jogo. */
    @SubscribeEvent
    public static void aboutToStart(ServerAboutToStartEvent e) {
        try {
            MinecraftServer server = e.getServer();
            var pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
            for (String type : VILLAGE_TYPES) {
                ResourceLocation id = ResourceLocation.withDefaultNamespace("village/" + type + "/houses");
                StructureTemplatePool pool = pools.getOptional(id).orElse(null);
                if (pool == null) continue;
                addToPool(pool, CLINIC_STRUCTURE.toString(), 3);
            }
        } catch (Exception ex) {
            SandevistanMod.LOGGER_WARN("Could not add the Ripperdoc clinic to the villages: " + ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addToPool(StructureTemplatePool pool, String location, int weight) throws Exception {
        // a lista usada na geracao e a unica ObjectArrayList da classe (acha pelo tipo, nao pelo nome)
        for (Field f : StructureTemplatePool.class.getDeclaredFields()) {
            if (f.getType() == ObjectArrayList.class) {
                f.setAccessible(true);
                ObjectArrayList<StructurePoolElement> list = (ObjectArrayList<StructurePoolElement>) f.get(pool);
                StructurePoolElement el = StructurePoolElement.single(location).apply(StructureTemplatePool.Projection.RIGID);
                for (int i = 0; i < weight; i++) list.add(el);
                return;
            }
        }
    }
}
