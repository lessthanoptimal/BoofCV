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
import boofcv.alg.feature.detect.line.HoughTransformLinePolar;
import boofcv.alg.feature.detect.line.ImageLinePruneMerge;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.image.GrayU8;
import georegression.struct.line.LineParametric2D_F32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * <p>
 * Complete process chain for detecting lines along the edges of objects using {@link HoughTransformLinePolar}.
 * The user provides a binary image. This is then feed into the hough transform.
 * Additional processing is done to remove duplicate lines.
 * </p>
 *
 * @see HoughTransformLinePolar
 *
 * @author Peter Abeles
 */
public class DetectLineHoughPolarBinary {

	// transform algorithm
	HoughTransformLinePolar alg = new HoughTransformLinePolar(null,1,1);

	// extractor used by hough transform
	NonMaxSuppression extractor;

	// tuning parameters for merging
	protected double mergeAngle = Math.PI*0.05;
	protected double mergeDistance = 10;

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

	List<LineParametric2D_F32> foundLines;

	/**
	 * Configures hough line detector.
	 *
	 * @param localMaxRadius Radius for local maximum suppression.  Try 2.
	 * @param minCounts Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 * @param resolutionRange Resolution of line range in pixels.  Try 2
	 * @param resolutionAngle Resolution of line angle in radius.  Try PI/180
	 * @param maxLines Maximum number of lines to return. If &le; 0 it will return them all.
	 */
	public DetectLineHoughPolarBinary(int localMaxRadius,
									  int minCounts,
									  double resolutionRange ,
									  double resolutionAngle ,
									  int maxLines )
	{
		this.localMaxRadius = localMaxRadius;
		this.resolutionRange = resolutionRange;
		this.resolutionAngle = resolutionAngle;
		this.maxLines = maxLines <= 0 ? Integer.MAX_VALUE : maxLines;
		extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(localMaxRadius, minCounts, 0, false));
	}

	public void detect( GrayU8 binary ) {
		setInputSize(binary.width,binary.height);
		alg.transform(binary);
		foundLines = pruneLines(binary.width, binary.height);
	}

	public void setInputSize( int width , int height ) {
		double r = Math.sqrt(width*width + height*height);
		int numBinsRange = (int)Math.ceil(r/resolutionRange);
		int numBinsAngle = (int)Math.ceil(Math.PI/resolutionAngle);

		if( numBinsRange != alg.getNumBinsRange() || numBinsAngle != alg.getNumBinsAngle() ) {
			alg = new HoughTransformLinePolar(extractor,numBinsRange,numBinsAngle);
		}
	}

	private List<LineParametric2D_F32> pruneLines( int width , int height  ) {
		FastQueue<LineParametric2D_F32> lines = alg.extractLines();
		float intensity[] = alg.getFoundIntensity();

		post.reset();
		for( int i = 0; i < lines.size(); i++ ) {
			post.add(lines.get(i),intensity[i]);
		}

		post.pruneSimilar((float)mergeAngle, (float)mergeDistance, width, height);
		post.pruneNBest(maxLines);

		return post.createList();
	}

	public double getMergeAngle() {
		return mergeAngle;
	}

	public void setMergeAngle(double mergeAngle) {
		this.mergeAngle = mergeAngle;
	}

	public double getMergeDistance() {
		return mergeDistance;
	}

	public void setMergeDistance(double mergeDistance) {
		this.mergeDistance = mergeDistance;
	}

	public HoughTransformLinePolar getTransform() {
		return alg;
	}

	public List<LineParametric2D_F32> getFoundLines() {
		return foundLines;
	}
}
