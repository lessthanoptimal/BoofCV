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

package boofcv.alg.feature.detect.line;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Hough transform which uses a polar line representation, distance from origin and angle (0 to 180 degrees).
 * Standard implementation of a hough transform. 1) Gradient intensity image is used to find edge pixels.
 * 2) All possible lines passing through that point are found. 3) Line parameters are summed up in the line image,
 * in which each pixel represents a coordinate in parameter space.
 * 3) Local maximums are found.
 * </p>
 * <p> By the nature of this algorithms, lines are forced to be discretized into parameter space. The range
 * can vary from +- the maximum range inside the image and the angle from 0 to PI radians. How
 * finely discretized an image is effects line detection accuracy. If too fine lines might not be detected
 * or it will be too noisy.
 * </p>
 * <p>
 * In the line image, the transform from line parameter coordinate to pixel coordinate is as follow:<br>
 * x = r*cos(theta) + centerX<br>
 * y = r*sin(theta) + centerY<br>
 * </p>
 *
 * <p>
 * USAGE NOTE: Duplicate/very similar lines are possible due to angles being cyclical. What happens is that if
 * a line's orientation lies along a boundary point its angles will be split up between top and bottom
 * of the transform. When lines are extracted using non-maximum it will detects peaks at the top
 * and bottom.
 * </p>
 *
 * @author Peter Abeles
 */
public class HoughTransformBinary {
	// extracts line from the transform
	NonMaxSuppression extractor;
	// stores returned lines
	DogArray<LineParametric2D_F32> linesAll = new DogArray<>(10, LineParametric2D_F32::new);
	// Lines after similar ones have been merged together
	List<LineParametric2D_F32> linesMerged = new ArrayList<>();
	// contains a set of counts for detected lines in each pixel
	// floating point image used because that's what FeatureExtractor's take as input
	GrayF32 transform = new GrayF32(1, 1);
	// found lines in transform space
	QueueCorner foundPeaks = new QueueCorner(10);
	// line intensities for later pruning
	DogArray_F32 foundIntensity = new DogArray_F32(10);

	HoughTransformParameters parameters;

	// post processing pruning
	ImageLinePruneMerge post = new ImageLinePruneMerge();

	// tuning parameters for merging
	double mergeAngle = Math.PI*0.05;
	double mergeDistance = 10;
	int maxLines = 0;

	// threshold for number of counts. relative is relative to total area of transform. fixed is number of counts
	ConfigLength thresholdCounts = ConfigLength.relative(0.001, 1);

	/**
	 * Specifies parameters of transform. The minimum number of points specified in the extractor
	 * is an important tuning parameter.
	 *
	 * @param extractor Extracts local maxima from transform space.
	 */
	public HoughTransformBinary( NonMaxSuppression extractor, HoughTransformParameters parameters ) {
		this.extractor = extractor;
		this.parameters = parameters;
	}

	/**
	 * Computes the Hough transform of the image.
	 *
	 * @param binary Binary image that indicates which pixels lie on edges.
	 */
	public void transform( GrayU8 binary ) {
		parameters.initialize(binary.width, binary.height, transform);
		ImageMiscOps.fill(transform, 0);

		computeParameters(binary);

		extractLines();
		if (maxLines <= 0) {
			linesMerged.clear();
			linesMerged.addAll(linesAll.toList());
		} else {
			mergeLines(binary.width, binary.height);
		}
	}

	void computeParameters( GrayU8 binary ) {
		for (int y = 0; y < binary.height; y++) {
			int start = binary.startIndex + y*binary.stride;
			int stop = start + binary.width;

			for (int index = start; index < stop; index++) {
				if (binary.data[index] != 0) {
					parameters.parameterize(index - start, y, transform);
				}
			}
		}
	}

	/**
	 * Searches for local maximals and converts into lines.
	 */
	protected void extractLines() {
		linesAll.reset();
		foundPeaks.reset();
		foundIntensity.reset();

		extractor.setThresholdMaximum((float)thresholdCounts.compute(transform.width*transform.height));
		extractor.process(transform, null, null, null, foundPeaks);

		for (int i = 0; i < foundPeaks.size(); i++) {
			Point2D_I16 p = foundPeaks.get(i);

			if (!parameters.isTransformValid(p.x, p.y))
				continue;

			parameters.transformToLine(p.x, p.y, linesAll.grow());
			foundIntensity.push(transform.get(p.x, p.y));
		}
	}

	protected void mergeLines( int width, int height ) {
		post.reset();
		for (int i = 0; i < linesAll.size(); i++) {
			post.add(linesAll.get(i), foundIntensity.get(i));
		}

		// NOTE: angular accuracy is a function of range from sub image center. This pruning
		// function uses a constant value for range accuracy. A custom algorithm should really
		// be used here.
		post.pruneSimilar((float)mergeAngle, (float)mergeDistance, width, height);
		post.pruneNBest(maxLines);

		post.createList(linesMerged);
	}

	/**
	 * //	 * Returns the Hough transform image.
	 *
	 * @return Transform image.
	 */
	public GrayF32 getTransform() {
		return transform;
	}

	/**
	 * Returns the intensity/edge count for each returned line. Useful when doing
	 * post processing pruning.
	 *
	 * @return Array containing line intensities.
	 */
	public float[] getFoundIntensity() {
		return foundIntensity.data;
	}

	public DogArray<LineParametric2D_F32> getLinesAll() {
		return linesAll;
	}

	public List<LineParametric2D_F32> getLinesMerged() {
		return linesMerged;
	}

	public double getMergeAngle() {
		return mergeAngle;
	}

	public void setMergeAngle( double mergeAngle ) {
		this.mergeAngle = mergeAngle;
	}

	public double getMergeDistance() {
		return mergeDistance;
	}

	public void setMergeDistance( double mergeDistance ) {
		this.mergeDistance = mergeDistance;
	}

	public int getMaxLines() {
		return maxLines;
	}

	public void setMaxLines( int maxLines ) {
		this.maxLines = maxLines;
	}

	public HoughTransformParameters getParameters() {
		return parameters;
	}

	public void setNumberOfCounts( ConfigLength counts ) {
		this.thresholdCounts = counts;
	}
}
