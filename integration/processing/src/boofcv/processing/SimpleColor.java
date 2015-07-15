/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.processing;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;

/**
 * Simplified interface for handling color images
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class SimpleColor extends SimpleImage<MultiSpectral>{

	public SimpleColor(MultiSpectral image) {
		super(image);
	}

	public SimpleColor blurMean( int radius ) {
		return new SimpleColor(GBlurImageOps.mean(image, null, radius, null));
	}

	public SimpleColor blurMedian( int radius ) {
		return new SimpleColor(GBlurImageOps.median(image, null, radius));
	}

	/**
	 * Removes perspective distortion.  4 points must be in 'this' image must be in clockwise order.
	 *
	 * @param outWidth Width of output image
	 * @param outHeight Height of output image
	 * @return Image with perspective distortion removed
	 */
	public SimpleColor removePerspective( int outWidth , int outHeight,
										 double x0, double y0,
										 double x1, double y1,
										 double x2, double y2,
										 double x3, double y3 )
	{
		MultiSpectral output = (MultiSpectral)image._createNew(outWidth,outHeight);

		// Homography estimation algorithm.  Requires a minimum of 4 points
		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

		// Specify the pixel coordinates from destination to target
		ArrayList<AssociatedPair> associatedPairs = new ArrayList<AssociatedPair>();
		associatedPairs.add(new AssociatedPair(new Point2D_F64(0,0),new Point2D_F64(x0,y0)));
		associatedPairs.add(new AssociatedPair(new Point2D_F64(outWidth-1,0),new Point2D_F64(x1,y1)));
		associatedPairs.add(new AssociatedPair(new Point2D_F64(outWidth-1,outHeight-1),new Point2D_F64(x2,y2)));
		associatedPairs.add(new AssociatedPair(new Point2D_F64(0,outHeight-1),new Point2D_F64(x3,y3)));

		// Compute the homography
		DenseMatrix64F H = new DenseMatrix64F(3,3);
		computeHomography.process(associatedPairs, H);

		// Create the transform for distorting the image
		PointTransformHomography_F32 homography = new PointTransformHomography_F32(H);
		PixelTransform_F32 pixelTransform = new PointToPixelTransform_F32(homography);

		// Apply distortion and show the results
		DistortImageOps.distortMS(image, output, pixelTransform, null, TypeInterpolate.BILINEAR);

		return new SimpleColor(output);
	}

	/**
	 * @see boofcv.alg.filter.blur.GBlurImageOps#gaussian
	 */
	public SimpleColor blurGaussian( double sigma, int radius ) {
		return new SimpleColor(GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	/**
	 * Converts the color image into a gray scale image by averaged each pixel across the bands
	 */
	public SimpleGray grayMean() {
		ImageSingleBand out =
				GeneralizedImageOps.createSingleBand(image.imageType.getDataType(),image.width,image.height);

		GConvertImage.average(image, out);

		return new SimpleGray(out);
	}

	public SimpleGray getBand( int band ) {
		return new SimpleGray(image.getBand(band));
	}

	public int getNumberOfBands() {
		return image.getNumBands();
	}

	public PImage convert() {
		PImage out = new PImage(image.width,image.height, PConstants.RGB);
		if( image.getBandType() == ImageFloat32.class) {
			ConvertProcessing.convert_MSF32_RGB(image, out);
		} else if( image.getBandType() == ImageUInt8.class ) {
			ConvertProcessing.convert_MSU8_RGB(image,out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}
}
