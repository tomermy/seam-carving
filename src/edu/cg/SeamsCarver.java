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
    private BufferedImage greyScaledImage;


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
        int originalImgWidth = workingImage.getWidth();
        int originalImgHeight = workingImage.getHeight();

        transformMatrix = new int[originalImgWidth][originalImgHeight];
        setForEachParameters(outWidth, originalImgHeight);
        forEach((y, x) -> {
            // contains the x location on the original image (y is always the same)
            transformMatrix[x][y] = x;
        });

        // grey scaled copy of the the working image
        greyScaledImage = greyscale();


        //TODO: Initialize your additional fields and apply some preliminary calculations:

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

    // todo: magnitude will be calculated locally (on the spot)
    // todo: save greyscale version of the original image (not going to change)
    private long[][] getCostMatrix(BufferedImage gradMagnitudeImage) {
        int originalImgWidth = gradMagnitudeImage.getWidth();
        int originalImgHeight = gradMagnitudeImage.getHeight();

        // initialize the begining values of the cost matrix according to magnitude
        long[][] tempCostMatrix = new long[transformMatrix[0].length][transformMatrix.length];

        // width, height
        setForEachParameters(transformMatrix[0].length, transformMatrix.length);
        forEach((y, x) -> {
            Color currentPixelColor = new Color(gradMagnitudeImage.getRGB(transformMatrix[y][x], y));
            tempCostMatrix[y][x] = currentPixelColor.getRed();
        });

        // update the cost matrix values in Mxy
        forEach((y, x) -> {
            if (y == 0) {
                tempCostMatrix[y][x] = tempCostMatrix[y][x];
            } else {

                // todo: Test that this is the right indexes
                int cV, cL = 0, cR = 0;
                if (x - 1 < 0){
                    cV = (new Color(greyScaledImage.getRGB(transformMatrix[y][x + 1], y))).getRed();
                    cL = cV + (new Color(greyScaledImage.getRGB(transformMatrix[y - 1][x], y - 1))).getRed();
                } else if (x + 1 > transformMatrix[0].length - 1) {
                    cV = (new Color(greyScaledImage.getRGB(transformMatrix[y][x - 1], y))).getRed();
                    cR = cV + (new Color(greyScaledImage.getRGB(transformMatrix[y - 1][x], y - 1))).getRed();
                } else {
                    cV = Math.abs((new Color(greyScaledImage.getRGB(transformMatrix[y][x + 1], y))).getRed()
                            - (new Color(greyScaledImage.getRGB(transformMatrix[y][x - 1], y))).getRed());
                }

                if (x - 1 >= 0){
                    cL = cV + Math.abs((new Color(greyScaledImage.getRGB(transformMatrix[y - 1][x], y - 1))).getRed()
                            - (new Color(greyScaledImage.getRGB(transformMatrix[y][x - 1], y))).getRed());
                }

                if (x + 1 <= transformMatrix[0].length - 1) {
                    cR = cV +Math.abs((new Color(greyScaledImage.getRGB(transformMatrix[y - 1][x], y - 1))).getRed()
                            - (new Color(greyScaledImage.getRGB(transformMatrix[y][x + 1], y))).getRed());
                }

                long left = x - 1 < 0 ? Long.MAX_VALUE : tempCostMatrix[y - 1][x - 1] + cL;
                long up = tempCostMatrix[y - 1][x] + cV;
                long right = x + 1 > tempCostMatrix[0].length - 1 ? Long.MAX_VALUE : tempCostMatrix[y - 1][x + 1] + cR;


                // minimum
                long min = Math.min(up, Math.min(left, right));

                // todo: set the value of each cell to be...
                // energy + min
                // reminder: magnitude "next pixel logic" (if x+1 is out of bounds, go x-1)
            }
        });
        // replace
        return tempCostMatrix;
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
