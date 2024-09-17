package mech.mania.starterpack.strategy;

import mech.mania.starterpack.game.Plane;
import mech.mania.starterpack.game.Position;

import java.lang.Math;

public class Utils {
    /**
     * Returns the intersection of two lines, one of which goes through points p1 and p2,
     * and the other of which goes through points q1 and q2.
     * 
     * Returns null if the lines are parallel.
     * @param p1
     * @param p2
     * @param q1
     * @param q2
     * @return Position of intersection point, or null if none exists
     */
    public static Position intersectionPoint(Position p1, Position p2, Position q1, Position q2) {
        double slopeP = (p2.y() - p1.y()) / (p2.x() - p1.x());
        double slopeQ = (q2.y() - q1.y()) / (q2.x() - q1.x());

        if (Double.isInfinite(slopeP)) {
            slopeP = Double.MAX_VALUE;
        }
        if (Double.isInfinite(slopeQ)) {
            slopeQ = Double.MAX_VALUE;
        }

        if (slopeP == slopeQ) {
            return null; // Lines are parallel
        }

        double x, y;
        if (Double.isInfinite(slopeP)) {
            x = p1.x();
            y = slopeQ * (x - q1.x()) + q1.y();
        } else if (Double.isInfinite(slopeQ)) {
            x = q1.x();
            y = slopeP * (x - p1.x()) + p1.y();
        } else {
            x = ((q1.y() - p1.y()) + slopeP * p1.x() - slopeQ * q1.x()) / (slopeP - slopeQ);
            y = slopeP * (x - p1.x()) + p1.y();
        }

        return new Position(x, y);
    }

    /**
     * Returns the angle between two vectors a, b.
     * @param a
     * @param b
     * @return
     */
    public static Double angleBetweenPositions(Position a, Position b) {
        double dotProd = a.dot(b);
        double magnitudeA = a.norm();
        double magnitudeB = b.norm();

        if (magnitudeA < 0.0001 || magnitudeB < 0.0001) {
            return null;
        }

        double cosAngle = Math.min(1, dotProd / (magnitudeA * magnitudeB));
        return Math.acos(cosAngle);
    }
    /**
     * Returns a steer that travels along a circle with specific radius.
     *
     * NOTE: This function has no bounds checking. Check if your returned values are within range (e.g. steer between -1 and 1).
     */
    public static double radiusToSteer(double radius, double minTurn) {
        return minTurn / radius;
    }

    /**
     * Gives the radius of turning circle given a steer.
     * @param steer
     * @param minTurn
     */
    public static double steerToRadius(double steer, double minTurn) {
        return steer * minTurn;
    }

    /**
     * Given a change of degree, returns corresponding radius of the turning circle
     * @param degree
     * @param speed
     */
    public static double degreeToRadius(double degree, double speed) {
        return speed / Math.toRadians(degree);
    }

    /**
     * Given a radius of a turning circle, returns the degrees changed per turn
     * @param radius
     * @param speed
     */
    public static double radiusToDegree(double radius, double speed) {
        return Math.toDegrees(speed / radius);
    }

    /**
     * Returns the OFFSET of a plane with a given steer and at a given turn
     * 
     * @param t The queried turn. t can be any double (e.g, t=.05 means after half a turn)
     * @param steer The given steer, which is assumed to be constant.
     * @param initAngle The inital facing of a given plane.
     * @param speed The current speed of a given plane.
     * @param minTurn The smallest turning circle this plane can achieve
     * @return Position representing the positional OFFSET from the inital position
     */
    public static Position getPathOffset(double t, double steer, double initAngle, double speed, double minTurn) {
        double radius = steerToRadius(steer, minTurn);
        double initAngleRad = Math.toRadians(initAngle);

        if (steer == 0) {
            return new Position(speed * Math.cos(initAngleRad), speed * Math.sin(initAngleRad));
        } else if (steer < 0) {
            initAngleRad += Math.PI / 2;
        } else {
            initAngleRad -= Math.PI / 2;
        }

        double x = Math.cos(t * (speed / radius) + initAngleRad) - Math.cos(initAngleRad);
        double y = Math.sin(t * (speed / radius) + initAngleRad) - Math.sin(initAngleRad);

        return new Position(x * Math.abs(radius), y * Math.abs(radius));
    }

    /**
     * Returns the ABSOLUTE location and angle of a plane with a given steer and at a given turn
     * 
     * @param t The queried turn. t can be any real number (e.g, t=.05 means after half a turn)
     * @param steer The given steer, which is assumed to be constant.
     * @param plane The queried plane.
     * @return Position representing the actual position of the plane after t turns.
     */
    public static Position planePathOffset(double t, double steer, Plane plane) {
        double turnRadius = degreeToRadius(plane.getStats().getTurnSpeed(), plane.getStats().getSpeed());
        Position off = getPathOffset(t, steer, plane.getAngle(), plane.getStats().getSpeed(), turnRadius);
        return plane.getPosition().add(off);
    }

