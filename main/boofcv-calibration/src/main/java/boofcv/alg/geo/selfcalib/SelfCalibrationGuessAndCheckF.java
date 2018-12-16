/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.List;

/**
 * <p>
 *     Computes the best projective to metric 4x4 rectifying homography matrix by guessing different values
 *     for focal lengths of the first two views. Focal lengths are guessed using a log scale. Skew and image center
 *     are both assumed to be known and have to be specified by the user. This strategy shows better convergence
 *     than methods which attempt to guess the focal length using linear or gradient descent approaches due
 *     to the vast number of local minima in the search space.
 * </p>
 *
 * <ul>
 *     <li>if sameFocus is set to true then the first two views are assumed to have approximately the same focal length</li>
 *     <li>Internally, the plane at infinity is computed using the known intrinsic parameters.</li>
 *     <li>Rectifying homography is computed using known K in first view and plane at infinity. lambda of 1 is assumed</li>
 * </ul>
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
public class SelfCalibrationGuessAndCheckF {

	// storage for internally normalized camera matrices
	FastQueue<DMatrixRMaj> normalizedP;

	// used to estimate the plane at infinity
	EstimatePlaneAtInfinityGivenK estimatePlaneInf = new EstimatePlaneAtInfinityGivenK();
	Vector3D_F64 planeInf = new Vector3D_F64();

	// if true the first two cameras are assumed to have the same or approximately the same focus length
	boolean sameFocus;

	// intrinsic camera matrix for view 1
	DMatrixRMaj K = new DMatrixRMaj(3,3);
	DMatrixRMaj K2 = new DMatrixRMaj(3,3);

	// projective to metric homography
	DMatrixRMaj H = new DMatrixRMaj(4,4);
	DMatrixRMaj bestH = new DMatrixRMaj(4,4);

	// camera normalization matrices
	DMatrixRMaj V = new DMatrixRMaj(3,3);
	DMatrixRMaj Vinv = new DMatrixRMaj(3,3);

	// Defines which focus lengths are sampled based on a log scale
	// Note that image has been normalized and 1.0 = focal length of image diagonal
	double sampleMin=0.3,sampleMax=3,numSamples=50;

	// Weights for score function
	double w_sk = 1.0/0.01; // zero skew
	double w_ar = 1.0/0.2;  // aspect ratio
	double w_uo = 1.0/0.1;  // zero principle point

	DMatrixRMaj tmp = new DMatrixRMaj(3,3);

	public SelfCalibrationGuessAndCheckF() {
		normalizedP = new FastQueue<DMatrixRMaj>(DMatrixRMaj.class,true ) {
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
	}

	/**
	 * Computes the best rectifying homography given the set of camera matrices. Must call {@link #setCamera} first.
	 *
	 * @param cameraMatrices camera matrices for view 2 and beyound. view 1 will have P = [I|0]
	 * @return true if successful or false if it fails
	 */
	public boolean process(List<DMatrixRMaj> cameraMatrices) {
		if( cameraMatrices.size() == 0 )
			throw new IllegalArgumentException("Must contain at least 1 matrix");

		Vinv.print();
		// P = inv(V)*P/||P(2,0:2)||
		this.normalizedP.reset();
		for (int i = 0; i < cameraMatrices.size(); i++) {
			DMatrixRMaj A = cameraMatrices.get(i);
			double a0 = A.get(2,0);
			double a1 = A.get(2,1);
			double a2 = A.get(2,2);
			double scale = Math.sqrt(a0*a0 + a1*a1 + a2*a2);

			DMatrixRMaj Pi = normalizedP.grow();
			CommonOps_DDRM.mult(1.0/1.0,Vinv,A,Pi);
		}

		// Find the best combinations of focal lengths
		double bestScore;
		if( sameFocus ) {
			bestScore = findBestFocusOne(normalizedP.get(0));
		} else {
			bestScore = findBestFocusTwo(normalizedP.get(0));
		}

		// undo normalization
//		CommonOps_DDRM.extract(bestH,0,0,tmp);
//		CommonOps_DDRM.mult(V,tmp, K2);
//		CommonOps_DDRM.insert(K2,bestH,0,0);

		return bestScore != Double.MAX_VALUE;
	}

	private double findBestFocusOne(DMatrixRMaj P2) {
		// coeffients for linear to log scale
		double b = Math.log(sampleMax/sampleMin)/(numSamples-1);
		double bestScore = Double.MAX_VALUE;

		for (int i = 0; i < numSamples; i++) {
			double f =sampleMin*Math.exp(b*i);

			if( !computeRectifyH(f,f,P2,H))
				continue;

			double score = scoreResults();
			if( score < bestScore ) {
				bestScore = score;
				bestH.set(H);
			}
			System.out.println(i+"  f="+f+" score = "+score);
		}
		return bestScore;
	}

	private double findBestFocusTwo(DMatrixRMaj P1) {
		// coeffients for linear to log scale
		double b = Math.log(sampleMax/sampleMin)/(numSamples-1);
		double bestScore = Double.MAX_VALUE;

		for (int i = 0; i < numSamples; i++) {
			double f1 =sampleMin*Math.exp(b*i);
			for (int j = 0; j < numSamples; j++) {
				double f2 =sampleMin*Math.exp(b*i);

				if( !computeRectifyH(f1,f2,P1,H))
					continue;

				double score = scoreResults();
				if( score < bestScore ) {
					bestScore = score;
					bestH.set(H);
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
		K.zero();
		K.set(0,0,f1);K.set(1,1,f1);K.set(2,2,1);
		CommonOps_DDRM.mult(V,K,K2);

		double fx = K2.get(0,0);
		double fy = K2.get(1,1);
		double skew = K2.get(0,1);
		double cx = K2.get(0,2);
		double cy = K2.get(1,2);
		// have to undo normalization for first since its camera matrix remains un-normalized
		estimatePlaneInf.setCamera1(fx,fy,skew,cx,cy);
		estimatePlaneInf.setCamera2(f2,f2,0,0,0);

		if( !estimatePlaneInf.estimatePlaneAtInfinity(P2,planeInf) )
			return false;

//		K2.zero();
//		K2.set(0,0,f1*100);
//		K2.set(1,1,f1*100);
//		K2.set(2,2,1);

		MultiViewOps.createProjectiveToMetric(K2,planeInf.x,planeInf.y,planeInf.z,1,H);
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
		DMatrixRMaj K = new DMatrixRMaj(3,3);
		Se3_F64 a = new Se3_F64();
		DMatrixRMaj Pn = new DMatrixRMaj(3,4);

		double totalScore = 0;

		for (int i = 0; i < normalizedP.size; i++) {
			DMatrixRMaj P = normalizedP.get(i);
			CommonOps_DDRM.mult(P,H,Pn);

			// Decompose the metric camera projection matrix.
			// NOTE: This could be speed lot and memory usage reduced by creating a class for it
			MultiViewOps.decomposeMetricCamera(Pn,K,a);

			double score = 0;
			score += w_sk*Math.abs(K.get(0,1));
			score += w_ar*Math.abs(K.get(0,0)-K.get(1,1));
			score += w_uo*(Math.abs(K.get(0,2)) + Math.abs(K.get(1,2)));

			totalScore += score;
		}
		return totalScore;
	}

	public boolean isSameFocus() {
		return sameFocus;
	}

	public void setSameFocus(boolean sameFocus) {
		this.sameFocus = sameFocus;
	}

	/**
	 * Returns the projective to metric rectifying homography
	 */
	public DMatrixRMaj getRectifyingHomography() {
		return bestH;
	}
}
