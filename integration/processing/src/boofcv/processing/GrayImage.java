package boofcv.processing;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * High level interface for handling gray scale images
 *
 * @author Peter Abeles
 */
public class GrayImage {
	ImageSingleBand image;

	public GrayImage(ImageSingleBand image) {
		this.image = image;
	}

	public GrayImage blurMean( int radius ) {
		return new GrayImage(GBlurImageOps.mean(image, null, radius, null));
	}

	public GrayImage blurMedian( int radius ) {
		return new GrayImage(GBlurImageOps.median(image, null, radius));
	}

	public GrayImage blurGaussian( double sigma, int radius ) {
		return new GrayImage(GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	public BinaryImage threshold(double threshold, boolean down ) {
		return new BinaryImage(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	public BinaryImage thresholdSquare( int radius, double bias, boolean down ) {
		return new BinaryImage(GThresholdImageOps.adaptiveSquare(image, null,radius,bias,down,null,null));
	}

	public BinaryImage thresholdGaussian( int radius, double bias, boolean down ) {
		return new BinaryImage(GThresholdImageOps.adaptiveGaussian(image, null,radius,bias,down,null,null));
	}

	public PImage convert() {
		PImage out = new PImage(image.width,image.height, PConstants.RGB);
		if( image instanceof ImageFloat32) {
			ConvertProcessing.convert_F32_RGB((ImageFloat32)image,out);
		} else if( image instanceof ImageUInt8 ) {
			ConvertProcessing.convert_U8_RGB((ImageUInt8) image, out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}

	public ImageSingleBand getImage() {
		return image;
	}
}
