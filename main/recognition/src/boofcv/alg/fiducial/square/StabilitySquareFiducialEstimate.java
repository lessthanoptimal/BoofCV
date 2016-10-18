/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.square;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Used to estimate the stability of {@link BaseDetectFiducialSquare} fiducials.  Each corner point is disturbed by
 * adding the user provided "disturbance" independently to the x and y axis in the positive and negative direction.
 * The maximum resulting delta is then reported.  Maximum change in angle and orientation is computed.
 *
 * @author Peter Abeles
 */
public class StabilitySquareFiducialEstimate {

	// estimates the pose of the fiducial
	private QuadPoseEstimator estimator;
	// storage for corner observations
	private Quadrilateral_F64 work = new Quadrilateral_F64();

	// the pose estimate at the current unmodified location
	private Se3_F64 referenceCameraToWorld = new Se3_F64();

	// storage for the difference between a sample pose estimate and the reference
	private Se3_F64 difference = new Se3_F64();

	// storage for all the samples
	private FastQueue<Se3_F64> samples = new FastQueue<>(Se3_F64.class, true);

	private Rodrigues_F64 rodrigues = new Rodrigues_F64();

	// compute metrics.
	private double maxLocation;
	private double maxOrientation;

	public StabilitySquareFiducialEstimate(QuadPoseEstimator estimator) {
		this.estimator = estimator;
	}

	/**
	 * Processes the observation and generates a stability estimate
	 *
	 * @param sampleRadius Radius around the corner pixels it will sample
	 * @param input Observed corner location of the fiducial in distorted pixels.  Must be in correct order.
	 * @return true if successful or false if it failed
	 */
	public boolean process( double sampleRadius , Quadrilateral_F64 input ) {
		work.set(input);

		samples.reset();
		estimator.process(work);
		estimator.getWorldToCamera().invert(referenceCameraToWorld);

		samples.reset();

		createSamples(sampleRadius,work.a,input.a);
		createSamples(sampleRadius,work.b,input.b);
		createSamples(sampleRadius,work.c,input.c);
		createSamples(sampleRadius,work.d,input.d);

		if( samples.size() < 10 )
			return false;

		maxLocation = 0;
		maxOrientation = 0;
		for (int i = 0; i < samples.size(); i++) {
			referenceCameraToWorld.concat(samples.get(i), difference);

			ConvertRotation3D_F64.matrixToRodrigues(difference.getR(),rodrigues);

			double theta = Math.abs(rodrigues.theta);

			double d = difference.getT().norm();

			if( theta > maxOrientation ) {
				maxOrientation = theta;
			}
			if( d > maxLocation ) {
				maxLocation = d;
			}

		}

		return true;
	}

	/**
	 * Samples around the provided corner +- in x and y directions
	 */
	private void createSamples( double sampleRadius , Point2D_F64 workPoint , Point2D_F64 originalPoint ) {

		workPoint.x = originalPoint.x + sampleRadius;
		if( estimator.process(work) ) {
			samples.grow().set( estimator.getWorldToCamera() );
		}
		workPoint.x = originalPoint.x - sampleRadius;
		if( estimator.process(work) ) {
			samples.grow().set( estimator.getWorldToCamera() );
		}
		workPoint.x = originalPoint.x;

		workPoint.y = originalPoint.y + sampleRadius;
		if( estimator.process(work) ) {
			samples.grow().set( estimator.getWorldToCamera() );
		}
		workPoint.y = originalPoint.y - sampleRadius;
		if( estimator.process(work) ) {
			samples.grow().set( estimator.getWorldToCamera() );
		}
		workPoint.set(originalPoint);
	}

	public double getLocationStability() {
		return maxLocation;
	}

	public double getOrientationStability() {
		return maxOrientation;
	}
}
