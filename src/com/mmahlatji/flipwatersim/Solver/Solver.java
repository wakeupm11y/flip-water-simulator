package com.mmahlatji.flipwatersim.Solver;


import com.mmahlatji.flipwatersim.Boundaries.Boundary;
import com.mmahlatji.flipwatersim.HashTable.SpatialHashTable;
import com.mmahlatji.flipwatersim.Particles.Particle;

public class Solver {
    private static final Vector2D GRAVITY = new Vector2D(0, 9.81f * 50f);

    private final SpatialHashTable table;
    private final Boundary boundary;
    private final Particle[] particles;
    private final Grid grid;
    private final float timestep;
    private final float overrelaxation;
    private final int relaxationIterations;
    private final int pushApartIterations;

    private final float[] initX;
    private final float[] initY;

    public Solver(final Particle[] particles, final Grid grid, final Boundary boundary,
                     final float overrelaxation, final int relaxationIterations,
                     final int pushApartIterations, final float timestep) {
        this.particles = particles;
        this.grid      = grid;
        this.boundary  = boundary;
        this.overrelaxation = overrelaxation;
        this.relaxationIterations = relaxationIterations;
        this.pushApartIterations = pushApartIterations;
        this.timestep  = timestep;
        this.table = new SpatialHashTable(grid.getCellSize(), particles.length);

        initX = new float[particles.length];
        initY = new float[particles.length];
        for (int i = 0; i < particles.length; i++) {
            initX[i] = particles[i].getPosition().getX();
            initY[i] = particles[i].getPosition().getY();
        }
    }

    public void reset() {
        for (int i = 0; i < particles.length; i++) {
            particles[i].setPosition(new Vector2D(initX[i], initY[i]));
            particles[i].setVelocity(new Vector2D(0f, 0f));
        }
        grid.clearCurrent();
    }

    public void update() {

        for (Particle p : particles) {
            Vector2D vel = p.getVelocity().add(GRAVITY.scale(timestep));
            p.setVelocity(vel);
        }
        
        table.create(particles);

        pushParticlesApart(pushApartIterations);
        particlesToGrid();
        solvePressure(relaxationIterations);
        gridToParticle();

        for (Particle p : particles) {
            Vector2D pos = p.getPosition().add(p.getVelocity().scale( timestep));
            p.setPosition(pos);
            boundary.apply(p);
        }
    }

    private void particlesToGrid() {
        grid.clearCurrent();

        float cs = grid.getCellSize();
        boundary.markBoundary(grid);
        // 2. Mark fluid cells based on particle positions
        for (Particle p : particles) {
            float px = p.getPosition().getX();
            float py = p.getPosition().getY();
            int xi = (int) Math.floor(px / cs);
            int yi = (int) Math.floor(py / cs);
            // Clamp to valid cell indices
            xi = Math.clamp(xi, 0, grid.getNumX() - 1);
            yi = Math.clamp(yi, 0, grid.getNumY() - 1);
            int idx = grid.cellIndex(xi, yi);
            // Only set to FLUID if not SOLID (keep solid boundaries)
            if (grid.getCellType(idx) != Grid.SOLID) {
                grid.setCellType(idx, Grid.FLUID);
            }
        }

        // 3. Transfer velocities from particles to grid faces
        for (Particle p : particles) {
            float px = p.getPosition().getX();
            float py = p.getPosition().getY();
            float vx = p.getVelocity().getX();
            float vy = p.getVelocity().getY();

            // ---- Horizontal velocity (u) ----
            int x0 = (int) Math.floor(px / cs);
            int y0 = (int) Math.floor((py - 0.5 * cs) / cs);
            x0 = Math.clamp(x0, 0, grid.getNumX());
            y0 = Math.clamp(y0, 0, grid.getNumY() - 1);
            float dx = px - x0 * cs;
            float dy = (float)  ((py - 0.5 * cs) - y0 * cs);
            dx = Math.clamp(dx, 0, cs);
            dy = Math.clamp(dy, 0, cs);

            int x1 = x0 + 1;
            int y1 = y0 + 1;

            float w1 = (1 - dx / cs) * (1 - dy / cs);
            float w2 = (dx / cs) * (1 - dy / cs);
            float w3 = (dx / cs) * (dy / cs);
            float w4 = (1 - dx / cs) * (dy / cs);

            if (x0 >= 0 && x0 <= grid.getNumX() && y0 >= 0 && y0 < grid.getNumY()) {
                grid.accumH(grid.horizontalIndex(x0, y0), w1 * vx);
                grid.accumInfH(grid.horizontalIndex(x0, y0), w1);
            }
            if (x1 >= 0 && x1 <= grid.getNumX() && y0 >= 0 && y0 < grid.getNumY()) {
                grid.accumH(grid.horizontalIndex(x1, y0), w2 * vx);
                grid.accumInfH(grid.horizontalIndex(x1, y0), w2);
            }
            if (x1 >= 0 && x1 <= grid.getNumX() && y1 >= 0 && y1 < grid.getNumY()) {
                grid.accumH(grid.horizontalIndex(x1, y1), w3 * vx);
                grid.accumInfH(grid.horizontalIndex(x1, y1), w3);
            }
            if (x0 >= 0 && x0 <= grid.getNumX() && y1 >= 0 && y1 < grid.getNumY()) {
                grid.accumH(grid.horizontalIndex(x0, y1), w4 * vx);
                grid.accumInfH(grid.horizontalIndex(x0, y1), w4);
            }

            // ---- Vertical velocity (v) ----
            x0 = (int) Math.floor((px - 0.5 * cs) / cs);
            y0 = (int) Math.floor(py / cs);
            x0 = Math.clamp(x0, 0, grid.getNumX() - 1);
            y0 = Math.clamp(y0, 0, grid.getNumY());
            dx = (float) ((px - 0.5 * cs) - x0 * cs);
            dy = py - y0 * cs;
            dx = Math.clamp(dx, 0, cs);
            dy = Math.clamp(dy, 0, cs);

            x1 = x0 + 1;
            y1 = y0 + 1;

            w1 = (1 - dx / cs) * (1 - dy / cs);
            w2 = (dx / cs) * (1 - dy / cs);
            w3 = (dx / cs) * (dy / cs);
            w4 = (1 - dx / cs) * (dy / cs);

            if (x0 >= 0 && x0 < grid.getNumX() && y0 >= 0 && y0 <= grid.getNumY()) {
                grid.accumV(grid.verticalIndex(x0, y0), w1 * vy);
                grid.accumInfV(grid.verticalIndex(x0, y0), w1);
            }
            if (x1 >= 0 && x1 < grid.getNumX() && y0 >= 0 && y0 <= grid.getNumY()) {
                grid.accumV(grid.verticalIndex(x1, y0), w2 * vy);
                grid.accumInfV(grid.verticalIndex(x1, y0), w2);
            }
            if (x1 >= 0 && x1 < grid.getNumX() && y1 >= 0 && y1 <= grid.getNumY()) {
                grid.accumV(grid.verticalIndex(x1, y1), w3 * vy);
                grid.accumInfV(grid.verticalIndex(x1, y1), w3);
            }
            if (x0 >= 0 && x0 < grid.getNumX() && y1 >= 0 && y1 <= grid.getNumY()) {
                grid.accumV(grid.verticalIndex(x0, y1), w4 * vy);
                grid.accumInfV(grid.verticalIndex(x0, y1), w4);
            }
        }

        grid.normalise();
        grid.saveToOld();
    }

