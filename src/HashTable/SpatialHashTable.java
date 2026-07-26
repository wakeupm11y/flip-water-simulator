package HashTable;

import java.util.Arrays;
import Solver.*;
import Particles.Particle;

public class SpatialHashTable {
    
    private final float spacing;
    private final int tableSize;
    private final int[] cellStart;
    private final int[] cellEntries;
    private final int[] queryIds;
    private int querySize;
    
    public SpatialHashTable(float spacing, int maxObjects) {
        this.spacing = spacing;
        this.tableSize = 2*maxObjects;
        this.cellStart = new int[this.tableSize + 1];
        this.cellEntries = new int[maxObjects];
        this.queryIds = new int[Math.max(maxObjects, 64)];
        this.querySize = 0;
    }

    public int getQuerySize() {
        return querySize;
    }

    public int[] getQueryIds() {
        return queryIds;
    }

    /**
     * Computes a bucket index for the grid cell at integer coordinates
     * {@code (x, y)}.
     *
     * Uses large-prime spatial hashing to distribute cells uniformly:
     * 
     *   hash = |x * 92837111 XOR y * 689287499| mod numObjects
     * 
     * 
     *
     * @param x the integer grid x-coordinate
     * @param y the integer grid y-coordinate
     * @return a non-negative bucket index in {@code [0, numObjects)}
     */
    public int hashCoords(int x, int y) {
        int hash = (x * 92837111) ^ (y * 689287499);
        return Math.abs(hash) % tableSize;
    }


    public int intCoords(float coord) {
        return (int) Math.floor(coord / spacing);
    }

    public int hashPos(Vector2D pos) {
        return hashCoords(intCoords(pos.getX()), intCoords(pos.getY()));
    }

    public void create(Particle[] particles) {
        int numObjects = particles.length;

        // reset everything
        Arrays.fill(this.cellStart, 0);
        Arrays.fill(this.cellEntries, 0);

        // Count the particles per cell
        for (Particle particle: particles) {
            int hash = hashPos(particle.getPosition());
            this.cellStart[hash]++;
        }

        // prefix sum
        int start = 0;
        for (int i = 0; i < this.tableSize; i++) {
            start += this.cellStart[i];
            this.cellStart[i] = start;
        }
        this.cellStart[this.tableSize] = start;

        // do the mapping
        for (int i = 0; i < numObjects; i++) {
            int h = hashPos(particles[i].getPosition());
            this.cellStart[h]--;
            this.cellEntries[this.cellStart[h]] = i;
        }
    }

    public void query(Particle particle, int maxDist) {
        int x0 = intCoords(particle.getPosition().getX() - maxDist);
        int y0 = intCoords(particle.getPosition().getY() - maxDist);

        int x1 = intCoords(particle.getPosition().getX() + maxDist);
        int y1 = intCoords(particle.getPosition().getY() + maxDist);
        
        this.querySize = 0; // reset the number of queries

        for (int xi = x0; xi <= x1; xi++) {
            for (int yi = y0; yi <= y1; yi++) {
                int hash = hashCoords(xi, yi);
                int start = this.cellStart[hash];
                int end = this.cellStart[hash + 1];

                for (int i = start; i < end; i++) {
                    this.queryIds[this.querySize] = this.cellEntries[i];
                    this.querySize++;
                }
            }
        }
    } 
}
