package com.dyingdark.items;

public enum ConsumableType {
    HEAL_POTION  ("HealPotion",  "Зелье лечения",  60,  35, "Восстанавливает 60 HP."),
    MANA_POTION  ("ManaPotion",  "Зелье маны",     0,   40, "Восстанавливает атаку заклинания."),
    SHIELD_SCROLL("ShieldScroll","Свиток щита",    0,   55, "Временный щит на 5 секунд.");

    public final String id;
    public final String displayName;
    public final int    healAmount;
    public final int    price;
    public final String description;

    ConsumableType(String id, String name, int heal, int price, String desc) {
        this.id          = id;
        this.displayName = name;
        this.healAmount  = heal;
        this.price       = price;
        this.description = desc;
    }
}
