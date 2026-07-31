package com.mmahlatji.flipwatersim.Factory;


/**
 * Factory for generating particle positions using hexagonal packing.
 * Based on the setupScene() function from the original JavaScript implementation.
 */
public class ParticleFactory {

    /**
     * Configuration data produced by the factory.
     */
    public static class ParticleSetup {
        public final float[] posX;
        public final float[] posY;
        public final int numParticles;
        public final float particleRadius;
        public final float cellSize;
        public final int gridNumX;
        public final int gridNumY;

        public ParticleSetup(float[] posX, float[] posY, int numParticles,
                             float particleRadius, float cellSize,
                             int gridNumX, int gridNumY) {
            this.posX = posX;
            this.posY = posY;
            this.numParticles = numParticles;
            this.particleRadius = particleRadius;
            this.cellSize = cellSize;
            this.gridNumX = gridNumX;
            this.gridNumY = gridNumY;
        }
    }

    /**
     * Creates particles in a hexagonal arrangement within a specified water region.
     *
     * @param tankWidth           Total width of the simulation tank
     * @param tankHeight          Total height of the simulation tank
     * @param res                 Grid resolution (number of cells along the height; cellSize = tankHeight / res)
     * @param relWaterWidth       Fraction of tank width occupied by water (0..1)
     * @param relWaterHeight      Fraction of tank height occupied by water (0..1)
     * @param radiusMultiplier    Particle radius relative to cell size (typically 0.3)
     * @return A ParticleSetup object containing positions, particle radius, cell size, and grid dimensions
     */
    public static ParticleSetup createParticles(float tankWidth, float tankHeight,
                                                int res, float relWaterWidth, float relWaterHeight,
                                                float radiusMultiplier) {

        float h = tankHeight / res;                   // cell size
        float r = radiusMultiplier * h;               // particle radius
        float dx = 2.0f * r;                          // horizontal spacing (diameter)
        float dy = (float) (Math.sqrt(3.0) / 2.0 * dx); // vertical spacing for hexagonal packing

        // Compute number of particles that fit in the water region,
        // subtracting margins of 2*h + 2*r as in the original JS code.
        float availableWidth = relWaterWidth * tankWidth - 2.0f * h - 2.0f * r;
        float availableHeight = relWaterHeight * tankHeight - 2.0f * h - 2.0f * r;

        int numX = (int) Math.floor(availableWidth / dx);
        int numY = (int) Math.floor(availableHeight / dy);

        // Ensure at least one particle
        numX = Math.max(numX, 0);
        numY = Math.max(numY, 0);

        int numParticles = numX * numY;
        float[] posX = new float[numParticles];
        float[] posY = new float[numParticles];

        int idx = 0;
        for (int j = 0; j < numY; j++) {
            for (int i = 0; i < numX; i++) {
                // x: start at h+r, shift every other row by r (half diameter)
                float x = h + r + dx * i + (j % 2 == 0 ? 0.0f : r);
                // y: start at h+r, increase by dy per row
                float y = h + r + dy * j;
                posX[idx] = x;
                posY[idx] = y;
                idx++;
            }
        }

        // Grid dimensions for the whole tank (ceil to cover the tank)
        int gridNumX = (int) Math.ceil(tankWidth / h);
        int gridNumY = (int) Math.ceil(tankHeight / h);

        return new ParticleSetup(posX, posY, numParticles, r, h, gridNumX, gridNumY);
    }

    /**
     * Convenience method using the default radius multiplier (0.3).
     */
    public static ParticleSetup createParticles(float tankWidth, float tankHeight,
                                                int res, float relWaterWidth, float relWaterHeight) {
        return createParticles(tankWidth, tankHeight, res, relWaterWidth, relWaterHeight, 0.3f);
    }
}