    private void solvePressure(int iterations) {
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 1; i < grid.getNumX() - 1; i++) {
                for (int j = 1; j < grid.getNumY() - 1; j++) {
                    int type = grid.getCellType(grid.cellIndex(i, j));
                    if (type == Grid.AIR || type == Grid.SOLID) continue;

                    boolean leftSolid   = grid.getCellType(grid.cellIndex(i - 1, j)) == Grid.SOLID;
                    boolean rightSolid  = grid.getCellType(grid.cellIndex(i + 1, j)) == Grid.SOLID;
                    boolean topSolid    = grid.getCellType(grid.cellIndex(i, j - 1)) == Grid.SOLID;
                    boolean bottomSolid = grid.getCellType(grid.cellIndex(i, j + 1)) == Grid.SOLID;

                    int sLeft   = leftSolid   ? 0 : 1;
                    int sRight  = rightSolid  ? 0 : 1;
                    int sTop    = topSolid    ? 0 : 1;
                    int sBottom = bottomSolid ? 0 : 1;
                    int sSum = sLeft + sRight + sTop + sBottom;
                    if (sSum == 0) continue;

                    int idxLeft   = grid.horizontalIndex(i, j);
                    int idxRight  = grid.horizontalIndex(i + 1, j);
                    int idxTop    = grid.verticalIndex(i, j);
                    int idxBottom = grid.verticalIndex(i, j + 1);

                    // Enforce zero velocity at solid walls
                    if (leftSolid)   grid.setHorizontal(idxLeft, 0.0f);
                    if (rightSolid)  grid.setHorizontal(idxRight, 0.0f);
                    if (topSolid)    grid.setVertical(idxTop, 0.0f);
                    if (bottomSolid) grid.setVertical(idxBottom, 0.0f);

                    float vRight = grid.getHorizontal(idxRight);
                    float vLeft  = grid.getHorizontal(idxLeft);
                    float vBot   = grid.getVertical(idxBottom);
                    float vTop   = grid.getVertical(idxTop);

                    float divergence = (vRight - vLeft) + (vBot - vTop);
                    divergence *= overrelaxation;
                    float d = divergence / sSum;

                    // Correct signs: reduce divergence
                    if (!leftSolid)   grid.accumH(idxLeft, d);
                    if (!rightSolid)  grid.accumH(idxRight, -d);
                    if (!topSolid)    grid.accumV(idxTop, d);
                    if (!bottomSolid) grid.accumV(idxBottom, -d);
                }
            }
        }
    }

    private void gridToParticle() {
        float cs = grid.getCellSize();
        for (Particle p : particles) {
            float px = p.getPosition().getX();
            float py = p.getPosition().getY();

            // ---- Horizontal interpolation ----
            int x0 = (int) Math.floor(px / cs);
            int y0 = (int) Math.floor((py - 0.5 * cs) / cs);
            x0 = Math.clamp(x0, 0, grid.getNumX());
            y0 = Math.clamp(y0, 0, grid.getNumY() - 1);
            float dx = px - x0 * cs;
            float dy = (py - 0.5f * cs) - y0 * cs;
            dx = Math.clamp(dx, 0, cs);
            dy = Math.clamp(dy, 0, cs);

            int x1 = x0 + 1;
            int y1 = y0 + 1;

            float w1 = (1 - dx / cs) * (1 - dy / cs);
            float w2 = (dx / cs) * (1 - dy / cs);
            float w3 = (dx / cs) * (dy / cs);
            float w4 = (1 - dx / cs) * (dy / cs);

            float sumW = 0;
            float newVx = 0;
            if (x0 >= 0 && x0 <= grid.getNumX() && y0 >= 0 && y0 < grid.getNumY()) {
                newVx += w1 * grid.getHorizontal(grid.horizontalIndex(x0, y0));
                sumW += w1;
            }
            if (x1 >= 0 && x1 <= grid.getNumX() && y0 >= 0 && y0 < grid.getNumY()) {
                newVx += w2 * grid.getHorizontal(grid.horizontalIndex(x1, y0));
                sumW += w2;
            }
            if (x1 >= 0 && x1 <= grid.getNumX() && y1 >= 0 && y1 < grid.getNumY()) {
                newVx += w3 * grid.getHorizontal(grid.horizontalIndex(x1, y1));
                sumW += w3;
            }
            if (x0 >= 0 && x0 <= grid.getNumX() && y1 >= 0 && y1 < grid.getNumY()) {
                newVx += w4 * grid.getHorizontal(grid.horizontalIndex(x0, y1));
                sumW += w4;
            }
            if (sumW > 1e-12) newVx /= sumW;

            // ---- Vertical interpolation ----
            x0 = (int) Math.floor((px - 0.5 * cs) / cs);
            y0 = (int) Math.floor(py / cs);
            x0 = Math.clamp(x0, 0, grid.getNumX() - 1);
            y0 = Math.clamp(y0, 0, grid.getNumY());
            dx = (px - 0.5f * cs) - x0 * cs;
            dy = py - y0 * cs;
            dx = Math.clamp(dx, 0, cs);
            dy = Math.clamp(dy, 0, cs);

            x1 = x0 + 1;
            y1 = y0 + 1;

            w1 = (1 - dx / cs) * (1 - dy / cs);
            w2 = (dx / cs) * (1 - dy / cs);
            w3 = (dx / cs) * (dy / cs);
            w4 = (1 - dx / cs) * (dy / cs);

            sumW = 0;
            float newVy = 0;
            if (x0 >= 0 && x0 < grid.getNumX() && y0 >= 0 && y0 <= grid.getNumY()) {
                newVy += w1 * grid.getVertical(grid.verticalIndex(x0, y0));
                sumW += w1;
            }
            if (x1 >= 0 && x1 < grid.getNumX() && y0 >= 0 && y0 <= grid.getNumY()) {
                newVy += w2 * grid.getVertical(grid.verticalIndex(x1, y0));
                sumW += w2;
            }
            if (x1 >= 0 && x1 < grid.getNumX() && y1 >= 0 && y1 <= grid.getNumY()) {
                newVy += w3 * grid.getVertical(grid.verticalIndex(x1, y1));
                sumW += w3;
            }
            if (x0 >= 0 && x0 < grid.getNumX() && y1 >= 0 && y1 <= grid.getNumY()) {
                newVy += w4 * grid.getVertical(grid.verticalIndex(x0, y1));
                sumW += w4;
            }
            if (sumW > 1e-12) newVy /= sumW;

            p.setVelocity(new Vector2D(newVx,  newVy));
        }
    }

    private void pushParticlesApart(int iterations) {
        float minDist = (float) particles[0].getRadius() * 2; 

        for (int z = 0; z < iterations; z++) {
            for (int i = 0; i < particles.length; i++) {
                Particle outerParticle = particles[i];
                table.query(outerParticle, (int) (minDist + 0.01f));

                int[] queries = table.getQueryIds();
                for (int q = 0; q < table.getQuerySize(); q++) {
                    int j = queries[q];

                    if (i >= j) continue;

                    Particle innerParticle = particles[j];
                    
                    float distance = outerParticle.getPosition().distance(innerParticle.getPosition());

                    if (distance >= minDist || distance == 0) continue;

                    if (distance < minDist) {
                        double scaleFactor = (0.5 * (minDist - distance))/ distance;
                        float correctionFactor = (float) (distance * scaleFactor);
                        Vector2D corrected = (outerParticle.getPosition().sub(innerParticle.getPosition()).scale(correctionFactor));

                        outerParticle.setPosition(outerParticle.getPosition().sub(corrected));
                        innerParticle.setPosition(innerParticle.getPosition().add(corrected));
                    }   
                }
            }
        }
    }













}
