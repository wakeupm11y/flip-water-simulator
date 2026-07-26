package Boundaries;


import Solver.Grid;
import Solver.Vector2D;
import Particles.Particle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;


public class CircularBoundary implements Boundary {

    private final Vector2D center;
    private final float radius;
    private final Color color;

    public CircularBoundary(float xCenter, float yCenter, float radius, Color color) {
        this.center = new Vector2D(xCenter, yCenter);
        this.radius = radius;
        this.color  = color;
    }

    public float getRadius() {
        return radius;
    }

    public Vector2D getCenter() {
        return new Vector2D(center);
    }


    @Override
    public void apply(Particle particle) {
        Vector2D delta = particle.getPosition().sub( center);
        float dist = (float)  delta.magnitude();
        float edge = (float)  (radius - particle.getRadius());
        
        if (dist > edge - 1e-4) {
            // Safe check to prevent division by zero
            Vector2D n;
            if (dist > 1e-6) {
                n = delta.divide(dist);
            } else {
                n = new Vector2D(1.0f, 0.0f); // Default fallback pointing outwards
            }

            particle.setPosition(
                center.add(n.scale((float) (radius - particle.getRadius())))
            );

            float vDotN = particle.getVelocity().dotProduct(n);
            if (vDotN > 0) {
                particle.setVelocity(
                    particle.getVelocity().sub(n.scale(vDotN * 1.0f))
                );
            }
        }
    }

    @Override
    public void markBoundary(Grid grid) {
        float cs = (float)  grid.getCellSize();
        for (int i = 0; i < grid.getNumX(); i++) {
            for (int j = 0; j < grid.getNumY(); j++) {
                boolean isBorder = (i == 0 || j == 0 || i == grid.getNumX() - 1 || j == grid.getNumY() - 1);
                float cellCx = (float) ((i + 0.5) * cs - center.getX());
                float cellCy = (float) ((j + 0.5) * cs - center.getY());
                boolean outsideCircle = (cellCx * cellCx + cellCy * cellCy) > (radius * radius);
                if (isBorder || outsideCircle) {
                    grid.setCellType(grid.cellIndex(i, j), Grid.SOLID);
                }
            }
        }
    }


    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(color);
        g2.fillOval(
            (int) (center.getX() - radius),
            (int) (center.getY() - radius),
            (int) radius * 2, 
            (int) radius * 2
            );
    }
    
}
