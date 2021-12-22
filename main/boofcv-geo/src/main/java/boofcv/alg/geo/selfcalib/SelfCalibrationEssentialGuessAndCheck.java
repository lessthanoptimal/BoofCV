/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedPair;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * <p>
 * Brute force sampling approach to perform self calibration of a partially calibrated image. The focal length
 * for the first and second images are selected by sampling a grid of values and selecting the hypothesis with
 * the smallest model fit error is selected.
 * </p>
 *
 * <p>
 * Assumptions: fx and fy are identical, zero principle point, and no lens distortion.
 * There are no false positives in the input set.
 * </p>
 *
 * Steps:
 * <ol>
 *     <li>Create a list of different focal length hypothesis</li>
 *     <li>Use a given hypothesis to compute the Essential matrix</li>
 *     <li>Decompose essential matrix and get 4 extrinsic hypotheses</li>
 *     <li>Select best hypothesis and save result. Repeat for all focal lengths</li>
 * </ol>
 * The list of hypothetical focal lengths is generated using a log scale causing it to generate more hypotheses on
 * the lower end. The camera matrix for view-1 is an 3x4 identity matrix.
 *
 * The output comes in various forms:
 * <ul>
 *     <li>Select focal lengths for camera</li>
 *     <li>Rectifying homography</li>
 * </ul>
 *
 * <ol>
 * <li> P. Abeles, "BoofCV Technical Report: Automatic Camera Calibration" 2020-1 </li>
 * </ol>
 *
 * @author Peter Abeles
 * @see TwoViewToCalibratingHomography
 */
public class SelfCalibrationEssentialGuessAndCheck implements VerbosePrint {

	//---------------------------- Configuration Parameters
	/** Range of values focal length values will sample. Fraction relative to  {@link #imageLengthPixels} */
	public @Getter double sampleFocalRatioMin = 0.3, sampleFocalRatioMax = 2.5;
	/** Number of values it will sample */
	public @Getter int numberOfSamples = 50;
	/** if true the focus is assumed to be the same for the first two images */
	public @Getter boolean fixedFocus = false;

	//--------------------------- Output Variables
	/** The selected focal length for the first image */
	public @Getter double focalLengthA;
	/** The selected focal length for the second image */
	public @Getter double focalLengthB;
	/** The selected rectifying homography */
	public @Getter final DMatrixRMaj rectifyingHomography = new DMatrixRMaj(4, 4);
	/** If true that indicates that the selected focal length was at the upper or lower limit. This can indicate a fault */
	public @Getter boolean isLimit;

	/** The length of the longest side in the image. In pixels. */
	public int imageLengthPixels = 0;

	/** Generates and scores a hypothesis given two intrinsic camera matrices */
	public @Getter final TwoViewToCalibratingHomography calibrator = new TwoViewToCalibratingHomography();

	// SVD use to compute fit score. Just need singular values
	private final SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(3, 3, false, false, false);
	// storage for essential matrix used
	final DMatrixRMaj E = new DMatrixRMaj(3, 3);
	// storage for guess intrinsic camera matrices
	final DMatrixRMaj K1 = new DMatrixRMaj(3, 3);
	final DMatrixRMaj K2 = new DMatrixRMaj(3, 3);

	// If not null then verbose information is printed
	private @Nullable PrintStream verbose;

	/**
	 * Specifies the range of focal lengths it will evaluate as ratio of {@link #imageLengthPixels}
	 *
	 * @param sampleFocalRatioMin The minimum allowed focal length ratio
	 * @param sampleFocalRatioMax Tha maximum allowed focal length ratio
	 */
	public void configure( double sampleFocalRatioMin, double sampleFocalRatioMax ) {
		this.sampleFocalRatioMin = sampleFocalRatioMin;
		this.sampleFocalRatioMax = sampleFocalRatioMax;
	}

	/**
	 * Selects the best focal length(s) given the trifocal tensor and observations
	 *
	 * @param F21 (Input) Fundamental matrix between view-1 and view-2
	 * @param P2 (Input) Projective camera matrix for view-1 with inplicit identity matrix view-1
	 * @param observations (Input) Observation for all three views. Highly recommend that RANSAC or similar is used to
	 * remove false positives first.
	 * @return true if successful
	 */
	public boolean process( DMatrixRMaj F21, DMatrixRMaj P2, List<AssociatedPair> observations ) {
		// sanity check configurations
		checkTrue(imageLengthPixels > 0, "Must set imageLengthPixels to max(imageWidth,imageHeight)");
		checkTrue(sampleFocalRatioMin != 0 && sampleFocalRatioMax != 0, "You must call configure");
		BoofMiscOps.checkTrue(sampleFocalRatioMin < sampleFocalRatioMax && sampleFocalRatioMin > 0);
		BoofMiscOps.checkTrue(observations.size() > 0);
		BoofMiscOps.checkTrue(numberOfSamples > 0);

		// Pass in the trifocal tensor so that it can estimate self calibration
		calibrator.initialize(F21, P2);

		// coeffients for linear to log scale
		double logCoef = Math.log(sampleFocalRatioMax/sampleFocalRatioMin)/(numberOfSamples - 1);

		isLimit = false;

		if (fixedFocus) {
			searchFixedFocus(logCoef);
		} else {
			searchDynamicFocus(logCoef);
		}
		// compute the rectifying homography from the best solution
		computeHomography(focalLengthA, focalLengthB, observations);

		// DESIGN NOTE:
		// Could fit a 1-D or 2-D quadratic and get additional accuracy. Then compute the H at that value
		// In real data it seems that outliers drive the error more than a slightly incorrect focal length
		// so it's probably not worth the effort.

		return true;
	}

