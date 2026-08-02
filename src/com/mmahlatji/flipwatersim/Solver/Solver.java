package com.mmahlatji.flipwatersim.Solver;

import com.mmahlatji.flipwatersim.Boundaries.Boundary;
import com.mmahlatji.flipwatersim.HashTable.SpatialHashTable;
import java.util.Arrays;

public class Solver {
    // private static final Vector2D GRAVITY = new Vector2D(0, 9.81f * 50f);
    private static final float GRAVITY = 9.81f * 50f;
    public static final float RADIUS = 1f; // make final for testing purposes, consider making it configurable
    private static final float MASS = 1f;

    private final SpatialHashTable table;
    private final Boundary boundary;
    // private final Particle[] particles;
    private final float[] velocityX;
    private final float[] velocityY;
    private final float[] positionX;
    private final float[] positionY;
    // private final float[] mass;
    // private final float[] radius;  // we probably do not need this because we can just make it a constant for each particle
    private final Grid grid;
    private final float timestep;
    private final float overrelaxation;
    private final int relaxationIterations;
    private final int pushApartIterations;
    private final int numParticles;

    private final float[] initX;
    private final float[] initY;

    public Solver(
        final int numParticles, 
        final Grid grid, 
        final Boundary boundary,
        final float[] positionX,
        final float[] positionY,
        final float overrelaxation, 
        final int relaxationIterations,
        final int pushApartIterations, 
        final float timestep
    ) {


        // this.particles = particles;
        this.grid      = grid;
        this.numParticles = numParticles;
        this.boundary  = boundary;
        this.overrelaxation = overrelaxation;
        this.relaxationIterations = relaxationIterations;
        this.pushApartIterations = pushApartIterations;
        this.timestep  = timestep;
        this.velocityX = new float[numParticles];
        this.velocityY = new float[numParticles];
        this.positionX = positionX;
        this.positionY = positionY;

        this.table = new SpatialHashTable(grid.getCellSize(), numParticles);

        initX = new float[numParticles];
        initY = new float[numParticles];
        for (int i = 0; i < numParticles; i++) {
            initX[i] = positionX[i];
            initY[i] = positionY[i];
        }
    }

    

    public void reset() {
        for (int i = 0; i < numParticles; i++) {
            positionX[i] = initX[i];
            positionY[i] = initY[i];
        }
        Arrays.fill(velocityX, 0);
        Arrays.fill(velocityY, 0);
        grid.clearCurrent();
    }

    public void update() {

        // for (Particle p : particles) {
        //     Vector2D vel = p.getVelocity().add(GRAVITY.scale(timestep));
        //     p.setVelocity(vel);
        // }

        for (int i = 0; i < numParticles; i++) {
            velocityY[i] += GRAVITY * timestep;   
        }
        
        table.create(positionX, positionY);

        pushParticlesApart(pushApartIterations);
        particlesToGrid();
        solvePressure(relaxationIterations);
        gridToParticle();

        // for (Particle p : particles) {
        //     Vector2D pos = p.getPosition().add(p.getVelocity().scale( timestep));
        //     p.setPosition(pos);
        //     boundary.apply(p); // modify this so that it accepts coordinates, not a vector
        // }
        for (int i = 0; i < numParticles; i++) {
            positionX[i] += velocityX[i] * timestep;
            positionY[i] += velocityY[i] * timestep;
            boundary.apply(positionX, positionY, velocityX, velocityY, i);
        }
    }

    private void particlesToGrid() {
        grid.clearCurrent();

        float cs = grid.getCellSize();
        boundary.markBoundary(grid);
        // 2. Mark fluid cells based on particle positions
        for (int i = 0; i < numParticles; i++) {
            float px = positionX[i];
            float py = positionY[i];
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
        for (int i = 0; i < numParticles; i++) {
            float px = positionX[i];
            float py = positionY[i];
            float vx = velocityX[i];
            float vy = velocityY[i];

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
            for (int i = 0; i < grid.getNumX() - 1; i++) {
                for (int j = 0; j < grid.getNumY() - 1; j++) {
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
        for (int i = 0; i < numParticles; i++) {
            float px = positionX[i];
            float py = positionY[i];

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
            velocityX[i] = newVx;
            velocityY[i] = newVy;
        }
    }

    private void pushParticlesApart(int iterations) {
        float minDist = RADIUS * 2f; 

        for (int z = 0; z < iterations; z++) {
            for (int i = 0; i < numParticles; i++) {
                // outer particle
                float pXi = positionX[i];
                float pYi = positionY[i];
                table.query(pXi, pYi, (int) (minDist + 0.01f));

                int[] queries = table.getQueryIds();
                for (int q = 0; q < table.getQuerySize(); q++) {
                    int j = queries[q];

                    if (i >= j) continue;
                    // inner particle
                    float pXj = positionX[j];
                    float pYj = positionY[j];
                    
                    float distance = (float) Math.sqrt(Math.pow(pXj - pXi, 2) + Math.pow(pYj - pYi, 2));

                    if (distance >= minDist || distance == 0) continue;

                    if (distance < minDist) {
                        float dx = pXi - pXj;
                        float dy = pYi - pYj;
                        float delta = 0.5f * (minDist - distance) / distance;
                        float correctX = dx * delta;
                        float correctY = dy * delta;

                        positionX[i] += correctX;
                        positionY[i] += correctY;
                        positionX[j] -= correctX;
                        positionY[j] -= correctY;
                    }   
                }
            }
        }
    }

    public float[] getPositionX() {
    return positionX;
    }

    public float[] getPositionY() {
        return positionY;
    }

    public int getNumParticles() {
        return numParticles;
    }
}
