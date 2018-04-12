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
    private int[][] allSeams;
    private int k = 0;
//    private int[][] currentCostMatrix;


    //TODO: Add some additional fields:


    //MARK: Constructor
    public SeamsCarver(Logger logger, BufferedImage workingImage,
                       int outWidth, RGBWeights rgbWeights) {
        super(logger, workingImage, rgbWeights, outWidth, workingImage.getHeight());

        numOfSeams = Math.abs(outWidth - inWidth);

        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        if (numOfSeams > inWidth / 2)
            throw new RuntimeException("Can not apply seam carving: too many seams...");

        //Sets resizeOp with an appropriate method reference
        if (outWidth > inWidth)
            resizeOp = this::increaseImageWidth;
        else if (outWidth < inWidth)
            resizeOp = this::reduceImageWidth;
        else
            resizeOp = this::duplicateWorkingImage;

        // todo: validate the order of width and heigh
//        int originalImgWidth = workingImage.getWidth();
//        int originalImgHeight = workingImage.getHeight();

        transformMatrix = new int[inHeight][inWidth];
        setForEachParameters(outWidth, inHeight);
        forEach((y, x) -> {
            // contains the x location on the original image (y is always the same)
            transformMatrix[y][x] = x;
        });

        // init greyScaled version of the working image
        initializeGreyScaledImage();
        // todo: verify it gets the right value
        k = Math.abs(inWidth - outWidth);
        allSeams = new int[k][inHeight];

        //TODO: Initialize your additional fields and apply some preliminary calculations:

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
        //TODO: Implement this method, remove the exception.
        throw new UnimplementedMethodException("reduceImageWidth");
    }

    //        this.logger.log("finds the " + this.numOfSeams + " minimal seams.");
//        do {
//            this.calculateCostsMatrix();
//            this.logger.log("finds seam no: " + (this.k + 1) + ".");
//            this.removeSeam();
//        } while (++this.k < this.numOfSeams);
//    }
    // todo: test after activating method
    private void findKSeams() {
        for (int i = 0; i < k; i++) {
            findSeam();
            shiftLeftTransformMatrix();
        }
    }

    private void findSeam() {
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
        allSeams[k][currCostMatrix.length - 1] = minimalXIndex;
        for (int y = currCostMatrix.length - 1; y > 0 ; y++) {
            allSeams[k][y - 1] = minParentsPaths[y][minimalXIndex];
            minimalXIndex = minParentsPaths[y][minimalXIndex];
        }
    }

    private void shiftLeftTransformMatrix() {
        int[][] updatedTransformMatrix = new int[inHeight][inWidth - (k + 1)];

        setForEachParameters(inWidth - (k + 1), inHeight);
        forEach((y, x) -> {
            // min pixel index of the k-ish seam
            int skipIndex = allSeams[k][y];
            if (x >= skipIndex) {
                updatedTransformMatrix[y][x] = transformMatrix[y][x + 1];
            }
            else {
                updatedTransformMatrix[y][x] = transformMatrix[y][x];
            }
        });
        transformMatrix = updatedTransformMatrix;
    }

    // todo: magnitude will be calculated locally (on the spot)
    // todo: save greyscale version of the original image (not going to change)
    private long[][] getCostMatrix() {

        long[][] tempCostMatrix = new long[transformMatrix[0].length][transformMatrix.length];
        // init cost matrix clone for path recovery
        minParentsPaths = new int[transformMatrix[0].length][transformMatrix.length];

        // width, height
        setForEachParameters(transformMatrix[0].length, transformMatrix.length);

        // initialize the beginning values of the cost matrix according to magnitude
//        forEach((y, x) -> {
//            Color currentPixelColor = new Color(gradMagnitudeImage.getRGB(transformMatrix[y][x], y));
//
//            tempCostMatrix[y][x] = currentPixelColor.getRed();
//        });

        // update the cost matrix values in Mxy
        forEach((y, x) -> {
            if (y == 0) {
                tempCostMatrix[y][x] = tempCostMatrix[y][x];
            } else {

                // todo: Test that this is the right indexes
                MinimalCost min = getMinimalCost(tempCostMatrix, y, x);
                minParentsPaths[y][x] = min.getParentXIndex();


                int nextX = x + 1 <= transformMatrix[0].length - 1 ? x + 1 : x - 1;
                tempCostMatrix[y][x] = min.getCost()
                        + Math.abs(greyScaledImage[y][transformMatrix[y][x]]
                            - greyScaledImage[y][transformMatrix[y][nextX]]);
            }
        });
        return tempCostMatrix;
    }

    private MinimalCost getMinimalCost(long[][] tempCostMatrix, Integer y, Integer x) {
        int cV, cL, cR;
        if (x - 1 < 0){
            cV = greyScaledImage[y][transformMatrix[y][x + 1]];
//                    cL = cV + (new Color(greyScaledImage.getRGB(transformMatrix[y - 1][x], y - 1))).getRed();
        } else if (x + 1 > transformMatrix[0].length - 1) {
            cV = greyScaledImage[y][transformMatrix[y][x - 1]];
//                    cR = cV + (new Color(greyScaledImage.getRGB(transformMatrix[y - 1][x], y - 1))).getRed();
        } else {
            cV = Math.abs(greyScaledImage[y][transformMatrix[y][x + 1]]
                    - greyScaledImage[y][transformMatrix[y][x - 1]]);
        }

        if (x - 1 >= 0){
            cL = cV + Math.abs(greyScaledImage[y - 1][transformMatrix[y - 1][x]]
                    - greyScaledImage[y][transformMatrix[y][x - 1]]);
        } else {
            cL = 0;
        }

        if (x + 1 <= transformMatrix[0].length - 1) {
            cR = cV + Math.abs(greyScaledImage[y - 1][transformMatrix[y - 1][x]]
                    - greyScaledImage[y][transformMatrix[y][x + 1]]);
        } else {
            cR = 0;
        }

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

    private BufferedImage increaseImageWidth() {
        //TODO: Implement this method, remove the exception.
        throw new UnimplementedMethodException("increaseImageWidth");
    }

    public BufferedImage showSeams(int seamColorRGB) {
        //TODO: Implement this method (bonus), remove the exception.
        throw new UnimplementedMethodException("showSeams");
    }
}
