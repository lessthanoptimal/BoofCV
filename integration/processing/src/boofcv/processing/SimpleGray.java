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

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.enhance.GEnhanceImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * High level interface for handling gray scale images
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class SimpleGray extends SimpleImage<ImageSingleBand>{

	public SimpleGray(ImageSingleBand image) {
		super(image);
	}

	public SimpleGray blurMean( int radius ) {
		return new SimpleGray(GBlurImageOps.mean(image, null, radius, null));
	}

	public SimpleGray blurMedian( int radius ) {
		return new SimpleGray(GBlurImageOps.median(image, null, radius));
	}

	/**
	 * Equalizes the histogram across the entire image
	 *
	 * @see EnhanceImageOps
	 * @return New SimpleGray after equalize histogram has been applied
	 */
	public SimpleGray histogramEqualize() {
		if (!(image instanceof ImageUInt8))
			throw new RuntimeException("Image must be of type ImageUInt8 to adjust its histogram");

		ImageUInt8 adjusted = new ImageUInt8(image.width, image.height);

		int histogram[] = new int[256];
		int transform[] = new int[256];

		ImageStatistics.histogram((ImageUInt8) image, histogram);
		EnhanceImageOps.equalize(histogram, transform);
		EnhanceImageOps.applyTransform((ImageUInt8) image, transform, adjusted);

		return new SimpleGray(adjusted);
	}

	/**
	 * Equalizes the local image histogram
	 * @see EnhanceImageOps
	 * @param radius Radius of the region used to localize
	 * @return New SimpleGray after equalize histogram has been applied
	 */
	public SimpleGray histogramEqualizeLocal( int radius ) {
		if (!(image instanceof ImageUInt8))
			throw new RuntimeException("Image must be of type ImageUInt8 to adjust its histogram");

		ImageUInt8 adjusted = new ImageUInt8(image.width, image.height);
		EnhanceImageOps.equalizeLocal((ImageUInt8) image, radius, adjusted, new int[256], new int[256]);

		return new SimpleGray(adjusted);
	}

	/**
	 * Applies a sharpen with a connect-4 rule
	 *
	 * @see EnhanceImageOps
	 *
	 * @return New SimpleGray
	 */
	public SimpleGray enhanceSharpen4() {
		if (!(image instanceof ImageUInt8))
			throw new RuntimeException("Image must be of type ImageUInt8 to adjust its histogram");

		ImageSingleBand adjusted = (ImageSingleBand)image._createNew(image.width, image.height);
		GEnhanceImageOps.sharpen4(image, adjusted);

		return new SimpleGray(adjusted);
	}

	/**
	 * Applies a sharpen with a connect-8 rule
	 *
	 * @see EnhanceImageOps
	 *
	 * @return New SimpleGray
	 */
	public SimpleGray enhanceSharpen8() {
		if (!(image instanceof ImageUInt8))
			throw new RuntimeException("Image must be of type ImageUInt8 to adjust its histogram");

		ImageSingleBand adjusted = (ImageSingleBand)image._createNew(image.width, image.height);
		GEnhanceImageOps.sharpen8(image, adjusted);

		return new SimpleGray(adjusted);
	}

	public List<LineParametric2D_F32> linesHoughPolar( ConfigHoughPolar config ) {
		Class inputType = image.getClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DetectLine detector = FactoryDetectLineAlgs.houghPolar(config,inputType,derivType);
		return detector.detect(image);
	}

	public List<LineParametric2D_F32> linesHoughFoot( ConfigHoughFoot config ) {
		Class inputType = image.getClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DetectLine detector = FactoryDetectLineAlgs.houghFoot(config, inputType, derivType);
		return detector.detect(image);
	}

	public List<LineParametric2D_F32> linesHoughFootSub( ConfigHoughFootSubimage config ) {
		Class inputType = image.getClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DetectLine detector = FactoryDetectLineAlgs.houghFootSub(config,inputType,derivType);
		return detector.detect(image);
	}

	/**
	 * Removes perspective distortion.  4 points must be in 'this' image must be in clockwise order.
	 *
	 * @param outWidth Width of output image
	 * @param outHeight Height of output image
	 * @return Image with perspective distortion removed
	 */
	public SimpleGray removePerspective( int outWidth , int outHeight,
										 double x0, double y0,
										 double x1, double y1,
										 double x2, double y2,
										 double x3, double y3 )
	{
		ImageSingleBand output = (ImageSingleBand)image._createNew(outWidth,outHeight);

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
		DistortImageOps.distortSingle(image, output, pixelTransform, TypeInterpolate.BILINEAR, BorderType.SKIP);

		return new SimpleGray(output);
	}

	/**
	 * @see GBlurImageOps#gaussian
	 */
	public SimpleGray blurGaussian( double sigma, int radius ) {
		return new SimpleGray(GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	/**
	 * @see GThresholdImageOps#threshold
	 */
	public SimpleBinary threshold(double threshold, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#computeOtsu
	 */
	public SimpleBinary thresholdOtsu(boolean down ) {
		double threshold = GThresholdImageOps.computeOtsu(image,0,255);
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#computeEntropy
	 */
	public SimpleBinary thresholdEntropy(boolean down ) {
		double threshold = GThresholdImageOps.computeEntropy(image,0,255);
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#adaptiveSquare
	 */
	public SimpleBinary thresholdSquare( int radius, double bias, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.adaptiveSquare(image, null, radius, bias, down, null, null));
	}

	/**
	 * @see GThresholdImageOps#adaptiveGaussian
	 */
	public SimpleBinary thresholdGaussian( int radius, double bias, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.adaptiveGaussian(image, null,radius,bias,down,null,null));
	}

	/**
	 * @see GThresholdImageOps#adaptiveSauvola
	 *
	 * @param radius Radius of adaptive region
	 * @param k Positive parameter used to tune threshold.  Try 0.3
	 */
	public SimpleBinary thresholdSauvola( int radius, double k , boolean down ) {
		return new SimpleBinary(GThresholdImageOps.adaptiveSauvola(image, null, radius, (float) k, down));
	}

	public SimpleGradient gradientSobel() {
		return gradient(FactoryDerivative.sobel(image.getClass(), null));
	}

	public SimpleGradient gradientPrewitt() {
		return gradient(FactoryDerivative.prewitt(image.getClass(), null));
	}

	public SimpleGradient gradientThree() {
		return gradient(FactoryDerivative.three(image.getClass(), null));
	}

	public SimpleGradient gradientTwo0() {
		return gradient(FactoryDerivative.two0(image.getClass(), null));
	}

	public SimpleGradient gradientTwo1() {
		return gradient(FactoryDerivative.two1(image.getClass(), null));
	}

	/**
	 * @see GImageStatistics#mean
	 */
	public double mean() {
		return GImageStatistics.mean(image);
	}

	/**
	 * @see GImageStatistics#max
	 */
	public double max() {
		return GImageStatistics.max(image);
	}

	/**
	 * @see GImageStatistics#maxAbs
	 */
	public double maxAbs() {
		return GImageStatistics.maxAbs(image);
	}

	/**
	 * @see GImageStatistics#sum
	 */
	public double sum() {
		return GImageStatistics.sum(image);
	}

	private SimpleGradient gradient(ImageGradient gradient) {
		SimpleGradient ret = new SimpleGradient(gradient.getDerivativeType(),image.width,image.height);
		gradient.process(image,ret.dx,ret.dy);

		return ret;
	}

	public PImage visualizeSign() {
		if( image instanceof ImageFloat32) {
			float max = ImageStatistics.maxAbs((ImageFloat32) image);
			return VisualizeProcessing.colorizeSign((ImageFloat32)image,max);
		} else if( image instanceof ImageInteger) {
			int max = (int)GImageStatistics.maxAbs(image);
			return VisualizeProcessing.colorizeSign((ImageInteger) image, max);
		} else {
			throw new RuntimeException("Unknown image type");
		}
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

	/**
	 * Converts the internal image type into {@link ImageFloat32}.
	 */
	public void convertToF32() {
		if( image instanceof ImageFloat32 )
			return;

		ImageFloat32 a = new ImageFloat32(image.width,image.height);
		GConvertImage.convert(image,a);
		image = a;
	}

	/**
	 * Converts the internal image type into {@link ImageUInt8}.
	 */
	public void convertToU8() {
		if( image instanceof ImageUInt8 )
			return;

		ImageUInt8 a = new ImageUInt8(image.width,image.height);
		GConvertImage.convert(image,a);
		image = a;
	}
}
