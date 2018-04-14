package edu.cg;

public class SeamData {
    private final int originalImageX;
    private final int localReducedImageX;

    public SeamData(int localReducedImageX, int originalImageX) {
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
