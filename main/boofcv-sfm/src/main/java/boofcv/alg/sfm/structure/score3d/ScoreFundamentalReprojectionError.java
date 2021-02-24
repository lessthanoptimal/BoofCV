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

package boofcv.alg.sfm.structure.score3d;

import boofcv.alg.geo.robust.DistanceFundamentalGeometric;
import boofcv.alg.geo.robust.DistanceHomographySq;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.alg.sfm.structure.EpipolarScore3D;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Runs RANSAC to find the fundamental matrix. Then a homography is fit to the inliers. Errors are compared
 * between the fundamental matrix and homography. Fundamental matrix will naturally have a lower error since
 * it's distance from the epipolar line while homography is distance from a point.
 *
 * @author Peter Abeles
 */
public class ScoreFundamentalReprojectionError implements EpipolarScore3D {
	/** Robust model matching algorithm. Inlier set is used but not the error */
	@Getter ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;

	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 4.0;

	/** Smoothing parameter and avoid divide by zero. This is typically < 1.0 since error is computed in pixels */
	@Getter @Setter double eps = 0.5;

	// Storage for inliers
	List<AssociatedPair> inliers = new ArrayList<>();

	// Storage for errors
	DogArray_F64 errors = new DogArray_F64();

	// Compute the homography
	GenerateHomographyLinear estimateH = new GenerateHomographyLinear(true);
	// Error/distance computed using the homography
	DistanceHomographySq distanceH = new DistanceHomographySq();
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

	public ScoreFundamentalReprojectionError( ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D ) {
		this.ransac3D = ransac3D;
	}

	@Override public boolean process( List<AssociatedPair> pairs, DMatrixRMaj fundamental, DogArray_I32 inliersIdx ) {
		// Not enough points to compute F
		if (pairs.size() < ransac3D.getMinimumSize())
			return false;

		if (!ransac3D.process(pairs)) {
			// assume it failed because the data was noise free and pure rotation encountered
			// Even if it failed due to NaN in the input, it's still true there was no geometric info!
			is3D = false;
			score = 0.0;
			return true;
		}

		// Save the inliers and compute the epipolar geometric error for F
		fundamental.setTo(ransac3D.getModelParameters());
		distanceF.setModel(ransac3D.getModelParameters());
		inliersIdx.resize(ransac3D.getMatchSet().size());
		inliers.clear();
		errors.resize(inliersIdx.size);
		for (int i = 0; i < inliersIdx.size; i++) {
			inliersIdx.set(i, ransac3D.getInputIndex(i));
			inliers.add(pairs.get(inliersIdx.get(i)));
			errors.set(i, distanceF.distance(pairs.get(inliersIdx.get(i))));
		}

		// use 50% error since it's less sensitive to outliers
		errors.sort();
		double errorF = errors.getFraction(0.5);

		estimateH.generate(inliers, H2);
		UtilHomography_F64.convert(H2, H);
		distanceH.setModel(H);
		errors.resize(inliersIdx.size);
		for (int i = 0; i < inliersIdx.size; i++) {
			errors.set(i, distanceH.distance(pairs.get(inliersIdx.get(i))));
		}
		errors.sort();
		double errorH = Math.sqrt(errors.getFraction(0.5));

		score = (errorH + eps)/(errorF + eps);
		is3D = score > ratio3D;

		if (verbose != null)
			verbose.println("score=" + score + " errorH=" + errorH + " errorF=" + errorF + " 3d=" + is3D);

		// Figuring out if we are dealing with a translation viewing a plane or pure rotation isn't trivial

		// Failed attempts:
		// 1) F and H compatibility. only false for non-planar scenes
		// 2) F -> camera matrix, setting "translation" to (0,0,0). That's not how projective cameras work
		// 3) Self calibration using pure rotation fails because there aren't enough views
		// 4) Triangulating projective points, forcing p.w=0, compare reprojection error. Always massive error

		// I think the major issue where is that in both cases points lie on a plane. For pure rotation, the plane is
		// at infinity but it's still a plane.
		//
		// What might work is looking at the apparent motion of the points.  There won't be a vanishing point
		// if its pure rotation. However rotation along z-axis and pure translation might be difficult to distinguish
		// from pure translation along x-axis.

		// Thia also can't assume that both cameras have the same intrinsic parameters

		return true;
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
