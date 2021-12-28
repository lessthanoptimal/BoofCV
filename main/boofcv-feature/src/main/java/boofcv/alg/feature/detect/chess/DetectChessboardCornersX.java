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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.detect.intensity.XCornerAbeles2019Intensity;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.filter.convolve.ConvolveImageMean;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.peak.ConfigMeanShiftSearch;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.QueueCorner;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import pabeles.concurrency.GrowArray;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.CircularIndex.addOffset;

/**
 * Chessboard corner detector that's designed to be robust and fast. Specifically tested under many different lighting
 * conditions, with/without fisheye distortion, motion blur, out of focus, up close, far away, ... etc. This class
 * only operates at a single scale. See {@link DetectChessboardCornersXPyramid} for multi scale that's needed
 * to work in noisy images.
 *
 * Overview:
 * <ol>
 *     <li>X-Corner detector</li>
 *     <li>Mean blur x-corner intensity 2x2 kernel</li>
 *     <li>Non-maximum suppression to find candidate corners</li>
 *     <li>Test corner properties</li>
 *     <li>Use mean-shift to refine location estimate</li>
 *     <li>Test more corner properties</li>
 *     <li>Save remaining corners</li>
 * </ol>
 *
 * At different steps various adaptive filters are applied. See code for details.
 *
 * @author Peter Abeles
 * @see XCornerAbeles2019Intensity
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectChessboardCornersX {
	/**
	 * The largest x-corner intensity is found in the image then multiplied by this factor to select the cutoff point
	 * for non-max suppression
	 */
	@Getter @Setter public float nonmaxThresholdRatio = 0.05f;
	/** Used to prune features with the smallest edge intensity in the image */
	@Getter @Setter public double edgeIntensityRatioThreshold = 0.01;
	/** The smallest allowed edge ratio allowed */
	@Getter @Setter public double edgeAspectRatioThreshold = 0.1;
	/** The smallest allowed corner intensity */
	@Getter @Setter public double refinedXCornerThreshold = 0.001;
	/**
	 * Tolerance number of "spokes" in the wheel which break symmetry. Symmetry is defined as both sides being above
	 * or below the mean value. Larger the value more tolerant it is.
	 */
	public int symmetricTol = 3;
	/**
	 * Amount of blurred applied to input image. A radius of 1 was selected so that a 3x3 region would be sampled
	 * when computing x-corner feature intensity.
	 */
	public int blurRadius = 1;

	// dynamically computed thresholds
	float nonmaxThreshold;

	@Getter GrayF32 blurred = new GrayF32(1, 1);
	BlurFilter<GrayF32> blurFilter;

	SearchLocalPeak<GrayF32> meanShift;

	private final DogArray<ChessboardCorner> corners = new DogArray<>(ChessboardCorner::new);
	List<ChessboardCorner> filtered = new ArrayList<>();

	// storage for corner detector output
	@Getter GrayF32 intensityRaw = new GrayF32(1, 1);
	@Getter GrayF32 intensity2x2 = new GrayF32(1, 1);
	// Reference to the intensity image used to extract features from. it will be one of the two above
	GrayF32 _intensity;
	/**
	 * Maximum pixel value in the corner intensity image
	 */
	public float maxIntensityImage;
	/**
	 * The maximum of this value of the image's intensity will be used when dynamically computing the non-max threshold.
	 * Useful when computing features across a pyramid. The layer with maximum intensity is likely to be a better
	 * threshold for corners.
	 */
	public float considerMaxIntensityImage = 0;

	// Used to compute line integrals of spokes around a corner
	final ImageBorder<GrayF32> borderInput = FactoryImageBorder.generic(BorderType.EXTENDED, ImageType.SB_F32);
	final ImageLineIntegral integral = new ImageLineIntegral();

	// for mean-shift
	final ImageBorder_F32 borderBlur = (ImageBorder_F32)FactoryImageBorder.generic(BorderType.EXTENDED, ImageType.SB_F32);
	final InterpolatePixelS<GrayF32> inputInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);
	public boolean useMeanShift = true;

	// Find corners in intensity image
	NonMaxSuppression nonmax;
	QueueCorner foundNonmax = new QueueCorner();

	// predeclare memory for compute a feature's orientation
	private final int numSpokes = 32;
	private final int numSpokeDiam = numSpokes/2;
	private final double[] spokesRadi = new double[numSpokes];
	private final double[] spokesDiam = new double[numSpokeDiam];
	private final double[] smoothedDiam = new double[numSpokeDiam];
	private final double[] scoreDiam = new double[numSpokeDiam];
	private final Kernel1D_F64 kernelSmooth = FactoryKernelGaussian.gaussian(1, true, 64, -1, numSpokeDiam/4);

	// used to check up and down patterns of intensity image
	DogArray<Point2D_I32> outsideCircle4 = new DogArray<>(Point2D_I32::new);
	DogArray<Point2D_I32> outsideCircle3 = new DogArray<>(Point2D_I32::new);
	private final float[] outsideCircleValues;

	// Workspace
	GrayF32 tmp = new GrayF32(1, 1);
	GrowArray<DogArray_F32> fwork = new GrowArray<>(DogArray_F32::new);

	/**
	 * Declares internal data structures
	 */
	public DetectChessboardCornersX() {
		{
			ConfigExtract config = new ConfigExtract();
			config.radius = 1;
			config.threshold = 0;
			config.detectMaximums = true;
			config.detectMinimums = false;
			config.useStrictRule = true;
			nonmax = FactoryFeatureExtractor.nonmax(config);
		}

		blurFilter = FactoryBlurFilter.gaussian(ImageType.SB_F32, -1, blurRadius);

		// just give it something. this will be changed later
		borderInput.setImage(new GrayF32(1, 1));
		integral.setImage(FactoryGImageGray.wrap(borderInput));

		{
			ConfigMeanShiftSearch config = new ConfigMeanShiftSearch(5, 1e-6);
			config.positiveOnly = true;
			config.odd = false;
			meanShift = FactorySearchLocalPeak.meanShiftGaussian(config, GrayF32.class);
			meanShift.setSearchRadius(2);
		}

		DiscretizedCircle.coordinates(4, outsideCircle4);
		DiscretizedCircle.coordinates(3, outsideCircle3);
		outsideCircleValues = new float[outsideCircle4.size];

		borderBlur.setImage(blurred);
	}

	/**
	 * Computes chessboard corners inside the image
	 *
	 * @param input Gray image. Not modified.
	 */
	public void process( GrayF32 input ) {
		// Initialize data structures
		filtered.clear();
		corners.reset();
		foundNonmax.reset();
		borderInput.setImage(input);
		inputInterp.setImage(input);

		// The x-corner detector requires a little bit of blur to be applied ot the input image
		blurFilter.process(input, blurred);
		XCornerAbeles2019Intensity.process(blurred, intensityRaw);
		// x-corner intensity is results in a symmetric 2x2 region under ideal conditions. Applying a 2x2 mean filter
		// breaks the symmetry. There will be a single unique optimal value, but it will be biased by 0.5 of a pixel.
		ConvolveImageMean.horizontal(intensityRaw, tmp, 0, 2);
		ConvolveImageMean.vertical(tmp, intensity2x2, 0, 2, fwork);

		// NOTE: There's a small improvement if raw is used after detection. That's why everything is so funky
		//       the small performance improvement might be a result of over fitting to test set
		this._intensity = intensityRaw;
//		this._intensity = intensity2x2; // Don't forget to adjust means shift kernel, and add offset after mean shift
//		double intensityOffset = _intensity==intensityRaw?0.0:0.5;
		meanShift.setImage(_intensity);

		// Compute the maximum value in the x-corner intensity image
		// If computed as a pyramid the maximum value in another layer might be "considered"
		// Considered just means use whichever one has a larger value. It's also fine that consider is zero by default
		// since anything less than zero is not a corner.
		// This makes a big difference in heavily blurred images at high resolution where the highest resolution image
		// is unlikely to have any x-corners and 100,000s of false positives in intensity image.
		maxIntensityImage = ImageStatistics.max(intensity2x2);
		// intensity is squared, so the ratio is squared too
		nonmaxThreshold = Math.max(considerMaxIntensityImage, maxIntensityImage)*nonmaxThresholdRatio*nonmaxThresholdRatio;

		// Find features using non-maximum suppression and the threshold computed above
		nonmax.setThresholdMaximum(nonmaxThreshold);
		nonmax.process(intensity2x2, null, null, null, foundNonmax);

		// Convert found points to corner data structure
		for (int i = 0; i < foundNonmax.size; i++) {
			Point2D_I16 c = foundNonmax.get(i);
			ChessboardCorner corner = corners.grow();
			corner.reset();
			corner.setTo(c.x + 0.5, c.y + 0.5);
		}

		double maxEdge = 0;
//		double maxIntensity = 0;

//		System.out.println("  * features.size = "+packed.size());
		for (int i = 0; i < corners.size(); i++) {
			ChessboardCorner c = corners.get(i);

			int xx = (int)(c.x + 0.5f);
			int yy = (int)(c.y + 0.5f);

			// A bunch of code below will crash if it's near the border
			if (xx < 3 || yy < 3 || xx >= input.width - 3 || yy >= input.height - 3)
				continue;

			// Very crude checks to remove situations where there was a little bit of noise that caused a corner
			// They work by seeing if there's a consistent pattern of x-corner like pixels near the center
			// and non-x-corner like pixels in the outside
			if (!checkPositiveInside(xx, yy, 4)) {
				continue;
			}

			if (!checkNegativeInside(xx, yy, 12)) {
				continue;
			}

			// Check to see if there's the expected up/down pattern in the surrounding pixels in a circle around
			if (!checkChessboardCircle((float)c.x, (float)c.y, outsideCircle4, 3, 6, symmetricTol)) {
				continue;
			}

			if (!checkChessboardCircle((float)c.x, (float)c.y, outsideCircle3, 3, 4, symmetricTol)) {
				continue;
			}

			// Refines the corner location estimate using mean-shift
			if (useMeanShift) {
				meanShift.search((float)c.x, (float)c.y);
				c.x = meanShift.getPeakX(); // No shift here since mean-shift is run on RAW
				c.y = meanShift.getPeakY();
			}

			// tighter tolerance now that the center is known
			if (!checkChessboardCircle((float)c.x, (float)c.y, outsideCircle4, 4, 4, symmetricTol - 1)) {
				c.edgeIntensity = -1;
				continue;
			}

			// See if it's a corner also using the eigen value definition
			if (!checkEigenCorner(c)) {
				c.edgeIntensity = -1;
				continue;
			}

			// Computes features like orientation
			if (!computeFeatures(c)) {
				c.edgeIntensity = -1;
				continue;
			}

			// account for bias due to discretion
			c.x += 0.5f;
			c.y += 0.5f;

			// Save the max values for the entire image for use in later pruning
			maxEdge = Math.max(maxEdge, c.edgeIntensity);
//			maxIntensity = Math.max(c.intensity, maxIntensity);
		}

		// Filter corners based on edge intensity of found corners
		for (int i = corners.size - 1; i >= 0; i--) {
			ChessboardCorner c = corners.get(i);
			if (c.edgeIntensity >= edgeIntensityRatioThreshold*maxEdge) {
				filtered.add(c);
			}
		}

//		int dropped = corners.size-filtered.size();
//		System.out.printf("  max-pixel %3.1f corners %4d filters %5d dropped = %4.1f%%\n",
//				maxIntensityImage,corners.size,filtered.size(),(100*dropped/(double)corners.size));
	}

	/**
	 * Around the corer there should be around 4 pixels which have a positive x-corner score
	 */
	boolean checkPositiveInside( int cx, int cy, int threshold ) {
		int radius = 1;

		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius + 1;
		final int y1 = cy + radius + 1;

		int count = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if (_intensity.unsafe_get(x, y) >= nonmaxThreshold)
					count++;
			}
		}
		return count >= threshold;
	}

	/**
	 * Outside of the center most of the pixels should have a negative x-corner score
	 */
	boolean checkNegativeInside( int cx, int cy, int threshold ) {
		int radius = 3;

		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius + 1;
		final int y1 = cy + radius + 1;

		int count = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if (_intensity.unsafe_get(x, y) <= -nonmaxThreshold)
					count++;
			}
		}

		return count >= threshold;
	}

	/**
	 * Looks for an up down pattern in a circle around the corner
	 */
	private boolean checkChessboardCircle( float cx, float cy, DogArray<Point2D_I32> outside, int min, int max, int symmetric ) {
		// NOTE: using `mean = (max(:) + min(:))/2` produced slightly better results, but that might have been
		//       over fitting to the dataset

		float mean = 0;
		for (int i = 0; i < outside.size; i++) {
			Point2D_I32 p = outside.get(i);
			float v = inputInterp.get(cx + p.x, cy + p.y);
			outsideCircleValues[i] = v;
			mean += v;
		}
		mean /= outside.size;

		// Compute the number of times the pixel value transition below and above the mean
		// There should be 4 transitions in a chessboard
		int numUpDown = 0;
		int prevDir = outsideCircleValues[0] > mean ? 1 : -1;
		for (int i = 1; i < outside.size; i++) {
			int dir = outsideCircleValues[i] > mean ? 1 : -1;
			if (prevDir != dir) {
				numUpDown++;
				prevDir = dir;
			}
		}

		// Sample points around the circle should be symmetric. This checks to see if a pixle that was above
		// the mean is also above the mean on the other side, and vis-versa
		int numMirror = 0;
		int halfCount = outside.size/2;
		for (int i = 0; i < halfCount; i++) {
			int dirI = outsideCircleValues[i] > mean ? 1 : -1;
			int dirJ = outsideCircleValues[i + halfCount] > mean ? 1 : -1;

			if (dirI == dirJ)
				numMirror++;
		}

		return numUpDown >= min && numUpDown <= max && numMirror >= halfCount - symmetric;
	}

	/**
	 * Computes how much like an Eigenvalue corner it is. heavily fisheye's images are very poor eigen corners
	 * near the chessboard corner, but even with a very forgiving threshold this eliminates a lot of the false
	 * positives
	 */
	private boolean checkEigenCorner( ChessboardCorner c ) {
		int radius = 3;

		int cx = (int)(c.x + 0.5f);
		int cy = (int)(c.y + 0.5f);

		float xx = 0, yy = 0, xy = 0;

		int width = radius*2 + 1;

		for (int iy = 0; iy < width; iy++) {
			for (int ix = 0; ix < width; ix++) {

				int y = cy + iy - radius;
				int x = cx + ix - radius;

				float dx = borderBlur.get(x + 1, y) - borderBlur.get(x - 1, y);
				float dy = borderBlur.get(x, y + 1) - borderBlur.get(x, y - 1);

				xx += dx*dx;
				xy += dx*dy;
				yy += dy*dy;
			}
		}

		float totalWeight = width*width;
		xx /= totalWeight;
		xy /= totalWeight;
		yy /= totalWeight;

		float left = (xx + yy)*0.5f;
		float b = (xx - yy)*0.5f;
		float right = (float)Math.sqrt(b*b + xy*xy);

		// tempting to use edge intensity as a way to filter out false positives
		// but that makes the corner no longer invariant to affine changes in light, e.g. changes in scale and offset
		c.edgeIntensity = left - right; // smallest eigen value
		// the smallest eigenvalue divided by largest. A perfect corner would be 1. As it approaches zero it indicates
		// that there's more of a line.
		c.edgeRatio = (left - right)/(left + right);

		// NOTE: Setting the Eigen ratio to a higher value is an effective ratio, but for fisheye images it will
		//       filter out many of the corners at the border where they are highly distorted
		return c.edgeRatio >= edgeAspectRatioThreshold;
	}

	/**
	 * Computes features for the corner (angle and intensity) using line integrals in a spokes pattern.
	 *
	 * The feature's angle has a value from -pi/2 to pi/2 radians. It is found by finding the line/spoke with the
	 * minimum value that maximizes distance from the bright lines.
	 *
	 * Intensity is found by subtracting bright lines from the dark line on the other side. dark/light lines are
	 * offset by 90 degrees.
	 */
	private boolean computeFeatures( ChessboardCorner corner ) {
		// Sample radius for the spokes
		final double r = 4;
		// magnitude of the difference is used remove false chessboard corners caused by the corners on black
		// squares. In that situation there will be a large difference between the left and right values
		// in the integral below for 1/2 the line
		double cx = corner.x;
		double cy = corner.y;
		for (int i = 0; i < numSpokeDiam; i++) {
			int j = (i + numSpokeDiam)%numSpokes;
			double angle = Math.PI*i/numSpokeDiam;
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			double valA = spokesRadi[i] = integral.compute(cx, cy, cx + r*c, cy + r*s)/r;
			double valB = spokesRadi[j] = integral.compute(cx, cy, cx - r*c, cy - r*s)/r;

			spokesDiam[i] = valA + valB;
		}
		// NOTE: There used to be a check to see if there are 4 transitions between high and low. It used the mean
		//       as the dividing point. It worked, but in highly skewed scenarios it degraded results a lot.

		smoothSpokeDiam();
		// Select the orientation
		int bestSpoke = -1;
		double bestScore = Double.MAX_VALUE;
		for (int i = 0; i < numSpokeDiam; i++) {
			// j = 90 off, which should be the opposite color
			int j = (i + numSpokeDiam/2)%numSpokeDiam;
			double score = scoreDiam[i] = smoothedDiam[i] - smoothedDiam[j];
			// select black 'i', which will negative because white has a higher value
			if (score < bestScore) {
				bestScore = score;
				bestSpoke = i;
			}
		}

		// Use a quadratic to estimate the peak's location to a sub-bin accuracy
		double value0 = scoreDiam[addOffset(bestSpoke, -1, numSpokeDiam)];
		double value2 = scoreDiam[addOffset(bestSpoke, 1, numSpokeDiam)];

		double adjustedIndex = bestSpoke + FastHessianFeatureDetector.polyPeak(value0, bestSpoke, value2);
		corner.orientation = UtilAngle.boundHalf(Math.PI*adjustedIndex/numSpokeDiam);

		// Why this score? Basically, simplicity.
		// A direct analog of Abeles2019 was tested here and produced slightly worse results on extreme fisheye.
		// Another similar equation "intensity=-max(a-mean,b-mean) + max(c-mean,d-mean)" produced very similar results
		// to what is below.
		corner.intensity = -bestScore;

		// Compute difference between white and black region
		corner.contrast = (scoreDiam[(bestSpoke + numSpokeDiam/2)%numSpokeDiam] - scoreDiam[bestSpoke])/2.0;

		return corner.intensity >= refinedXCornerThreshold;
	}

	private void smoothSpokeDiam() {
		// smooth by applying a block filter. This will ensure it doesn't point towards an edge which just happens
		// to be slightly darker than the center
		int r_smooth = kernelSmooth.getRadius();
		int w_smooth = kernelSmooth.getWidth();
		for (int i = 0; i < numSpokeDiam; i++) {
			int start = addOffset(i, -r_smooth, numSpokeDiam);

			double sum = 0;
			for (int j = 0; j < w_smooth; j++) {
				int index = addOffset(start, j, numSpokeDiam);
				sum += spokesDiam[index]*kernelSmooth.data[j];
			}
			smoothedDiam[i] = sum;
		}
	}

	public List<ChessboardCorner> getCorners() {
		return filtered;
	}

	public int getNonmaxRadius() {
		return nonmax.getSearchRadius();
	}

	public void setNonmaxRadius( int nonmaxRadius ) {
		nonmax.setSearchRadius(nonmaxRadius);
	}
}
