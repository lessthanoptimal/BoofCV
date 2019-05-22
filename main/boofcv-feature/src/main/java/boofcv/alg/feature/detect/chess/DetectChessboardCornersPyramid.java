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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects chessboard corners at multiple scales. This adds robustness against out of focus images and motion blur.
 * There's also the option to prune corners which are not detected at multiple scales. This is a good way
 * to remove noise since random features are unlikely to have this property.
 *
 * @author Peter Abeles
 */
public class DetectChessboardCornersPyramid<T extends ImageGray<T>, D extends ImageGray<D>> {
	// minimum number of pixels in the top most level in the pyramid
	// If <= 0 then have a single layer at full resolution
	int pyramidTopSize = 100;
	// List of layers in the pyramid
	List<T> pyramid = new ArrayList<>();

	// Corner detector
	DetectChessboardCorners<T,D> detector;

	// Detection results for each layer in the pyramid
	FastQueue<PyramidLevel> featureLevels = new FastQueue<>(PyramidLevel.class, PyramidLevel::new);

	// Storage for final output corners
	FastQueue<ChessboardCorner> corners = new FastQueue<>(ChessboardCorner.class,true);

	// Nearest-Neighbor search data structures
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	FastQueue<NnData<ChessboardCorner>> nnResults = new FastQueue(NnData.class,true);

	public DetectChessboardCornersPyramid(DetectChessboardCorners<T,D> detector) {
		this.detector = detector;
	}

	public DetectChessboardCornersPyramid( Class<T> imageType ) {
		this( new DetectChessboardCorners<>(imageType));
	}

	/**
	 * Detects corner features inside the input gray scale image.
	 */
	public void process(T input ) {
		constructPyramid(input);

		corners.reset();

		// top to bottom. This way the intensity image is at the input image's scale. Which is useful
		// for visualiztion purposes
		double scale = Math.pow(2.0,pyramid.size()-1);
		for (int level = pyramid.size()-1; level >= 0; level--) {
			// find the corners
			detector.process(pyramid.get(level));

			// Add found corners to this level's list
			PyramidLevel featsLevel = featureLevels.get(level);

			FastQueue<ChessboardCorner> corners = detector.getCorners();
			featsLevel.corners.reset();

			for (int i = 0; i < corners.size; i++) {
				ChessboardCorner cf = corners.get(i);

				// convert the coordinate into input image coordinates
				double x = cf.x*scale;
				double y = cf.y*scale;

				ChessboardCorner cl = featsLevel.corners.grow();
				cl.first = true;
				cl.set(x,y,cf.orientation,cf.intensity);
			}
			scale /= 2.0;
		}

		// Create a combined set of features from all the levels. Only add each feature once by searching
		// for it in the next level down
		for (int levelIdx = 0; levelIdx < pyramid.size(); levelIdx++) {
			PyramidLevel level0 = featureLevels.get(levelIdx);

			// mark features in the next level as seen if they match ones in this level
			for( int nextIdx = levelIdx+1; nextIdx < pyramid.size(); nextIdx++ ) {
				PyramidLevel level1 = featureLevels.get(nextIdx);
				markSeenAsFalse(level0.corners,level1.corners);
			}
		}

		for (int levelIdx = 0; levelIdx < pyramid.size(); levelIdx++) {
			PyramidLevel level = featureLevels.get(levelIdx);
			// only add corners if they were first seen in this level
			for (int i = 0; i < level.corners.size; i++) {
				ChessboardCorner c = level.corners.get(i);
				if( c.first )
					corners.grow().set(c);
			}
		}
	}

	/**
	 * Finds corners in list 1 which match corners in list 0. If the feature in list 0 has already been
	 * seen then the feature in list 1 will be marked as seen. Otherwise the feature which is the most intense
	 * is marked as first.
	 */
	void markSeenAsFalse(FastQueue<ChessboardCorner> corners0 , FastQueue<ChessboardCorner> corners1) {
		nn.setPoints(corners1.toList(),false);
		// radius of the blob in the intensity image is 2*kernelRadius
		// Add some buffer because things tend to shift a bit across scales
		int radius = detector.shiRadius*2+2;
		for (int i = 0; i < corners0.size; i++) {
			ChessboardCorner c0 = corners0.get(i);
			if( !c0.first )
				continue;

			nnSearch.findNearest(c0,radius,5,nnResults);

			// IDEA: Could make this smarter by looking at the orientation too?
			double maxIntensity = 0;
			for (int j = 0; j < nnResults.size; j++) {
				ChessboardCorner c1 = nnResults.get(j).point;
				maxIntensity = Math.max(c1.intensity,maxIntensity);
			}
			// the lower resolution feature must be much more intense than the current level to preferred. This
			// results in more accurate feature localization
			if( maxIntensity < c0.intensity*1.5) {
				for (int j = 0; j < nnResults.size; j++) {
					ChessboardCorner c1 = nnResults.get(j).point;
					c1.first = false;
				}
			} else {
				c0.first = false;
			}
		}
	}

	/**
	 * Creates an image pyrmaid by 2x2 average down sampling the input image. The original input image is at layer
	 * 0 with each layer after that 1/2 the resolution of the previous. 2x2 down sampling is used because it doesn't
	 * add blur or aliasing.
	 */
	void constructPyramid(T input) {
		if( pyramid.size() == 0 ){
			pyramid.add(input);
		} else {
			pyramid.set(0,input);
		}

		// make sure the top most layer in the pyramid isn't too small
		int pyramidTopSize = this.pyramidTopSize;
		if( pyramidTopSize != 0 && pyramidTopSize < (1+2*detector.getKernelRadius())*5  ) {
			pyramidTopSize = (1+2*detector.getKernelRadius())*5;
		}

		int levelIndex = 1;
		int divisor = 2;
		while( true ) {
			int width = input.width/divisor;
			int height = input.height/divisor;
			if( pyramidTopSize == 0 || width < pyramidTopSize || height < pyramidTopSize)
				break;
			T level;
			if( pyramid.size() <= levelIndex ) {
				level = (T)GeneralizedImageOps.createSingleBand(detector.imageType,width,height);
				pyramid.add(level);
			} else {
				level = pyramid.get(levelIndex);
				level.reshape(width,height);
			}
			AverageDownSampleOps.down(pyramid.get(levelIndex-1),2,level);
			divisor *= 2;
			levelIndex += 1;
		}
		while( pyramid.size() > levelIndex) {
			pyramid.remove( pyramid.size()-1 );
		}

		featureLevels.resize(pyramid.size());
	}

	private static class PyramidLevel {
		FastQueue<ChessboardCorner> corners = new FastQueue<>(ChessboardCorner.class,true);
	}

	public DetectChessboardCorners<T,D>  getDetector() {
		return detector;
	}

	public FastQueue<ChessboardCorner> getCorners() {
		return corners;
	}

	public int getPyramidTopSize() {
		return pyramidTopSize;
	}

	public void setPyramidTopSize(int pyramidTopSize) {
		this.pyramidTopSize = pyramidTopSize;
	}
}
