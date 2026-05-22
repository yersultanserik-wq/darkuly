package com.dyingdark.races;

public enum Race {
    HUMAN   ("Human",    150, 35, 10, 120f, "Balanced. Moderate in everything."),
    ELF     ("Elf",      110, 30,  6, 170f, "Fast and agile, but fragile."),
    ORC     ("Orc",      220, 55, 18,  90f, "Slow giant with massive damage."),
    NECRO   ("Necromancer", 90, 60,  4, 130f, "Fragile mage with powerful magic."),
    DWARF   ("Dwarf",    180, 38, 14, 135f, "Sturdy and slightly faster than human.");

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
