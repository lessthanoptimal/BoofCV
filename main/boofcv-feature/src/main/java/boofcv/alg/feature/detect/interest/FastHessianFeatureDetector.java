/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.detect.intensity.GIntegralImageFeatureIntensity;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.feature.detect.selector.SampleIntensityImage;
import boofcv.alg.feature.detect.selector.SampleIntensityScalePoint;
import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.core.image.ImageBorderValue;
import boofcv.struct.QueueCorner;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I16;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.util.List;

/**
 * <p>
 * The Fast Hessian (FH) [1] interest point detector is designed to be a fast multi-scale "blob" detector. FH
 * is intended for use as a feature detector for SURF [1]. It works  by computing an approximation of the
 * image Hessian's determinant using "box-lets" type features. Unlike traditional scale-space algorithms
 * the feature itself is rescaled and is efficiently computed using an {@link boofcv.alg.transform.ii.IntegralImageOps integral image}.
 * </p>
 *
 * <p>
 * This class is intended to be a faithful implementation of the algorithm described in [1]. Deviations
 * from that paper are noted in the code an in the comments below. This detector can be used to implement
 * the FH-9 and FH-15 detectors. For the FH-15 detector the input image needs to be doubled in size prior
 * to processing and the feature location rescaled.
 * </p>
 *
 * <p>
 * Description of scale space approach, see [1] for a more detailed and complete description. Features are detected in
 * a series of octaves. Each octave is defined as a set of scales where higher octaves contain larger scales.
 * A scale is defined by a feature's size in pixels, the size is the feature's width/height. Improved accuracy in done
 * by interpolating feature location in pixel coordinates and scale.
 * </p>
 *
 * <p>
 * For example, the FH-9 detector has 4 octaves with the following detector sizes:<br>
 * Octave 1: Sizes = 9,15,21,27<br>
 * Octave 2: Sizes = 15,27,39,51<br>
 * Octave 3: Sizes = 27,51,75,99<br>
 * Octave 4: Sizes = 51,99,147,195
 * </p>
 *
 * <p>
 * Features are only detected for sizes which have a size smaller and larger. For the first octave in the example
 * above that would be for sizes 15 and 21. Sizes 9 and 27 are only used to identify local maximums in scale space.
 * </p>
 *
 * <p>
 * Laplacian sign is used to decide if a point is white or not. This can be used to split detected points into two sets.
 * </p>
 *
 * <p>
 * Note: Interpolation is performed by fitting a second order polynomial instead of a quadratic, as
 * suggested in the paper. See comments in {@link #polyPeak(float, float, float)}.
 * </p>
 *
 * <p>
 * [1] Herbert Bay, Andreas Ess, Tinne Tuytelaars, and Luc Van Gool, "Speeded-Up Robust Features (SURF)",
 * CVIU June, 2008, Volume 110, Issue 3, pages 346-359
 * </p>
 *
 * @author Peter Abeles
 * @see boofcv.factory.feature.detect.interest.FactoryInterestPoint
 */
@SuppressWarnings({"NullAway.Init"})
public class FastHessianFeatureDetector<II extends ImageGray<II>> {

	// Reference to pass in integral image
	private II integral;

	// finds features from 2D intensity image
	private final NonMaxSuppression extractor;
	// If too many features have been selected this is used to resolve the ambiguity
	private final FeatureSelectLimitIntensity<Point2D_I16> selectFeaturesInScale;
	private final FastArray<Point2D_I16> selectedScale = new FastArray<>(Point2D_I16.class);
	/** the maximum number of returned feature per scale. If <= 0 then all are returned. */
	public int maxFeaturesPerScale = -1;

	// Used for selecting the features across all scales
	private final FeatureSelectLimitIntensity<ScalePoint> selectFeaturesAll;
	private final FastArray<ScalePoint> selectedAll = new FastArray<>(ScalePoint.class);
	/** Maximum number of features after combining results across all scales. if <= 0 then all are returned */
	public int maxFeaturesAll = -1;

	// local sub-space
	private GrayF32[] intensity;
	private int spaceIndex = 0;
	private final QueueCorner foundFeatures = new QueueCorner(100);

	// List of found feature points from all scales combined together
	private final DogArray<ScalePoint> featuresAllScales = new DogArray<>(10, ScalePoint::new);

