package com.dyingdark.items;

public enum ConsumableType {
    HEAL_POTION  ("HealPotion",  "Heal Potion",  60,  35, "Restores 60 HP."),
    MANA_POTION  ("ManaPotion",  "Mana Potion",     0,   40, "Restores spell energy."),
    SHIELD_SCROLL("ShieldScroll","Shield Scroll",    0,   55, "Temporary shield for 5 seconds.");

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
