package com.dyingdark.entities;

public class Enemy extends Entity {
    public enum MoveBehavior { CHASE, FLEE, PATROL }
    public enum AttackBehavior { MELEE, RANGED }

    private final MoveBehavior moveBehavior;
    private final AttackBehavior attackBehavior;
    private final float speed;
    private final float attackRange;
    private float attackCooldown = 0;
    private float patrolDir = 1;
    private final int goldDrop;
    private final String type;

    public Enemy(String name, String type, float x, float y,
                 int hp, int atk, int def, int gold,
                 MoveBehavior move, AttackBehavior attack, float speed, float range) {
        super(name, x, y, hp, atk, def);
        this.type = type; this.goldDrop = gold;
        this.moveBehavior = move; this.attackBehavior = attack;
        this.speed = speed; this.attackRange = range;
    }

    public void update(float delta, Player player) {
        if (!isAlive()) return;
        if (attackCooldown > 0) attackCooldown -= delta;

        float dx = player.getX() - x;
        float dy = player.getY() - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Movement
        switch (moveBehavior) {
            case CHASE -> {
                if (dist > 20f) {
                    x += (dx / dist) * speed * delta;
                    y += (dy / dist) * speed * delta;
                }
            }
            case FLEE -> {
                if (dist < 200f) {
                    x -= (dx / dist) * speed * delta;
                    y -= (dy / dist) * speed * delta;
                }
            }
            case PATROL -> {
                x += patrolDir * speed * delta;
                if (x > 900 || x < 60) patrolDir *= -1;
            }
        }
        // Clamp to screen
        x = Math.max(40, Math.min(920, x));
        y = Math.max(40, Math.min(580, y));
    }

    public boolean canAttack(Player player) {
        if (attackCooldown > 0 || !isAlive()) return false;
        float dx = player.getX() - x;
        float dy = player.getY() - y;
        return Math.sqrt(dx * dx + dy * dy) <= attackRange;
    }

    public void doAttack(Player player) {
        player.takeDamage(attack);
        attackCooldown = 1.2f;
        stateName = "ATTACKING";
    }

    public String getType()          { return type; }
    public int getGoldDrop()         { return goldDrop; }
    public AttackBehavior getAttackBehavior() { return attackBehavior; }
    public float getAttackRange()    { return attackRange; }
}
