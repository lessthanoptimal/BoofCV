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

package boofcv.alg.structure.score3d;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.robust.DistanceFundamentalGeometric;
import boofcv.alg.geo.selfcalib.RefineTwoViewPinholeRotation;
import boofcv.alg.structure.EpipolarScore3D;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Determines the amount of 3D information by comparing the results from robustly fitting a Fundamental matrix vs
 * fitting pure rotation/self calibration. A generic
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ScoreFundamentalVsRotation implements EpipolarScore3D {
	/** Robust model matching algorithm. */
	@Getter ModelMatcher<DMatrixRMaj, AssociatedPair> robust3D;

	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 1.5;

	/**
	 * The error ratio can get massive and this number prevents large values for being weighted too much in the score
	 */
	public @Getter @Setter double maxRatioScore = 10.0;

	/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs. */
	public @Getter final ConfigLength minimumInliers = ConfigLength.relative(0.1, 30);

	/**
	 * A pair of observations are within tolerance to the model if the error is less than or equal to this
	 */
	public @Getter @Setter double inlierErrorTol = 1.0;

	/** Assumes pure rotation then refines rotation and intrinsics to observations */
	public @Getter final RefineTwoViewPinholeRotation fitRotation = new RefineTwoViewPinholeRotation();

	// Error/distance computed using Fundamental matrix
	DistanceFundamentalGeometric distanceF = new DistanceFundamentalGeometric();

	// Storage for inliers from pure rotation
	protected final DogArray_I32 inliersRotationIdx = new DogArray_I32();
	protected final DMatrixRMaj K1 = new DMatrixRMaj(3, 3);     // storage for initial calibration view-1
	protected final DMatrixRMaj K2 = new DMatrixRMaj(3, 3);     // storage for initial calibration view-2
	protected final DMatrixRMaj R = new DMatrixRMaj(3, 3);       // storage for initial rotation matrix
	private final CameraPinhole pinhole1 = new CameraPinhole();
	private final CameraPinhole pinhole2 = new CameraPinhole();

	// predicted pixel in view-2
	final Point2D_F64 predictedPixel = new Point2D_F64();

	// Storage for inliers
	List<AssociatedPair> inliersRansac = new ArrayList<>();

	// Output parameters
	double score;
	boolean is3D;

	// If not null then verbose output
	@Nullable PrintStream verbose;

	public ScoreFundamentalVsRotation( ModelMatcher<DMatrixRMaj, AssociatedPair> robust3D ) {
		this.robust3D = robust3D;

		// Configure camera assumptions for pure rotation self calibration
		fitRotation.converge.maxIterations = 100;
		fitRotation.setAssumeUnityAspect(true); // hard assumption made throughout the code
		fitRotation.setKnownFocalLength(false);
		fitRotation.setZeroSkew(true);
	}

	@Override public void process( CameraPinholeBrown cameraA, @Nullable CameraPinholeBrown cameraB,
								   int featuresA, int featuresB,
								   List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx ) {
		BoofMiscOps.checkTrue(featuresA >= pairs.size());
		BoofMiscOps.checkTrue(featuresB >= pairs.size());

		inliersRotationIdx.reset();
		inliersIdx.reset();
		is3D = false;
		score = 0.0;

		// determine if there's only one set of intrinsics for the two views
		boolean sameCamera = cameraB == null;
		if (cameraB == null) // needed to get rid of NullAway false positive
			cameraB = cameraA;
		pinhole1.setTo(cameraA);
		pinhole2.setTo(cameraB);

		// Not enough points to compute F
		if (pairs.size() < robust3D.getMinimumSize()) {
			if (verbose != null) verbose.printf("pairs.size=%d less than robust3D.getMinimumSize()\n", pairs.size());
			return;
		}

		if (!fitFundamentalMatrix(pairs, fundamental))
			return;

		// Compute the goodness of fit for ransac and create an inlier list. Inliers are done here because
		// we want to make sure the same methods are used as we use with pure rotation
		int fitFundamental = countFitFundamental(robust3D.getModelParameters(), pairs, inliersIdx);

		int fitRotation = fitPureRotation(pairs, sameCamera);

		// Use all the results and select the beset solution
		selectBestModel(pairs, inliersIdx, fitFundamental, fitRotation);
	}

	/**
	 * Uses a robust method to fit a fundamental matrix to the inputs
	 *
	 * @param pairs (Input) Associated image features
	 * @param fundamental (Output) Found fundamental matrix
	 * @return true if nothing went wrong
	 */
	private boolean fitFundamentalMatrix( List<AssociatedPair> pairs,
										  DMatrixRMaj fundamental ) {
		final int minimumAllowed = minimumInliers.computeI(pairs.size());
		if (pairs.size() < minimumAllowed) {
			if (verbose != null)
				verbose.printf("REJECTED: pairs.size=%d < minimum.size=%d\n", pairs.size(), minimumAllowed);
			return false;
		}

		if (!robust3D.process(pairs)) {
			// assume it failed because the data was noise free and pure rotation encountered
			// Even if it failed due to NaN in the input, it's still true there was no geometric info!
			is3D = false;
			score = 0.0;
			if (verbose != null) verbose.println("ransac failed. not 3D");
			return false;
		}

		// Save the inliers and compute the epipolar geometric error for F
		fundamental.setTo(robust3D.getModelParameters());

		// Point all the points which are inliers from robust matching into a set. This is used to estimate pure
		// rotation below.
		int numInliers = robust3D.getMatchSet().size();
		inliersRansac.clear();
		for (int i = 0; i < numInliers; i++) {
			inliersRansac.add(pairs.get(robust3D.getInputIndex(i)));
		}
		return true;
	}

	/**
	 * Attempts to see if a pure rotational motion can describe the scene. This requires it to "self calibrate"
	 * while estimating the rotation. The solution is accepted of nothing blows up and the self calbration isn't
	 * total bonkers.
	 *
	 * @param pairs (Input) All the image features
	 * @param sameCamera (Input) true if intrinsics are constant across both views
	 * @return Number of matching points to pure rotation or a negative number of the solution was rejected
	 */
	private int fitPureRotation( List<AssociatedPair> pairs,
								 boolean sameCamera ) {
		// Initialize the algorithm with no rotation
		CommonOps_DDRM.setIdentity(R);

		// Performance can improve significantly if this constraint is enabled. For example, if moving forward
		// and there are two cameras, then the second view could be zoomed in and there is no translation
		this.fitRotation.setAssumeSameIntrinsics(sameCamera);

		// Fit against the inliers only since those two sets of points should be similar
		// and this will remove some noise
		if (!this.fitRotation.refine(inliersRansac, R, pinhole1, pinhole2)) {
			return -1;
		}
		// Check to see if the self calibration diverged. That's a strong indication that the data
		// doesn't fit the model
		boolean badRotation = false;
		badRotation |= pinhole1.cx < 0 || pinhole1.cy < 0 || pinhole1.cx >= pinhole1.width || pinhole1.cy >= pinhole1.height;
		badRotation |= pinhole2.cx < 0 || pinhole2.cy < 0 || pinhole2.cx >= pinhole2.width || pinhole2.cy >= pinhole2.height;

		if (badRotation) {
			return -2;
		}

		// Compute a homography from the found rotation and camera intrinsics
		PerspectiveOps.pinholeToMatrix(pinhole1, K1);
		PerspectiveOps.pinholeToMatrix(pinhole2, K2);
		DMatrixRMaj H21 = MultiViewOps.homographyFromRotation(R, K1, K2, null);

		// See how many points match this model
		return countFitRotation(H21, pairs, inliersRotationIdx);
	}

	/**
	 * Returns the number of times an observations is within tolerance of the model
	 */
	private int countFitFundamental( DMatrixRMaj fundamental, List<AssociatedPair> pairs, DogArray_I32 inliersIdx ) {
		distanceF.setModel(fundamental);
		double threshold = inlierErrorTol*inlierErrorTol*2.0;

		inliersIdx.reset();
		inliersIdx.reserve(pairs.size());
		for (int i = 0; i < pairs.size(); i++) {
			double doubleDistanceSq = distanceF.distance(pairs.get(i));
			if (doubleDistanceSq <= threshold) {
				inliersIdx.add(i);
			}
		}

		return inliersIdx.size;
	}

	private int countFitRotation( DMatrixRMaj H21, List<AssociatedPair> pairs, DogArray_I32 inliersIdx ) {
		// Checking to see if it's inside the image is valid here since it does self calibration.

		// epipolar distance is relative to a line. This is relative to a point
		double adjustedTol = inlierErrorTol*2.0;
		double threshold = adjustedTol*adjustedTol;
		inliersIdx.reset();
		inliersIdx.reserve(pairs.size());
		for (int i = 0; i < pairs.size(); i++) {
			AssociatedPair obs = pairs.get(i);

			GeometryMath_F64.mult(H21, obs.p1, predictedPixel);

			if (!pinhole2.isInside(predictedPixel.x, predictedPixel.y))
				continue;

			double error = predictedPixel.distance2(obs.p2);
			if (error >= threshold)
				continue;

			inliersIdx.add(i);
		}

		return inliersIdx.size;
	}

	private void selectBestModel( List<AssociatedPair> pairs, DogArray_I32 inliersIdx, int fitModelF, int fitRotation ) {
		// Use whichever inlier set is larger as it's more likely to be correct
		if (inliersRotationIdx.size > inliersIdx.size) {
			inliersIdx.setTo(inliersRotationIdx);
		}

		// if there are too few matches then it's probably noise
		final int minimumAllowed = minimumInliers.computeI(pairs.size());
		if (inliersIdx.size() < minimumAllowed) {
			if (verbose != null)
				verbose.printf("REJECTED: Inliers, pairs.size=%d inlier.size=%d < minimum.size=%d\n",
						pairs.size(), inliersIdx.size(), minimumAllowed);
			is3D = false;
			return;
		}

		// Use a ratio test to decide of there is enough information to declare this relationship has having 3D
		// information.
		is3D = Math.max(1, fitRotation)*ratio3D <= fitModelF;

		// Prefer a more distinctive F and more points. /200 is cosmetic
		double ratio = fitModelF/(double)Math.max(1, fitRotation);
		score = Math.min(maxRatioScore, ratio)*inliersIdx.size/200.0;

		if (verbose != null)
			verbose.printf("score=%7.2f pairs=%d inliers=%d ratio=%6.2f, fitR=%d fitF=%d 3d=%s\n",
					score, pairs.size(), inliersIdx.size, ratio, fitRotation, fitModelF, is3D);
	}

	@Override public double getScore() {
		return score;
	}

	@Override public boolean is3D() {
		return is3D;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, fitRotation);
	}
}
