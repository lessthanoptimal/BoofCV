/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.line;


import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.HoughTransformLinePolar;
import boofcv.alg.feature.detect.line.ImageLinePruneMerge;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Full processing chain for detecting lines using a Hough transform with polar parametrization.
 * </p>
 *
 * <p>
 * USAGE NOTES: Blurring the image prior to processing can often improve performance.
 * Results will not be perfect and to detect all the obvious lines in the image several false
 * positives might be returned.
 * </p>
 *
 * @see boofcv.alg.feature.detect.line.HoughTransformLinePolar
 *
 * @author Peter Abeles
 */
public class DetectLineHoughPolar<I extends ImageGray, D extends ImageGray> implements DetectLine<I> {

	// transform algorithm
	HoughTransformLinePolar alg;

	// extractor used by hough transform
	NonMaxSuppression extractor;

	// computes image gradient
	ImageGradient<I,D> gradient;

	// used to create binary edge image
	float thresholdEdge;

	// image gradient
	D derivX;
	D derivY;

	// edge intensity image
	GrayF32 intensity = new GrayF32(1,1);

	// detected edge image
	GrayU8 binary = new GrayU8(1,1);

	GrayF32 suppressed = new GrayF32(1,1);
//	GrayF32 angle = new GrayF32(1,1);
//	ImageSInt8 direction = new ImageSInt8(1,1);

	// angle tolerance for post processing pruning
	float pruneAngleTol;
	// range tolerance for post processing pruning
	float pruneRangeTol;

	// size of range bin in pixels
	double resolutionRange;
	// size of angle bin in radians
	double resolutionAngle;

	// radius for local max
	int localMaxRadius;
	// the maximum number of lines it will return
	int maxLines;

	// post processing pruning
	ImageLinePruneMerge post = new ImageLinePruneMerge();

	/**
	 * Configures hough line detector.
	 *
	 * @param localMaxRadius Radius for local maximum suppression.  Try 2.
	 * @param minCounts Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 * @param resolutionRange Resolution of line range in pixels.  Try 2
	 * @param resolutionAngle Resolution of line angle in radius.  Try PI/180
	 * @param thresholdEdge Edge detection threshold. Try 50.
	 * @param maxLines Maximum number of lines to return. If &le; 0 it will return them all.
	 * @param gradient Algorithm for computing image gradient.
	 */
	public DetectLineHoughPolar(int localMaxRadius,
								int minCounts,
								double resolutionRange ,
								double resolutionAngle ,
								float thresholdEdge,
								int maxLines ,
								ImageGradient<I, D> gradient)
	{
		pruneAngleTol = (float)((localMaxRadius+1)*resolutionAngle);
		pruneRangeTol = (float)((localMaxRadius+1)*resolutionRange);
		this.localMaxRadius = localMaxRadius;
		this.gradient = gradient;
		this.thresholdEdge = thresholdEdge;
		this.resolutionRange = resolutionRange;
		this.resolutionAngle = resolutionAngle;
		this.maxLines = maxLines <= 0 ? Integer.MAX_VALUE : maxLines;
		extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(localMaxRadius, minCounts, 0, false));
		derivX = gradient.getDerivativeType().createImage(1, 1);
		derivY = gradient.getDerivativeType().createImage(1, 1);
	}

	@Override
	public List<LineParametric2D_F32> detect(I input) {
		// see if the input image shape has changed.
		if( derivX.width != input.width || derivY.height != input.height ) {
			double r = Math.sqrt(input.width*input.width + input.height*input.height);
			int numBinsRange = (int)Math.ceil(r/resolutionRange);
			int numBinsAngle = (int)Math.ceil(Math.PI/resolutionAngle);

			alg = new HoughTransformLinePolar(extractor,numBinsRange,numBinsAngle);
			derivX.reshape(input.width,input.height);
			derivY.reshape(input.width,input.height);
			intensity.reshape(input.width,input.height);
			binary.reshape(input.width, input.height);
//		angle.reshape(input.width, input.height);
//		direction.reshape(input.width, input.height);
			suppressed.reshape(input.width, input.height);
		}

		gradient.process(input, derivX, derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, intensity);

		// non-max suppression reduces the number of line pixels, reducing the number of false positives
		// When too many pixels are flagged, then more curves randomly cross over in transform space causing
		// false positives

//		GGradientToEdgeFeatures.direction(derivX, derivY, angle);
//		GradientToEdgeFeatures.discretizeDirection4(angle, direction);
//		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction, suppressed);

		GGradientToEdgeFeatures.nonMaxSuppressionCrude4(intensity,derivX,derivY,suppressed);

		ThresholdImageOps.threshold(suppressed, binary, thresholdEdge, false);

		alg.transform(binary);
		FastQueue<LineParametric2D_F32> lines = alg.extractLines();

		List<LineParametric2D_F32> ret = new ArrayList<>();
		for( int i = 0; i < lines.size; i++ )
			ret.add(lines.get(i));

		ret = pruneLines(input, ret);

		return ret;
	}

	private List<LineParametric2D_F32> pruneLines(I input, List<LineParametric2D_F32> ret) {
		float intensity[] = alg.getFoundIntensity();
		post.reset();
		for( int i = 0; i < ret.size(); i++ ) {
			post.add(ret.get(i),intensity[i]);
		}

		post.pruneSimilar(pruneAngleTol, pruneRangeTol, input.width, input.height);
		post.pruneNBest(maxLines);

		return post.createList();
	}

	public HoughTransformLinePolar getTransform() {
		return alg;
	}

	public D getDerivX() {
		return derivX;
	}

	public D getDerivY() {
		return derivY;
	}

	public GrayF32 getEdgeIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}
}
