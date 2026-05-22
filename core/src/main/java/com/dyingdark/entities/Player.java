package com.dyingdark.entities;

import com.dyingdark.items.ArmorItem;
import com.dyingdark.items.ArmorSlot;
import com.dyingdark.items.WeaponType;
import com.dyingdark.races.Race;

import java.util.EnumMap;
import java.util.Map;

public class Player extends Entity {

    private Race race;
    private float speed;
    private float attackCooldown = 0;
    private WeaponType equippedWeapon = WeaponType.SWORD;
    private final Map<ArmorSlot, ArmorItem> armor = new EnumMap<>(ArmorSlot.class);

    // Mana for staff / bow
    private int mana = 100;
    private int maxMana = 100;

    public Player(float x, float y, Race race) {
        super(race.displayName, x, y, race.baseHp, race.baseAttack, race.baseDefense);
        this.race  = race;
        this.speed = race.baseSpeed;
        // Necromancer gets mana bonus
        if (race == Race.NECRO) { this.mana = 150; this.maxMana = 150; }
    }

    public void update(float delta) {
        if (attackCooldown > 0) attackCooldown -= delta;
        if (stateName.equals("ATTACKING") && attackCooldown <= 0) stateName = "IDLE";
    }

    public boolean canAttack() {
        return attackCooldown <= 0 && isAlive();
    }

    public void startAttack() {
        stateName = "ATTACKING";
        attackCooldown = equippedWeapon.cooldown;
    }

    /** Effective attack = base + weapon damage */
    @Override
    public int getAttack() { return attack + equippedWeapon.damage; }

    /** Effective defense = base + all armor */
    @Override
    public int getDefense() {
        int d = defense;
        for (ArmorItem a : armor.values()) d += a.defenseBonus;
        return d;
    }

    /** Effective max HP = base + armor bonuses */
    @Override
    public int getMaxHp() {
        int m = maxHp;
        for (ArmorItem a : armor.values()) m += a.hpBonus;
        return m;
    }

    public void equipWeapon(WeaponType w) { this.equippedWeapon = w; }
    public WeaponType getEquippedWeapon() { return equippedWeapon; }

    public void equipArmor(ArmorItem item) {
        armor.put(item.slot, item);
    }
    public ArmorItem getArmor(ArmorSlot slot) { return armor.get(slot); }

    public float getSpeed()          { return speed; }
    public void  setSpeed(float s)   { this.speed = s; }
    public Race  getRace()           { return race; }
    public int   getMana()           { return mana; }
    public int   getMaxMana()        { return maxMana; }
    public void  restoreMana(int v)  { mana = Math.min(maxMana, mana + v); }
    public void  useMana(int v)      { mana = Math.max(0, mana - v); }
    public void  addMaxHp(int bonus) { maxHp += bonus; hp += bonus; }
    public void  addAttack(int b)    { attack += b; }
    public void  addDefense(int b)   { defense += b; }
    public float getAttackCooldownRatio() {
        if (equippedWeapon.cooldown <= 0) return 0;
        return Math.max(0, attackCooldown / equippedWeapon.cooldown);
    }
    public float getAttackRadius() { return equippedWeapon.attackRadius; }
}
