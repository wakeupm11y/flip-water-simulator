package com.mmahlatji.flipwatersim.Boundaries;

import com.mmahlatji.flipwatersim.Solver.Grid;
import com.mmahlatji.flipwatersim.Particles.Particle;
import java.awt.Graphics;

public interface Boundary {

    public void apply(Particle particle);

    public void paint(Graphics g);

    public void markBoundary(Grid grid);

}
