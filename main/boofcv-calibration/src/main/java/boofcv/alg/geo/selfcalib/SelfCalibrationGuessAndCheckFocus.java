/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.PrintStream;
import java.util.List;

/**
 * <p>
 *     Computes the best projective to metric 4x4 rectifying homography matrix by guessing different values
 *     for focal lengths of the first two views. Focal lengths are guessed using a log scale. Skew and image center
 *     are both assumed to be known and have to be specified by the user. This strategy shows better convergence
 *     than methods which attempt to guess the focal length using linear or gradient descent approaches due
 *     to the vast number of local minima in the search space. Non-linear refinement is highly recommended after
 *     using this algorithm due to its approximate nature.
 * </p>
 * <p>
 *     NOTE: Performance on noise free synthetic data replicates paper claims. Have not been able to replicate
 *     performance on real data. Authors were contacted for a reference implementation and was told source code
 *     is not publicly available.
 * </p>
 *
 * <ul>
 *     <li>if sameFocus is set to true then the first two views are assumed to have approximately the same focal length</li>
 *     <li>Internally, the plane at infinity is computed using the known intrinsic parameters.</li>
 *     <li>Rectifying homography is computed using known K in first view and plane at infinity. lambda of 1 is assumed</li>
 * </ul>
 *
 * Changes from paper:
 * <ol>
 *     <li>Extracting K using absolute quadratic instead of rectifying homography</li>
 * </ol>
 *
 * @see EstimatePlaneAtInfinityGivenK
 *
 * <p>
 * <li>Gherardi, Riccardo, and Andrea Fusiello. "Practical autocalibration."
 * European Conference on Computer Vision. Springer, Berlin, Heidelberg, 2010.</li>
 * </p>
 *
 * @author Peter Abeles
 */
public class SelfCalibrationGuessAndCheckFocus {

	// storage for internally normalized camera matrices
	FastQueue<DMatrixRMaj> normalizedP;

	// used to estimate the plane at infinity
	EstimatePlaneAtInfinityGivenK estimatePlaneInf = new EstimatePlaneAtInfinityGivenK();
	Vector3D_F64 planeInf = new Vector3D_F64();

	// if true the first two cameras are assumed to have the same or approximately the same focus length
	boolean sameFocus;

	// intrinsic camera calibration matrix for view 1
	DMatrixRMaj K1 = new DMatrixRMaj(3,3);

	// Work space for view 1 projective matrix
	DMatrixRMaj P1 = new DMatrixRMaj(3,4);
	DMatrixRMaj tmpP = new DMatrixRMaj(3,4);

	// projective to metric homography
	DMatrixRMaj H = new DMatrixRMaj(4,4);
	DMatrixRMaj bestH = new DMatrixRMaj(4,4);

	// Absolute dual quadratic
	DMatrixRMaj Q = new DMatrixRMaj(4,4);

	// camera normalization matrices
	DMatrixRMaj V = new DMatrixRMaj(3,3);
	DMatrixRMaj Vinv = new DMatrixRMaj(3,3);

	// Defines which focus lengths are sampled based on a log scale
	// Note that image has been normalized and 1.0 = focal length of image diagonal
	double sampleMin=0.3,sampleMax=3;
	int numSamples=50;
	double scores[] = new double[numSamples];

	// Weights for score function
	double w_sk = 1.0/0.01; // zero skew
	double w_ar = 1.0/0.2;  // aspect ratio
	double w_uo = 1.0/0.1;  // zero principle point

	CameraPinhole intrinsic = new CameraPinhole();

	DMatrixRMaj tmp = new DMatrixRMaj(3,3);

	// Is the best score at a local minimum? If not that means it probably diverged
	boolean localMinimum;

	// if not null debug info is printed
	PrintStream verbose;

	public SelfCalibrationGuessAndCheckFocus() {
		normalizedP = new FastQueue<DMatrixRMaj>(DMatrixRMaj::new ) {
			@Override
			protected DMatrixRMaj createInstance() {
				return new DMatrixRMaj(3,4);
			}
		};
	}

