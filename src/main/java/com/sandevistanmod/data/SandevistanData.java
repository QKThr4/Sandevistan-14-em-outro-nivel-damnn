package com.sandevistanmod.data;

import com.sandevistanmod.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Estado dos implantes de um jogador (salvo no jogador e copiado ao morrer). */
public class SandevistanData implements INBTSerializable<CompoundTag> {
    public static final float MAX_MIND = 100f;
    /** Tempo, em segundos reais, que a barra de Mente cheia dura com o Sandevistan ligado. */
    public static final float USE_SECONDS = 30f;
    /** Tempo, em segundos reais, para a barra encher do zero (1 segundo de uso a cada 3 segundos parado) com o Sandevistan desligado. */
    public static final float REGEN_SECONDS = 90f;
    /** Mente minima para conseguir ligar o Sandevistan. */
    public static final float MIN_TO_START = 10f;
    /** Tick rate do jogo inteiro enquanto o Sandevistan esta ligado. */
    public static final int SLOW_TICK_RATE = 3;

    /** Ciberpsicose maxima guardada (a barra mostra 100%, mas ela pode passar disso). */
    public static final float MAX_PSY = 300f;
    /** A cada 50 de Mente gasta (50% de uso) o jogador ganha 5 de ciberpsicose. */
    public static final float PSY_USE_STEP = 50f;
    public static final float PSY_PER_STEP = 5f;
    /** Segundos parado (sem ligar o Sandevistan) ate os efeitos negativos comecarem. */
    public static final float PSY_IDLE_SECONDS = 0f;
    /** Duracao do efeito Mente Boa do remedio e tempo (s) para regenerar 1 de Mente com ele. */
    public static final float GOOD_MIND_SECONDS = 60f;
    public static final float GOOD_MIND_REGEN_SECONDS = 30f;

    // ---- implantes: encaixes 0 costas (Sandevistan), 1 braco direito, 2 braco esquerdo, 3 link neural ----
    public static final int SLOT_BACK = 0;
    public static final int SLOT_ARM_R = 1;
    public static final int SLOT_ARM_L = 2;
    public static final int SLOT_NEURAL = 3;
    public static final int SLOT_BONE = 4;
    public static final int SLOT_NERVE = 5;
    public static final int SLOT_COUNT = 6;

    /** Quanto cada implante soma na Carga Cibernetica (%) e cada chip. */
    public static final float LOAD_SANDEVISTAN = 30f;
    public static final float LOAD_ARM = 10f;
    public static final float LOAD_NEURAL = 0f;
    /** Estrutura Ossea Reforcada e Sistema Nervoso Aprimorado. */
    public static final float LOAD_BONE = 5f;
    public static final float LOAD_NERVE = 5f;
    public static final float LOAD_CHIP = 5f;
    /** Mente permanente que cada chip inserido tira e segundos para ele baixar antes de funcionar. */
    public static final float CHIP_MIND = 5f;
    public static final float CHIP_LOAD_SECONDS = 10f;
    /** Duracao de cada Imunossupressor (s). */
    public static final float DOSE_SECONDS = 60f;

    public static final int CHIP_MARTIAL = 1;
    public static final int CHIP_PAIN = 2;
    public static final int CHIP_MINING = 3;
    public static final int CHIP_WALKER = 4;
    public static final int CHIP_TYPES = 4;
    /** Durabilidade de cada chip (1 uso = 1 de durabilidade). */
    private static final int[] CHIP_DURABILITY = {0, 800, 500, 2000, 400};

    public static int chipMax(int type) {
        return type >= 1 && type <= CHIP_TYPES ? CHIP_DURABILITY[type] : 0;
    }

    public static float loadOf(int slot) {
        return switch (slot) {
            case SLOT_BACK -> LOAD_SANDEVISTAN;
            case SLOT_ARM_R, SLOT_ARM_L -> LOAD_ARM;
            case SLOT_BONE -> LOAD_BONE;
            case SLOT_NERVE -> LOAD_NERVE;
            default -> LOAD_NEURAL;
        };
    }

    /** Carga que o Sandevistan soma, por modelo (0 Apogee ... 4 QianT). */
    public static float loadOfVariant(int variant) {
        return SandevistanVariant.of(variant).load;
    }

    /** True depois que o video da primeira vez ja tocou para este implante das costas. */
    public boolean introSeen;
    public boolean implanted;
    public boolean armR;
    public boolean armL;
    public boolean neural;
    public boolean bone;
    public boolean nerve;
    /** Modelo do Sandevistan implantado: 0 Apogee, 1 Falcon, 2 Dynalar, 3 Zetatech, 4 QianT. */
    public int variant;
    /** Os bracos de ferro aparecem no modelo do jogador. */
    public boolean armsVisible = true;
    /** Dois encaixes de chip no Link Neural: tipo (0 vazio), dano de durabilidade e segundos que faltam para baixar. */
    public final int[] chip = new int[2];
    public final int[] chipDmg = new int[2];
    public final float[] chipLoad = new float[2];

