package com.dyingdark.entities;

public class Player extends Entity {
    private float speed = 120f; // pixels per second
    private int scrollCharges = 3;
    private float attackCooldown = 0;
    private static final float ATTACK_CD = 0.4f;

    public Player(float x, float y) {
        super("Hero", x, y, 150, 35, 10);
    }

    public void update(float delta) {
        if (attackCooldown > 0) attackCooldown -= delta;
        if (stateName.equals("ATTACKING") && attackCooldown <= 0) stateName = "IDLE";
    }

    public boolean canAttack() { return attackCooldown <= 0 && isAlive(); }

    public void startAttack() {
        stateName = "ATTACKING";
        attackCooldown = ATTACK_CD;
    }

    public float getSpeed()         { return speed; }
    public void setSpeed(float s)   { this.speed = s; }
    public int getScrollCharges()   { return scrollCharges; }
    public void useScroll()         { if (scrollCharges > 0) scrollCharges--; }
    public void addScrollCharge()   { scrollCharges++; }
    public void addMaxHp(int bonus) { maxHp += bonus; hp += bonus; }
    public void addAttack(int bonus){ attack += bonus; }
    public void addDefense(int b)   { defense += b; }
}
