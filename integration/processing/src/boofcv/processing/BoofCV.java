package boofcv.processing;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * Convince class for Processing.
 *
 * @author Peter Abeles
 */
// TODO Edge detection
// TODO contour
// TODO shape fitting
// TODO KLT tracker
// TODO Object tracker
// TODO Dense flow
// TODO Detect corners
// TODO Detect SURF
// TODO Associate two images
// TODO Compute homography
// TODO Apply homography

public class BoofCV {

	PApplet parent;

	public BoofCV(PApplet parent) {
		this.parent = parent;
	}

	public static ImageFloat32 convertF32( PImage image ) {
		ImageFloat32 out = new ImageFloat32(image.width,image.height);

		switch( image.format ) {
			case PConstants.RGB :
			case PConstants.ARGB:
				ConvertProcessing.convert_RGB_F32(image,out);
				break;

			default:
				throw new RuntimeException("Unsupported image type");
		}

		return out;
	}

	public static ImageUInt8 convertU8( PImage image ) {
		ImageUInt8 out = new ImageUInt8(image.width,image.height);

		switch( image.format ) {
			case PConstants.RGB :
			case PConstants.ARGB:
				ConvertProcessing.convert_RGB_U8(image,out);
				break;

			default:
				throw new RuntimeException("Unsupported image type");
		}

		return out;
	}

	public static PImage convert( ImageSingleBand image ) {
		PImage out = new PImage(image.width,image.height,PConstants.RGB);
		if( image instanceof ImageFloat32 ) {
			ConvertProcessing.convert_F32_RGB((ImageFloat32)image,out);
		} else if( image instanceof ImageUInt8 ) {
			ConvertProcessing.convert_U8_RGB((ImageUInt8) image, out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}

	public static <T extends ImageSingleBand>
	T blurMean( T input , int radius ) {
		return (T)GBlurImageOps.mean(input,null,radius,null);
	}

	public static <T extends ImageSingleBand>
	T blurMedian( T input , int radius ) {
		return (T)GBlurImageOps.median(input, null, radius);
	}

	public static <T extends ImageSingleBand>
	T blurGaussian( T input , double sigma, int radius ) {
		return (T)GBlurImageOps.gaussian(input,null,sigma,radius,null);
	}

	public static <T extends ImageSingleBand>
	ImageUInt8 threshold( T input , double threshold, boolean down ) {
		return GThresholdImageOps.threshold(input, null, threshold,down);
	}

	public static <T extends ImageSingleBand>
	ImageUInt8 thresholdSquare( T input , int radius, double bias, boolean down ) {
		return GThresholdImageOps.adaptiveSquare(input, null,radius,bias,down,null,null);
	}

	public static <T extends ImageSingleBand>
	ImageUInt8 thresholdGaussian( T input , int radius, double bias, boolean down ) {
		return GThresholdImageOps.adaptiveGaussian(input, null,radius,bias,down,null,null);
	}

	public static ImageUInt8 logicAnd( ImageUInt8 imgA , ImageUInt8 imgB ) {
		ImageUInt8 out = new ImageUInt8(imgA.width,imgA.height);
		BinaryImageOps.logicAnd(imgA,imgB,out);
		return out;
	}

	public static ImageUInt8 logicOr( ImageUInt8 imgA , ImageUInt8 imgB ) {
		ImageUInt8 out = new ImageUInt8(imgA.width,imgA.height);
		BinaryImageOps.logicOr(imgA,imgB,out);
		return out;
	}

	public static ImageUInt8 logicXor( ImageUInt8 imgA , ImageUInt8 imgB ) {
		ImageUInt8 out = new ImageUInt8(imgA.width,imgA.height);
		BinaryImageOps.logicXor(imgA,imgB,out);
		return out;
	}

	public static ImageUInt8 erode4( ImageUInt8 img , int numTimes ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.erode4(img,numTimes,out);
		return out;
	}

	public static ImageUInt8 erode8( ImageUInt8 img , int numTimes ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.erode8(img,numTimes,out);
		return out;
	}

	public static ImageUInt8 dilate4( ImageUInt8 img , int numTimes ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.dilate4(img,numTimes,out);
		return out;
	}

	public static ImageUInt8 dilate8( ImageUInt8 img , int numTimes ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.dilate8(img,numTimes,out);
		return out;
	}

	public static ImageUInt8 edge4( ImageUInt8 img ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.edge4(img,out);
		return out;
	}

	public static ImageUInt8 edge8( ImageUInt8 img ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.edge8(img,out);
		return out;
	}

	public static PImage visualizeBinary( ImageUInt8 binary ) {
		PImage out = new PImage(binary.width,binary.height,PConstants.RGB);

		int indexOut = 0;
		for (int y = 0; y < binary.height; y++) {
			int indexIn = binary.startIndex + binary.stride*y;
			for (int x = 0; x < binary.width; x++,indexIn++,indexOut++) {
				out.pixels[indexIn] = binary.data[indexOut] == 0 ? 0xFF000000 : 0xFFFFFFFF;
			}
		}

		return out;
	}
}
