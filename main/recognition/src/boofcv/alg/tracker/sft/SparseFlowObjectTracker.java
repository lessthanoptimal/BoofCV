/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.sft;

import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.struct.FastQueue;
import boofcv.struct.RectangleRotate_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.sfm.ScaleTranslateRotate2D;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;

/**
 * @author Peter Abeles
 */
public class SparseFlowObjectTracker<Image extends ImageSingleBand, Derivative extends ImageSingleBand>
{

	private int trackerGridWidth;

	// for the current image
	private ImagePyramid<Image> currentImage;
	private Derivative[] currentDerivX;
	private Derivative[] currentDerivY;

	// previous image
	private ImagePyramid<Image> previousImage;
	private Derivative[] previousDerivX;
	private Derivative[] previousDerivY;

	// Derivative image type
	private Class<Derivative> derivType;

	// tracks features from frame-to-frame
	private PyramidKltTracker<Image, Derivative> klt;

	FastQueue<PyramidKltFeature> tracks = new FastQueue<PyramidKltFeature>(PyramidKltFeature.class,false);

	private FastQueue<AssociatedPair> pairs = new FastQueue<AssociatedPair>(AssociatedPair.class,true);

	// used for estimating motion from track locations
	private LeastMedianOfSquares<ScaleTranslateRotate2D,AssociatedPair> estimateMotion;

	// size of features being tracked
	private int featureRadius;

	private boolean trackLost;

	public void init( Image input , RectangleRotate_F64 region ) {

	}

	public boolean update( Image input , RectangleRotate_F64 region ) {
		return true;
	}

	private void declarePyramid() {

	}

	private void updatePyramid() {

	}

	private void swapPyramids() {

	}

	public boolean isTrackLost() {
		return trackLost;
	}
}

