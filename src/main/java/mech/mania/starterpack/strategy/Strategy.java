package mech.mania.starterpack.strategy;

import mech.mania.starterpack.game.BaseStrategy;
import mech.mania.starterpack.game.Plane;
import mech.mania.starterpack.game.PlaneStats;
import mech.mania.starterpack.game.PlaneType;
import mech.mania.starterpack.game.Vector;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Strategy extends BaseStrategy {
    private int myCounter = 0;
    private final Random random = new Random();
    private final Map<PlaneType, BiFunction<Plane, List<Plane>, Double>> strategyMap;
    private final Map<PlaneType, Integer> priceMap;
    private int KAMIKAZE_HEALTH = 4;
    private int RUN_DISTANCE = 20;
    private int MAX_ATTEMPT = 10;
    private double lb = -50, rb = 50, db = -50, ub = 50; // Example boundaries

    public Strategy(String team) {
        super(team);

        strategyMap = new HashMap<>();
        strategyMap.put(PlaneType.STANDARD, this::standardPlaneStrategy);
        strategyMap.put(PlaneType.THUNDERBIRD, this::thunderbirdStrategy);
        strategyMap.put(PlaneType.PIGEON, this::pigeonStrategy);

        priceMap = new HashMap<>();
        priceMap.put(PlaneType.FLYING_FORTRESS, 300);
        priceMap.put(PlaneType.STANDARD, 200);
        priceMap.put(PlaneType.THUNDERBIRD, 200);
        priceMap.put(PlaneType.SCRAPYARD_RESCUE, 100);
        priceMap.put(PlaneType.PIGEON, 10);
    }

    @Override
    public Map<PlaneType, Integer> selectPlanes() {
        return Map.of(
                PlaneType.STANDARD, 3,
                PlaneType.THUNDERBIRD, 2);
    }

    @Override
    public Map<String, Double> steerInput(Map<String, Plane> planes) {
        Map<String, Double> response = new HashMap<>();
        List<Plane> opponents = new ArrayList<>();
        List<Plane> teamPlanes = new ArrayList<>();

        for (Map.Entry<String, Plane> entry : planes.entrySet()) {
            Plane plane = entry.getValue();
            if (!plane.getTeam().equals(this.team)) {
                opponents.add(plane);
            } else {
                teamPlanes.add(plane);
            }
        }

        for (Plane p : teamPlanes) {
            Double steer = getSteerForPlane(p, opponents);
            response.put(p.getId(), steer);
        }

        myCounter++;
        return response;
    }

    private Double getSteerForPlane(Plane plane, List<Plane> opponents) {
        Double steer = getStrategy(plane, opponents);
        if (Utils.steerCrashesPlane(steer, plane)) {
            steer = checkFallbackSteers(plane);
        }
        return steer;
    }

    private Double checkFallbackSteers(Plane plane) {
        if (!Utils.steerCrashesPlane(1.0, plane)) {
            return 1.0;
        }
        if (!Utils.steerCrashesPlane(-1.0, plane)) {
            return -1.0;
        }
        return 0.0;
    }

    private Double getStrategy(Plane plane, List<Plane> opponents) {
        return clampSteer(
                strategyMap.getOrDefault(plane.getType(), (p, o) -> getRandomSteer(plane)).apply(plane, opponents));
    }

    private Double standardPlaneStrategy(Plane plane, List<Plane> opponents) {
        return run(plane, opponents);
    }

    private Double pigeonStrategy(Plane plane, List<Plane> opponents) {
        return kamikazeNearest(plane, opponents);
    }

    private Double thunderbirdStrategy(Plane plane, List<Plane> opponents) {
        if (plane.getHealth() <= KAMIKAZE_HEALTH) {
            return clampSteer(validateSteer(plane, kamikaze(plane, opponents)));
        }
        return clampSteer(validateSteer(plane,
                Utils.nvl(pursuit(plane, opponents),
                        Utils.nvl(kamikaze(plane, opponents), run(plane, opponents)))));
    }

    private Double validateSteer(Plane plane, Double steer) {
        return Utils.steerCrashesPlane(steer, plane) ? checkFallbackSteers(plane) : steer;
    }

    private Double pursuit(Plane plane, List<Plane> opponents) {
        List<Plane> validTargets = opponents.stream()
                .filter(p -> isInFront(plane, p))
                .sorted(Comparator.comparingInt(Plane::getHealth)
                        .thenComparingDouble(p -> Utils.planeFindPathToPoint(((Plane) p).getPosition(), plane)[1]))
                .filter(p -> validatePursuit(plane, p))
                .collect(Collectors.toList());

        for (Plane target : validTargets) {
            double[] steerAndSteps = Utils.planeFindPathToPoint(target.getPosition(), plane);
            double steer = clampSteer(steerAndSteps[0]);
            int steps = (int) steerAndSteps[1];

            if (validatePath(plane, steer, steps)) {
                return steer;
            }
        }

        return run(plane, opponents);
    }
    

    private boolean betterTrade(Plane plane, Plane opponent) {
        return getTradeValue(plane) <= getTradeValue(opponent);
    }

    private boolean validatePursuit(Plane plane, Plane opponent) {
        if (opponent.getHealth() == 1) {
            return true;
        }
        if (plane.getPosition().distance(opponent.getPosition()) - plane.getStats().getSpeed() - opponent.getStats().getSpeed() < 1) {
            if (isInFront(opponent, plane)) {
                return false;
            }
        }
        return true;
    }

    private double getTradeValue(Plane plane) {
        PlaneStats stats = plane.getStats();
        return priceMap.get(plane.getType()) * ((double) plane.getHealth() / stats.getMaxHealth());
    }

    private boolean validatePath(Plane plane, double steer, int steps) {
        double turnRadius = Utils.degreeToRadius(plane.getStats().getTurnSpeed(), plane.getStats().getSpeed());
        Vector pos = plane.getPosition();
        for (int i = 0; i < steps; i++) {
            Vector off = Utils.getPathOffset(1, steer, plane.getAngle(), plane.getStats().getSpeed(), turnRadius);
            pos = pos.add(off);
            if (Utils.unavoidableCrash(pos, plane.getAngle() + (plane.getStats().getTurnSpeed() * steer), turnRadius,
                    lb, rb, db, ub)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInFront(Plane plane, Plane target) {
        Vector planeDirection = new Vector(Math.cos(Math.toRadians(plane.getAngle())),
                Math.sin(Math.toRadians(plane.getAngle())));
        Vector toTarget = target.getPosition().sub(plane.getPosition());
        double dotProduct = planeDirection.dot(toTarget);
        return dotProduct > 0;
    }

    private Double kamikaze(Plane plane, List<Plane> opponents) {
        List<Plane> validTargets = opponents.stream()
                .filter(p -> isInFront(plane, p))
                .filter(p -> betterTrade(plane, p))
                .sorted(Comparator.comparingDouble(p -> getTradeValue((Plane) p))
                        .thenComparingDouble(p -> Utils.planeFindPathToPoint(((Plane) p).getPosition(), plane)[1]))
                .collect(Collectors.toList());

        for (Plane target : validTargets) {
            double[] steerAndSteps = Utils.planeFindPathToPoint(target.getPosition(), plane);
            double steer = clampSteer(steerAndSteps[0]);
            int steps = (int) steerAndSteps[1];

            if (validatePath(plane, steer, steps)) {
                return steer; // Return the valid steer for kamikaze
            }
        }

        return run(plane, opponents); // Fallback if no valid targets
    }

    private Double kamikazeNearest(Plane plane, List<Plane> opponents) {
        List<Plane> validTargets = opponents.stream()
                .filter(p -> isInFront(plane, p))
                .filter(p -> betterTrade(plane, p))
                .sorted(Comparator.comparingDouble(p -> Utils.planeFindPathToPoint(p.getPosition(), plane)[1]))
                .collect(Collectors.toList());

        for (Plane target : validTargets) {
            double[] steerAndSteps = Utils.planeFindPathToPoint(target.getPosition(), plane);
            double steer = clampSteer(steerAndSteps[0]);
            int steps = (int) steerAndSteps[1];

            if (validatePath(plane, steer, steps)) {
                return steer; // Return the valid steer for kamikaze
            }
        }

        return run(plane, opponents); // Fallback if no valid targets
    }

    private Double run(Plane plane, List<Plane> opponents) {
        Vector planePosition = plane.getPosition();
        Vector safeDirection = new Vector(0, 0);
        boolean isSafe = true;

        for (Plane enemy : opponents) {
            Vector toEnemy = enemy.getPosition().sub(planePosition);
            double distance = toEnemy.norm();

            if (distance < RUN_DISTANCE) {
                isSafe = false;
                Vector awayFromEnemy = Utils.normalize(toEnemy.mul(-1));
                double escapeDistance = RUN_DISTANCE - distance;
                safeDirection = safeDirection.add(awayFromEnemy.mul(escapeDistance));
            }
        }

        if (isSafe) {
            return checkFallbackSteers(plane);
        }

        if (safeDirection.norm() > 0) {
            safeDirection = Utils.normalize(safeDirection);
        }

        Vector targetPosition = planePosition.add(safeDirection.mul(RUN_DISTANCE));
        double steer = clampSteer(Utils.planeFindPathToPoint(targetPosition, plane)[0]);

        return steer;
    }

    private Double getRandomSteer(Plane plane) {
        Double steer;
        int attempts = 0;

        do {
            steer = clampSteer(-1 + 2 * random.nextDouble());
            attempts++;
        } while (Utils.steerCrashesPlane(steer, plane) && attempts < MAX_ATTEMPT);

        if (attempts >= MAX_ATTEMPT) {
            steer = checkFallbackSteers(plane);
        }

        return steer;
    }

    private Double clampSteer(Double steer) {
        return Math.max(-1.0, Math.min(1.0, steer));
    }

}
