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
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		//TODO: Implement this method, remove the exception.
		logger.log("Prepareing for greyscale changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int weightedRed = r*c.getRed();
			int weightedGreen = g*c.getGreen();
			int weightedBlue = b*c.getBlue();
			int greyColor = (weightedRed + weightedGreen + weightedBlue) / (r + g + b);
			Color resultColor = new Color(greyColor, greyColor, greyColor);
			ans.setRGB(x, y, resultColor.getRGB());
		});

		logger.log("Changing greyscale done!");

		return ans;
	}

	public BufferedImage gradientMagnitude() {
		//TODO: Implement this method, remove the exception.
		logger.log("Prepareing for greyscale changing...");

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

			int dxSquare = (int)Math.pow(currGreyColor - horizontalGreyColor, 2);
			int dySquare = (int)Math.pow(currGreyColor - verticalGreyColor, 2);

			int gradientMagnitude = (int)Math.sqrt((dxSquare + dySquare) / 2);
			Color resultColor = new Color(gradientMagnitude, gradientMagnitude, gradientMagnitude);

			ans.setRGB(x, y, resultColor.getRGB());
		});

		logger.log("gradient magnitude done!");

		return ans;
	}


	
	public BufferedImage nearestNeighbor() {
		//TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("nearestNeighbor");
	}
	
	public BufferedImage bilinear() {
		//TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("bilinear");
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
