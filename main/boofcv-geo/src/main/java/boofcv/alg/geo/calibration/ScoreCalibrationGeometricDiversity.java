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

import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Estimates if there is enough geometry diversity to compute an initial estimate of the camera calibration parameters
 * by computing a linear estimate and looking at its singular values. There should only be one null space.
 *
 * @author Peter Abeles
 */
public class ScoreCalibrationGeometricDiversity {

	Zhang99ComputeTargetHomography computeHomography;
	Zhang99CalibrationMatrixFromHomographies computeCalib;

	List<DMatrixRMaj> homographies = new ArrayList<>();

	/**
	 * A number from 0 to 1.0 for how close to having the correct geometry it is. 1.0 = done
	 */
	@Getter double score = 0;

	public ScoreCalibrationGeometricDiversity( boolean assumeZeroSkew ) {
		computeCalib = new Zhang99CalibrationMatrixFromHomographies(assumeZeroSkew);
		computeHomography = new Zhang99ComputeTargetHomography();
	}

	/**
	 * Adds information from the provided set of observations
	 *
	 * @param observation Observation of calibration target
	 * @param layout The layout of the calibration target
	 */
	public void addObservation( List<PointIndex2D_F64> observation, List<Point2D_F64> layout ) {
		if (observation.size() <= 4)
			return;
		computeHomography.setTargetLayout(layout);
		if (!computeHomography.computeHomography(observation)) {
			System.err.println("Failed to compute homography");
			return;
		}
		homographies.add(computeHomography.getCopyOfHomography());
	}

	/**
	 * Computes the score from all the found homographies
	 */
	public void computeScore() {
		try {
			computeCalib.process(homographies);

			double[] values = computeCalib.getSolverNull().getSingularValues();
			Arrays.sort(values, 0, 3);

//			System.out.println("raw singularity score = " + (values[1]/values[2]));

			// 0.2 was a threshold that was empirically determined
			score = Math.min(1.0, (values[1]/values[2])/0.2);
		} catch (RuntimeException e) {
			score = 0;
		}
	}
}
