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
import boofcv.alg.geo.robust.DistanceFundamentalGeometric;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.alg.structure.EpipolarScore3D;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Runs RANSAC to find the fundamental matrix. Score is computed by computing a homography then recomputing
 * the fundamental matrix from the homography and an epipole. If the homography is compatible with F then F
 * will be recomputed correctly, otherwise an the recomputed F will be incorrect. The homography will be
 * compatible if the scene is planar or the motion was purely rotational.
 *
 * The score is found by computing the number of pairs that are "inliers" between the two versions of F. The ratio
 * of inliers is used to determine if there's 3D information and compute the score. The score also seeds
 * to encourage more image features in the RANSAC inlier set.
 *
 * The inlier threshold used for RANSAC and {@link #inlierErrorTol} do not need to be the same but they would
 * probably be similar.
 *
 * WARNING: Has known issues with dominant planes where it will mark them as not having 3D as it mistakes it for
 * pure rotation.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ScoreFundamentalHomographyCompatibility implements EpipolarScore3D {
	/** Robust model matching algorithm. Inlier set is used but not the error */
	@Getter ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;

	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 1.5;

	/**
	 * The error ratio can get massive and this number prevents large values for being weighted too much in the score
	 */
	public @Getter @Setter double maxRatioScore = 5.0;

	/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs. */
	public @Getter final ConfigLength minimumInliers = ConfigLength.fixed(30);

	/**
	 * A pair of observations are within tolerance to the model if the error is less than or equal to this
	 */
	public @Getter @Setter double inlierErrorTol = 1.0;

	// Storage for inliers
	List<AssociatedPair> inliers = new ArrayList<>();

	// Compute the homography
	GenerateHomographyLinear estimateH = new GenerateHomographyLinear(true);
	// Error/distance computed using Fundamental matrix
	DistanceFundamentalGeometric distanceF = new DistanceFundamentalGeometric();

	// storage for homography
	DMatrixRMaj H = new DMatrixRMaj(3, 3);
	Homography2D_F64 H2 = new Homography2D_F64();

	// Output parameters
	double score;
	boolean is3D;

	// If not null then verbose output
	@Nullable PrintStream verbose;

	// Workspace variables
	Point3D_F64 e1 = new Point3D_F64();
	Point3D_F64 e2 = new Point3D_F64();
	DMatrixRMaj F_alt = new DMatrixRMaj(3, 3);

	public ScoreFundamentalHomographyCompatibility( ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D ) {
		this.ransac3D = ransac3D;
	}

	@Override public void process( CameraPinholeBrown cameraA, @Nullable CameraPinholeBrown cameraB,
								   int featuresA, int featuresB,
								   List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx ) {
		is3D = false;
		score = 0.0;

		// Not enough points to compute F
		if (pairs.size() < ransac3D.getMinimumSize()) {
			if (verbose != null) verbose.printf("pairs.size=%d less than ransac3D.getMinimumSize()\n", pairs.size());
			return;
		}

		final int minimumAllowed = minimumInliers.computeI(pairs.size());
		if (pairs.size() < minimumAllowed) {
			if (verbose != null)
				verbose.printf("REJECTED: pairs.size=%d < minimum.size=%d\n", pairs.size(), minimumAllowed);
			return;
		}

		if (!ransac3D.process(pairs)) {
			// assume it failed because the data was noise free and pure rotation encountered
			// Even if it failed due to NaN in the input, it's still true there was no geometric info!
			is3D = false;
			score = 0.0;
			if (verbose != null) verbose.println("ransac failed. not 3D");
			return;
		}

		// if there are too few matches then it's probably noise
		if (ransac3D.getMatchSet().size() < minimumAllowed) {
			if (verbose != null)
				verbose.printf("REJECTED: pairs.size=%d inlier.size=%d < minimum.size=%d\n",
						pairs.size(), ransac3D.getMatchSet().size(), minimumAllowed);
			return;
		}

		// Save the inliers and compute the epipolar geometric error for F
		fundamental.setTo(ransac3D.getModelParameters());
		inliersIdx.resize(ransac3D.getMatchSet().size());
		inliers.clear();
		for (int i = 0; i < inliersIdx.size; i++) {
			inliersIdx.set(i, ransac3D.getInputIndex(i));
			inliers.add(pairs.get(inliersIdx.get(i)));
		}
		int fitModelF = countFitModel(ransac3D.getModelParameters());

		// Compute a homography to detect degenerate geometry
		estimateH.generate(inliers, H2);
		UtilHomography_F64.convert(H2, H);

		// We will now use a trick to compute the error as a function of the fundamental matrix again
		// The most straight forward way to do this is by computing the homography directly, but the error
		// function will then be very different.
		//
		// F = cross(e2)*H                             see H&Z p335
		//
		// This only works if H is compatible with F. That only happens for planar scenes or pure rotation
		MultiViewOps.extractEpipoles(fundamental, e1, e2);
		GeometryMath_F64.multCrossA(e2, H, F_alt);

		// Compute errors using the mangled fundamental matrix
		int fitModelH = countFitModel(F_alt) + 1;

		is3D = fitModelH*ratio3D <= fitModelF;

		// Prefer a more distinctive F and more points. /200 is cosmetic
		double ratio = fitModelF/(double)fitModelH;
		score = Math.min(maxRatioScore, ratio)*inliersIdx.size/200.0;

		if (verbose != null)
			verbose.printf("score=%7.2f pairs=%d inliers=%d ratio=%6.2f fitH=%4d fitF=%4d 3d=%s\n",
					score, pairs.size(), inliersIdx.size, ratio, fitModelH, fitModelF, is3D);
	}

	/**
	 * Returns the number of times an observations is within tolerance of the model
	 */
	private int countFitModel( DMatrixRMaj fundamental ) {
		distanceF.setModel(fundamental);
		int total = 0;
		double threshold = inlierErrorTol*inlierErrorTol*2.0;

		for (int i = 0; i < inliers.size(); i++) {
			double doubleDistanceSq = distanceF.distance(inliers.get(i));
			if (doubleDistanceSq <= threshold)
				total++;
		}
		return total;

		// NOTE: Originally a score was computed based on error. This  was excessively sensitive as the ratio
		//       would explode quickly as H became a miss match. mean and percentile error was explored.
	}

	@Override public double getScore() {
		return score;
	}

	@Override public boolean is3D() {
		return is3D;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> param ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
