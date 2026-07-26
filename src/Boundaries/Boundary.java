package Boundaries;

import Solver.Grid;
import Particles.Particle;
import java.awt.Graphics;

public interface Boundary {

    public void apply(Particle particle);

    public void paint(Graphics g);

    public void markBoundary(Grid grid);

}