    public boolean active;
    public float mind = MAX_MIND;
    /** Ciberpsicose (0 a 300; 100 = limite da barra) e Mente Boa restante (segundos reais). */
    public float psy;
    public float goodMind;
    /** Mente perdida PARA SEMPRE pela ciberpsicose (a Mente maxima cai); so o Imunossupressor recupera. */
    public float mindLost;
    /** Doses de Imunossupressor ativas: {segundos restantes, ciberpsicose que volta quando acabar}. Cada uma tem o seu tempo. */
    public final List<float[]> doses = new ArrayList<>();
    /** Apagao da ciberpsicose (a partir de 70%): o jogador nao ve nada e age sozinho. Enviado ao cliente. */
    public boolean blind;
    /** Sequencia de Imunossupressores: quantos seguidos e segundos restantes para a sequencia quebrar. */
    public int immuSeq;
    public float immuSeqT;
    /** Mente gasta ainda nao convertida em ciberpsicose. */
    public float useAcc;
    /** Sintomas ativos agora (parou de usar ha mais de 5 s): enviado ao cliente para os efeitos de tela. */
    public boolean psychotic;

    // temporizadores so do servidor (nao salvos)
    public transient float idle;
    public transient float decayT;
    public transient float drainT;
    public transient float secT;
    public transient float goodT;
    public transient boolean zeroWarned;
    public transient float zeroT;
    public transient float zeroDmgT;
    public transient float riseT;
    public transient float goodHealT;
    public transient float blindT;
    public transient float forceT;
    public transient float zeroHitCd;
    public transient float loadT;

    public static SandevistanData get(Player p) {
        return p.getData(ModRegistries.SANDEVISTAN);
    }

    public boolean has(int slot) {
        return switch (slot) {
            case SLOT_BACK -> implanted;
            case SLOT_ARM_R -> armR;
            case SLOT_ARM_L -> armL;
            case SLOT_BONE -> bone;
            case SLOT_NERVE -> nerve;
            default -> neural;
        };
    }

    public void set(int slot, boolean value) {
        switch (slot) {
            case SLOT_BACK -> implanted = value;
            case SLOT_ARM_R -> armR = value;
            case SLOT_ARM_L -> armL = value;
            case SLOT_BONE -> bone = value;
            case SLOT_NERVE -> nerve = value;
            default -> neural = value;
        }
    }

    public boolean hasAny() {
        return implanted || armR || armL || neural || bone || nerve;
    }

    public int arms() {
        return (armR ? 1 : 0) + (armL ? 1 : 0);
    }

    public int chips() {
        return (chip[0] != 0 ? 1 : 0) + (chip[1] != 0 ? 1 : 0);
    }

    public SandevistanVariant variant() {
        return SandevistanVariant.of(variant);
    }

    /** Carga Cibernetica (%): Sandevistan 10 a 30 (pelo modelo), cada braco 10, cada chip 5, osso e nervos 5, Link Neural 0. */
    public float capacity() {
        return (implanted ? variant().load : 0f) + LOAD_ARM * arms() + LOAD_CHIP * chips()
                + (bone ? LOAD_BONE : 0f) + (nerve ? LOAD_NERVE : 0f);
    }

    /** Este tipo de chip esta inserido, ja baixou e ainda tem durabilidade? */
    public boolean chipActive(int type) {
        for (int i = 0; i < 2; i++) {
            if (chip[i] == type && chipLoad[i] <= 0f && chipDmg[i] < chipMax(type)) return true;
        }
        return false;
    }

    /** Encaixe (0 ou 1) onde esta o chip deste tipo, ou -1. */
    public int chipSlot(int type) {
        for (int i = 0; i < 2; i++) if (chip[i] == type) return i;
        return -1;
    }

    /** Mente permanente que os chips inseridos tiram. */
    public float chipMind() {
        return CHIP_MIND * chips();
    }

    /** Mente maxima atual (100 menos o que a ciberpsicose e os chips tiraram). */
    public float maxMind() {
        return Math.max(0f, Math.min(MAX_MIND, MAX_MIND - mindLost - chipMind()));
    }

    /** Segundos reais restantes de uso com a Mente atual. */
    public int secondsLeft() {
        return Math.max(0, (int) Math.ceil(mind / (MAX_MIND / variant().seconds)));
    }

