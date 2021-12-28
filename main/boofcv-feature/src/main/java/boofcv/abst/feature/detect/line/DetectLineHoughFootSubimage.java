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

package boofcv.abst.feature.detect.line;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.HoughParametersFootOfNorm;
import boofcv.alg.feature.detect.line.HoughTransformGradient;
import boofcv.alg.feature.detect.line.ImageLinePruneMerge;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Detects lines inside the image by breaking it up into subimages for improved precision. Inside
 * each subimage a hough transform is independently computed. See [1] for more details.
 * </p>
 *
 * <p>
 * USAGE NOTES: Blurring the image prior to processing can often improve performance.
 * Results will not be perfect and to detect all the obvious lines in the image several false
 * positives might be returned.
 * </p>
 *
 * <p>
 * [1] Section 9.3 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd Ed. 2005
 * </p>
 *
 * @author Peter Abeles
 * @see boofcv.alg.feature.detect.line.HoughParametersFootOfNorm
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectLineHoughFootSubimage<D extends ImageGray<D>>
		implements DetectEdgeLines<D> {
	int totalHorizontalDivisions;
	int totalVerticalDivisions;

	// transform algorithm
	HoughTransformGradient alg;

	// used to create binary edge image
	float thresholdEdge;

	// image gradient, from input
	D derivX;
	D derivY;

	// edge intensity image
	GrayF32 intensity = new GrayF32(1, 1);

	// detected edge image
	GrayU8 binary = new GrayU8(1, 1);

	// post processing pruning of duplicate lines
	ImageLinePruneMerge post = new ImageLinePruneMerge();

	// the maximum number of lines it will return
	int maxLines;

	@Nullable List<LineParametric2D_F32> foundLines;

	/**
	 * Specifies detection parameters. The suggested parameters should be used as a starting point and will
	 * likely need to be tuned significantly for each different scene.
	 *
	 * @param localMaxRadius Lines in transform space must be a local max in a region with this radius. Try 5;
	 * @param minCounts Minimum number of counts/votes inside the transformed image. Try 5.
	 * @param minDistanceFromOrigin Lines which are this close to the origin of the transformed image are ignored. Try 5.
	 * @param thresholdEdge Threshold for classifying pixels as edge or not. Try 30.
	 * @param maxLines Maximum number of lines it will detect. Try 10.
	 */
	public DetectLineHoughFootSubimage( int localMaxRadius,
										int minCounts,
										int minDistanceFromOrigin,
										float thresholdEdge,
										int totalHorizontalDivisions,
										int totalVerticalDivisions,
										int maxLines,
										Class<D> derivType ) {
		this.thresholdEdge = thresholdEdge;
		this.totalHorizontalDivisions = totalHorizontalDivisions;
		this.totalVerticalDivisions = totalVerticalDivisions;
		this.maxLines = maxLines;
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmaxCandidate(
				new ConfigExtract(localMaxRadius, minCounts, 0, false));
		alg = new HoughTransformGradient<>(extractor, new HoughParametersFootOfNorm(minDistanceFromOrigin), derivType);
	}

	@Override
	public void detect( D derivX, D derivY ) {
		this.derivX = derivX;
		this.derivY = derivY;
		foundLines = null;
		intensity.reshape(derivX.width, derivX.height);
		binary.reshape(derivX.width, derivX.height);

		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, intensity);

		ThresholdImageOps.threshold(intensity, binary, thresholdEdge, false);

		List<LineParametric2D_F32> ret = new ArrayList<>();
		post.reset();

		for (int i = 0; i < totalVerticalDivisions; i++) {
			int y0 = intensity.height*i/totalVerticalDivisions;
			int y1 = intensity.height*(i + 1)/totalVerticalDivisions;

			for (int j = 0; j < totalHorizontalDivisions; j++) {
				int x0 = intensity.width*j/totalVerticalDivisions;
				int x1 = intensity.width*(j + 1)/totalVerticalDivisions;

				processSubimage(x0, y0, x1, y1, ret);
			}
		}

		// removing duplicate lines  caused by processing sub-images
		foundLines = pruneLines(derivX.width, derivX.height);

		removeReferences();
	}

	@SuppressWarnings("NullAway")
	private void removeReferences() {
		// remove reference to external data structures
		this.derivX = null;
		this.derivY = null;
	}

	@Override
	public List<LineParametric2D_F32> getFoundLines() {
		return Objects.requireNonNull(foundLines, "Did you run detect?");
	}

	private List<LineParametric2D_F32> pruneLines( int width, int height ) {
		// NOTE: angular accuracy is a function of range from sub image center. This pruning
		// function uses a constant value for range accuracy. A custom algorithm should really
		// be used here.
		// NOTE: Thresholds should not be hardcoded...
		post.pruneSimilar((float)(Math.PI*0.04), 10, width, height);
		post.pruneNBest(maxLines);

		return post.createList(null);
	}

	private void processSubimage( int x0, int y0, int x1, int y1,
								  List<LineParametric2D_F32> found ) {
		D derivX = this.derivX.subimage(x0, y0, x1, y1);
		D derivY = this.derivY.subimage(x0, y0, x1, y1);
		GrayU8 binary = this.binary.subimage(x0, y0, x1, y1);

		alg.transform(derivX, derivY, binary);
		DogArray<LineParametric2D_F32> lines = alg.getLinesAll();
		float[] intensity = alg.getFoundIntensity();

		for (int i = 0; i < lines.size; i++) {
			// convert from the sub-image coordinate system to original image coordinate system
			LineParametric2D_F32 l = lines.get(i).copy();
			l.p.x += x0;
			l.p.y += y0;
			found.add(l);
			post.add(l, intensity[i]);
		}
	}

	public HoughTransformGradient getTransform() {
		return alg;
	}

	public GrayF32 getEdgeIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}
}
