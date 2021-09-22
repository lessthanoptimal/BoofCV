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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.GeoModelEstimatorN;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;

import java.util.List;

/**
 * Computes the left camera pose from a fully calibrated stereo camera system using a PnP algorithm. Observations
 * from the left camera are used to solve the PnP problem while observations from the right camera are used to
 * select the best solution if its ambiguous.
 *
 * Observations past the minimum number are used just for selecting the best hypothesis.
 *
 * @author Peter Abeles
 */
public class PnPStereoEstimator implements GeoModelEstimator1<Se3_F64, Stereo2D3D> {
	// PnP pose estimator
	private final GeoModelEstimatorN<Se3_F64, Point2D3D> alg;
	// Used to resolve ambiguous solutions
	private final DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceLeft;
	private final DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceRight;

	// Stereo observations converted into a monocular observation
	private final DogArray<Point2D3D> monoPoints = new DogArray<>(10, Point2D3D::new);

	// known transform from left camera view into the right camera view
	private Se3_F64 leftToRight = new Se3_F64();

	// computed transform from worldToRight
	private final Se3_F64 worldToRight = new Se3_F64();

	private final DogArray<Se3_F64> solutions = new DogArray<>(4, Se3_F64::new);

	// extra observation used for testing solutions
	int extraForTest;

	/**
	 * @param extraForTest Right camera is used so zero is the minimum number
	 */
	public PnPStereoEstimator( GeoModelEstimatorN<Se3_F64, Point2D3D> alg,
							   DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceLeft,
							   DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceRight,
							   int extraForTest ) {
		this.alg = alg;
		this.distanceLeft = distanceLeft;
		this.distanceRight = distanceRight;
		this.extraForTest = extraForTest;
	}

	public void setLeftToRight( Se3_F64 leftToRight ) {
		this.leftToRight.setTo(leftToRight);
	}

	public void setLeftToRightReference( Se3_F64 leftToRight ) {
		this.leftToRight = leftToRight;
	}

	@Override
	public boolean process( List<Stereo2D3D> points, Se3_F64 estimatedModel ) {
		int N = alg.getMinimumPoints();

		// create a list of observation from the left camera
		monoPoints.reset();
		for (int i = 0; i < N; i++) {
			Stereo2D3D s = points.get(i);
			Point2D3D p = monoPoints.grow();
			p.observation = s.leftObs;
			p.location = s.location;
		}

		// compute solutions
		solutions.reset();
		alg.process(monoPoints.toList(), solutions);

		// use one for temporary storage when computing distance
		Point2D3D p = monoPoints.get(0);

		// use observations from the left and right cameras to select the best solution
		Se3_F64 bestMotion = null;
		double bestError = Double.MAX_VALUE;
		for (int i = 0; i < solutions.size; i++) {
			Se3_F64 worldToLeft = solutions.data[i];

			double totalError = 0;

			// use extra observations from the left camera
			distanceLeft.setModel(worldToLeft);
			for (int j = N; j < points.size(); j++) {
				Stereo2D3D s = points.get(i);
				p.observation = s.leftObs;
				p.location = s.location;
				totalError += distanceLeft.distance(p);
			}

			// Use all observations from the right camera
			worldToLeft.concat(leftToRight, worldToRight);
			distanceRight.setModel(worldToRight);
			for (int j = 0; j < points.size(); j++) {
				Stereo2D3D s = points.get(j);
				p.observation = s.rightObs;
				p.location = s.location;
				totalError += distanceRight.distance(p);
			}

			if (totalError < bestError) {
				bestError = totalError;
				bestMotion = worldToLeft;
			}
		}

		if (bestMotion == null)
			return false;

		estimatedModel.setTo(bestMotion);
		return true;
	}

	public void setStereoParameters( StereoParameters param ) {
		param.right_to_left.invert(leftToRight);
		distanceLeft.setIntrinsic(0, param.left);
		distanceRight.setIntrinsic(0, param.right);
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints() + extraForTest;
	}
}
