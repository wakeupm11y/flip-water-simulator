package com.mmahlatji.flipwatersim.Solver;


import java.util.Arrays;

public class Grid {

    private final int numX;
    private final int numY;
    private final float invCellSize;
    private final float cellSize;

    private final float[] h; // horizontal velocity
    private final float[] v; // vertical velocity


    private final float[] oldH;
    private final float[] oldV;

    private final float[] weightH;
    private final float[] weightV;


    private final int[] cellType;

    public static final int AIR = 0;
    public static final int FLUID = 1;
    public static final int SOLID = 2;

    public Grid(final int numX, final int numY, float cellSize) {
        this.numX = numX;
        this.numY = numY;

        this.h = new float[(numX + 1) * numY];
        this.v = new float[numX * (numY + 1)];

        this.oldH = new float[(numX + 1) * numY];
        this.oldV = new float[numX * (numY + 1)];

        this.weightH = new float[(numX + 1) * numY];
        this.weightV = new float[numX * (numY + 1)];

        this.cellType = new int[numX * numY];
        this.invCellSize = 1.0f/cellSize;
        this.cellSize = cellSize;


    }

    public float getInverseCellSize() {
        return invCellSize;
    }

    public float getCellSize() {
        return cellSize;
    }

    public int size() {
        return numX;
    }

    public int getNumX() {
        return numX;
    }

    public int getNumY() {
        return numY;
    }

    public int getHorizontalLength() {
        return h.length;
    }

     public int getVerticalLength() {
        return v.length;
    }
    public int cellIndex(int x, int y) {
        return x * numY + y;
    }
    
    public void accumV(int index, float value) {
        this.v[index] += value;
    }

    public void accumH(int index, float value) {
        this.h[index] += value;
    }

    public void accumInfH(int index, float value) {
        this.weightH[index] += value;
    }

    public void accumInfV(int index, float value) {
        this.weightV[index] += value;
    }


    public int horizontalIndex(int x, int y) {
        // Clamp to make sure it stays bounded
        x = Math.max(0, Math.min(x, numX));
        y = Math.max(0, Math.min(y, numY - 1));
        return x * numY + y; 
    }

    public int verticalIndex(int x, int y) {
        x = Math.max(0, Math.min(x, numX - 1));
        y = Math.max(0, Math.min(y, numY));
        return x * (numY + 1) + y;
    }

    public float getVertical(int index) {
        return v[index];
    }

    public float getHorizontal(int index) {
        return h[index];
    }

    public void setVertical(int index, float value) {
        this.v[index] = value;
    }

    public void setHorizontal(int index, float value) {
        this.h[index] = value;
    }


    public int getCellType(int index) {
        return cellType[index];
    }

    public void setCellType(int index, int type) {
        cellType[index] = type;
    }

    public void normalise() {
        final float EPSILON = 1e-9f;
        
        for (int i = 0; i < h.length; i++) {
            if (weightH[i] > EPSILON) {
                h[i] = h[i] / weightH[i];
            } else {
                h[i] = 0.0f; // Clean baseline reset for unvisited faces
            }
        }
        for (int i = 0; i < v.length; i++) {
            if (weightV[i] > EPSILON) {
                v[i] = v[i] / weightV[i];
            } else {
                v[i] = 0.0f; // Clean baseline reset for unvisited faces
            }
        }
    }

    public void saveToOld() {
        System.arraycopy(h, 0, oldH, 0, h.length);
        System.arraycopy(v, 0, oldV, 0, v.length);
    }

    public void clearCurrent() {
        Arrays.fill(h, 0);
        Arrays.fill(v, 0);

        Arrays.fill(weightH, 0);
        Arrays.fill(weightV, 0);

        Arrays.fill(cellType, 0);

    }



    
}
