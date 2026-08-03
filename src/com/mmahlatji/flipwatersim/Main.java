package com.mmahlatji.flipwatersim;
import com.mmahlatji.flipwatersim.Boundaries.Boundary;
import com.mmahlatji.flipwatersim.Boundaries.BoxBoundary;
import com.mmahlatji.flipwatersim.Factory.ParticleFactory;
import com.mmahlatji.flipwatersim.Gui.SimulationPanel;
import com.mmahlatji.flipwatersim.Solver.Grid;
import com.mmahlatji.flipwatersim.Solver.Solver;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    

    public static void main(String[] args) {
        // 1. Define Boundary Box (in world units/meters)
        float minX = 0.0f;
        float minY = 0.0f;
        float maxX = 800f;
        float maxY = 600f;

        Boundary boundary = new BoxBoundary(minX, minY, maxX, maxY, Color.BLACK, 0.0f);

        // 2. Grid & Particle Setup
        int res = 100;
        float tankHeight = maxY - minY;
        float tankWidth = maxX - minX;

        // Create particles strictly inside the boundary box
        ParticleFactory.ParticleSetup setup = ParticleFactory.createParticles(
            tankWidth, 
            tankHeight, 
            res, 
            0.4f, // relWaterWidth (40% of box width)
            0.8f  // relWaterHeight (80% of box height)
        );
        System.out.printf("Generated %d particles", setup.numParticles);

        // 3. Offset Particle Coordinates by Boundary Origin (minX, minY)
        // This aligns particle position coordinates with the BoxBoundary's world space offset
        for (int i = 0; i < setup.numParticles; i++) {
            setup.posX[i] += minX;
            setup.posY[i] += minY;
        }

        // 4. Initialize Grid & Solver
        Grid grid = new Grid(setup.gridNumX, setup.gridNumY, setup.cellSize);

        Solver solver = new Solver(
            setup.numParticles,
            grid,
            boundary,
            setup.posX,
            setup.posY,
            1.9f,            // overrelaxation
            10,              // pressure iterations
            2,               // push-apart iterations
            1.0f / 60.0f     // timestep dt
        );

        // 4. Launch GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FLIP Water Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SimulationPanel panel = new SimulationPanel(solver,
                    minX, minY, maxX, maxY);
            frame.add(panel);

            // Stop simulation when window closes
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    panel.stop();
                }
            });

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Start the simulation thread
            panel.start();
        });
    }
}
