package com.dyingdark.entities;

public abstract class Entity {
    protected String name;
    protected float x, y;
    protected int hp, maxHp;
    protected int attack, defense;
    protected String stateName = "IDLE";

    public Entity(String name, float x, float y, int hp, int attack, int defense) {
        this.name = name; this.x = x; this.y = y;
        this.hp = maxHp = hp;
        this.attack = attack; this.defense = defense;
    }

    public void takeDamage(int damage) {
        int eff = Math.max(0, damage - defense);
        hp = Math.max(0, hp - eff);
        if (hp <= 0) stateName = "DEAD";
    }

    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }

    public boolean isAlive()    { return hp > 0; }
    public String getName()     { return name; }
    public float getX()         { return x; }
    public float getY()         { return y; }
    public int getHp()          { return hp; }
    public int getMaxHp()       { return maxHp; }
    public int getAttack()      { return attack; }
    public int getDefense()     { return defense; }
    public String getStateName(){ return stateName; }
    public void setX(float x)   { this.x = x; }
    public void setY(float y)   { this.y = y; }
    public void setHp(int hp)   { this.hp = Math.max(0, Math.min(maxHp, hp)); }
}
