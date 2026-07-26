package Solver;

/**
 * An immutable-style 2D vector with float-precision components.
 *
 * All arithmetic operations ({@link #add}, {@link #sub}, {@link #scale},
 * {@link #divide}, {@link #rotate}) return a new {@code Vector2D}
 * instance and do not modify the receiver, making this class safe to share
 * across physics calculations without defensive copying.
 *
 * Typical usage within the physics engine:
 * {@code
 * Vector2D pos   = new Vector2D(100, 200);
 * Vector2D vel   = new Vector2D(3, -1);
 * Vector2D next  = pos.add(vel.scale(dt));
 * }
 */
public class Vector2D {

    private float x;
    private float y;

    public Vector2D() {
        this.x = 0;
        this.y = 0;
    }


    public Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }


    public Vector2D(Vector2D vector2D) {
        this.x = vector2D.x;
        this.y = vector2D.y;
    }


    public Vector2D clone(Vector2D vector2D) {
        return new Vector2D(vector2D);
    }

    public Vector2D add(Vector2D vector) {
        return new Vector2D(x + vector.x, y + vector.y);
    }


    public Vector2D sub(Vector2D vector) {
        return new Vector2D(x - vector.x, y - vector.y);
    }

    public float dotProduct(Vector2D vector) {
        return this.x * vector.x + this.y * vector.y;
    }


    public Vector2D cross(Vector2D vector) {
        return new Vector2D(x * vector.y, y * vector.x);
    }

    public float distance(Vector2D vector) {
        float dx = this.x - vector.x;
        float dy = this.y - vector.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns a unit vector pointing in the direction specified by
     * {@code angle} (in radians), measured counter-clockwise from the
     * positive x-axis.
     *
     * Note: the receiver's own components are ignored; this method acts
     * as a factory for direction vectors rather than rotating {@code this}.
     *
     * @param angle the angle in radians
     * @return {@code (cos(angle), sin(angle))}
     */
    public Vector2D rotate(float angle) {
        return new Vector2D((float) Math.cos(angle), (float) Math.sin(angle));
    }

    public float direction() {
        return (float) Math.atan2(y, x);
    }

    public Vector2D scale(float scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }


    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public Vector2D divide(float scalar) {
        return new Vector2D(x / scalar, y / scalar);
    }

  
    @Override
    public Vector2D clone() {
        return new Vector2D(this.x, this.y);
    }

    public boolean equals(Vector2D vector) {
        return x == vector.x && y == vector.y;
    }

    public void setPoints(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    
    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }
}