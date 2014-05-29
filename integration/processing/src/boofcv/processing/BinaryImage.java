package boofcv.processing;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageUInt8;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * High level interface for handling binary images
 *
 * @author Peter Abeles
 */
public class BinaryImage {
	ImageUInt8 image;

	public BinaryImage(ImageUInt8 image) {
		this.image = image;
	}

	public BinaryImage logicAnd( BinaryImage imgB ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.logicAnd(image, imgB.image, out);
		return new BinaryImage(out);
	}

	public BinaryImage logicOr( BinaryImage imgB ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.logicOr(image,imgB.image,out);
		return new BinaryImage(out);
	}

	public BinaryImage logicXor( BinaryImage imgB ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.logicXor(image,imgB.image,out);
		return new BinaryImage(out);
	}

	public BinaryImage erode4( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.erode4(image,numTimes,out);
		return new BinaryImage(out);
	}

	public BinaryImage erode8( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.erode8(image,numTimes,out);
		return new BinaryImage(out);
	}

	public BinaryImage dilate4( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.dilate4(image,numTimes,out);
		return new BinaryImage(out);
	}

	public BinaryImage dilate8( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.dilate8(image,numTimes,out);
		return new BinaryImage(out);
	}

	public BinaryImage edge4( ImageUInt8 img ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.edge4(img,out);
		return new BinaryImage(out);
	}

	public BinaryImage edge8( ImageUInt8 img ) {
		ImageUInt8 out = new ImageUInt8(img.width,img.height);
		BinaryImageOps.edge8(img,out);
		return new BinaryImage(out);
	}

	public PImage visualize() {
		PImage out = new PImage(image.width, image.height, PConstants.RGB);

		int indexOut = 0;
		for (int y = 0; y < image.height; y++) {
			int indexIn = image.startIndex + image.stride*y;
			for (int x = 0; x < image.width; x++,indexIn++,indexOut++) {
				out.pixels[indexIn] = image.data[indexOut] == 0 ? 0xFF000000 : 0xFFFFFFFF;
			}
		}

		return out;
	}

	public ImageUInt8 getImage() {
		return image;
	}
}
