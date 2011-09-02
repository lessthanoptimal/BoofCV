/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.struct.QueueCorner;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;
import jgrl.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects scale invariant interest/corner features.  The general ideal of this approach to {@link GaussianScaleSpace scale space} feature
 * detection is to use feature detector which is robust in 2D to find candidate feature points then use a metric
 * which is robust in scale space to identify local maximums in scale space.  See [1] for more details.
 * </p>
 *
 * <p>
 * This implementation is a generalized and tweaked version of the algorithm presented in [1], which was
 * designed specifically for the {@link boofcv.alg.feature.detect.intensity.HarrisCornerIntensity Harris} corner detector.
 * </p>
 *
 * <p>
 * Algorithm Summary:
 * <ol>
 * <li>Detect feature intensities and local maximums (candidates) in each image scale.</li>
 * <li>For each candidate search the local 3x3 region in the scale images above and below it to see if it i
 * a local maximum in scale-space. </li>
 * </ol>
 * Candidates are not considered in the lower and upper most scale-spaces.
 * </p>
 *
 *
 * <p>
 * To normalize feature intensity across scales each feature intensity is multiplied by the scale to the power of 'scalePower'.
 * See [1,2] for how to compute 'scalePower'.
 * </p>
 *
 * <p>
 * [1] Krystian Mikolajczyk and Cordelia Schmid, "Indexing based on scale invariant interest points"  ICCV 2001. Proceedings.<br>
 * [2] Lindeberg, T., "Feature detection with automatic scale selection." IJCV 30(2) (1998) 79 – 116
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FeatureLaplaceScaleSpace<T extends ImageBase, D extends ImageBase> {

	// generalized feature detector.  Used to find candidate features in each scale's image
	private GeneralFeatureDetector<
			T,D> detector;
	private float baseThreshold;
	// feature intensity in the pyramid
	protected T localSpace[];
	protected int spaceIndex = 0;

	protected List<Point2D_I16> maximums[];

	// List of found feature points
	protected List<ScalePoint> foundPoints = new ArrayList<ScalePoint>();

	// how much the feature intensity is scaled in each level
	// varies depending on feature type
	protected double scalePower;

	// Function used to compute the Laplacian at each scale point
	private ImageFunctionSparse<T> sparseLaplace;

	/**
	 * Create a feature detector.
	 *
	 * @param detector Point feature detector which is used to find candidates in each scale level
	 * @param scalePower Used to adjust feature intensity in each level to make them comparable.  Feature dependent, but
	 * most common features should use a value of 2.
	 */
	public FeatureLaplaceScaleSpace( GeneralFeatureDetector<T, D> detector ,
									 ImageFunctionSparse<T> sparseLaplace , double scalePower ) {
		this.detector = detector;
		this.sparseLaplace = sparseLaplace;
		this.baseThreshold = detector.getThreshold();
		this.scalePower = scalePower;

		localSpace = (T[])new ImageBase[3];

		maximums = new List[ 3 ];
		maximums[0] = new ArrayList<Point2D_I16>();
		maximums[1] = new ArrayList<Point2D_I16>();
		maximums[2] = new ArrayList<Point2D_I16>();
	}

	/**
	 * Searches for features inside the provided scale space
	 *
	 * @param ss Scale space of an image
	 */
	public void detect( GaussianScaleSpace<T,D> ss ) {
		spaceIndex = 0;
		foundPoints.clear();

		// compute feature intensity in each level
		for( int i = 0; i < ss.getTotalScales(); i++ ) {
			ss.setActiveScale(i);

			// save local scale space
			T image = ss.getScaledImage();

			if( localSpace[spaceIndex] == null ) {
				localSpace[spaceIndex] = (T)image.clone();
			} else {
				localSpace[spaceIndex].reshape(image.width,image.height);
				localSpace[spaceIndex].setTo(image);
			}

			// detect features in 2D space
			// don't need to detect features for scale spaces at the tail end
			if( i > 0 && i < ss.getTotalScales()-1)
				detectCandidateFeatures(ss,ss.getCurrentScale());

			spaceIndex++;
			if( spaceIndex >= 3 )
				spaceIndex = 0;

			// check to see if they are local features in scale-space
			if( i >= 2 ) {
				findLocalScaleSpaceMax(ss,i-1);
			}
		}
	}


	/**
	 * Use the feature detector to find candidate features in each level.  Only compute the needed image derivatives.
	 */
	private void detectCandidateFeatures( GaussianScaleSpace<T,D> ss , double scale ) {
		// adjust corner intensity threshold based upon the current scale factor
		float scaleThreshold = (float)(baseThreshold/Math.pow(scale,scalePower));
		detector.setThreshold(scaleThreshold);

		D derivX = null, derivY = null;
		D derivXX = null, derivYY = null, derivXY = null;

		if( detector.getRequiresGradient() ) {
			derivX = ss.getDerivative(true);
			derivY = ss.getDerivative(false);
		}
		if( detector.getRequiresHessian() ) {
			derivXX = ss.getDerivative(true,true);
			derivYY = ss.getDerivative(false,false);
			derivXY = ss.getDerivative(true,false);
		}

		T image = ss.getScaledImage();
		detector.process(image,derivX,derivY,derivXX,derivYY,derivXY);
		List<Point2D_I16> m = maximums[spaceIndex];
		m.clear();
		QueueCorner q = detector.getFeatures();
		for( int i = 0; i < q.num; i++ ) {
			m.add( q.get(i).copy() );
		}
	}

	/**
	 * Searches the pyramid layers up and down to see if the found 2D features are also scale space maximums.
	 */
	protected void findLocalScaleSpaceMax( GaussianScaleSpace<T,D> ss , int layerID ) {
		int index0 = spaceIndex;
		int index1 = (spaceIndex + 1) % 3;
		int index2 = (spaceIndex + 2) % 3;

		List<Point2D_I16> candidates = maximums[index1];
		T image0 = localSpace[index0];
		T image1 = localSpace[index1];
		T image2 = localSpace[index2];

		float scale0 = (float)ss.getScale(layerID-1);
		float scale1 = (float)ss.getScale(layerID);
		float scale2 = (float)ss.getScale(layerID+1);

		float ss0 = (float)Math.pow(scale0,scalePower);
		float ss1 = (float)Math.pow(scale1,scalePower);
		float ss2 = (float)Math.pow(scale2,scalePower);

		for( Point2D_I16 c : candidates ) {
			sparseLaplace.setImage(image1);
			float val = ss1*(float)Math.abs(sparseLaplace.compute(c.x,c.y));

			if( checkMax(image0, ss0, val, c.x, c.y) && checkMax(image2, ss2, val, c.x, c.y) ) {
				// put features into the scale of the upper image
				foundPoints.add( new ScalePoint(c.x,c.y,scale1));
			}
		}
	}

	private boolean checkMax(T image, float scoreAdjust, float bestScore, int c_x, int c_y) {
		sparseLaplace.setImage(image);
		boolean isMax = true;
		beginLoop:
		for( int i = c_y -1; i <= c_y+1; i++ ) {
			for( int j = c_x-1; j <= c_x+1; j++ ) {

				if( scoreAdjust*Math.abs(sparseLaplace.compute(j,i)) >= bestScore ) {
					isMax = false;
					break beginLoop;
				}
			}
		}
		return isMax;
	}


	public List<ScalePoint> getInterestPoints() {
		return foundPoints;
	}
}