	/**
	 * Assumes that each camera can have an independent focal length value and searches a 2D grid
	 *
	 * @param logCoef coeffient for log scale
	 */
	private void searchDynamicFocus( double logCoef ) {
		K1.set(2, 2, 1);
		K2.set(2, 2, 1);

		double bestError = Double.MAX_VALUE;

		for (int idxA = 0; idxA < numberOfSamples; idxA++) {
			double focalRatioA = sampleFocalRatioMin*Math.exp(logCoef*idxA);
			double focalPixelsA = focalRatioA*imageLengthPixels;
			K1.set(0, 0, focalPixelsA);
			K1.set(1, 1, focalPixelsA);

			for (int idxB = 0; idxB < numberOfSamples; idxB++) {
				double focalRatioB = sampleFocalRatioMin*Math.exp(logCoef*idxB);
				double focalPixelsB = focalRatioB*imageLengthPixels;

				K2.set(0, 0, focalPixelsB);
				K2.set(1, 1, focalPixelsB);

				PerspectiveOps.multTranA(K2, calibrator.F21, K1, E);

				if (!svd.decompose(E))
					continue;

				double error = computeFitError();

				if (verbose != null)
					verbose.printf("[%3d,%3d] f1=%5.2f f2=%5.2f error=%f\n", idxA, idxB, focalPixelsA, focalPixelsB, error);
				if (error < bestError) {
					isLimit = idxA == 0 || idxA == numberOfSamples - 1;
					isLimit |= idxB == 0 || idxB == numberOfSamples - 1;
					bestError = error;
					focalLengthA = focalPixelsA;
					focalLengthB = focalPixelsB;
				}
			}
		}
	}

	private void computeHomography( double F1, double F2, List<AssociatedPair> observations ) {
		DMatrixRMaj K1 = CommonOps_DDRM.diag(F1, F1, 1);
		DMatrixRMaj K2 = CommonOps_DDRM.diag(F2, F2, 1);

		calibrator.process(K1, K2, observations);
		rectifyingHomography.setTo(calibrator.getCalibrationHomography());
	}

	/**
	 * Assumes that there is only one focal length value and searches for the optical value
	 *
	 * @param logCoef coeffient for log scale
	 */
	private void searchFixedFocus( double logCoef ) {
		K1.set(2, 2, 1);
		double bestError = Double.MAX_VALUE;

		for (int idxA = 0; idxA < numberOfSamples; idxA++) {
			double focalRatioA = sampleFocalRatioMin*Math.exp(logCoef*idxA);
			double focalPixelsA = focalRatioA*imageLengthPixels;

//			System.out.println("FOCUS = "+focalA);
			K1.set(0, 0, focalPixelsA);
			K1.set(1, 1, focalPixelsA);

			// Use known calibration to compute essential matrix
			PerspectiveOps.multTranA(K1, calibrator.F21, K1, E);

			// Use the singular values to evaluate
			if (!svd.decompose(E))
				continue;

			double error = computeFitError();

			if (verbose != null) verbose.printf("[%3d] f=%5.2f svd-error=%f\n", idxA, focalPixelsA, error);

			if (error < bestError) {
				isLimit = idxA == 0 || idxA == numberOfSamples - 1;
				bestError = error;
				focalLengthA = focalPixelsA;
			}
		}
		// Copy results to the other camera
		focalLengthB = focalLengthA;
	}

	/**
	 * Checks the two singular values are the same like it should be in a real essential matrix
	 */
	private double computeFitError() {
		double[] sv = svd.getSingularValues();
		// Find the two largest singular values
		double v0, v1;
		if (sv[0] > sv[1]) {
			v0 = sv[0];
			v1 = sv[1];
		} else {
			v0 = sv[1];
			v1 = sv[0];
		}
		if (v1 < sv[2]) {
			v1 = sv[2];
		}
		Arrays.sort(sv, 0, 3);
		double mean = (v0 + v1)/2.0;
		return Math.abs(v0 - mean)/mean;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
