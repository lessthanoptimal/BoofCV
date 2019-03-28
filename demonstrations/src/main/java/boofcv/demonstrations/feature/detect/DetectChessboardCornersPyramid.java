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

package boofcv.demonstrations.feature.detect;

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.demonstrations.feature.detect.DetectChessboardCorners.Corner;
import boofcv.struct.image.GrayF32;
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
	// TODO create pyramid by 2x2 averaging
	// TODO see if same feature has been detected at multiple levels
	// TODO have flag that prunes corners which haven't been detected at multiple levels

	int topLength = 100;
	List<GrayF32> pyramid = new ArrayList<>();

	DetectChessboardCorners detector;

	FastQueue<PyramidLevel> featureLevels = new FastQueue<>(PyramidLevel.class, PyramidLevel::new);

	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);

	public DetectChessboardCornersPyramid(DetectChessboardCorners detector) {
		this.detector = detector;
	}

	public DetectChessboardCornersPyramid() {
		this( new DetectChessboardCorners());
	}

	public void process(GrayF32 input ) {
		constructPyramid(input);

		corners.reset();

		// top to bottom
		double scale = Math.pow(2.0,pyramid.size()-1);

		for (int level = pyramid.size()-1; level >= 0; level--) {
			detector.process(pyramid.get(level));
			PyramidLevel feats = featureLevels.get(level);

			FastQueue<Corner> corners = detector.getCorners();
			feats.corners.reset();
			for (int i = 0; i < corners.size; i++) {
				Corner c = corners.get(i);

				feats.corners.grow().set(corners.get(i));
				this.corners.grow().set(c,scale);
			}

			scale /= 2.0;
		}

		// TODO remove identical features
		// match using orientation.

	}

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
			if( width < topLength || height < topLength)
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
}
