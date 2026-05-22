package com.dyingdark.races;

public enum Race {
    HUMAN   ("Human",    150, 35, 10, 120f, "Сбалансированный. Умеренный во всём."),
    ELF     ("Elf",      110, 30,  6, 170f, "Быстрый и ловкий, но хрупкий."),
    ORC     ("Orc",      220, 55, 18,  90f, "Медленный гигант с огромным уроном."),
    NECRO   ("Necromancer", 90, 60,  4, 130f, "Хрупкий маг с мощной магией."),
    DWARF   ("Dwarf",    180, 38, 14, 135f, "Крепкий и немного быстрее человека.");

    public final String displayName;
    public final int    baseHp;
    public final int    baseAttack;
    public final int    baseDefense;
    public final float  baseSpeed;
    public final String description;

    Race(String displayName, int hp, int atk, int def, float spd, String desc) {
        this.displayName = displayName;
        this.baseHp      = hp;
        this.baseAttack  = atk;
        this.baseDefense = def;
        this.baseSpeed   = spd;
        this.description = desc;
    }
}
