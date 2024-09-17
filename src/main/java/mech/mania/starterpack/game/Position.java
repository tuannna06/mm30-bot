package mech.mania.starterpack.game;

import java.lang.Math;

public record Position(double x, double y) {
    public Position add(Position other) {
        return new Position(other.x + this.x, other.y + this.y);
    }
    public Position sub(Position other) {
        return new Position(this.x - other.x, this.y - other.y);
    }
    public Position mul(double s) {
        return new Position(this.x * s, this.y * s);
    }
    public Position neg() {
        return new Position(-this.x, -this.y);
    }
    public double dot(Position other) {
        return this.x*other.x + this.y*other.y;
    }
    public double norm() {
        return Math.sqrt(this.x*this.x + this.y*this.y);
    }
    public double distance(Position other) {
        return Math.sqrt((this.x-other.x)*(this.x-other.x) + (this.y-other.y)*(this.y-other.y));
    }
}