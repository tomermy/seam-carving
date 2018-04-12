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
    private int kNumOfSeams;
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
//        setForEachParameters(outWidth, inHeight); // Alon: why did we choose outWidth?? XD
//        setForEachParameters(inWidth, inHeight);
        setForEachInputParameters();
        forEach((y, x) -> {
            // contains the x location on the original image (y is always the same)
            transformMatrix[y][x] = x;
        });

        // init greyScaled version of the working image
        initializeGreyScaledImage();
        // todo: verify it gets the right value
        kNumOfSeams = Math.abs(inWidth - outWidth);
        allSeams = new int[kNumOfSeams][inHeight];

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
//        //TODO: Implement this method, remove the exception.
//        throw new UnimplementedMethodException("reduceImageWidth");
        findKSeams();
        BufferedImage ans = newEmptyOutputSizedImage();
//        pushForEachParameters();
//        setForEachWidth(outWidth);
        setForEachParameters(outWidth, outHeight);
        forEach((y, x) -> {
            int mappedX = transformMatrix[y][x];
            int colorFromOriginal = workingImage.getRGB(mappedX, y);
            ans.setRGB(x, y, colorFromOriginal);
        });

        return ans;
    }

    //        this.logger.log("finds the " + this.numOfSeams + " minimal seams.");
//        do {
//            this.calculateCostsMatrix();
//            this.logger.log("finds seam no: " + (this.kNumOfSeams + 1) + ".");
//            this.removeSeam();
//        } while (++this.kNumOfSeams < this.numOfSeams);
//    }
    // todo: test after activating method
    private void findKSeams() {
        for (int i = 0; i < kNumOfSeams; i++) {
            findSeam(i);
            shiftLeftTransformMatrix(i);
        }
    }

    // Alon: added an index for knowing on which seam we are currently working
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
        // Alon: we stored the seams upside down instead from 0 to K
//        allSeams[kNumOfSeams][currCostMatrix.length - 1] = minimalXIndex;
        allSeams[currentSeamIndex][currCostMatrix.length - 1] = minimalXIndex;
        for (int y = currCostMatrix.length - 1; y > 0 ; y--) {
            // Alon: had y++ instead of y--
            allSeams[currentSeamIndex][y - 1] = minParentsPaths[y][minimalXIndex];
            minimalXIndex = minParentsPaths[y][minimalXIndex];
        }
    }

    private void shiftLeftTransformMatrix(int currentSeamIndex) {
        // Alon: again, added param for current seam index we work on
//        int[][] updatedTransformMatrix = new int[inHeight][inWidth - (kNumOfSeams + 1)];
        int currentOutputWidth = inWidth - (currentSeamIndex + 1);
        int[][] updatedTransformMatrix = new int[inHeight][currentOutputWidth];

        setForEachParameters(currentOutputWidth, inHeight);
        forEach((y, x) -> {
            // min pixel from seam
            int skipIndex = allSeams[currentSeamIndex][y];
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

        // Alon: we confused width and height
        long[][] tempCostMatrix = new long[transformMatrix.length][transformMatrix[0].length];
        // init cost matrix clone for path recovery
        minParentsPaths = new int[transformMatrix.length][transformMatrix[0].length];

        // width, height
        // Alon: we forgot to add -1, out of bounds exception
        // Alon: HUGE BUG with setParams, it runs from 0 to MAX - 1 !
        setForEachParameters(transformMatrix[0].length, transformMatrix.length);

        // update the cost matrix values in Mxy
        forEach((y, x) -> {
            if (y == 0) {
                tempCostMatrix[y][x] = tempCostMatrix[y][x];
            } else {

                // todo: Test that this is the right indexes
                MinimalCost min = getMinimalCost(tempCostMatrix, y, x);
                minParentsPaths[y][x] = min.getParentXIndex();


                int nextX = x + 1 <= transformMatrix[0].length - 1 ? x + 1 : x - 1;
                int pixelEnergy = Math.abs(greyScaledImage[y][transformMatrix[y][x]]
                        - greyScaledImage[y][transformMatrix[y][nextX]]);

                tempCostMatrix[y][x] = min.getCost() + pixelEnergy;
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