    /**
     * Codigo enviado aos outros jogadores: bit 1 Sandevistan, bit 2 ligado, bit 4 com sintomas, bits 3 a 9 ciberpsicose (0 a 100),
     * bit 10 braco direito, bit 11 braco esquerdo, bit 12 Link Neural, bit 13 bracos visiveis, bits 14 a 16 modelo do Sandevistan.
     */
    public int stateCode() {
        int level = Math.max(0, Math.min(100, (int) Math.floor(psy)));
        boolean sick = implanted && psychotic && psy > 0f;
        return (implanted ? 1 : 0) | (active && implanted ? 2 : 0) | (sick ? 4 : 0) | ((sick ? level : 0) << 3)
                | (armR ? 1 << 10 : 0) | (armL ? 1 << 11 : 0) | (neural ? 1 << 12 : 0) | (armsVisible ? 1 << 13 : 0)
                | ((variant & 7) << 14);
    }

    /** Muda quando algo que o cliente precisa ver mudou (usado para decidir se reenvia os dados). */
    public long syncSig() {
        long h = 17L;
        h = h * 31L + (long) Math.floor(psy);
        h = h * 31L + (long) Math.ceil(goodMind);
        h = h * 31L + (psychotic ? 1 : 0);
        h = h * 31L + (blind ? 1 : 0);
        h = h * 31L + (long) Math.floor(mindLost);
        h = h * 31L + (implanted ? 1 : 0) + (armR ? 2 : 0) + (armL ? 4 : 0) + (neural ? 8 : 0) + (armsVisible ? 16 : 0)
                + (bone ? 32 : 0) + (nerve ? 64 : 0) + variant * 128L;
        for (int i = 0; i < 2; i++) {
            h = h * 31L + chip[i];
            h = h * 31L + chipDmg[i];
            h = h * 31L + (long) Math.ceil(chipLoad[i]);
        }
        h = h * 31L + doses.size();
        for (float[] ds : doses) h = h * 31L + (long) Math.ceil(ds[0]);
        return h;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("implanted", implanted);
        t.putBoolean("intro", introSeen);
        t.putBoolean("armR", armR);
        t.putBoolean("armL", armL);
        t.putBoolean("neural", neural);
        t.putBoolean("bone", bone);
        t.putBoolean("nerve", nerve);
        t.putInt("var", variant);
        t.putBoolean("armvis", armsVisible);
        t.putIntArray("chip", new int[] {chip[0], chip[1]});
        t.putIntArray("chipd", new int[] {chipDmg[0], chipDmg[1]});
        t.putFloat("chipl0", chipLoad[0]);
        t.putFloat("chipl1", chipLoad[1]);
        t.putBoolean("active", active);
        t.putFloat("mind", mind);
        t.putFloat("psy", psy);
        t.putFloat("lost", mindLost);
        ListTag list = new ListTag();
        for (float[] ds : doses) {
            CompoundTag c = new CompoundTag();
            c.putFloat("t", ds[0]);
            c.putFloat("p", ds[1]);
            list.add(c);
        }
        t.put("doses", list);
        t.putBoolean("blind", blind);
        t.putInt("is", immuSeq);
        t.putFloat("ist", immuSeqT);
        t.putFloat("good", goodMind);
        t.putFloat("acc", useAcc);
        t.putBoolean("psychotic", psychotic);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        implanted = t.getBoolean("implanted");
        introSeen = !t.contains("intro") || t.getBoolean("intro");
        armR = t.getBoolean("armR");
        armL = t.getBoolean("armL");
        neural = t.getBoolean("neural");
        bone = t.getBoolean("bone");
        nerve = t.getBoolean("nerve");
        variant = Math.max(0, Math.min(SandevistanVariant.values().length - 1, t.getInt("var")));
        armsVisible = !t.contains("armvis") || t.getBoolean("armvis");
        int[] c = t.getIntArray("chip");
        int[] cd = t.getIntArray("chipd");
        for (int i = 0; i < 2; i++) {
            chip[i] = i < c.length && c[i] >= 1 && c[i] <= CHIP_TYPES ? c[i] : 0;
            chipDmg[i] = chip[i] != 0 && i < cd.length ? Math.max(0, Math.min(chipMax(chip[i]), cd[i])) : 0;
            chipLoad[i] = chip[i] != 0 ? Math.max(0f, t.getFloat("chipl" + i)) : 0f;
        }
        active = t.getBoolean("active");
        mind = t.contains("mind") ? t.getFloat("mind") : MAX_MIND;
        psy = t.getFloat("psy");
        mindLost = Math.max(0f, Math.min(MAX_MIND, t.getFloat("lost")));
        doses.clear();
        ListTag list = t.getList("doses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag d = list.getCompound(i);
            float left = d.getFloat("t");
            if (left > 0f) doses.add(new float[] {left, Math.max(0f, d.getFloat("p"))});
        }
        blind = t.getBoolean("blind");
        immuSeq = Math.max(0, t.getInt("is"));
        immuSeqT = Math.max(0f, t.getFloat("ist"));
        goodMind = t.getFloat("good");
        useAcc = t.getFloat("acc");
        psychotic = t.getBoolean("psychotic");
    }
}
