package mech.mania.starterpack.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaneStats {
    private static Map<PlaneType, PlaneStats> PLANE_TYPE_TO_STATS;
    private final double speed;
    private final double turnSpeed;
    private final int maxHealth;
    private final double attackSpreadAngle;
    private final double attackRange;

    @JsonCreator
    public PlaneStats(
            @JsonProperty("speed") double speed,
            @JsonProperty("turnSpeed") double turnSpeed,
            @JsonProperty("health") int maxHealth,
            @JsonProperty("attackSpreadAngle") double attackSpreadAngle,
            @JsonProperty("attackRange") double attackRange) {
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.maxHealth = maxHealth;
        this.attackSpreadAngle = attackSpreadAngle;
        this.attackRange = attackRange;
    }

    public static PlaneStats getByType(PlaneType type) {
        return PlaneStats.PLANE_TYPE_TO_STATS.get(type);
    }

    public double getSpeed() {
        return speed;
    }

    public double getTurnSpeed() {
        return turnSpeed;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public double getAttackSpreadAngle() {
        return attackSpreadAngle;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public static void setPlaneTypeToStats(Map<PlaneType, PlaneStats> planeTypeToStats) {
        PlaneStats.PLANE_TYPE_TO_STATS = planeTypeToStats;
    }
}