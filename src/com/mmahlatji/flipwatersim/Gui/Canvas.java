package com.mmahlatji.flipwatersim.Gui;

import com.mmahlatji.flipwatersim.Solver.Solver;
import com.mmahlatji.flipwatersim.Solver.Vector2D;
import com.mmahlatji.flipwatersim.Solver.Grid;
import com.mmahlatji.flipwatersim.Particles.Particle;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import javax.swing.*;



public class Canvas extends JPanel {

    private static final Color BACKGROUND     = new Color(10, 12, 20);
    private static final Color PARTICLE_COLOR = new Color(100, 180, 255);
    private static final Color PARTICLE_GLOW  = new Color(100, 180, 255, 60);
    private static final Color BOUNDARY_COLOR = new Color(40, 50, 70);
    private static final Color GRID_COLOR     = new Color(25, 30, 45);
    private static final Color ATTRACTOR_COLOR = new Color(255, 100, 100, 80);
    private static final Color ATTRACTOR_GLOW  = new Color(255, 100, 100, 30);

    private final Solver solver;
    private final Grid grid;
    private final Particle[] particles;

    private boolean showGrid = false;
    private boolean isAttracting = false;
    private float attractorX = 0;
    private float attractorY = 0;

    public Canvas(Solver solver, Grid grid, Particle[] particles) {
        this.solver    = solver;
        this.grid      = grid;
        this.particles = particles;
        setBackground(BACKGROUND);
        setDoubleBuffered(true);
        
        // setupMouseListeners();
    }


    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // fill background
        g2.setColor(BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (showGrid) {
            drawGrid(g2);
        }

        drawParticles(g2);
        
        // Draw attractor if active
        if (isAttracting) {
            drawAttractor(g2);
        }
    }

    private void drawAttractor(Graphics2D g2) {
        // Outer glow
        g2.setColor(ATTRACTOR_GLOW);
        g2.fill(new Ellipse2D.Double(attractorX - 60, attractorY - 60, 120, 120));
        
        // Middle ring
        g2.setColor(ATTRACTOR_COLOR);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Ellipse2D.Double(attractorX - 30, attractorY - 30, 60, 60));
        
        // Inner dot
        g2.setColor(new Color(255, 150, 150));
        g2.fill(new Ellipse2D.Double(attractorX - 4, attractorY - 4, 8, 8));
        
        // Crosshair lines
        g2.setColor(new Color(255, 100, 100, 100));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine((int) (attractorX - 15),(int) attractorY, (int) (attractorX + 15),(int)  attractorY);
        g2.drawLine((int) attractorX, (int) (attractorY - 15), (int) attractorX,(int) (attractorY + 15));
    }

    private void drawGrid(Graphics2D g2) {
        double cs  = grid.getCellSize();
        int    nx  = grid.getNumX();
        int    ny  = grid.getNumY();

        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(0.5f));

        for (int x = 0; x <= nx; x++) {
            int px = (int) (x * cs);
            g2.drawLine(px, 0, px, (int) (ny * cs));
        }
        for (int y = 0; y <= ny; y++) {
            int py = (int) (y * cs);
            g2.drawLine(0, py, (int) (nx * cs), py);
        }
    }

    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            Vector2D pos = p.getPosition();
            double   r   = p.getRadius();
            double   x   = pos.getX() - r;
            double   y   = pos.getY() - r;
            double   d   = r * 2;

            // soft glow
            g2.setColor(PARTICLE_GLOW);
            g2.fill(new Ellipse2D.Double(x - r * 0.8, y - r * 0.8, d + r * 1.6, d + r * 1.6));

            // core
            g2.setColor(PARTICLE_COLOR);
            g2.fill(new Ellipse2D.Double(x, y, d, d));
        }
    }
}