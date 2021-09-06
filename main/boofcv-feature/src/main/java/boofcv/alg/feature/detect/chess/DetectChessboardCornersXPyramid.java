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

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.misc.ImageNormalization;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects chessboard corners at multiple scales. This adds robustness against out of focus images and motion blur.
 * There's also the option to prune corners which are not detected at multiple scales. This is a good way
 * to remove noise since random features are unlikely to have this property.
 *
 * @author Peter Abeles
 */
public class DetectChessboardCornersXPyramid<T extends ImageGray<T>> {
	/**
	 * minimum number of pixels in the top most level in the pyramid
	 * If &le; 0 then have a single layer at full resolution
	 */
	public @Getter @Setter int pyramidTopSize = 100;

	/** Maximum number of corners it will return in kNN search */
	public @Getter @Setter int searchMaxCount = 10;

	// Input image with normalized pixel values
	GrayF32 normalized = new GrayF32(1, 1);

	// List of layers in the pyramid
	List<GrayF32> pyramid = new ArrayList<>();

	// search radius when checking to see if the same feature has been detected at multiple scales
	public @Getter @Setter int radius = 7;

	/** Corner detector */
	public @Getter DetectChessboardCornersX detector;

	// Detection results for each layer in the pyramid
	DogArray<PyramidLevel> featureLevels = new DogArray<>(PyramidLevel.class, PyramidLevel::new);

	/** Storage for final output corners */
	public @Getter DogArray<ChessboardCorner> corners = new DogArray<>(ChessboardCorner::new);

	// Nearest-Neighbor search data structures
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	DogArray<NnData<ChessboardCorner>> nnResults = new DogArray(NnData::new);

	public @Getter ImageType<T> imageType;

	public DetectChessboardCornersXPyramid( DetectChessboardCornersX detector, ImageType<T> imageType ) {
		this.detector = detector;
		this.imageType = imageType;
	}

	public DetectChessboardCornersXPyramid( ImageType<T> imageType ) {
		this(new DetectChessboardCornersX(), imageType);
	}

	/**
	 * Detects corner features inside the input gray scale image.
	 */
	public void process( T input ) {
		constructPyramid(input);

		corners.reset();

		// top to bottom i.e. low res to high res.
		// Two reasons. 1) maximum image intensity can be feed into high resolution images, see below.
		//              2) The intensity image is at the input image's scale. Which is useful for visualization
		//                 purposes
		float maxIntensityImage = 0;
		detector.considerMaxIntensityImage = maxIntensityImage;
		for (int level = pyramid.size() - 1; level >= 0; level--) {
			double scale = Math.pow(2.0, level);

			// In blurred images the x-corner intensity is likely to be much greater at lower resolutions
			// At higher resolution there might be no corners and a very small non-maximum threshold is selected.
			// This will cause a large percentage of the image to be selected as an x-corner, slowing things down!
			// Thus the maximum intensity found so far is used in each layer.
			detector.considerMaxIntensityImage = maxIntensityImage;
			detector.process(pyramid.get(level));
			maxIntensityImage = Math.max(maxIntensityImage, detector.maxIntensityImage);

			// Add found corners to this level's list
			PyramidLevel featsLevel = featureLevels.get(level);

			List<ChessboardCorner> corners = detector.getCorners();
			featsLevel.corners.resetResize(corners.size());

			for (int i = 0; i < corners.size(); i++) {
				ChessboardCorner cf = corners.get(i);

				// convert the coordinate into input image coordinates
				double x = cf.x*scale;
				double y = cf.y*scale;

				ChessboardCorner cl = featsLevel.corners.get(i);
				cl.first = true;
				cl.setTo(x, y, cf.orientation, cf.intensity);
				cl.contrast = cf.contrast;
				cl.levelMax = level;
				cl.level1 = level;
				cl.level2 = level;
			}
		}

		// Perform non-maximum suppression against features in each scale.
		// Because of the scale difference the search radius changes depending on the scale of the layer in the pyramid
		for (int level = 0; level < pyramid.size(); level++) {
			PyramidLevel level0 = featureLevels.get(level);

			// mark features in the next level as seen if they match ones in this level
			for (int upperLevel = level + 1; upperLevel < pyramid.size(); upperLevel++) {
				PyramidLevel level1 = featureLevels.get(upperLevel);
				considerLocalizingAtThisScale(level0.corners, level1.corners, upperLevel);
			}
		}

		// Only keep flagged features for the final output
//		int dropped = 0;
		for (int levelIdx = 0; levelIdx < pyramid.size(); levelIdx++) {
			PyramidLevel level = featureLevels.get(levelIdx);
			// only add corners if they were first seen in this level
			for (int i = 0; i < level.corners.size; i++) {
				ChessboardCorner c = level.corners.get(i);
				if (c.first) {
					corners.grow().setTo(c);
//				} else {
//					dropped++;
				}
			}
		}
//		System.out.println("Found Pyramid "+corners.size+" dropped "+dropped);
	}

