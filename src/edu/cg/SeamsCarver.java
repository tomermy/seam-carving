package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SeamsCarver extends ImageProcessor {

    //MARK: An inner interface for functional programming.
    @FunctionalInterface
    interface ResizeOperation {
        BufferedImage apply();
    }

    //MARK: Fields
    private int numOfSeams;
    private ResizeOperation resizeOp;
    private int[][] transformMatrix;
    private int[][] greyScaledImage;
    private int[][] minParentsPaths;
    private Seam[][] allSeams;
    private int kNumOfSeams;

    //MARK: Constructor
    public SeamsCarver(Logger logger, BufferedImage workingImage,
                       int outWidth, RGBWeights rgbWeights) {
        super(logger, workingImage, rgbWeights, outWidth, workingImage.getHeight());

        numOfSeams = Math.abs(outWidth - inWidth);

        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        if (numOfSeams > inWidth / 2)
            throw new RuntimeException("Can not apply seam carving: too many seams...");

        // Sets resizeOp with an appropriate method reference
        if (outWidth > inWidth)
            resizeOp = this::increaseImageWidth;
        else if (outWidth < inWidth)
            resizeOp = this::reduceImageWidth;
        else
            resizeOp = this::duplicateWorkingImage;

        transformMatrix = new int[inHeight][inWidth];
        setForEachInputParameters();
        forEach((y, x) -> {
            // contains the x location on the original image (y is always the same)
            transformMatrix[y][x] = x;
        });

        // init greyScaled version of the working image
        initializeGreyScaledImage();
        kNumOfSeams = Math.abs(inWidth - outWidth);
        allSeams = new Seam[kNumOfSeams][inHeight];

    }

    private void initializeGreyScaledImage() {
        BufferedImage tempGreyScaledImage = greyscale();
        greyScaledImage = new int[this.inHeight][this.inWidth];
        forEach((y, x) -> greyScaledImage[y][x] = new Color(tempGreyScaledImage.getRGB(x,y)).getRed());
    }

    //MARK: Methods
    public BufferedImage resize() {
        return resizeOp.apply();
    }

    //MARK: Unimplemented methods
    private BufferedImage reduceImageWidth() {
        findKSeams();
        BufferedImage reducedSizeImage = newEmptyOutputSizedImage();
        setForEachParameters(outWidth, outHeight);
        forEach((y, x) -> {
            int mappedX = transformMatrix[y][x];
            int colorFromOriginal = workingImage.getRGB(mappedX, y);
            reducedSizeImage.setRGB(x, y, colorFromOriginal);
        });

        return reducedSizeImage;
    }

    // todo: test after activating method
    private void findKSeams() {
        for (int i = 0; i < kNumOfSeams; i++) {
            findSeam(i);
            shiftLeftTransformMatrix(i);
        }
    }

    private void findSeam(int currentSeamIndex) {
        long[][] currCostMatrix = getCostMatrix();

        // find minimum at bottom row of matrix
        int minimalXIndex = 0;
        for (int x = 0; x < currCostMatrix[0].length; x++) {
            if (currCostMatrix[currCostMatrix.length - 1][x]
                    < currCostMatrix[currCostMatrix.length - 1][minimalXIndex]) {

                minimalXIndex = x;
            }
        }

        // constructing the seam path
        allSeams[currentSeamIndex][inHeight - 1] =
                new Seam(minimalXIndex, transformMatrix[inHeight - 1][minimalXIndex]);

        for (int y = inHeight - 1; y > 0 ; y--) {
            int nextXIndexUp = minParentsPaths[y][minimalXIndex];
            allSeams[currentSeamIndex][y - 1] = new Seam(nextXIndexUp, transformMatrix[y - 1][nextXIndexUp]);
            minimalXIndex = nextXIndexUp;
        }
    }

    private void shiftLeftTransformMatrix(int currentSeamIndex) {
        int currentTransformWidth = inWidth - (currentSeamIndex + 1);
        int[][] updatedTransformMatrix = new int[inHeight][currentTransformWidth];

        setForEachParameters(currentTransformWidth, inHeight);
        forEach((y, x) -> {
            // min pixel from seam
            int skipIndex = allSeams[currentSeamIndex][y].getLocalReducedImageX();
            if (x >= skipIndex) {
                updatedTransformMatrix[y][x] = transformMatrix[y][x + 1];
            }
            else {
                updatedTransformMatrix[y][x] = transformMatrix[y][x];
            }
        });
        transformMatrix = updatedTransformMatrix;
    }

    private long[][] getCostMatrix() {
        long[][] tempCostMatrix = new long[transformMatrix.length][transformMatrix[0].length];

        // init cost matrix clone for fast path recovery
        minParentsPaths = new int[transformMatrix.length][transformMatrix[0].length];
        setForEachParameters(transformMatrix[0].length, transformMatrix.length);

        // update the cost matrix values in Mxy
        forEach((y, x) -> {
            int mappedX = transformMatrix[y][x];
            int nextX = x + 1 <= transformMatrix[0].length - 1 ? x + 1 : x - 1;
            int pixelEnergy = Math.abs(greyScaledImage[y][mappedX]
                    - greyScaledImage[y][transformMatrix[y][nextX]]);

            if (y == 0) {
                tempCostMatrix[y][x] = pixelEnergy;
            }
            else {
                MinimalCost min = getMinimalCost(tempCostMatrix, y, x);
                minParentsPaths[y][x] = min.getParentXIndex();
                tempCostMatrix[y][x] = min.getCost() + pixelEnergy;
            }
        });
        return tempCostMatrix;
    }

    private MinimalCost getMinimalCost(long[][] tempCostMatrix, Integer y, Integer x) {
        int cV, cL, cR;
        cV = getInitialCV(y, x);
        cL = calculateCL(y, x, cV);
        cR = calculateCR(y, x, cV);

        long left = x - 1 < 0 ? Long.MAX_VALUE : tempCostMatrix[y - 1][x - 1] + cL;
        long up = tempCostMatrix[y - 1][x] + cV;
        long right = x + 1 > tempCostMatrix[0].length - 1 ? Long.MAX_VALUE : tempCostMatrix[y - 1][x + 1] + cR;

        // Store the min path direction for fast path recovery
        int parentX = x;
        long minCost = Math.min(up, Math.min(left, right));
        if (minCost == right && x + 1 <= transformMatrix[0].length - 1) {
            parentX += 1;
        }
        else if (minCost == left && x - 1 >= 0) {
            parentX -= 1;
        }

        return new MinimalCost(minCost,parentX);
    }

    private int calculateCR(Integer y, Integer x, int cV) {
        int cR;
        if (x + 1 <= transformMatrix[0].length - 1) {
            cR = cV + Math.abs(greyScaledImage[y - 1][transformMatrix[y - 1][x]]
                    - greyScaledImage[y][transformMatrix[y][x + 1]]);
        } else {
            cR = 0;
        }
        return cR;
    }

    private int calculateCL(Integer y, Integer x, int cV) {
        int cL;
        if (x - 1 >= 0){
            cL = cV + Math.abs(greyScaledImage[y - 1][transformMatrix[y - 1][x]]
                    - greyScaledImage[y][transformMatrix[y][x - 1]]);
        } else {
            cL = 0;
        }
        return cL;
    }

    private int getInitialCV(Integer y, Integer x) {
        int cV;
        if (x - 1 < 0){
            cV = greyScaledImage[y][transformMatrix[y][x + 1]];
        } else if (x + 1 > transformMatrix[0].length - 1) {
            cV = greyScaledImage[y][transformMatrix[y][x - 1]];
        } else {
            cV = Math.abs(greyScaledImage[y][transformMatrix[y][x + 1]]
                    - greyScaledImage[y][transformMatrix[y][x - 1]]);
        }
        return cV;
    }

    private BufferedImage increaseImageWidth() {
        //TODO: Implement this method, remove the exception.
        throw new UnimplementedMethodException("increaseImageWidth");
    }

    public BufferedImage showSeams(int seamColorRGB) {
        BufferedImage coloredImage = duplicateWorkingImage();
        if (kNumOfSeams > 0) {
            findKSeams();
            setForEachHeight(inHeight);
            for (Seam[] currentSeam : allSeams) {
                forEachHeight(y -> {
                   int currentSeamXIndex = currentSeam[y].getOriginalImageX();
                   coloredImage.setRGB(currentSeamXIndex, y, seamColorRGB);
                });
            }
        }
        return coloredImage;
    }
}