	// size of detected feature at the smallest scale
	private @Getter final int initialSize;
	// increment between kernel sizes as it goes up in scale
	private @Getter final int scaleStepSize;
	// the number of octaves it examines
	private @Getter final int numberOfOctaves;

	// local variables that are predeclared
	private final int[] sizes;

	// storage for kernels used to compute laplacian sign
	protected IntegralKernel kerXX;
	protected IntegralKernel kerYY;

	// storage for hessian kernels
	protected IntegralKernel hessXX = new IntegralKernel(2);
	protected IntegralKernel hessYY = new IntegralKernel(2);
	protected IntegralKernel hessXY = new IntegralKernel(2);

	// Pre-declare. Used in findLocalScaleSpaceMax
	protected ImageBorder_F32 inten0 = new ImageBorderValue.Value_F32(0.0f);
	protected ImageBorder_F32 inten2 = new ImageBorderValue.Value_F32(0.0f);

	// how often the image is sampled in the first octave
	// a value of 1 would mean every pixel is sampled
	private final int initialSampleRate;

	/**
	 * <p>
	 * Defines the feature detector by specifying the size of features.
	 * </p>
	 * <p>
	 * Configuration for FH-9: initialSampleSize=1, initialSize=9, numberScalesPerOctave=4, numberOfOctaves=4<br>
	 * Configuration for FH-15: initialSampleSize=1, initialSize=15, numberScalesPerOctave=5, numberOfOctaves=4<br>
	 * * Note that FH-15 requires the image to be up sampled first. See [1] for details.
	 * </p>
	 *
	 * @param extractor Feature extractor used to find local maximums in 2D image.
	 * @param selectFeaturesInScale How to prune excessive features inside a single scale
	 * @param selectFeaturesAll How to prune excessive features after combining all scales
	 * @param initialSampleRate How often pixels are sampled in the first octave.
	 * @param initialSize Size/width of the smallest feature/kernel in the lowest octave.
	 * @param numberScalesPerOctave How many different feature sizes are considered in a single octave
	 * @param numberOfOctaves How many different octaves are considered.
	 * @param scaleStepSize Increment between kernel sizes as it goes up in scale. Try 6
	 */
	public FastHessianFeatureDetector( NonMaxSuppression extractor,
									   FeatureSelectLimitIntensity<Point2D_I16> selectFeaturesInScale,
									   FeatureSelectLimitIntensity<ScalePoint> selectFeaturesAll,
									   int initialSampleRate, int initialSize,
									   int numberScalesPerOctave,
									   int numberOfOctaves, int scaleStepSize ) {
		this.extractor = extractor;
		this.selectFeaturesInScale = selectFeaturesInScale;
		this.selectFeaturesAll = selectFeaturesAll;
		this.initialSampleRate = initialSampleRate;
		this.initialSize = initialSize;
		this.numberOfOctaves = numberOfOctaves;
		this.scaleStepSize = scaleStepSize;

		sizes = new int[numberScalesPerOctave];

		selectFeaturesInScale.setSampler(new SampleIntensityImage.I16());
		selectFeaturesAll.setSampler(new SampleIntensityScalePoint());
	}

	/**
	 * Detect interest points inside of the image.
	 *
	 * @param integral Image transformed into an integral image.
	 */
	public void detect( II integral ) {
		if (intensity == null) {
			intensity = new GrayF32[3];
			for (int i = 0; i < intensity.length; i++) {
				intensity[i] = new GrayF32(integral.width, integral.height);
			}
		}
		featuresAllScales.reset();

		// computes feature intensity every 'skip' pixels
		int skip = initialSampleRate;
		// increment between kernel sizes
		int sizeStep = scaleStepSize;
		// initial size of the kernel in the first octave
		int octaveSize = initialSize;
		for (int octave = 0; octave < numberOfOctaves; octave++) {
			for (int i = 0; i < sizes.length; i++) {
				sizes[i] = octaveSize + i*sizeStep;
			}
			// if the maximum kernel size is larger than the image don't process
			// the image any more
			int maxSize = sizes[sizes.length - 1];
			if (maxSize > integral.width || maxSize > integral.height)
				break;
			// detect features inside of this octave
			detectOctave(integral, skip, sizes);
			skip += skip;
			octaveSize += sizeStep;
			sizeStep += sizeStep;
		}
		// todo save previously computed sizes for reuse in higher octaves and reuse it
		// Select points using intensity information from the final list. The scale point's intensity is used
		// so the image can be passed in as null
		if (maxFeaturesAll > 0)
			selectFeaturesAll.select(null, integral.width, integral.height, true, null, featuresAllScales, maxFeaturesAll, selectedAll);
	}

