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

import boofcv.struct.geo.AssociatedPair;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.assertBoof;

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
 * @see TwoViewToCalibratingHomography
 *
 * @author Peter Abeles
 */
public class SelfCalibrationEssentialGuessAndCheck implements VerbosePrint {

	//---------------------------- Configuration Parameters
	/** Range of values focal length values will sample. Fraction relative to  {@link #imageLengthPixels}*/
	public @Getter double sampleFocalRatioMin=0.3, sampleFocalRatioMax=2.5;
	/** Number of values it will sample */
	public @Getter int numberOfSamples=50;
	/** if true the focus is assumed to be the same for the first two images*/
	public @Getter boolean fixedFocus =false;

	//--------------------------- Output Variables
	/** The selected focal length for the first image */
	public @Getter double focalLengthA;
	/** The selected focal length for the second image */
	public @Getter double focalLengthB;
	/** The selected rectifying homography */
	public @Getter final DMatrixRMaj rectifyingHomography = new DMatrixRMaj(4,4);
	/** If true that indicates that the selected focal length was at the upper or lower limit. This can indicate a fault */
	public @Getter boolean isLimit;

	/** The length of the longest side in the image. In pixels. */
	public int imageLengthPixels=0;

	/** Generates and scores a hypothesis given two intrinsic camera matrices */
	public @Getter final TwoViewToCalibratingHomography calibrator = new TwoViewToCalibratingHomography();

	// If not null then verbose information is printed
	private PrintStream verbose;

	/**
	 * Specifies the range of focal lengths it will evaluate as ratio of {@link #imageLengthPixels}
	 * @param sampleFocalRatioMin The minimum allowed focal length ratio
	 * @param sampleFocalRatioMax Tha maximum allowed focal length ratio
	 */
	public void configure( double sampleFocalRatioMin , double sampleFocalRatioMax ) {
		this.sampleFocalRatioMin = sampleFocalRatioMin;
		this.sampleFocalRatioMax = sampleFocalRatioMax;
	}

	/**
	 * Selects the best focal length(s) given the trifocal tensor and observations
	 * @param F21 (Input) Fundamental matrix between view-1 and view-2
	 * @param P2 (Input) Projective camera matrix for view-1 with inplicit identity matrix view-1
	 * @param observations (Input) Observation for all three views. Highly recommend that RANSAC or similar is used to
	 *                     remove false positives first.
	 * @return true if successful
	 */
	public boolean process(DMatrixRMaj F21, DMatrixRMaj P2, List<AssociatedPair> observations ) {
		// sanity check configurations
		assertBoof(imageLengthPixels>0,"Must set imageLengthPixels to max(imageWidth,imageHeight)");
		assertBoof(sampleFocalRatioMin !=0 && sampleFocalRatioMax !=0,"You must call configure");
		assertBoof(sampleFocalRatioMin < sampleFocalRatioMax && sampleFocalRatioMin > 0);
		assertBoof(observations.size()>0);
		assertBoof(numberOfSamples>0);

		// Pass in the trifocal tensor so that it can estimate self calibration
		calibrator.initialize(F21,P2);

		// coeffients for linear to log scale
		double logCoef = Math.log(sampleFocalRatioMax / sampleFocalRatioMin)/(numberOfSamples-1);

		DMatrixRMaj K1 = new DMatrixRMaj(3,3);
		K1.set(2,2,1);

		isLimit = false;

		if(fixedFocus) {
			searchFixedFocus(observations, logCoef, K1);
		} else {
			searchDynamicFocus(observations, logCoef, K1);
		}

		// DESIGN NOTE:
		// Could fit a 1-D or 2-D quadratic and get additional accuracy. Then compute the H at that value
		// In real data it seems that outliers drive the error more than a slightly incorrect focal length
		// so it's probably not worth the effort.

		return true;
	}

	/**
	 * Assumes that each camera can have an independent focal length value and searches a 2D grid
	 *
	 * @param observations observations of the features used to select beset result
	 * @param logCoef coeffient for log scale
	 * @param K1 intrinsic camera calibration matrix for view-1
	 */
	private void searchDynamicFocus(List<AssociatedPair> observations, double logCoef, DMatrixRMaj K1) {
		double bestError = Double.MAX_VALUE;

		var K2 = new DMatrixRMaj(3,3);
		K2.set(2,2,1);

		for (int idxA = 0; idxA < numberOfSamples; idxA++) {
			double focalRatioA = sampleFocalRatioMin * Math.exp(logCoef * idxA);
			double focalPixelsA = focalRatioA*imageLengthPixels;
			K1.set(0, 0, focalPixelsA);
			K1.set(1, 1, focalPixelsA);

			for (int idxB = 0; idxB < numberOfSamples; idxB++) {
				double focalRatioB = sampleFocalRatioMin * Math.exp(logCoef * idxB);
				double focalPixelsB = focalRatioB*imageLengthPixels;

				K2.set(0, 0, focalPixelsB);
				K2.set(1, 1, focalPixelsB);

				calibrator.process(K1, K2, observations);

				double error = calibrator.bestModelError;
				if( verbose != null )
					verbose.printf("[%3d,%3d] f1=%5.2f f2=%5.2f error=%f invalid=%d\n",idxA,idxB,focalPixelsA,focalPixelsB,error,calibrator.bestInvalid);
				if( error < bestError ) {
					isLimit = idxA == 0 || idxA == numberOfSamples-1;
					isLimit |= idxB == 0 || idxB == numberOfSamples-1;
					bestError = error;
					focalLengthA = focalPixelsA;
					focalLengthB = focalPixelsB;
					rectifyingHomography.set(calibrator.getCalibrationHomography());
				}
			}
		}
	}

	/**
	 * Assumes that there is only one focal length value and searches for the optical value
	 *
	 * @param observations observations of the features used to select beset result
	 * @param logCoef coeffient for log scale
	 * @param K1 intrinsic camera calibration matrix for view-1
	 */
	private void searchFixedFocus(List<AssociatedPair> observations, double logCoef, DMatrixRMaj K1) {
		double bestError = Double.MAX_VALUE;

		for (int idxA = 0; idxA < numberOfSamples; idxA++) {
			double focalRatioA = sampleFocalRatioMin * Math.exp(logCoef * idxA);
			double focalPixelsA = focalRatioA*imageLengthPixels;

//			System.out.println("FOCUS = "+focalA);
			K1.set(0, 0, focalPixelsA);
			K1.set(1, 1, focalPixelsA);

			if( !calibrator.process(K1, K1, observations) )
				continue;

			// Using known constraints on K doesn't seem to work as a better error metric
//			double error = Math.abs(K3.get(0,1)) + Math.abs(K3.get(0,2)) + Math.abs(K3.get(1,2));

			double error = calibrator.bestModelError;
			if( verbose != null )
				verbose.printf("[%3d] f=%5.2f svd-error=%f invalid=%d\n", idxA, focalPixelsA, error, calibrator.bestInvalid);

			if( error < bestError ) {
				isLimit = idxA == 0 || idxA == numberOfSamples-1;
				bestError = error;
				focalLengthA = focalPixelsA;
				rectifyingHomography.set(calibrator.getCalibrationHomography());
			}
		}
		// Copy results to the other camera
		focalLengthB = focalLengthA;
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		this.verbose = out;
	}
}
