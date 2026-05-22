package com.dyingdark.items;

public class ArmorItem {
    public final String   id;
    public final String   displayName;
    public final ArmorSlot slot;
    public final int      tier;       // 1-4 (tier 4-5 names are legendary)
    public final int      defenseBonus;
    public final int      hpBonus;
    public final int      price;

    // Tier names per slot
    private static final String[][] HELMET_NAMES = {
        {}, // 0 unused
        {"Кожаный шлем"},
        {"Кольчужный шлем"},
        {"Стальной шлем"},
        {"Рунический шлем"},
        {"Шлем Тьмы"}
    };
    private static final String[][] CHEST_NAMES = {
        {},
        {"Кожаный нагрудник"},
        {"Кольчужный нагрудник"},
        {"Стальной нагрудник"},
        {"Рунический нагрудник"},
        {"Нагрудник Тьмы"}
    };
    private static final String[][] LEGS_NAMES = {
        {},
        {"Кожаные поножи"},
        {"Кольчужные поножи"},
        {"Стальные поножи"},
        {"Рунические поножи"},
        {"Поножи Тьмы"}
    };

    public ArmorItem(ArmorSlot slot, int tier) {
        this.slot  = slot;
        this.tier  = tier;
        String name = switch (slot) {
            case HELMET     -> HELMET_NAMES[tier][0];
            case CHESTPLATE -> CHEST_NAMES[tier][0];
            case LEGGINGS   -> LEGS_NAMES[tier][0];
        };
        this.displayName  = name;
        this.id           = slot.name() + "_T" + tier;
        this.defenseBonus = tier * 6;
        this.hpBonus      = tier * 12;
        this.price        = tier * 90;
    }
}