    /**
     * Two points and a tangent line uniquely defines a circle. Thus, this function returns the
     * the steer needed to travel along said circle, as well as the number of turns to reach
     * the desired point.
     * 
     * NOTE: This function gives especially poor steers when the given offset is "behind" the plane,
     * since this function will then try to travel around the circle the long way.
     * 
     * NOTE: This function has no bounds checking. Check if your returned values are within range
     * (e.g. steer between -1 and 1).
     * 
     * @param off The OFFSET position to fly towards.
     * @param initAngle The angle of the plane in degrees.
     * @param minTurn The smallest turning circle this plane can achieve.
     * @param speed The speed of the plane.
     * @return [steer, turns], where (steer) is the steer required for a plane to pass through the given OFFSET point
     * after (turns) turns
     */
    public static double[] flyToOffset(Position off, double initAngle, double minTurn, double speed) {
        double x = off.x();
        double y = off.y();

        if (x == 0 && y == 0) {
            return new double[] {0, 0};
        }

        double rad = Math.toRadians(initAngle);
        Position headingPerpVec = new Position(Math.cos(rad + Math.PI / 2), Math.sin(rad + Math.PI / 2));
        Position otherVecStart = new Position(x / 2, y / 2);
        Position otherVecEnd = new Position(-y + x / 2, x + y / 2);

        Position center = intersectionPoint(new Position(0, 0), headingPerpVec, otherVecStart, otherVecEnd);

        if (center == null) {
            return new double[] {0, (new Position(x, y)).norm() / speed};
        }

        double radius = center.norm();
        if (headingPerpVec.dot(center) < 0) {
            radius = -radius;
        }

        double angle = angleBetweenPositions(new Position(-center.x(), -center.y()), new Position(-center.x() + x, -center.y() + y));
        return new double[] {radiusToSteer(radius, minTurn), angle * Math.abs(radius) / speed};
    }

    /**
     * Wrapper around fly_to_offset for a plane.
     * Gives [steer, num_turns] for a plane to pass through a given ABSOLUTE point.
     * 
     * NOTE: This function has no bounds checking. Check if your returned values are within range (e.g. steer between -1 and 1).
     * 
     * @param target The ABSOLUTE position to fly towards.
     * @param plane A plane object with stats and a position.
     * @return [steer, turns]: The (steer) required for a plane to pass through a given ABSOLUTE point after (turns) turns
     */
    public static double[] planeFindPathToPoint(Position target, Plane plane) {
        Position off = target.add(new Position(-plane.getPosition().x(), -plane.getPosition().y()));
        double turnRadius = degreeToRadius(plane.getStats().getTurnSpeed(), plane.getStats().getSpeed());
        return flyToOffset(off, plane.getAngle(), turnRadius, plane.getStats().getSpeed());
    }

    /**
     * Returns true if a plane at a given point and angle cannot avoid flying out of the given bounds
     * 
     * NOTE: This function returns a false positive if the plane is close to the boundary but flying away.
     * This is normally impossible to achieve assuming speed, turn rate, and boundary locations are constant.
     * 
     * @param pos position of the plane.
     * @param angle facing angle of the plane in degrees.
     * @param minTurn the radius of the turning circle of the sharpest turn a plane can make
     * @param lb The leftmost boundary, given by the line x=lb
     * @param rb The rightmost boundary, given by the line x=rb
     * @param db The downmost boundary, given by the line y=db
     * @param ub The uppermost boundary, given by the line y=ub
     * 
     * @return  Returns True if plane in position cannot avoid flying out of bounds.
                Returns False otherwise.
     */
    public static boolean unavoidableCrash(Position pos, double angle, double minTurn, double lb, double rb, double db, double ub) {
        double x = pos.x();
        double y = pos.y();

        if (x < lb || x > rb || y < db || y > ub) {
            return true;
        }

        double rad = Math.toRadians(angle);
        Position perpVec = new Position(Math.cos(rad + Math.PI / 2), Math.sin(rad + Math.PI / 2));
        Position lvec = new Position(x, y).add(new Position(minTurn * perpVec.x(), minTurn * perpVec.y()));
        Position rvec = new Position(x, y).add(new Position(-minTurn * perpVec.x(), -minTurn * perpVec.y()));

        boolean lob = lvec.x() + minTurn > rb || lvec.x() - minTurn < lb || lvec.y() + minTurn > ub || lvec.y() - minTurn < db;
        boolean rob = rvec.x() + minTurn > rb || rvec.x() - minTurn < lb || rvec.y() + minTurn > ub || rvec.y() - minTurn < db;

        return lob && rob;
    }

    /**
     * Wrapper around unavoidable_crash.
     * 
     * Returns True if a plane flying with the given steer will not be able to avoid crashing
     * after flying one turn with the given steer. Returns False otherwise.
     * 
     * NOTE: This function returns a false positive if the plane is close to the boundary but flying away.
     * This is normally impossible to achieve assuming speed, turn rate, and boundary locations are constant.
     * 
     * @param steer The given steer of a plane.
     * @param plane A queried plane.
     * @return Returns True if plane in the given position will be unable to avoid a crash after
        one turn of flying at the given steer. Returns False otherwise.
     */
    public static boolean steerCrashesPlane(double steer, Plane plane) {
        double turnRadius = degreeToRadius(plane.getStats().getTurnSpeed(), plane.getStats().getSpeed());
        Position off = getPathOffset(1, steer, plane.getAngle(), plane.getStats().getSpeed(), turnRadius);
        Position pos = plane.getPosition().add(off);
        return unavoidableCrash(pos, plane.getAngle() + (plane.getStats().getTurnSpeed() * steer), turnRadius, -50, 50, -50, 50);
    }
}