package com.mmahlatji.flipwatersim.Boundaries;


import com.mmahlatji.flipwatersim.Solver.Grid;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;


public class CircularBoundary implements Boundary {

    private final float cX;
    private final float cY;
    private final float radius;
    private final Color color;

    public CircularBoundary(float xCenter, float yCenter, float radius, Color color) {
        this.cY = yCenter;
        this.cX = xCenter;
        this.radius = radius;
        this.color  = color;
    }

    public float getRadius() {
        return radius;
    }

    public float getCenterX() {
        return cX;
    }

    public float getCenterY() {
        return cY;
    }


    @Override
    public void apply(float[] posX, float[] posY, float[] velX, float[] velY, int index) {
        float xDelta = posX[index] - cX;
        float yDelta = posY[index] - cY;
        float dist = (float) Math.sqrt((xDelta * xDelta) + (yDelta * yDelta));
        float edge = (radius - com.mmahlatji.flipwatersim.Solver.Solver.RADIUS);
        
        if (dist > edge - 1e-4) {
            // Safe check to prevent division by zero
            float nX; 
            float nY;
            if (dist > 1e-6) {
                nX = xDelta / dist;
                nY = yDelta / dist;
            } else {
                // Default fallback pointing outwards
                nX = 0;
                nY = 0; 
            }
            posX[index] = cX + (nX * (radius - com.mmahlatji.flipwatersim.Solver.Solver.RADIUS));
            posY[index] = cY + (nY * (radius - com.mmahlatji.flipwatersim.Solver.Solver.RADIUS));

            float vDotN = (velX[index] * nX) + (velY[index] * nY);

            if (vDotN > 0) {
                velX[index] -= nX * (vDotN * 1.0f);
                velY[index] -= nY * (vDotN * 1.0f);
            }
        }
    }

    @Override
    public void markBoundary(Grid grid) {
        float cs =  grid.getCellSize();
        for (int i = 0; i < grid.getNumX(); i++) {
            for (int j = 0; j < grid.getNumY(); j++) {
                boolean isBorder = (i == 0 || j == 0 || i == grid.getNumX() - 1 || j == grid.getNumY() - 1);
                float cellCx = (float) ((i + 0.5) * cs - cX);
                float cellCy = (float) ((j + 0.5) * cs - cY);
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
            (int) (cX - radius),
            (int) (cX - radius),
            (int) radius * 2, 
            (int) radius * 2
            );
    }
    
}
