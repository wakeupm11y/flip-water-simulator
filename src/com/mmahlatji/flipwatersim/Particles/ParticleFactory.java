package com.mmahlatji.flipwatersim.Particles;

import com.mmahlatji.flipwatersim.Solver.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates Particle arrays packed into a quadrilateral region.
 *
 * Strategy: hexagonal close-packing (HCP) — the densest possible 2-D
 * arrangement, achieving a theoretical packing fraction of π/(2√3) ≈ 0.9069.
 * Odd rows are offset by half a column pitch so each particle nestles into
 * the gap between the two particles on the row below it.
 *
 * Particles are placed row by row from bottom-left to top-right and clipped
 * to the axis-aligned bounding box [{@code minX}, {@code maxX}] ×
 * [{@code minY}, {@code maxY}].  If {@code count} particles fit before the
 * region is full, placement stops early; if the region fills before
 * {@code count} is reached a warning is printed.
 */
public final class ParticleFactory {

    private ParticleFactory() {}

    /**
     * Place {@code count} particles in a tight hexagonal close-packed grid
     * inside the rectangle [{@code minX}, {@code maxX}] × [{@code minY}, {@code maxY}].
     *
     * @param count    desired particle count
     * @param radius   particle radius
     * @param mass     particle mass
     * @param minX     left boundary of spawn region
     * @param minY     bottom boundary of spawn region
     * @param maxX     right boundary of spawn region
     * @param maxY     top boundary of spawn region
     * @return array of placed, non-overlapping Particle objects in HCP layout
     */
    public static Particle[] createNonOverlapping(
            int    count,
            float radius,
            float mass,
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
        // ── HCP geometry ──────────────────────────────────────────────────────
        // Diameter between touching particle centres on the same row.
        final float colStep = radius * 2.0f;
        // Row pitch for HCP: centres are √3 × r apart vertically so that
        // each particle sits exactly in the triangular gap of the row below.
        final float rowStep = (float) Math.sqrt(3.0) * radius;
        // Horizontal offset applied to every odd-numbered row.
        final float oddRowOffset = radius;

        List<float[]> accepted = new ArrayList<>();

        int row = 0;
        for (float cy = minY + radius; cy <= maxY - radius; cy += rowStep, row++) {
            float xStart = (float) (minX + radius + (row % 2 == 1 ? oddRowOffset : 0.0));
            for (float cx = xStart; cx <= maxX - radius; cx += colStep) {
                if (accepted.size() >= count) break;
                accepted.add(new float[]{ cx, cy });
            }
            if (accepted.size() >= count) break;
        }

        int placed = accepted.size();
        if (placed < count) {
            System.out.printf(
                "[ParticleFactory] Warning: only placed %d / %d particles " +
                "(region too small or radius too large)%n", placed, count);
        }

        // ── assemble Particle array ───────────────────────────────────────────
        Particle[] particles = new Particle[placed];
        for (int i = 0; i < placed; i++) {
            float[] pos = accepted.get(i);
            particles[i] = new Particle(
                new Vector2D(pos[0], pos[1]),
                new Vector2D(0, 0),
                radius,
                mass
            );
        }
        return particles;
    }

        public static Particle[] createInCircle(
            int    count,
            float radius,
            float mass,
            float centerX,
            float centerY,
            float spawnRadius
    ) {
        final float colStep      = radius * 2.0f;
        final float rowStep      = (float) Math.sqrt(3.0) * radius;
        final float oddRowOffset = radius;
 
        // Bounding box of the spawn circle
        float minX = centerX - spawnRadius;
        float maxX = centerX + spawnRadius;
        float minY = centerY - spawnRadius;
        float maxY = centerY + spawnRadius;
 
        // Inset by one radius so particle edges don't touch the spawn boundary
        float safeR = spawnRadius - radius;
 
        List<float[]> accepted = new ArrayList<>();
 
        int row = 0;
        for (float cy = minY + radius; cy <= maxY - radius; cy += rowStep, row++) {
            float xStart = (float) (minX + radius + (row % 2 == 1 ? oddRowOffset : 0.0));
            for (float cx = xStart; cx <= maxX - radius; cx += colStep) {
                if (accepted.size() >= count) break;
                // Clip to circle: only accept if centre is within safeR of spawn centre
                float dx = cx - centerX;
                float dy = cy - centerY;
                if (dx * dx + dy * dy <= safeR * safeR) {
                    accepted.add(new float[]{ cx, cy });
                }
            }
            if (accepted.size() >= count) break;
        }
 
        int placed = accepted.size();
        if (placed < count) {
            System.out.printf(
                "[ParticleFactory] Warning: only placed %d / %d particles " +
                "(spawn circle too small or radius too large)%n", placed, count);
        }
 
        Particle[] particles = new Particle[placed];
        for (int i = 0; i < placed; i++) {
            float[] pos = accepted.get(i);
            particles[i] = new Particle(
                new Vector2D(pos[0], pos[1]),
                new Vector2D(0, 0),
                radius,
                mass
            );
        }
        return particles;
    }

}