	/**
	 * Computes feature intensities for all the specified feature sizes and finds features
	 * inside of the middle feature sizes.
	 *
	 * @param integral Integral image.
	 * @param skip Pixel skip factor
	 * @param featureSize which feature sizes should be detected.
	 */
	protected void detectOctave( II integral, int skip, int... featureSize ) {

		this.integral = integral;
		int w = integral.width/skip;
		int h = integral.height/skip;

		// resize the output intensity image taking in account subsampling
		for (int i = 0; i < intensity.length; i++) {
			intensity[i].reshape(w, h);
		}

		// compute feature intensity in each level
		for (int i = 0; i < featureSize.length; i++) {
			GIntegralImageFeatureIntensity.hessian(integral, skip, featureSize[i], intensity[spaceIndex],
					hessXX, hessYY, hessXY);

			spaceIndex++;
			if (spaceIndex >= 3)
				spaceIndex = 0;

			// find maximum in scale space
			if (i >= 2) {
				findLocalScaleSpaceMax(featureSize, i - 1, skip);
			}
		}
	}

	/**
	 * Looks for features which are local maximums in the image and scale-space.
	 *
	 * @param size Size of features in different scale-spaces.
	 * @param level Which level in the scale-space
	 * @param skip How many pixels are skipped over.
	 */
	private void findLocalScaleSpaceMax( int[] size, int level, int skip ) {
		int index0 = spaceIndex;
		int index1 = (spaceIndex + 1)%3;
		int index2 = (spaceIndex + 2)%3;

		inten0.setImage(intensity[index0]); // outside of image returns a value of zero
		GrayF32 inten1 = intensity[index1];
		inten2.setImage(intensity[index2]);

		// find local maximums in image 2D space. Borders need to be ignored since
		// false positives are found around them as an artifact of pixels outside being
		// treated as being zero.
		foundFeatures.reset();
		extractor.setIgnoreBorder(size[level]/(2*skip));
		extractor.process(intensity[index1], null, null, null, foundFeatures);

		// Can't consider feature which are right up against the border since they might not be a true local
		// maximum when you consider the features on the other side of the ignore border
		int ignoreRadius = extractor.getIgnoreBorder() + extractor.getSearchRadius();
		int ignoreWidth = intensity[index1].width - ignoreRadius;
		int ignoreHeight = intensity[index1].height - ignoreRadius;

		// if configured to do so, only select the features with the highest intensity
		FastAccess<Point2D_I16> features;
		if (maxFeaturesPerScale > 0) {
			selectFeaturesInScale.select(intensity[index1], -1, -1, true, null, foundFeatures, maxFeaturesPerScale, selectedScale);
			features = selectedScale;
		} else {
			features = foundFeatures;
		}

		int levelSize = size[level];
		int sizeStep = levelSize - size[level - 1];

		// grow the internal array all at once if needed
		featuresAllScales.reserve(featuresAllScales.size + features.size);

		// see if these local maximums are also a maximum in scale-space
		for (int i = 0; i < features.size; i++) {
			Point2D_I16 f = features.get(i);

			// avoid false positives. see above comment
			if (f.x < ignoreRadius || f.x >= ignoreWidth || f.y < ignoreRadius || f.y >= ignoreHeight)
				continue;

			float intenF = inten1.get(f.x, f.y);

			// see if it is a max in scale-space too
			if (checkMax(inten0, intenF, f.x, f.y) && checkMax(inten2, intenF, f.x, f.y)) {

				// find the feature's location to sub-pixel accuracy using a second order polynomial
				// NOTE: In the original paper this was done using a quadratic. See comments above.
				// NOTE: Using a 2D polynomial for x and y might produce better results.
				float peakX = polyPeak(inten1.get(f.x - 1, f.y), inten1.get(f.x, f.y), inten1.get(f.x + 1, f.y));
				float peakY = polyPeak(inten1.get(f.x, f.y - 1), inten1.get(f.x, f.y), inten1.get(f.x, f.y + 1));
				float peakS = polyPeak(inten0.get(f.x, f.y), inten1.get(f.x, f.y), inten2.get(f.x, f.y));

				float interpX = (f.x + peakX)*skip;
				float interpY = (f.y + peakY)*skip;
				float interpS = levelSize + peakS*sizeStep;

				double scale = 1.2*interpS/9.0;
				boolean white = computeLaplaceSign((int)(interpX + 0.5), (int)(interpY + 0.5), scale);
				featuresAllScales.grow().setTo(interpX, interpY, scale, white, intenF);
			}
		}
	}