	/**
	 * Specifies known portions of camera intrinsic parameters
	 * @param skew skew
	 * @param cx image center x
	 * @param cy image center y
	 * @param width Image width
	 * @param height Image height
	 */
	public void setCamera( double skew , double cx , double cy , int width , int height ) {

		// Define normalization matrix
		// center points, remove skew, scale coordinates
		double d = Math.sqrt(width*width + height*height);
		V.zero();
		V.set(0,0,d/2); V.set(0,1,skew); V.set(0,2,cx);
		V.set(1,1,d/2); V.set(1,2,cy);
		V.set(2,2,1);

		CommonOps_DDRM.invert(V,Vinv);
	}

	/**
	 * Specifies how focal lengths are sampled on a log scale. Remember 1.0 = nominal length
	 *
	 * @param min min value. 0.3 is default
	 * @param max max value. 3.0 is default
	 * @param total Number of sample points. 50 is default
	 */
	public void setSampling( double min , double max , int total ) {
		this.sampleMin = min;
		this.sampleMax = max;
		this.numSamples = total;
		this.scores = new double[numSamples];
	}

	/**
	 * Computes the best rectifying homography given the set of camera matrices. Must call {@link #setCamera} first.
	 *
	 * @param cameraMatrices camera matrices for view 2 and beyond. view 1 is implicit and assumed to be P = [I|0]
	 * @return true if successful or false if it fails
	 */
	public boolean process(List<DMatrixRMaj> cameraMatrices) {
		if( cameraMatrices.size() == 0 )
			throw new IllegalArgumentException("Must contain at least 1 matrix");

		// Apply normalization as suggested in the paper, then force the first camera matrix to be [I|0] again
		CommonOps_DDRM.setIdentity(tmpP);
		CommonOps_DDRM.mult(Vinv,tmpP,P1);
		MultiViewOps.projectiveToIdentityH(P1,H);

		// P = inv(V)*P/||P(2,0:2)||
		this.normalizedP.reset();
		for (int i = 0; i < cameraMatrices.size(); i++) {
			DMatrixRMaj A = cameraMatrices.get(i);

			DMatrixRMaj Pi = normalizedP.grow();
			CommonOps_DDRM.mult(Vinv,A,tmpP);
			CommonOps_DDRM.mult(tmpP,H,Pi);
			double a0 = Pi.get(2,0);
			double a1 = Pi.get(2,1);
			double a2 = Pi.get(2,2);
			double scale = Math.sqrt(a0*a0 + a1*a1 + a2*a2);
			CommonOps_DDRM.scale(1.0/scale,Pi);
		}

		// Find the best combinations of focal lengths
		double bestScore;
		if( sameFocus ) {
			bestScore = findBestFocusOne(normalizedP.get(0));
		} else {
			bestScore = findBestFocusTwo(normalizedP.get(0));
		}

		// undo normalization
		CommonOps_DDRM.extract(bestH,0,0,tmp);
		CommonOps_DDRM.mult(V,tmp, K1);
		CommonOps_DDRM.insert(K1,bestH,0,0);

		// if it's not at a local minimum it almost definately failed
		return bestScore != Double.MAX_VALUE && localMinimum;
	}

	private double findBestFocusOne(DMatrixRMaj P2) {
		localMinimum = false;

		// coeffients for linear to log scale
		double b = Math.log(sampleMax/sampleMin)/(numSamples-1);
		double bestScore = Double.MAX_VALUE;
		int bestIndex = -1;

		for (int i = 0; i < numSamples; i++) {
			double f =sampleMin*Math.exp(b*i);

			if( !computeRectifyH(f,f,P2,H)) {
				scores[i] = Double.NaN;
				continue;
			}
			MultiViewOps.rectifyHToAbsoluteQuadratic(H,Q);

			double score = scoreResults();
			scores[i] = score;

			if( score < bestScore ) {
				bestScore = score;
				bestH.set(H);
				bestIndex = i;
			}

			if( verbose != null ) {
				verbose.printf("[%3d] f=%5.2f score=%f\n",i,f,score);
			}
		}

		if (bestIndex > 0 && bestIndex < numSamples - 1) {
			localMinimum = bestScore < scores[bestIndex - 1] && bestScore < scores[bestIndex + 1];
		}

		return bestScore;
	}

