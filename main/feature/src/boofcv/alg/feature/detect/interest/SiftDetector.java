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
import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.feature.detect.extract.SelectNBestFeatures;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.filter.convolve.FactoryConvolveSparse;
import boofcv.struct.FastQueue;
import boofcv.struct.QueueCorner;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;

import static boofcv.alg.feature.detect.interest.FastHessianFeatureDetector.polyPeak;

/**
 * <p>
 * Feature detector described in the Scale Invariant Feature Transform (SIFT) paper [1].  Location and scale of
 * blob like features are detected using a Difference of Gaussian (DOG) across scale-space.  Note the algorithmic
 * changes below.
 * </p>
 *
 * <p>
 * INTERPOLATION: Location and scale interpolation is done using a second order polynomial.  This avoids taking
 * the second order derivative numerically, which is very sensitive to noise.  Plus I disagree with his statement
 * that peaks outside the local region are valid and require iteration.
 * </p>
 *
 * <p>
 * LOW CONTRAST REJECTION: Try adjusting detection radius to reduce the number of low contrast returns
 * instead.  The technique proposed in the paper was not tested.
 * </p>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints".
 * International Journal of Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
public class SiftDetector {

	// Contains the image's  scale space representation
	protected SiftImageScaleSpace ss;

	// finds features from 2D intensity image
	private FeatureExtractor extractor;
	// selects the features with the largest intensity
	private SelectNBestFeatures selectBest;

	// target number of features for the extractor
	private int maxFeatures;
	// storage for found features
	private QueueCorner foundFeatures = new QueueCorner(10);

	// List of found feature points
	private FastQueue<ScalePoint> foundPoints = new FastQueue<ScalePoint>(10,ScalePoint.class,true);

	// Amount of blur applied to the current image being considered
	private double currentSigma;
	// Pixel scale factor for the current image being considered
	private double currentPixelScale;

	// Computes image derivatives. used in edge rejection
	private ImageConvolveSparse<ImageFloat32,?> derivXX;
	private ImageConvolveSparse<ImageFloat32,?> derivXY;
	private ImageConvolveSparse<ImageFloat32,?> derivYY;

	// Threshold for filtering out edges.
	private double edgeThreshold;

	/**
	 * Configures SIFT
	 *
	 * @param extractor Extracts local maximums from each scale.
	 * @param maxFeaturesPerScale Max detected features per scale.  Disable with < 0.  Try 500
	 * @param edgeThreshold Threshold for edge filtering.  Disable with a value <= 0.  Try 5
	 */
	public SiftDetector(FeatureExtractor extractor,
						int maxFeaturesPerScale,
						double edgeThreshold ) {
		this.extractor = extractor;
		if( maxFeaturesPerScale > 0 ) {
			// Each scale has detection run twice on it
			this.maxFeatures = maxFeaturesPerScale;
			selectBest = new SelectNBestFeatures(maxFeatures);
		}

		// ignore features along the border since a 3x3 region is assumed in parts of the code
		extractor.setIgnoreBorder(1);

		createDerivatives();

		this.edgeThreshold = edgeThreshold;
	}

	/**
	 * Define sparse image derivative operators.
	 */
	private void createDerivatives() {
		// TODO optimize usign a sparse kernel?
		Kernel2D_F32 kerX = new Kernel2D_F32(3,
				 0,0,0,
				-1,0,1,
				 0,0,0);
		Kernel2D_F32 kerY = new Kernel2D_F32(3,
				0,-1,0,
				0, 0,0,
				0, 1,0);
		Kernel2D_F32 kerXX = KernelMath.convolve2D(kerX, kerX);
		Kernel2D_F32 kerXY = KernelMath.convolve2D(kerX,kerY);
		Kernel2D_F32 kerYY = KernelMath.convolve2D(kerY,kerY);

		derivXX = FactoryConvolveSparse.create(ImageFloat32.class,kerXX);
		derivXY = FactoryConvolveSparse.create(ImageFloat32.class,kerXY);
		derivYY = FactoryConvolveSparse.create(ImageFloat32.class,kerYY);

		// treat pixels outside the image border as having a value of zero
		ImageBorder<ImageFloat32> border = FactoryImageBorder.value(ImageFloat32.class, 0);

		derivXX.setImageBorder(border);
		derivXY.setImageBorder(border);
		derivYY.setImageBorder(border);
	}

	public void process( SiftImageScaleSpace ss ) {
		// set up data structures
		foundPoints.reset();
		this.ss = ss;

		// extract features in each octave
		for( int octave = 0; octave < ss.actualOctaves; octave++ ) {
			// start processing at the second DOG since it needs the scales above and below
			int indexDOG = octave*(ss.numScales-1)+1;
			int indexScale = octave*ss.numScales+1;

			currentPixelScale = ss.pixelScale[octave];

			ss.storage.reshape( ss.scale[indexScale].width , ss.scale[indexScale].height );

			for( int scale = 1; scale < ss.numScales-2; scale++ , indexScale++,indexDOG++ ) {

				// use the scale-space image as input for derivatives
				derivXX.setImage(ss.scale[indexScale]);
				derivXY.setImage(ss.scale[indexScale]);
				derivYY.setImage(ss.scale[indexScale]);

				// the current scale factor being considered
				currentSigma = ss.computeScaleSigma(octave,scale);

				detectFeatures(indexDOG,true);
				detectFeatures(indexDOG,false);
			}
		}
	}

	/**
	 * Detect features inside the specified scale.
	 *
	 * @param positive Detect features with a positive or negative response
	 */
	private void detectFeatures( int indexDOG , boolean positive ) {
		// set up data structures
		foundFeatures.reset();

		// Local scale-space neighborhood
		ImageFloat32 scale0 = ss.dog[indexDOG-1];
		ImageFloat32 scale1 = ss.dog[indexDOG];
		ImageFloat32 scale2 = ss.dog[indexDOG+1];

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
			if( isScaleSpaceMax(scale0,scale2,p.x,p.y,value,signAdj)
					&& !isEdge(p.x,p.y) ) {
				addPoint(scale0,scale1,scale2,p.x,p.y,value,signAdj,positive);
			}
		}
	}

	/**
	 * Adds the detected feature to the list.  Interpolates the feature's location in the image and scale
	 * using 2nd order polynomial instead.  This is a change from the paper.
	 */
	private void addPoint(ImageFloat32 scale0 , ImageFloat32 scale1, ImageFloat32 scale2,
						  short x, short y, float value, float signAdj, boolean white ) {
		float x0 =  scale1.unsafe_get(x - 1, y)*signAdj;
		float x2 =  scale1.unsafe_get(x + 1, y)*signAdj;
		float y0 =  scale1.unsafe_get(x , y - 1)*signAdj;
		float y2 =  scale1.unsafe_get(x , y + 1)*signAdj;

		float s0 =  scale0.unsafe_get(x , y )*signAdj;
		float s2 =  scale2.unsafe_get(x , y )*signAdj;

		ScalePoint p = foundPoints.grow();

		p.x = currentPixelScale*(x + polyPeak(x0, value, x2));
		p.y = currentPixelScale*(y + polyPeak(y0, value, y2));

		p.scale = currentSigma + currentPixelScale*ss.sigma*polyPeak(s0, value, s2);
		p.white = white;
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

	/**
	 * Performs an edge test to remove false positives.  See 4.1 in [1].
	 */
	private boolean isEdge( int x , int y ) {
		if( edgeThreshold <= 0 )
			return false;

		double xx = derivXX.compute(x,y);
		double xy = derivXY.compute(x,y);
		double yy = derivYY.compute(x,y);

		double Tr = xx + yy;
		double det = xx*yy - xy*xy;
		double value = Tr*Tr/det;

		double threshold = edgeThreshold+2+1/edgeThreshold;

		// The SIFT paper does not show absolute value here nor have I put enough thought into it
		// to determine if this makes any sense.  However, it does seem to improve performance
		// quite a bit.
		return( Math.abs(value) > threshold);
	}

	/**
	 * Returns all the found points
	 */
	public FastQueue<ScalePoint> getFoundPoints() {
		return foundPoints;
	}
}
