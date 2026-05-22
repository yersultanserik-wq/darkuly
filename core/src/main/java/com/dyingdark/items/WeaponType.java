package com.dyingdark.items;

public enum WeaponType {
    SWORD  ("Sword",   "Меч",    WeaponShape.CROSS,    50, 0,   55f, 0.4f,  1f,   "Ближний бой, широкий удар."),
    AXE    ("Axe",     "Топор",  WeaponShape.AXE,      80, 0,   45f, 0.8f,  1.2f, "Медленный, огромный урон."),
    BOW    ("Bow",     "Лук",    WeaponShape.BOW,      35, 200, 65f, 0.6f,  0.8f, "Дальний бой, стрелы."),
    STAFF  ("Staff",   "Посох",  WeaponShape.STAFF,    55, 150, 75f, 0.7f,  1.1f, "Магия AOE, большой радиус."),
    DAGGER ("Dagger",  "Кинжал", WeaponShape.DAGGER,   25, 0,   40f, 0.25f, 0.7f, "Быстрый ближний удар.");

    public final String id;
    public final String displayName;
    public final WeaponShape shape;
    public final int    damage;
    public final float  range;        // 0 = melee use ATTACK_RADIUS
    public final float  attackRadius; // melee AoE
    public final float  cooldown;
    public final float  priceMultiplier; // base price * this
    public final String description;

    WeaponType(String id, String name, WeaponShape shape, int dmg, float range, float radius, float cd, float priceMult, String desc) {
        this.id              = id;
        this.displayName     = name;
        this.shape           = shape;
        this.damage          = dmg;
        this.range           = range;
        this.attackRadius    = radius;
        this.cooldown        = cd;
        this.priceMultiplier = priceMult;
        this.description     = desc;
    }

    public int getPrice(int tier) {
        return (int)(80 * priceMultiplier * tier);
    }
}
