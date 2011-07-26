/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.interest;

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.filter.ImageFunctionSparse;
import gecv.struct.QueueCorner;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageBase;
import jgrl.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects scale invariant interest/corner features.  This is a generalization of the algorithm presented in [1], which was
 * designed specifically for the {@link gecv.alg.detect.corner.HarrisCornerIntensity Harris} corner detector.  The
 * algorithm works by detecting corners in each scale in a {@link gecv.struct.gss.GaussianScaleSpace scale-space}.  Then
 * it selects features whose Laplacian are a local maximum in the scale space.
 * </p>
 *
 * <p>
 * [1] Krystian Mikolajczyk and Cordelia Schmid, "Indexing based on scale invariant interest points"  ICCV 2001. Proceedings.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class CornerLaplaceScaleSpace<T extends ImageBase, D extends ImageBase> {

	// corner detector
	private GeneralCornerDetector<T,D> detector;
	private float baseThreshold;

	//---- Variables related to the local scale-space
	// Which local scales are valid.
	private boolean activeLocal[] = new boolean[3];
	// input images different local scales
	private T localSpace[] = (T[])new ImageBase[3];
	// the values of the local scales
	private double scales[] = new double[3];

	// Function used to compute the Laplacian at each scale point
	private ImageFunctionSparse<T> sparseLaplace;

	// List of found feature points
	List<ScalePoint> foundPoints = new ArrayList<ScalePoint>();

	/**
	 * Create a feature detector.
	 *
	 * @param detector Point feature detector which is used to find candidates in each scale level
	 * @param sparseLaplace Used to validate found features
	 */
	public CornerLaplaceScaleSpace(GeneralCornerDetector<T, D> detector, ImageFunctionSparse<T> sparseLaplace ) {
		this.detector = detector;
		this.sparseLaplace = sparseLaplace;

		baseThreshold = detector.getThreshold();
	}

	/**
	 * Searches for features inside the provided scale space
	 *
	 * @param ss Scale space of an image
	 */
	public void detect( GaussianScaleSpace<T,D> ss ) {
		if( ss.getTotalScales() <= 2 ) {
			throw new IllegalArgumentException("There must be at least two scale levels");
		}
		// reset internal variables
		setupInternal(ss);

		// Go through each scale and search for features
		for( int i = 0; i < ss.getTotalScales(); i++ ) {
			ss.setActiveScale(i);
			// detect features in this scale space
			detectCandidateFeatures(ss);

			// Validate the found features using the Laplacian across the local scale space
			checkMaxInScaleSpace();

			// prepare the scale-space for the next iteration
			shiftLocalScaleSpace(ss, i);
		}
	}

	/**
	 * Changes the local scale-space by shifting it over by one
	 */
	private void shiftLocalScaleSpace(GaussianScaleSpace<T, D> ss, int scaleIndex) {
		T tmp = localSpace[0];
		localSpace[0] = localSpace[1];
		localSpace[1] = localSpace[2];
		localSpace[2] = tmp;
		scales[0] = scales[1];
		scales[1] = scales[2];
		activeLocal[0] = true;
		if( scaleIndex < ss.getTotalScales() - 2 ) {
			ss.setActiveScale(scaleIndex+2);
			localSpace[2].setTo(ss.getScaledImage());
			scales[2] = ss.getCurrentScale();
		} else {
			activeLocal[2] = false;
		}
	}

	/**
	 * Sets up internal data structures
	 */
	private void setupInternal(GaussianScaleSpace<T, D> ss) {
		foundPoints.clear();

		activeLocal[0] = false;
		activeLocal[1] = activeLocal[2] = true;

		// if needed declare the local scale-space
		if( localSpace[0] == null ) {
			T a = ss.getScaledImage();
			localSpace[0] = (T)a._createNew(a.width,a.height);
			localSpace[1] = (T)a._createNew(a.width,a.height);
			localSpace[2] = (T)a._createNew(a.width,a.height);
		}

		// initialize the local scale space
		ss.setActiveScale(0);
		localSpace[1].setTo(ss.getScaledImage());
		scales[1] = ss.getCurrentScale();
		ss.setActiveScale(1);
		localSpace[2].setTo(ss.getScaledImage());
		scales[2] = ss.getCurrentScale();
	}

	/**
	 * Use the feature detector to find candidate features in each level.  Only compute the needed image derivatives.
	 */
	private void detectCandidateFeatures(GaussianScaleSpace<T, D> ss) {
		double scale = ss.getCurrentScale();
		// adjust corner intensity threshold based upon the current scale factor
		float scaleThreshold = (float)(baseThreshold/(scale*scale));
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

		detector.process(ss.getScaledImage(),derivX,derivY,derivXX,derivYY,derivXY);
	}

	/**
	 * Checks to see the candidate feature points are a local maximum in the Laplacian's scale-space.
	 */
	private void checkMaxInScaleSpace() {
		QueueCorner corners = detector.getCorners();

		double l0,l1,l2;
		// set values to -1 so if its at the extreme edge of scale-space the value is ignored
		l0=l1=l2=-1;
		// pre-compute scale normalization
		double ss0 = scales[0]*scales[0];
		double ss1 = scales[1]*scales[1];
		double ss2 = scales[2]*scales[2];

		for( int i = 0; i < corners.num; i++ ) {
			Point2D_I16 c = corners.get(i);

			if( activeLocal[0] ) {
				sparseLaplace.setImage(localSpace[0]);
				l0 = ss0*Math.abs(sparseLaplace.compute(c.x,c.y));
			}
			if( activeLocal[1] ) {
				sparseLaplace.setImage(localSpace[1]);
				l1 = ss1*Math.abs(sparseLaplace.compute(c.x,c.y));
			}
			if( activeLocal[2] ) {
				sparseLaplace.setImage(localSpace[2]);
				l2 = ss2*Math.abs(sparseLaplace.compute(c.x,c.y));
			}
			if( l1 > l0 && l1 > l2 ) {
				foundPoints.add( new ScalePoint(c.x,c.y,scales[1]));
			}
		}
	}


	public List<ScalePoint> getInterestPoints() {
		return foundPoints;
	}
}
