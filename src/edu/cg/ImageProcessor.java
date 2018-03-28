package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {

    //MARK: Fields
    public final Logger logger;
    public final BufferedImage workingImage;
    public final RGBWeights rgbWeights;
    public final int inWidth;
    public final int inHeight;
    public final int workingImageType;
    public final int outWidth;
    public final int outHeight;

    //MARK: Constructors
    public ImageProcessor(Logger logger, BufferedImage workingImage,
                          RGBWeights rgbWeights, int outWidth, int outHeight) {
        super(); //Initializing for each loops...

        this.logger = logger;
        this.workingImage = workingImage;
        this.rgbWeights = rgbWeights;
        inWidth = workingImage.getWidth();
        inHeight = workingImage.getHeight();
        workingImageType = workingImage.getType();
        this.outWidth = outWidth;
        this.outHeight = outHeight;
        setForEachInputParameters();
    }

    public ImageProcessor(Logger logger,
                          BufferedImage workingImage,
                          RGBWeights rgbWeights) {
        this(logger, workingImage, rgbWeights,
                workingImage.getWidth(), workingImage.getHeight());
    }

    //MARK: Change picture hue - example
    public BufferedImage changeHue() {
        logger.log("Preparing for hue changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed() / max;
            int green = g * c.getGreen() / max;
            int blue = b * c.getBlue() / max;
            Color color = new Color(red, green, blue);
            ans.setRGB(x, y, color.getRGB());
        });

        logger.log("Changing hue done!");

        return ans;
    }


    //MARK: Unimplemented methods
    public BufferedImage greyscale() {
        //TODO: Implement this method, remove the exception.
        logger.log("Preparing for greyscale changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int weightedRed = r * c.getRed();
            int weightedGreen = g * c.getGreen();
            int weightedBlue = b * c.getBlue();
            int greyColor = (weightedRed + weightedGreen + weightedBlue) / (r + g + b);
            Color resultColor = new Color(greyColor, greyColor, greyColor);
            ans.setRGB(x, y, resultColor.getRGB());
        });

        logger.log("Changing greyscale done!");

        return ans;
    }

    public BufferedImage gradientMagnitude() {
        //TODO: Implement this method, remove the exception.
        logger.log("Preparing for greyscale changing...");

        BufferedImage greyScaledImage = greyscale();
        BufferedImage ans = greyscale();

        forEach((y, x) -> {
            Color currentPixel = new Color(greyScaledImage.getRGB(x, y));
            Color nextHorizontalPixel = new Color(greyScaledImage
                    .getRGB(x == (greyScaledImage.getWidth() - 1) ? x - 1 : x + 1, y));

            Color nextVerticalPixel = new Color(greyScaledImage
                    .getRGB(x, y == (greyScaledImage.getHeight() - 1) ? y - 1 : y + 1));

            int currGreyColor = currentPixel.getRed(); // RGB values are equal on grey
            int horizontalGreyColor = nextHorizontalPixel.getRed();
            int verticalGreyColor = nextVerticalPixel.getRed();

            int dxSquare = (int) Math.pow(currGreyColor - horizontalGreyColor, 2);
            int dySquare = (int) Math.pow(currGreyColor - verticalGreyColor, 2);

            int gradientMagnitude = (int) Math.sqrt((dxSquare + dySquare) / 2);
            Color resultColor = new Color(gradientMagnitude, gradientMagnitude, gradientMagnitude);

            ans.setRGB(x, y, resultColor.getRGB());
        });

        logger.log("gradient magnitude done!");

        return ans;
    }


    public BufferedImage nearestNeighbor() {
        //TODO: Implement this method, remove the exception.
        logger.log("Preparing for nearest neighbor resize...");
        setForEachOutputParameters();
        BufferedImage ans = newEmptyOutputSizedImage();

        forEach((y, x) -> {
            int sourceX = (int) Math.round(((double) x) / ans.getWidth() * workingImage.getWidth());
            int sourceY = (int) Math.round(((double) y) / ans.getHeight() * workingImage.getHeight());

            // Handle boundaries case
            sourceX = Math.min(sourceX, workingImage.getWidth() - 1);
            sourceY = Math.min(sourceY, workingImage.getHeight() - 1);

            int nearestNeighborRGB = workingImage.getRGB(sourceX, sourceY);
            ans.setRGB(x, y, nearestNeighborRGB);

        });

        logger.log("nearest neighbor resize done!");

        return ans;
    }

    public BufferedImage bilinear() {
        //TODO: Implement this method, remove the exception.
        logger.log("Preparing for nearest neighbor resize...");
        setForEachOutputParameters();
        BufferedImage ans = newEmptyOutputSizedImage();

        forEach((y, x) -> {
            double interpolatedX = ((double) x) / ans.getWidth() * workingImage.getWidth();
            double interpolatedY = ((double) y) / ans.getHeight() * workingImage.getHeight();

            // Find the four nearest points
            int sourceXLeft = (int) Math.floor(interpolatedX);
            int sourceXRight = (int) Math.ceil(interpolatedX);
//            int sourceXRight = sourceXLeft + 1;
            int sourceYBottom = (int) Math.floor(interpolatedY);
            int sourceYTop = (int) Math.ceil(interpolatedY);
//            int sourceYTop = sourceYBottom + 1;

            // Handle boundaries case
            sourceXLeft = Math.min(sourceXLeft, workingImage.getWidth() - 1);
            sourceXRight = Math.min(sourceXRight, workingImage.getWidth() - 1);
            sourceYBottom = Math.min(sourceYBottom, workingImage.getHeight() - 1);
            sourceYTop = Math.min(sourceYTop, workingImage.getHeight() - 1);

            // value of the four nearest points
            Color cLeftTop = new Color(workingImage.getRGB(sourceXLeft, sourceYTop));
            Color cRightTop = new Color(workingImage.getRGB(sourceXRight, sourceYTop));
            Color cLeftBottom = new Color(workingImage.getRGB(sourceXLeft, sourceYBottom));
            Color cRightBottom = new Color(workingImage.getRGB(sourceXRight, sourceYBottom));

            double xAxisTValue = sourceXRight - interpolatedX;
            double yAxisTValue = sourceYTop - interpolatedY;

            // interpolations on the X-axis
//            int vTop = (int) ((1 - xAxisTValue) * cLeftTop.getRGB() + xAxisTValue * cRightTop.getRGB());
            int vTopRed = (int) ((1 - xAxisTValue) * cLeftTop.getRed() + xAxisTValue * cRightTop.getRed());
            int vTopGreen = (int) ((1 - xAxisTValue) * cLeftTop.getGreen() + xAxisTValue * cRightTop.getGreen());
            int vTopBlue = (int) ((1 - xAxisTValue) * cLeftTop.getBlue() + xAxisTValue * cRightTop.getBlue());

//            int vBottom = (int) ((1 - xAxisTValue) * cLeftBottom.getRGB() + xAxisTValue * cRightBottom.getRGB());
            int vBottomRed = (int) ((1 - xAxisTValue) * cLeftBottom.getRed() + xAxisTValue * cRightBottom.getRed());
            int vBottomGreen = (int) ((1 - xAxisTValue) * cLeftBottom.getGreen() + xAxisTValue * cRightBottom.getGreen());
            int vBottomBlue = (int) ((1 - xAxisTValue) * cLeftBottom.getBlue() + xAxisTValue * cRightBottom.getBlue());

            // interpolation between X-axis results on the Y-axis
//            int vFinal = (int) ((1 - yAxisTValue) * vBottom + yAxisTValue * vTop);
            int vFinalRed = (int) ((1 - yAxisTValue) * vBottomRed + yAxisTValue * vTopRed);
            int vFinalGreen = (int) ((1 - yAxisTValue) * vBottomGreen + yAxisTValue * vTopGreen);
            int vFinalBlue = (int) ((1 - yAxisTValue) * vBottomBlue + yAxisTValue * vTopBlue);

            Color resultColor = new Color(vFinalRed, vFinalGreen, vFinalBlue);
            ans.setRGB(x, y, resultColor.getRGB());
        });

        logger.log("nearest neighbor resize done!");

        return ans;
    }

    //MARK: Utilities
    public final void setForEachInputParameters() {
        setForEachParameters(inWidth, inHeight);
    }

    public final void setForEachOutputParameters() {
        setForEachParameters(outWidth, outHeight);
    }

    public final BufferedImage newEmptyInputSizedImage() {
        return newEmptyImage(inWidth, inHeight);
    }

    public final BufferedImage newEmptyOutputSizedImage() {
        return newEmptyImage(outWidth, outHeight);
    }

    public final BufferedImage newEmptyImage(int width, int height) {
        return new BufferedImage(width, height, workingImageType);
    }

    public final BufferedImage duplicateWorkingImage() {
        BufferedImage output = newEmptyInputSizedImage();

        forEach((y, x) ->
                output.setRGB(x, y, workingImage.getRGB(x, y))
        );

        return output;
    }
}
