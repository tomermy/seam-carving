package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

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
    private SeamData[][] allSeams;

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
        allSeams = new SeamData[numOfSeams][inHeight];

    }

    private void initializeGreyScaledImage() {
        BufferedImage tempGreyScaledImage = greyscale();
        greyScaledImage = new int[this.inHeight][this.inWidth];
        forEach((y, x) -> greyScaledImage[y][x] = new Color(tempGreyScaledImage.getRGB(x, y)).getRed());
    }

    //MARK: Methods
    public BufferedImage resize() {
        return resizeOp.apply();
    }

    //MARK: Unimplemented methods
    private BufferedImage reduceImageWidth() {
        BufferedImage reducedSizeImage = newEmptyOutputSizedImage();
        findKSeams();

        // After finding K seams, transformMatrix represents the state
        // of all the pixels for the reduced size working image.
        setForEachParameters(outWidth, outHeight);
        forEach((y, x) -> {
            int mappedX = transformMatrix[y][x];
            int colorFromOriginal = workingImage.getRGB(mappedX, y);
            reducedSizeImage.setRGB(x, y, colorFromOriginal);
        });

        return reducedSizeImage;
    }

    private void findKSeams() {
        this.logger.log("finding " + this.numOfSeams + " minimal seams");
        for (int i = 0; i < numOfSeams; i++) {
            this.logger.log("finding seam no: " + (i + 1));
            findSeam(i);
            shiftLeftTransformMatrix(i);
        }
    }

    private void findSeam(int currentSeamIndex) {
        long[][] currCostMatrix = getCostMatrix();

        // find minimal cost pixel at bottom row of matrix
        this.logger.log("looking for the X index of the bottom row with minimal cost");
        int minimalXIndex = 0;
        for (int x = 0; x < currCostMatrix[0].length; x++) {
            if (currCostMatrix[currCostMatrix.length - 1][x]
                    < currCostMatrix[currCostMatrix.length - 1][minimalXIndex]) {

                minimalXIndex = x;
            }
        }

        // Store the seam data in a structure including:
        // X index of the original image
        // X index of the local state for the TransformationMatrix
        int originalImageMinX = transformMatrix[inHeight - 1][minimalXIndex];
        this.logger.log("minX = " + originalImageMinX);
        this.logger.log("constructing the path of minimal seam");
        allSeams[currentSeamIndex][inHeight - 1] =
                new SeamData(minimalXIndex, originalImageMinX);

        // Continue constructing the seam along the Y-axis.
        this.logger.log("stores the path.");
        for (int y = inHeight - 1; y > 0; y--) {
            int nextXIndexUp = minParentsPaths[y][minimalXIndex];
            allSeams[currentSeamIndex][y - 1] = new SeamData(nextXIndexUp, transformMatrix[y - 1][nextXIndexUp]);
            minimalXIndex = nextXIndexUp;
        }
    }

    private void shiftLeftTransformMatrix(int currentSeamIndex) {
        int currentTransformWidth = inWidth - (currentSeamIndex + 1);
        int[][] updatedTransformMatrix = new int[inHeight][currentTransformWidth];

        // Construct a smaller version of the transform matrix based on seam data
        this.logger.log("removing seam");
        setForEachParameters(currentTransformWidth, inHeight);
        forEach((y, x) -> {
            // min cost pixel from current seam that will be skipped
            int skipIndex = allSeams[currentSeamIndex][y].getLocalReducedImageX();
            if (x >= skipIndex) {
                updatedTransformMatrix[y][x] = transformMatrix[y][x + 1];
            } else {
                updatedTransformMatrix[y][x] = transformMatrix[y][x];
            }
        });

        // Update the current state of the transform matrix
        transformMatrix = updatedTransformMatrix;
    }

    private long[][] getCostMatrix() {
        this.logger.log("calculating the costs matrix");
        long[][] tempCostMatrix = new long[transformMatrix.length][transformMatrix[0].length];

        // init cost matrix clone for fast path recovery in seam construction
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
            } else {
                MinimalCost min = getMinimalCost(tempCostMatrix, y, x);
                minParentsPaths[y][x] = min.getParentXIndex();
                tempCostMatrix[y][x] = pixelEnergy + min.getCost();
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

        // Store the minimal path direction for fast path recovery
        int parentX = x;
        long minCost = Math.min(up, Math.min(left, right));
        if (minCost == right && x + 1 <= transformMatrix[0].length - 1) {
            parentX += 1;
        } else if (minCost == left && x - 1 >= 0) {
            parentX -= 1;
        }

        return new MinimalCost(minCost, parentX);
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
        if (x - 1 >= 0) {
            cL = cV + Math.abs(greyScaledImage[y - 1][transformMatrix[y - 1][x]]
                    - greyScaledImage[y][transformMatrix[y][x - 1]]);
        } else {
            cL = 0;
        }
        return cL;
    }

    private int getInitialCV(Integer y, Integer x) {
        int cV;
        if (x - 1 < 0) {
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
        BufferedImage increasedSizeImage = newEmptyOutputSizedImage();

        // Container for the enlarged size image
        int[][] enlargedImageIndexMatrix = new int[outHeight][outWidth];

        // Find the seams to be duplicated from original image
        findKSeams();

        // Insert the original image indexes to the new mapping matrix
        setForEachInputParameters();
        forEach((y, x) -> enlargedImageIndexMatrix[y][x] = x);

        // Add the duplicated pixel indexes to the new image container
        setForEachParameters(numOfSeams, outHeight);
        forEach((y, x) -> enlargedImageIndexMatrix[y][inWidth + x] = allSeams[x][y].getOriginalImageX());

        // Sort each row of the full image container with all indexes
        for (int[] newImageRow : enlargedImageIndexMatrix) {
            Arrays.sort(newImageRow);
        }

        // Build output image according to the index mapping
        setForEachParameters(outWidth, outHeight);
        forEach((y, x) -> {
            int rgb = workingImage.getRGB(enlargedImageIndexMatrix[y][x], y);
            increasedSizeImage.setRGB(x, y, rgb);
        });

        return increasedSizeImage;
    }

    /**
     * This method colors the pixels which would have been used
     * to construct seams in order to reduce/increase the image size
     * based on the Seam carving algorithm.
     * @param seamColorRGB - Color chosen for seam pixels.
     * @return - Copy of the image including the colored pixels.
     */
    public BufferedImage showSeams(int seamColorRGB) {
        BufferedImage coloredImage = duplicateWorkingImage();

        if (numOfSeams > 0) {
            findKSeams();
            setForEachHeight(inHeight);

            // Use the seams found in order to color the working image copy
            for (SeamData[] currentSeamData : allSeams) {
                forEachHeight(y -> {
                    int currentSeamXIndex = currentSeamData[y].getOriginalImageX();
                    coloredImage.setRGB(currentSeamXIndex, y, seamColorRGB);
                });
            }
        }

        return coloredImage;
    }
}
