/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.slam.score3d;

import boofcv.abst.geo.Triangulate2PointingMetricH;
import boofcv.alg.slam.EpipolarCalibratedScore3D;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Scores the geometric information between two views when the extrinsics and intrinsics are known. This is done
 * by triangulating points, removing points with too large of a residual error, then computing the outliers
 * if you set translation to zero.
 *
 * @author Peter Abeles
 */
public class EpipolarScoreKnownExtrinsics implements EpipolarCalibratedScore3D {

	/** Specifies max reproejction error for inlier. If relative, then width + height */
	public ConfigLength maxReprojectionError = ConfigLength.relative(0.006, 2);

	Triangulate2PointingMetricH triangulator = FactoryMultiView.triangulate2PointingMetricH(null);

	// Predicted and observed pixel coordinates
	Point2D_F64 predicted = new Point2D_F64();
	Point2D_F64 observed = new Point2D_F64();

	// Storage for the motion between the views without translation
	Se3_F64 motionNoTranslation = new Se3_F64();

	// Storage for triangulated point
	Point4D_F64 X = new Point4D_F64();

	// error tolerance for both images
	double errorTolA, errorTolB;

	// Computed 3D score
	double score3D;
	boolean is3D;

	PrintStream verbose;

	@Override
	public void process( ImageDimension imageShapeA, ImageDimension iamgeShapeB,
						 Point3Transform2_F64 pointToPixelA, Point3Transform2_F64 pointToPixelB,
						 List<Point3D_F64> obsA, List<Point3D_F64> obsB,
						 List<AssociatedIndex> pairs, @Nullable Se3_F64 a_to_b, DogArray_I32 inliersIdx ) {

		Objects.requireNonNull(a_to_b);
		inliersIdx.reset();
		motionNoTranslation.R.setTo(a_to_b.R);

		// Find the inlier set by looking at reprojection error in each view. Tolerance is computed for each
		// view since the image size could be different
		errorTolA = maxReprojectionError.compute((imageShapeA.width + imageShapeA.height)/2.0);
		errorTolB = maxReprojectionError.compute((iamgeShapeB.width + iamgeShapeB.height)/2.0);

		// Use squared error since it's faster to compute
		errorTolA *= errorTolA;
		errorTolB *= errorTolB;

		findInliers(pointToPixelA, pointToPixelB, obsA, pairs, a_to_b, inliersIdx);

		int countNoTranslation = findNoTranslationInliers(pointToPixelA, pointToPixelB, obsA, pairs, inliersIdx);

		if (verbose != null) verbose.printf("inliers=%d no_translation=%d\n", inliersIdx.size, countNoTranslation);

		// Want a higher score when there are more inliers, want a lower score when they are still inliers
		// after translation has been removed. Adjust so that a value close to zero is bad
		score3D = inliersIdx.size/(2.0 + countNoTranslation) - 1.0;
		is3D = inliersIdx.size > countNoTranslation*2;
	}

	/**
	 * Finds inliers by triangulating the points then seeing if the reprojection error in each view is too large
	 */
	private void findInliers( Point3Transform2_F64 pointToPixelA, Point3Transform2_F64 pointToPixelB,
							  List<Point3D_F64> obsA, List<AssociatedIndex> pairs,
							  Se3_F64 a_to_b, DogArray_I32 inliersIdx ) {

		for (int i = 0; i < pairs.size(); i++) {
			AssociatedIndex associated = pairs.get(i);
			Point3D_F64 oa = obsA.get(associated.src);
			Point3D_F64 ob = obsA.get(associated.dst);

			if (!triangulator.triangulate(oa, ob, a_to_b, X)) {
				if (verbose != null) verbose.println("Failed to triangulate? Bad points? " + oa + " " + ob);
				continue;
			}

			// Reproject the triangulated point and the original observation
			pointToPixelA.compute(X.x, X.y, X.z, predicted);
			pointToPixelA.compute(oa.x, oa.y, oa.z, observed);

			// See if the error is too large and this observation is an outlier
			if (observed.distance2(predicted) > errorTolA)
				continue;

			pointToPixelB.compute(X.x, X.y, X.z, predicted);
			pointToPixelB.compute(ob.x, ob.y, ob.z, observed);
			if (observed.distance2(predicted) > errorTolB)
				continue;

			inliersIdx.add(i);
		}
	}

	/**
	 * Finds the number of inliers if the translation is set to zero
	 */
	private int findNoTranslationInliers( Point3Transform2_F64 pointToPixelA, Point3Transform2_F64 pointToPixelB,
										  List<Point3D_F64> obsA, List<AssociatedIndex> pairs, DogArray_I32 inliersIdx ) {
		int remainingInliers = 0;
		for (int idx = 0; idx < inliersIdx.size; idx++) {
			int i = inliersIdx.get(idx);
			AssociatedIndex associated = pairs.get(i);
			Point3D_F64 oa = obsA.get(associated.src);
			Point3D_F64 ob = obsA.get(associated.dst);

			if (!triangulator.triangulate(oa, ob, motionNoTranslation, X)) {
				if (verbose != null) verbose.println("Failed to triangulate with no translation");
				continue;
			}

			pointToPixelA.compute(X.x, X.y, X.z, predicted);
			pointToPixelA.compute(oa.x, oa.y, oa.z, observed);
			if (observed.distance2(predicted) > errorTolA)
				continue;

			pointToPixelB.compute(X.x, X.y, X.z, predicted);
			pointToPixelB.compute(ob.x, ob.y, ob.z, observed);
			if (observed.distance2(predicted) > errorTolB)
				continue;

			remainingInliers++;
		}
		return remainingInliers;
	}

	@Override public double getScore() {
		return score3D;
	}

	@Override public boolean is3D() {
		return is3D;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
