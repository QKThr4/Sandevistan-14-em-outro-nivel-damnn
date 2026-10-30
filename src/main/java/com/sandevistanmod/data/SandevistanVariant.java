package com.sandevistanmod.data;

/**
 * Modelos de Sandevistan. Quanto mais ele desacelera o mundo, mais forte e mais caro (Carga Cibernetica) e mais curto.
 * Os bonus de movimento sao calculados a partir do tick rate: o jogador precisa ser 1,2x mais rapido do que o mundo ficou lento.
 */
public enum SandevistanVariant {
    //          nome        slow% seg  carga força haste resist esquiva knock  dano  ar    mitig  tela(ARGB)  silhueta
    APOGEE("apogee", 85, 30f, 30f, 3, 2, 3, 0.50f, 0.45, 1.0f, 1.0f, 1.0f, 0x1C00FF40, 0x30FF70),
    FALCON("falcon", 70, 35f, 25f, 2, 2, 2, 0.40f, 0.35, 1.0f, 1.0f, 1.0f, 0x1E00B038, 0x1FA84A),
    DYNALAR("dynalar", 50, 40f, 20f, 1, 1, 1, 0.30f, 0.25, 1.0f, 1.0f, 1.0f, 0x260A64FF, 0x2F8CFF),
    ZETATECH("zetatech", 30, 45f, 15f, -1, -1, -1, 0.20f, 0.20, 2.0f, 1.25f, 1.0f, 0x1CB8B48A, 0xC2C68A),
    QIANT("qiant", 20, 50f, 10f, -1, -1, 3, 0.0f, 0.10, 1.0f, 1.0f, 0.5f, 0x26FF8A1E, 0xFF8A1E);

    public final String id;
    /** Quanto desacelera o mundo (%). */
    public final int slow;
    /** Segundos de uso com a Mente cheia. */
    public final float seconds;
    /** Quanto soma na Carga Cibernetica (%). */
    public final float load;
    /** Niveis (valor = nivel - 1; -1 = sem o efeito) de Forca, Pressa e Resistencia. */
    public final int strength;
    public final int haste;
    public final int resist;
    public final float dodge;
    public final double knockback;
    /** Multiplicador do dano causado, extra se estiver no ar (Zetatech) e do dano recebido (QianT: 0,5 = metade). */
    public final float damage;
    public final float airDamage;
    public final float taken;
    public final int tint;
    public final int ghost;

    SandevistanVariant(String id, int slow, float seconds, float load, int strength, int haste, int resist,
                       float dodge, double knockback, float damage, float airDamage, float taken, int tint, int ghost) {
        this.id = id;
        this.slow = slow;
        this.seconds = seconds;
        this.load = load;
        this.strength = strength;
        this.haste = haste;
        this.resist = resist;
        this.dodge = dodge;
        this.knockback = knockback;
        this.damage = damage;
        this.airDamage = airDamage;
        this.taken = taken;
        this.tint = tint;
        this.ghost = ghost;
    }

    public static SandevistanVariant of(int index) {
        SandevistanVariant[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, index))];
    }

    /** Tick rate do jogo enquanto este modelo esta ligado (20 x o que sobra: 85% -> 3). */
    public int rate() {
        return Math.max(1, Math.round(20f * (100 - slow) / 100f));
    }

    /** Quantas vezes o mundo fica mais lento (20 / tick rate). */
    public float factor() {
        return 20f / rate();
    }

    /** 1 para o Apogee e proporcionalmente menos para os outros (usado nos bonus de pulo, gravidade e client). */
    public float scale() {
        return (factor() - 1f) / (SandevistanVariant.APOGEE.factor() - 1f);
    }

    /** Multiplicador final de movimento e de quebrar blocos (1,2x o quanto o mundo ficou mais lento). */
    public float speedMult() {
        return 1.2f * factor();
    }

    /** Nivel do efeito de Velocidade (valor = nivel - 1; cada nivel soma 20%). */
    public int speedAmp() {
        return Math.round((speedMult() - 1f) / 0.2f - 1f);
    }

    public double jumpAdd() {
        return 1.28 * scale();
    }

    public double gravityAdd() {
        return 0.5 * scale();
    }

    public double attackSpeedAdd() {
        return Math.max(10.0, 100.0 * scale());
    }

    /** Ticks extras por tick ao usar itens e nas recargas (o mundo fica mais lento, entao comer etc. fica mais rapido). */
    public int extraTicks() {
        return Math.max(0, (int) Math.floor(factor() - 1f));
    }

    /** Recuperacao: 1 segundo de uso a cada 3 segundos parado. */
    public float regenSeconds() {
        return seconds * 3f;
    }
}
