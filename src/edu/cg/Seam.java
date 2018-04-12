package edu.cg;

public class Seam {
    private final int originalImageX;
    private final int localReducedImageX;

    public Seam(int localReducedImageX, int originalImageX) {
        this.localReducedImageX = localReducedImageX;
        this.originalImageX = originalImageX;
    }

    public int getOriginalImageX() {
        return originalImageX;
    }

    public int getLocalReducedImageX() {
        return localReducedImageX;
    }
}
