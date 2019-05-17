/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.InputSanityCheck;
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
public class DetectLineHoughPolar<D extends ImageGray<D>> implements DetectEdgeLines<D> {

	// transform algorithm
	HoughTransformLinePolar alg;

	// extractor used by hough transform
	NonMaxSuppression extractor;

	// used to create binary edge image
	float thresholdEdge;

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

	int refineRadius = 0;

	// post processing pruning
	ImageLinePruneMerge post = new ImageLinePruneMerge();

	List<LineParametric2D_F32> foundLines;

	/**
	 * Configures hough line detector.
	 *
	 * @param localMaxRadius Radius for local maximum suppression.  Try 2.
	 * @param minCounts Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 * @param resolutionRange Resolution of line range in pixels.  Try 2
	 * @param resolutionAngle Resolution of line angle in radius.  Try PI/180
	 * @param thresholdEdge Edge detection threshold. Try 50.
	 * @param maxLines Maximum number of lines to return. If &le; 0 it will return them all.
	 */
	public DetectLineHoughPolar(int localMaxRadius,
								int minCounts,
								double resolutionRange ,
								double resolutionAngle ,
								float thresholdEdge,
								int maxLines )
	{
		pruneAngleTol = (float)((localMaxRadius+1)*resolutionAngle);
		pruneRangeTol = (float)((localMaxRadius+1)*resolutionRange);
		this.localMaxRadius = localMaxRadius;
		this.thresholdEdge = thresholdEdge;
		this.resolutionRange = resolutionRange;
		this.resolutionAngle = resolutionAngle;
		this.maxLines = maxLines <= 0 ? Integer.MAX_VALUE : maxLines;
		extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(localMaxRadius, minCounts, 0, false));
	}

	@Override
	public void detect( D derivX , D derivY ) {
		InputSanityCheck.checkSameShape(derivX,derivY);
		setInputSize(derivX.width,derivX.height);

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

		foundLines = new ArrayList<>();
		for( int i = 0; i < lines.size; i++ )
			foundLines.add(lines.get(i));

		foundLines = pruneLines(derivX, foundLines);
	}

	public void setInputSize( int width , int height ) {
		// see if the input image shape has changed.
		if( intensity.width != width || intensity.height != height ) {
			double r = Math.sqrt(width*width + height*height);
			int numBinsRange = (int)Math.ceil(r/resolutionRange);
			int numBinsAngle = (int)Math.ceil(Math.PI/resolutionAngle);
			alg = new HoughTransformLinePolar(extractor,numBinsRange,numBinsAngle);
			intensity.reshape(width,height);
			binary.reshape(width, height);
//			angle.reshape(width, height);
//			direction.reshape(width, height);
			suppressed.reshape(width, height);
		}
		alg.setRefineRadius(refineRadius);
	}

	private List<LineParametric2D_F32> pruneLines(D input, List<LineParametric2D_F32> ret) {
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

	public GrayF32 getEdgeIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public int getRefineRadius() {
		return refineRadius;
	}

	public void setRefineRadius(int refineRadius) {
		this.refineRadius = refineRadius;
	}

	@Override
	public List<LineParametric2D_F32> getFoundLines() {
		return foundLines;
	}
}
