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
import boofcv.struct.ConfigLength;
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
 * will be recomputed, if not then you have jump. These two versions of F then compute reprojection error
 * and if they are similar then the scene is dominated by a plane. That can be a physical plane or one at
 * infinity created by a pure rotation. See code comments.
 *
 * @author Peter Abeles
 */
public class ScoreFundamentalReprojectionError implements EpipolarScore3D {
	/** Robust model matching algorithm. Inlier set is used but not the error */
	@Getter ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;

	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 1.5;

	/** Smoothing parameter and avoid divide by zero. This is typically < 1.0 since error is computed in pixels */
	public @Getter @Setter double eps = 0.01;

	/**
	 * The error ratio can get massive and this number prevents large values for being weighted too much in the score
	 */
	public @Getter @Setter double maxRatioScore = 5.0;

	/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs. */
	public @Getter final ConfigLength minimumInliers = ConfigLength.fixed(30);

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
	PrintStream verbose;

	// Workspace variables
	Point3D_F64 e1 = new Point3D_F64();
	Point3D_F64 e2 = new Point3D_F64();
	DMatrixRMaj F_alt = new DMatrixRMaj(3, 3);

	public ScoreFundamentalReprojectionError( ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D ) {
		this.ransac3D = ransac3D;
	}

	@Override public boolean process( List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx ) {
		// Not enough points to compute F
		if (pairs.size() < ransac3D.getMinimumSize()) {
			if (verbose != null) verbose.printf("pairs.size=%d less than ransac3D.getMinimumSize()\n", pairs.size());
			return false;
		}

		final int minimumAllowed = minimumInliers.computeI(pairs.size());
		if (pairs.size() < minimumAllowed) {
			if (verbose != null)
				verbose.printf("pairs.size=%d less than the minimum.size=%d\n", pairs.size(), minimumAllowed);
			return false;
		}

		if (!ransac3D.process(pairs)) {
			// assume it failed because the data was noise free and pure rotation encountered
			// Even if it failed due to NaN in the input, it's still true there was no geometric info!
			is3D = false;
			score = 0.0;
			if (verbose != null) verbose.println("ransac failed. not 3D");
			return true;
		}

		// if there are too few matches then it's probably noise
		if (ransac3D.getMatchSet().size() < minimumAllowed) {
			if (verbose != null)
				verbose.printf("inlier.size=%d is too small. minimum.size=%d\n",
						ransac3D.getMatchSet().size(), minimumAllowed);
			return false;
		}

		// Save the inliers and compute the epipolar geometric error for F
		fundamental.setTo(ransac3D.getModelParameters());
		inliersIdx.resize(ransac3D.getMatchSet().size());
		inliers.clear();
		for (int i = 0; i < inliersIdx.size; i++) {
			inliersIdx.set(i, ransac3D.getInputIndex(i));
			inliers.add(pairs.get(inliersIdx.get(i)));
		}
		double errorF = computeAverageEuclideanError(ransac3D.getModelParameters());

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
		// TODO experiment with compatibility score? normalize magnitude of variables?

		// Compute errors using the mangled fundamental matrix
		double errorH = computeAverageEuclideanError(F_alt);

		double ratio = (errorH + eps)/(errorF + eps);
		is3D = ratio > ratio3D;

		// The final score prefers sets with more inliers and a much better errorF, but caps the benefit from a large
		// errorF since it can get ridiculously high and there's little benefit after a certain point
		score = Math.min(maxRatioScore, ratio)*inliers.size();
		score /= (100.0*maxRatioScore); // purely cosmetic to keep the numbers smaller

		if (verbose != null)
			verbose.printf("score=%7.2f pairs=%d inliers=%d ratio=%6.2f errorH=%6.2f errorF=%5.2f 3d=%s\n",
					score, pairs.size(), inliersIdx.size, ratio, errorH, errorF, is3D);

		if (is3D)
			return true;

		// Figuring out if we are dealing with a translation viewing a plane or pure rotation isn't trivial
		//
		// After going through the math more it looks like that pure rotation and general motion do have a
		// distinctive subsets of the general homography. However, even in the case of pure rotation, you basically
		// have to solve the camera calibration problem to find the best fit parameters.
		// Look at the equations for the motion field to see this. If this was done, then you could find
		// the best fit homography for pure rotation and see how much larger the residuals are compared to
		// the already known solution.
		//
		// Original comments are left below to avoid duplicating work in the future.
		//
		// Failed attempts:
		// 1) F and H compatibility. only false for non-planar scenes
		// 2) F -> camera matrix, setting "translation" to (0,0,0). That's not how projective cameras work
		// 3) Self calibration using pure rotation fails because there aren't enough views
		// 4) Triangulating projective points, forcing p.w=0, compare reprojection error. Always massive error
		//
		// I think the major issue where is that in both cases points lie on a plane. For pure rotation, the plane is
		// at infinity but it's still a plane.
		//
		// What might work is looking at the apparent motion of the points.  There won't be a vanishing point
		// if its pure rotation. However rotation along z-axis and pure translation might be difficult to distinguish
		// from pure translation along x-axis.
		//
		// Also can't assume that both cameras have the same intrinsic parameters

		return true;
	}

	/**
	 * The distance function computes the Euclidean distance squared for the feature in the left and right side from
	 * the closest point on the epipolar line. This function divides that error by 2 (now the average Euclidean
	 * squared) then takes the square root and sums it. Finally it divides it by the total count.
	 *
	 * this should have a linear response to changes in model performance
	 */
	private double computeAverageEuclideanError( DMatrixRMaj fundamental ) {
		distanceF.setModel(fundamental);
		double error = 0.0;
		for (int i = 0; i < inliers.size(); i++) {
			double doubleDistanceSq = distanceF.distance(inliers.get(i));
			error += Math.sqrt(doubleDistanceSq/2.0);
		}
		return error/inliers.size();
	}

	@Override public double getScore() {
		return score;
	}

	@Override public boolean is3D() {
		return is3D;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> param ) {
		this.verbose = out;
	}
}
