/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.alg.feature.detect.extract.SelectNBestFeatures;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.FastQueue;
import boofcv.struct.QueueCorner;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;

import static boofcv.alg.feature.detect.interest.FastHessianFeatureDetector.polyPeak;

/**
 *
 *
 *
 * <p>
 * Feature detector as described in the Scale Invariant Feature Transform (SIFT) paper [1].  Location and scale of
 * blob like features are detected using a Difference of Gaussian (DOG) feature detector across scale space.  Note that
 * there are  several algorithmic changes that are intended to improve stability and runtime speed.  See notes below.
 * </p>
 *
 * <p>
 * ALGORITHM CHANGE: Location and scale interpolation is done using a second order polynomial.  This avoids taking
 * the second order derivative numerically, which is very sensitive to noise.  Plus I disagree with his statement
 * that peaks outside the local region are valid and require iteration.
 * </p>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints".
 * International Journal of Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO Add dark/white blob label
// TODO Remove edge responses
public class SiftDetector {

	SiftImageScaleSpace ss;

	// finds features from 2D intensity image
	private FeatureExtractor extractor;
	// selects the features with the largest intensity
	private SelectNBestFeatures selectBest;

	int numOctaves;

	// target number of features for the extractor
	int maxFeatures;
	// storage for found features
	QueueCorner foundFeatures = new QueueCorner(10);

	// List of found feature points
	private FastQueue<ScalePoint> foundPoints = new FastQueue<ScalePoint>(10,ScalePoint.class,true);

	double currentScale;

	public SiftDetector(FeatureExtractor extractor,
						boolean doubleInputImage ,
						int numOfOctaves ,
						int numOfScales ,
						double scaleSigma ,
						int maxFeaturesPerScale) {
		this.extractor = extractor;
		this.numOctaves = numOfOctaves;
		if( maxFeaturesPerScale > 0 ) {
			// Each scale has detection run twice on it
			this.maxFeatures = maxFeaturesPerScale/2;
			selectBest = new SelectNBestFeatures(maxFeatures);
		}

		ss = new SiftImageScaleSpace(numOfScales,(float)scaleSigma,doubleInputImage );

		// ignore features along the border since a 3x3 region is assumed in parts of the code
		extractor.setIgnoreBorder(1);
	}

	public void process( ImageFloat32 input ) {
		// set up data structures
		foundPoints.reset();

		// compute initial octave's scale-space
		// todo sanity check input image size to make sure it is large enough
		ss.process(input);

		for( int octave = 0; octave < numOctaves; octave++ ) {
			if( octave > 0 )
				ss.computeNextOctave();
			ss.computeFeatureIntensity();

			for( int scale = 1; scale < ss.dog.length-1; scale++ ) {
				detectFeatures(scale,true);
				detectFeatures(scale,false);
			}
		}
	}

	private void detectFeatures(int scale , boolean positive ) {
		// set up data structures
		foundFeatures.reset();

		// the current scale factor being considered
		currentScale = ss.computeScaleSigma(scale);

		ImageFloat32 scale0 = ss.dog[scale-1];
		ImageFloat32 scale1 = ss.dog[scale];
		ImageFloat32 scale2 = ss.dog[scale+1];

		// adjusts sign so that just the peak can be compared
		float signAdj;

		if( positive ) {
			// detect maximums
			signAdj = 1;
			extractor.process(scale1,null, maxFeatures,foundFeatures);
		} else {
			// detect minimums
			signAdj = -1;
			PixelMath.multiply(scale1,-1,ss.storage);
			extractor.process(ss.storage,null, maxFeatures,foundFeatures);
		}

		// if configured to do so, only select the features with the highest intensity
		QueueCorner features;
		if( selectBest != null ) {
			selectBest.process(scale1,foundFeatures);
			features = selectBest.getBestCorners();
		} else {
			features = foundFeatures;
		}

		// see if they are a local max in scale space
		for( int i = 0; i < features.size; i++ ) {
			Point2D_I16 p = features.data[i];
			float value = scale1.unsafe_get(p.x, p.y);
			if( isScaleSpaceMax(scale0,scale2,p.x,p.y,value,signAdj) ) {
				addPoint(scale0,scale1,scale2,p.x,p.y,value,signAdj);
			}
		}
	}

	// todo NOTE this is a change from SIFT paper
	private void addPoint(ImageFloat32 scale0 , ImageFloat32 scale1, ImageFloat32 scale2,
						  short x, short y, float value, float signAdj) {

		// TODO remove low contract?
		// TODO eliminate edge response

		float x0 =  scale1.unsafe_get(x - 1, y)*signAdj;
		float x2 =  scale1.unsafe_get(x + 1, y)*signAdj;
		float y0 =  scale1.unsafe_get(x , y - 1)*signAdj;
		float y2 =  scale1.unsafe_get(x , y + 1)*signAdj;

		float s0 =  scale0.unsafe_get(x , y )*signAdj;
		float s2 =  scale2.unsafe_get(x , y )*signAdj;

		ScalePoint p = foundPoints.grow();

		p.x = ss.pixelScale*(x+ polyPeak(x0, value, x2));
		p.y = ss.pixelScale*(y + polyPeak(y0, value, y2));

		p.scale = currentScale + ss.pixelScale*ss.sigma*polyPeak(s0, value, s2);
	}

	/**
	 * See if the point is a local maximum in scale-space above and below.
	 *
	 * @param c_x x-coordinate of maximum
	 * @param c_y y-coordinate of maximum
	 * @param value The maximum value it is checking
	 * @param signAdj Adjust the sign so that it can check for maximums
	 * @return
	 */
	private boolean isScaleSpaceMax( ImageFloat32 scale0 , ImageFloat32 scale2,
									 int c_x , int c_y , float value , float signAdj ) {
		float v;

		value *= signAdj;

		for( int y = -1; y <= 1; y++ ) {
			for( int x = -1; x <= 1; x++ ) {
			    v = scale0.unsafe_get(c_x+x,c_y+y);
				if( v*signAdj >= value )
					return false;
				v = scale2.unsafe_get(c_x+x,c_y+y);
				if( v*signAdj >= value )
					return false;
			}
		}

		return true;
	}

	public FastQueue<ScalePoint> getFoundPoints() {
		return foundPoints;
	}
}
