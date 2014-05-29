package boofcv.processing;

import boofcv.struct.image.ImageFloat32;
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

	public static GrayImage convertF32( PImage image ) {
		ImageFloat32 out = new ImageFloat32(image.width,image.height);

		switch( image.format ) {
			case PConstants.RGB :
			case PConstants.ARGB:
				ConvertProcessing.convert_RGB_F32(image,out);
				break;

			default:
				throw new RuntimeException("Unsupported image type");
		}

		return new GrayImage(out);
	}

	public static GrayImage convertU8( PImage image ) {
		ImageUInt8 out = new ImageUInt8(image.width,image.height);

		switch( image.format ) {
			case PConstants.RGB :
			case PConstants.ARGB:
				ConvertProcessing.convert_RGB_U8(image,out);
				break;

			default:
				throw new RuntimeException("Unsupported image type");
		}

		return new GrayImage(out);
	}






}