	/**
	 * Compute the sign of the Laplacian using a sparse convolution.
	 *
	 * @param x center
	 * @param y center
	 * @param scale scale of the feature
	 * @return true if positive
	 */
	public boolean computeLaplaceSign( int x, int y, double scale ) {
		int s = (int)Math.ceil(scale);
		kerXX = DerivativeIntegralImage.kernelDerivXX(9*s, kerXX);
		kerYY = DerivativeIntegralImage.kernelDerivYY(9*s, kerYY);
		double lap = GIntegralImageOps.convolveSparse(integral, kerXX, x, y);
		lap += GIntegralImageOps.convolveSparse(integral, kerYY, x, y);

		return lap > 0;
	}

	/**
	 * Sees if the best score in the current layer is greater than all the scores in a 3x3 neighborhood
	 * in another layer.
	 */
	protected static boolean checkMax( ImageBorder_F32 inten, float bestScore, int c_x, int c_y ) {
		for (int y = c_y - 1; y <= c_y + 1; y++) {
			for (int x = c_x - 1; x <= c_x + 1; x++) {
				if (inten.get(x, y) >= bestScore) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * <p>
	 * Fits a second order polynomial to the data and determines the location of the peak.
	 * <br>
	 * y = a*x<sup>2</sup>+b*x + c<br>
	 * x = {-1,0,1}<br>
	 * y = Feature value
	 * </p>
	 *
	 * <p>
	 * Note: The original paper fit a 3D Quadratic to the data instead. This required the first
	 * and second derivative of the Laplacian to be estimated. Such estimates are error prone
	 * and using the technique found in OpenSURF produced erratic results and required some hackery
	 * to get to work. This should always produce stable results and is much faster.
	 * </p>
	 *
	 * @param lower Value at x=-1
	 * @param middle Value at x=0
	 * @param upper Value at x=1
	 * @return x-coordinate of the peak
	 */
	public static float polyPeak( float lower, float middle, float upper ) {
//		if( lower >= middle || upper >= middle )
//			throw new IllegalArgumentException("Crap");

		// only need two coefficients to compute the peak's location
		float a = 0.5f*lower - middle + 0.5f*upper;
		float b = 0.5f*upper - 0.5f*lower;

		if (a == 0.0f) {
			return 0.0f;
		} else {
			return -b/(2.0f*a);
		}
	}

	public static double polyPeak( double lower, double middle, double upper ) {
//		if( lower >= middle || upper >= middle )
//			throw new IllegalArgumentException("Crap");

		// only need two coefficients to compute the peak's location
		double a = 0.5*lower - middle + 0.5*upper;
		double b = 0.5*upper - 0.5*lower;

		if (a == 0.0) { // TODO or add EPS to denominator? for speed...
			return 0.0;
		} else {
			return -b/(2.0*a);
		}
	}

	public static double polyPeak( double lower, double middle, double upper,
								   double lowerVal, double middleVal, double upperVal ) {
		double offset = polyPeak(lower, middle, upper);
		if (offset < 0) {
			return -lowerVal*offset + (1.0 + offset)*middle;
		} else {
			return upperVal*offset + offset*middle;
		}
	}

	/**
	 * Returns all the found interest points.
	 *
	 * @return Found interest points.
	 */
	public List<ScalePoint> getFoundFeatures() {
		return (maxFeaturesAll <= 0 ? featuresAllScales : selectedAll).toList();
	}

	/**
	 * Returns the width of the smallest feature it can detect
	 */
	public int getSmallestWidth() {
		return initialSize;
	}
}
