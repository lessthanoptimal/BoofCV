/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Setter;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Given a description of the calibration grid and a set of observations compute the associated Homography.
 * First a linear approximation is computed follow by non-linear refinement. Part of calibration process
 * described in [1].
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations,",
 * International Conference on Computer Vision (ICCV'99), Corfu, Greece, pages 666-673, September 1999.99
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class Zhang99ComputeTargetHomography {
	/** Minimum number of points requires to process an image */
	public static int MINIMUM_POINTS = 4;

	private final Estimate1ofEpipolar computeHomography = FactoryMultiView.homographyDLT(true);
	private final DMatrixRMaj found = new DMatrixRMaj(3, 3);

	/**
	 * location of calibration points in the target frame, which is also the world coordinate system's origin.
	 * z-axis is assumed to be zero.
	 */
	@Setter @Nullable List<Point2D_F64> targetLayout;

	/**
	 * Computes the homography from a list of detected grid points in the image. The
	 * order of the grid points is important and must follow the expected row major
	 * starting at the top left.
	 *
	 * @param observedPoints List of ordered detected grid points in image pixels.
	 * @return True if it computed a Homography and false if it failed to compute a homography matrix.
	 */
	public boolean computeHomography( List<PointIndex2D_F64> observedPoints ) {
		Objects.requireNonNull(targetLayout, "Must specify targetLayout first");
		if (observedPoints.size() < MINIMUM_POINTS)
			throw new IllegalArgumentException("At least 4 points needed in each set of observations. " +
					" Filter these first please");

		var pairs = new ArrayList<AssociatedPair>();
		for (int i = 0; i < observedPoints.size(); i++) {
			int which = observedPoints.get(i).index;
			Point2D_F64 obs = observedPoints.get(i).p;

			pairs.add(new AssociatedPair(targetLayout.get(which), obs, true));
		}

		if (!computeHomography.process(pairs, found))
			return false;

		// todo do non-linear refinement. Take advantage of coordinates being fixed

		return true;
	}

	/**
	 * Returns a copy of the found homography matrix.
	 *
	 * @return Homography matrix.
	 */
	public DMatrixRMaj getCopyOfHomography() {
		return found.copy();
	}
}
