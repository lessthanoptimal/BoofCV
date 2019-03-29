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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.feature.detect.interest.DetectChessboardCorners.Corner;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.struct.image.GrayF32;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.nn.alg.KdTreeDistance;
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
public class DetectChessboardCornersPyramid {
	// TODO have flag that prunes corners which haven't been detected at multiple levels

	// minimum number of pixels in the top most level in the pyramid
	int pyramidTopSize = 100;
	// List of layers i nthe pyramid
	List<GrayF32> pyramid = new ArrayList<>();

	// Corner detector
	DetectChessboardCorners detector;

	// Detection results for each layer in the pyramid
	FastQueue<PyramidLevel> featureLevels = new FastQueue<>(PyramidLevel.class, PyramidLevel::new);

	// Storage for final output corners
	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);

	// Nearest-Neighbor search data structures
	NearestNeighbor<Corner> nn = FactoryNearestNeighbor.kdtree(new KdTreeDistance<Corner>() {
		@Override
		public double distance(Corner a, Corner b) {
			return a.distance(b);
		}

		@Override
		public double valueAt(Corner point, int index) {
			switch (index) {
				case 0: return point.x;
				case 1: return point.y;
			}
			throw new RuntimeException("Out of bounds");
		}

		@Override
		public int length() {
			return 2;
		}
	});
	NearestNeighbor.Search<Corner> nnSearch = nn.createSearch();
	FastQueue<NnData<Corner>> nnResults = new FastQueue(NnData.class,true);

	public DetectChessboardCornersPyramid(DetectChessboardCorners detector) {
		this.detector = detector;
	}

	public DetectChessboardCornersPyramid() {
		this( new DetectChessboardCorners());
	}

	/**
	 * Detects corner features inside the input gray scale image.
	 */
	public void process(GrayF32 input ) {
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

			FastQueue<Corner> corners = detector.getCorners();
			featsLevel.corners.reset();
			for (int i = 0; i < corners.size; i++) {
				Corner cf = corners.get(i);

				// convert the coordinate into input image coordinates
				double x = cf.x*scale;
				double y = cf.y*scale;
				// Compensate for how the pyramid was computed using an average down sample. It shifts
				// the coordinate system.
				if( scale > 1 ) {
					x += 0.5*scale;
					y += 0.5*scale;
				}

				Corner cl = featsLevel.corners.grow();
				cl.first = true;
				cl.set(x,y,cf.angle,cf.intensity);
			}
			scale /= 2.0;
		}

		// Create a combined set of features from all the levels. Only add each feature once by searching
		// for it in the next level down
		for (int levelIdx = 0; levelIdx < pyramid.size(); levelIdx++) {
			PyramidLevel level0 = featureLevels.get(levelIdx);

			// mark features in the next level as seen if they match ones in this level
			if( levelIdx+1< pyramid.size() ) {
				PyramidLevel level1 = featureLevels.get(levelIdx+1);
				markSeenAsFalse(level0.corners,level1.corners);
			}
		}

		for (int levelIdx = 0; levelIdx < pyramid.size(); levelIdx++) {
			PyramidLevel level = featureLevels.get(levelIdx);
			// only add corners if they were first seen in this level
			for (int i = 0; i < level.corners.size; i++) {
				Corner c = level.corners.get(i);
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
	void markSeenAsFalse( FastQueue<Corner> corners0 , FastQueue<Corner> corners1 ) {
		nn.setPoints(corners1.toList(),false);
		nnSearch.initialize();
		// radius of the blob in the intensity image is 2*kernelRadius
		int radius = detector.shiRadius *2+1;
		for (int i = 0; i < corners0.size; i++) {
			Corner c0 = corners0.get(i);
			nnSearch.findNearest(c0,radius,5,nnResults);
			// TODO does it ever find multiple matches?

			// Could make this smarter by looking at the orientation too
			for (int j = 0; j < nnResults.size; j++) {
				Corner c1 = nnResults.get(j).point;

				// if the current one wasn't first then none of its children can be first
				if( !c0.first ) {
					c1.first = false;
				} else if( c1.intensity < c0.intensity ) {
					// keeping the one with the best intensity score seems to help. Formally test this idea
					c1.first = false;
				} else {
					c0.first = false;
				}
			}
		}
	}

	/**
	 * Creates an image pyrmaid by 2x2 average down sampling the input image. The original input image is at layer
	 * 0 with each layer after that 1/2 the resolution of the previous. 2x2 down sampling is used because it doesn't
	 * add blur or aliasing.
	 */
	private void constructPyramid(GrayF32 input) {
		if( pyramid.size() == 0 ){
			pyramid.add(input);
		} else {
			pyramid.set(0,input);
		}
		int levelIndex = 1;
		int divisor = 2;
		while( true ) {
			int width = input.width/divisor;
			int height = input.height/divisor;
			if( width < pyramidTopSize || height < pyramidTopSize)
				break;
			GrayF32 level;
			if( pyramid.size() <= levelIndex ) {
				level = new GrayF32(width,height);
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
		FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);
	}

	public DetectChessboardCorners getDetector() {
		return detector;
	}

	public FastQueue<Corner> getCorners() {
		return corners;
	}

	public int getPyramidTopSize() {
		return pyramidTopSize;
	}

	public void setPyramidTopSize(int pyramidTopSize) {
		this.pyramidTopSize = pyramidTopSize;
	}
}