	private double findBestFocusTwo(DMatrixRMaj P2) {
		localMinimum = false;

		// coeffients for linear to log scale
		double b = Math.log(sampleMax/sampleMin)/(numSamples-1);
		double bestScore = Double.MAX_VALUE;

		for (int i = 0; i < numSamples; i++) {
			double f1 =sampleMin*Math.exp(b*i);

			boolean minimumChanged = false;
			int bestIndex = -1;

			for (int j = 0; j < numSamples; j++) {
				double f2 =sampleMin*Math.exp(b*j);

				if( !computeRectifyH(f1,f2,P2,H)) {
					scores[i] = Double.NaN;
					continue;
				}
				MultiViewOps.rectifyHToAbsoluteQuadratic(H,Q);

				double score = scoreResults();
				scores[j] = score;

				if( score < bestScore ) {
					minimumChanged = true;
					bestIndex = j;
					bestScore = score;
					bestH.set(H);
				}

				if( verbose != null ) {
					verbose.printf("[%3d,%3d] f1=%5.2f f2=%5.2f score=%f\n",i,j,f1,f2,score);
				}
			}

			if( minimumChanged ) {
				if (bestIndex > 0 && bestIndex < numSamples - 1) {
					localMinimum = bestScore< scores[bestIndex - 1] && bestScore < scores[bestIndex + 1];
				} else {
					localMinimum = false;
				}
			}
		}
		return bestScore;
	}

	/**
	 * Given the focal lengths for the first two views compute homography H
	 * @param f1 view 1 focal length
	 * @param f2 view 2 focal length
	 * @param P2 projective camera matrix for view 2
	 * @param H (Output) homography
	 * @return true if successful
	 */
	boolean computeRectifyH( double f1 , double f2 , DMatrixRMaj P2, DMatrixRMaj H ) {

		estimatePlaneInf.setCamera1(f1,f1,0,0,0);
		estimatePlaneInf.setCamera2(f2,f2,0,0,0);

		if( !estimatePlaneInf.estimatePlaneAtInfinity(P2,planeInf) )
			return false;

		// TODO add a cost for distance from nominal and scale other cost by focal length fx for each view
//		RefineDualQuadraticConstraint refine = new RefineDualQuadraticConstraint();
//		refine.setZeroSkew(true);
//		refine.setAspectRatio(true);
//		refine.setZeroPrinciplePoint(true);
//		refine.setKnownIntrinsic1(true);
//		refine.setFixedCamera(false);
//
//		CameraPinhole intrinsic = new CameraPinhole(f1,f1,0,0,0,0,0);
//		if( !refine.refine(normalizedP.toList(),intrinsic,planeInf))
//			return false;

		K1.zero();
		K1.set(0,0,f1);
		K1.set(1,1,f1);
		K1.set(2,2,1);
		MultiViewOps.createProjectiveToMetric(K1,planeInf.x,planeInf.y,planeInf.z,1,H);

		return true;
	}

	/**
	 * Extracts the calibration matrix for each view and computes the score according to:
	 *
	 * w_sk*|K[0,1]| + w_ar*|K[0,0]-K[1,1]| + w_ao*(|K[0,2]| + |K[1,2]|)
	 *
	 * which gives matrices which fit the constraints lower scores.
	 */
	double scoreResults() {

		double totalScore = 0;

		for (int i = 0; i < normalizedP.size; i++) {
			DMatrixRMaj P = normalizedP.get(i);
			MultiViewOps.intrinsicFromAbsoluteQuadratic(Q,P,intrinsic);

			double score = 0;

			// skew should be zero
			score += w_sk*Math.abs(intrinsic.skew);
			// aspect ratio unity
			score += w_ar*(Math.max(intrinsic.fx,intrinsic.fy)/Math.min(intrinsic.fx,intrinsic.fy) - 1);
			// principle point zero
			score += w_uo*(Math.abs(intrinsic.cx) + Math.abs(intrinsic.cy));

			totalScore += score;
		}
		return totalScore;
	}

	public boolean isSameFocus() {
		return sameFocus;
	}

	public void setSingleCamera(boolean sameFocus) {
		this.sameFocus = sameFocus;
	}

	/**
	 * Returns the projective to metric rectifying homography
	 */
	public DMatrixRMaj getRectifyingHomography() {
		return bestH;
	}

	public boolean isLocalMinimum() {
		return localMinimum;
	}

	public void setVerbose(PrintStream out , int level ) {
		this.verbose = out;
	}
}
