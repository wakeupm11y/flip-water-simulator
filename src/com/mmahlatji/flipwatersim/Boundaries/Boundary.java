package com.mmahlatji.flipwatersim.Boundaries;

import com.mmahlatji.flipwatersim.Solver.Grid;
import java.awt.Graphics;

public interface Boundary {

    public void apply(float[] posX, float[] posY, float[] velX, float[] velY, int index);

    public void paint(Graphics g);

    public void markBoundary(Grid grid);

}
