package com.mmahlatji.flipwatersim.Gui;

import com.mmahlatji.flipwatersim.Boundaries.*;
import com.mmahlatji.flipwatersim.Solver.Solver;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.Font;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;



public class SimulationPanel extends JPanel implements Runnable {

    private final Solver solver;
    private final float minX, minY, maxX, maxY;
    private volatile boolean running = true;
    private int fps = 0;
    private int frameCount = 0;
    private long lastFpsUpdate = 0;
    private Thread simThread;
    private static final int TARGET_FPS = 60;

    public SimulationPanel(
        Solver solver,
        float minX,
        float minY,
        float maxX,
        float maxY
    ) {
        this.solver = solver;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
        setFocusable(true);

        // Optional: press SPACE to pause/resume (you can implement later)
        addKeyListener(new KeyAdapter() {});
    }

    public void start() {
        simThread = new Thread(this);
        simThread.start();
    }

    public void stop() {
        running = false;
        if (simThread != null) {
            try {
                simThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            long frameStart = System.nanoTime();

            // Advance simulation by one timestep
            solver.update();

            // Schedule repaint on the Event Dispatch Thread
            SwingUtilities.invokeLater(this::repaint);

            // Frame rate limiting
            long elapsed = System.nanoTime() - frameStart;
            long targetTime = 1_000_000_000L / TARGET_FPS;
            if (elapsed < targetTime) {
                try {
                    long sleepMs = (targetTime - elapsed) / 1_000_000;
                    if (sleepMs > 0) Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        float worldWidth = maxX - minX;
        float worldHeight = maxY - minY;

        // Compute scale to fit the world into the panel while keeping aspect ratio
        float scaleX = panelWidth / worldWidth;
        float scaleY = panelHeight / worldHeight;
        float scale = Math.min(scaleX, scaleY);

        // Offset to center the world in the panel
        float offsetX = (panelWidth - scale * worldWidth) / 2;
        float offsetY = (panelHeight - scale * worldHeight) / 2;

        // ---- Draw boundary box ----
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        int x0 = (int) (offsetX + (minX - minX) * scale);
        int y0 = (int) (offsetY + (minY - minY) * scale);
        int boxWidth = (int) (worldWidth * scale);
        int boxHeight = (int) (worldHeight * scale);
        g2.drawRect(x0, y0, boxWidth, boxHeight);

        // ---- Draw particles ----
        float radius = Solver.RADIUS;
        float[] posX = solver.getPositionX();
        float[] posY = solver.getPositionY();
        int numParticles = solver.getNumParticles();

        g2.setColor(Color.BLUE);
        for (int i = 0; i < numParticles; i++) {
            float px = posX[i];
            float py = posY[i];

            int screenX = (int) (offsetX + (px - minX) * scale);
            int screenY = (int) (offsetY + (py - minY) * scale);
            int screenR = Math.max(1, (int) (radius * scale));

            g2.fillOval(screenX - screenR, screenY - screenR,
                        2 * screenR, 2 * screenR);
        }

        // ---- Display FPS ----
        updateFPS();
        g2.setColor(Color.RED);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("FPS: " + fps, 10, 25);
    }

    private void updateFPS() {
        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastFpsUpdate > 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsUpdate = now;
        }
    }
}