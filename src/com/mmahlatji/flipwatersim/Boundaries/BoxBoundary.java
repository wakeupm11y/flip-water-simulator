package com.mmahlatji.flipwatersim.Boundaries;

import com.mmahlatji.flipwatersim.Solver.Grid;
import com.mmahlatji.flipwatersim.Solver.Vector2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class BoxBoundary implements Boundary {
    
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;
    private final Color color;
    private final float restitution; // Bounce factor (0 = no bounce, 1 = perfect bounce)
    
    /**
     * Creates a rectangular boundary.
     * 
     * @param minX Left edge of the box
     * @param minY Bottom edge of the box
     * @param maxX Right edge of the box
     * @param maxY Top edge of the box
     * @param color Color to draw the boundary
     */
    public BoxBoundary(float minX, float minY, float maxX, float maxY, Color color) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.color = color;
        this.restitution = 0.8f; // Default bounce factor
    }
    
    /**
     * Creates a rectangular boundary with custom restitution.
     * 
     * @param minX Left edge of the box
     * @param minY Bottom edge of the box
     * @param maxX Right edge of the box
     * @param maxY Top edge of the box
     * @param color Color to draw the boundary
     * @param restitution Bounce factor (0 = no bounce, 1 = perfect bounce)
     */
    public BoxBoundary(float minX, float minY, float maxX, float maxY, Color color, float restitution) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.color = color;
        this.restitution = Math.max(0, Math.min(1, restitution)); // Clamp between 0 and 1
    }
    
    @Override
    public void apply(float[] posX, float[] posY, float[] velX, float[] velY, int index) {
        float pX = posX[index];
        float pY = posY[index];
        float vX = velX[index];
        float vY = velY[index];
        float radius = com.mmahlatji.flipwatersim.Solver.Solver.RADIUS;
        
      
        
        boolean bounced = false;
        
        // Left wall
        if (pX - radius < minX) {
            pX = minX + radius;
            vX = -vX * restitution;
            bounced = true;
        }
        
        // Right wall
        if (pX + radius > maxX) {
            pX = maxX - radius;
            vX= -vX * restitution;
            bounced = true;
        }
        
        // Bottom wall
        if (pY - radius < minY) {
            pY= minY + radius;
            vY = -vY * restitution;
            bounced = true;
        }
        
        // Top wall
        if (pY + radius > maxY) {
            pY = maxY - radius;
            vY = -vY * restitution;
            bounced = true;
        }
        
        if (bounced) {
            // Apply slight damping to prevent infinite bouncing
            if (Math.abs(vX) < 0.01f) vX = 0;
            if (Math.abs(vY) < 0.01f) vY = 0;

            posX[index] = pX;
            posY[index] = pY;
            velX[index] = vX;
            velY[index] = vY;
            
        }
    }

    @Override
    public void markBoundary(Grid grid) {

        float cs = grid.getCellSize();
        int maxX0 = grid.getNumX();
        int maxY0 = grid.getNumY();
        // which cells would the boundary map to 
        int x0 = (int) Math.floor(minX / cs);
        int y0 = (int) Math.floor(minY / cs);
        int x1 = (int) Math.floor(maxX / cs);
        int y1 = (int) Math.floor(maxY / cs);
        // clamp to avoid out of bounds errors
        x0 = Math.max(0, Math.min(x0, maxX0 - 1));
        y0 = Math.max(0, Math.min(y0, maxY0 - 1));
        x1 = Math.max(0, Math.min(x1, maxX0 - 1));
        y1 = Math.max(0, Math.min(y1, maxY0 - 1));

        // mark all the cells as solid
        for (int i = 0; i < maxX0; i++) {
            for (int j = 0; j < maxY0; j++) {
                grid.setCellType(grid.cellIndex(i, j), Grid.SOLID);
            }
        }

        for (int i = x0; i <= x1; i++) {
            for (int j = y0; j <= y1; j++) {
                // Only mark if within grid bounds and not on outer border
                if (i > 0 && i < maxX0 - 1 && j > 0 && j < maxY0 - 1) {
                    grid.setCellType(grid.cellIndex(i, j), Grid.AIR);
                }
            }
        }
    }
    
    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        // Save the original color and stroke
        Color originalColor = g2.getColor();
        java.awt.Stroke originalStroke = g2.getStroke();
        
        // Draw the boundary box
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.0f));
        
        int x = (int) minX;
        int y = (int) minY;
        int width = (int) (maxX - minX);
        int height = (int) (maxY - minY);
        
        g2.drawRect(x, y, width, height);
        
        // Optional: Add a subtle glow effect
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        g2.setStroke(new BasicStroke(6.0f));
        g2.drawRect(x - 2, y - 2, width + 4, height + 4);
        
        // Restore original color and stroke
        g2.setColor(originalColor);
        g2.setStroke(originalStroke);
    }
    
    // Getters for the boundary properties
    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getWidth() { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getCenterX() { return (minX + maxX) / 2; }
    public float getCenterY() { return (minY + maxY) / 2; }
    public Vector2D getCenter() { return new Vector2D(getCenterX(), getCenterY()); }
    public float getRestitution() { return restitution; }
    
    /**
     * Checks if a point is inside the boundary.
     * 
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @return true if the point is inside the boundary
     */
    public boolean boundaryContainsPoint(float x, float y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
    
    /**
     * Checks if a particle is inside the boundary.
     * 
     * @param particle The particle to check
     * @return true if the particle is inside the boundary
     */
    public boolean boundaryContainsParticle(float x, float y) {
        float radius = com.mmahlatji.flipwatersim.Solver.Solver.RADIUS;
        return x - radius >= minX && 
               x + radius <= maxX &&
               y - radius >= minY && 
               y+ radius <= maxY;
    }
}