	/**
	 * Examine matching corners at this scale and decide based on intensity if this is the scale the feature
	 * should take its location and orientation from. Also mark corners as seen or not.
	 */
	void considerLocalizingAtThisScale( DogArray<ChessboardCorner> corners0,
										DogArray<ChessboardCorner> corners1, int level1 ) {
		nn.setPoints(corners1.toList(), false);

		double searchRadius = 4*radius*(level1 + 1);
		for (int cornerIdx = 0; cornerIdx < corners0.size; cornerIdx++) {
			ChessboardCorner c0 = corners0.get(cornerIdx);

			nnSearch.findNearest(c0, searchRadius, searchMaxCount, nnResults);
			if (nnResults.isEmpty())
				continue;

			// Location accuracy is better at higher resolution but angle accuracy is better at lower resolution
			// accept the new angle if it has higher corner intensity
			ChessboardCorner resultsMax = nnResults.get(0).point;
			for (int candidateIdx = 0; candidateIdx < nnResults.size; candidateIdx++) {
				ChessboardCorner c1 = nnResults.get(candidateIdx).point;

				// Mark all candidates as false since they are "duplicates"
				c1.first = false;

				// Resolve ambiguity by selecting the corner with the largest response
				if (c1.intensity > resultsMax.intensity) {
					resultsMax = c1;
				}
			}

			// Make sure the lower level corner is a first one, if not there's nothing to update
			if (!c0.first)
				continue;

			// Prefer localizing at a level where the corner is more intense, as it will be more stable
			// but also prefer localizing at a lower level since the resolution is higher.
			final double intensity0 = c0.intensity/(1.0 + c0.levelMax);
			final double intensity1 = resultsMax.intensity/(1.0 + resultsMax.level1);

			// See if the original corner is better than the candidates
			if (intensity1 > intensity0) {
				// bit of a hack to prevent location drift across scales.
				// The entire search radius is a bit too generous but suppresses false positive corners
				if (c0.distance2(resultsMax) > radius*radius)
					continue;

				// Copy the original corner into the new candidate so that it can continue to higher pyramid levels
				int tmp = c0.level1;
				c0.setTo(resultsMax);
				c0.level1 = tmp;
				c0.first = true;
			} else {
				c0.level2 = resultsMax.level2;
			}
		}
	}

	/**
	 * Creates an image pyramid by 2x2 average down sampling the input image. The original input image is at layer
	 * 0 with each layer after that 1/2 the resolution of the previous. 2x2 down sampling is used because it doesn't
	 * add blur or aliasing.
	 */
	void constructPyramid( T input ) {
		ImageNormalization.maxAbsOfOne(input, normalized, null);
		if (pyramid.size() == 0) {
			pyramid.add(normalized);
		} else {
			pyramid.set(0, normalized);
		}

		// make sure the top most layer in the pyramid isn't too small
		int pyramidTopSize = this.pyramidTopSize;
		if (pyramidTopSize != 0 && pyramidTopSize < (1 + 2*radius)*5) {
			pyramidTopSize = (1 + 2*radius)*5;
		}

		int levelIndex = 1;
		int divisor = 2;
		while (true) {
			int width = input.width/divisor;
			int height = input.height/divisor;
			if (pyramidTopSize == 0 || width < pyramidTopSize || height < pyramidTopSize)
				break;
			GrayF32 level;
			if (pyramid.size() <= levelIndex) {
				level = new GrayF32(width, height);
				pyramid.add(level);
			} else {
				level = pyramid.get(levelIndex);
				level.reshape(width, height);
			}
			AverageDownSampleOps.down(pyramid.get(levelIndex - 1), 2, level);
			divisor *= 2;
			levelIndex += 1;
		}
		while (pyramid.size() > levelIndex) {
			pyramid.remove(pyramid.size() - 1);
		}

		featureLevels.resize(pyramid.size());
	}

	private static class PyramidLevel {
		DogArray<ChessboardCorner> corners = new DogArray<>(ChessboardCorner::new);
	}

	public int getNumberOfLevels() {
		return pyramid.size();
	}
}
