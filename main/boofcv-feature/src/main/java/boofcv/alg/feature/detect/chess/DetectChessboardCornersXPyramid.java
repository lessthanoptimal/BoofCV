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
	 *  If &le; 0 then have a single layer at full resolution
 	 */
	@Getter @Setter int pyramidTopSize = 100;

	// Input image with normalized pixel values
	GrayF32 normalized = new GrayF32(1, 1);

	// List of layers in the pyramid
	List<GrayF32> pyramid = new ArrayList<>();

	// search radius when checking to see if the same feature has been detected at multiple scales
	int radius = 7;

	/** Corner detector */
	@Getter DetectChessboardCornersX detector;

	// Detection results for each layer in the pyramid
	DogArray<PyramidLevel> featureLevels = new DogArray<>(PyramidLevel.class, PyramidLevel::new);

	/** Storage for final output corners */
	@Getter DogArray<ChessboardCorner> corners = new DogArray<>(ChessboardCorner::new);

	// Nearest-Neighbor search data structures
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	DogArray<NnData<ChessboardCorner>> nnResults = new DogArray(NnData::new);

	@Getter ImageType<T> imageType;

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
		double scale = Math.pow(2.0, pyramid.size() - 1);
		for (int level = pyramid.size() - 1; level >= 0; level--) {
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
			featsLevel.corners.reset();

			for (int i = 0; i < corners.size(); i++) {
				ChessboardCorner cf = corners.get(i);

				// convert the coordinate into input image coordinates
				double x = cf.x*scale;
				double y = cf.y*scale;

				ChessboardCorner cl = featsLevel.corners.grow();
				cl.first = true;
				cl.setTo(x, y, cf.orientation, cf.intensity);
				cl.contrast = cf.contrast;
				cl.levelMax = level;
				cl.level1 = level;
				cl.level2 = level;
			}
			scale /= 2.0;
		}

		// Perform non-maximum suppression against features in each scale.
		// Because of the scale difference the search radius changes depending on the scale of the layer in the pyramid
		double baseScale = 1.0;
		for (int levelIdx = 0; levelIdx < pyramid.size(); levelIdx++) {
			PyramidLevel level0 = featureLevels.get(levelIdx);

			scale = baseScale*2.0;
			// mark features in the next level as seen if they match ones in this level
			for (int nextIdx = levelIdx + 1; nextIdx < pyramid.size(); nextIdx++) {
				PyramidLevel level1 = featureLevels.get(nextIdx);
				considerLocalizingAtThisScale(level0.corners, level1.corners, scale);
				scale *= 2;
			}
			baseScale *= 2.0;
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
										DogArray<ChessboardCorner> corners1, double scale ) {
		nn.setPoints(corners1.toList(), false);

		double searchRadius = radius*scale;
		for (int i = 0; i < corners0.size; i++) {
			ChessboardCorner c0 = corners0.get(i);

			// In general, you want to use lower scales to localize your feature. However, images can be noisy and
			// blurred which will corrupt localization. So the optimal corner will have a high intensity and be
			// at a lower pyramid level. This is accomplished by requiring lower layers to have an intensity score
			// which is much higher than the score found in lower levels.
			final double intensity = c0.intensity*2.0;

			nnSearch.findNearest(c0, searchRadius, 10, nnResults);
			// true if the current value of the corner is a maximum
			boolean maximum = true;

			// Location accuracy is better at higher resolution but angle accuracy is better at lower resolution
			// accept the new angle if it has higher corner intensity
			ChessboardCorner resultsMax = c0;
			double distanceMax = 0;

			// set the second level to the lowest resolution a neighbor is found in
			int level2 = c0.level2;
			for (int j = 0; j < nnResults.size; j++) {
				ChessboardCorner c1 = nnResults.get(j).point;
				level2 = c1.level2;

				if (c1.intensity < intensity) {
					// This corner is not going to be this feature's corner in this level
					c1.first = false;
				} else {
					maximum = false;
				}
				if (c1.intensity > resultsMax.intensity) {
					distanceMax = nnResults.get(j).distance;
					resultsMax = c1;
				}
			}

			if (!maximum) {
				// another corner is going to be used for this feature
				c0.first = false;
			}

			// Require it to be within the non-maximum radius to actually be merged into c0
			// This is to prevent a feature from "drifting" and incorrectly appearing to go deep down in the pyramid
			if (distanceMax <= radius*radius) {
				c0.orientation = resultsMax.orientation;
				c0.intensity = resultsMax.intensity;
				c0.contrast = resultsMax.contrast;
				c0.levelMax = resultsMax.levelMax;
				c0.level2 = level2;
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
