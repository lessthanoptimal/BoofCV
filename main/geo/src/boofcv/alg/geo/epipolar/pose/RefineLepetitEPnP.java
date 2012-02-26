/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.epipolar.pose;

import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import boofcv.numerics.optimization.impl.UtilOptimize;
import georegression.fitting.MotionTransformPoint;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Optimizes solution found in EPnP algorithm
 *
 * @author Peter Abeles
 */
public class RefineLepetitEPnP {

	// the instance which it is optimizing
	PnPLepetitEPnP master;
	// optimization algorithm
	UnconstrainedLeastSquares optimizer;
	// maximum number of allowed iteration
	int maxIterations;

	ResidualsEPnP func = new ResidualsEPnP();
//	JacobianEPnP jacobian = new JacobianEPnP();

	// estimates rigid body motion between two associated sets of points
	private MotionTransformPoint<Se3_F64, Point3D_F64> motionFit =
			FitSpecialEuclideanOps_F64.fitPoints3D();

	// the estimated camera motion.  from world to camera
	private Se3_F64 solutionMotion = new Se3_F64();

	/**
	 * Configures the optimization algorithm
	 *
	 * @param master Instance whose parameters are being optimized
	 * @param tolerance convergence tolerance
	 * @param maxIterations Maximum number of allowed iterations
	 */
	public RefineLepetitEPnP(PnPLepetitEPnP master ,
							 double tolerance ,
							 int maxIterations ) {
		this.master = master;
		this.maxIterations = maxIterations;
		optimizer = FactoryOptimization.leastSquareLevenberg(tolerance, 0, 1e-4);
	}

	/**
	 * Performs non-linear refinement on the best set of betas from {@link PnPLepetitEPnP}.  After
	 * it is done it recomputes the position estimate.
	 */
	public void refine() {
		// set up the optimization
		func.setParameters(master.controlWorldPts, master.nullPts);
//		jacobian.setUp(func);

		double beta[] = master.getBestControlWeights();

		optimizer.setFunction(func, null);
		optimizer.initialize(beta);

		// optimize the parameters
		UtilOptimize.process(optimizer, maxIterations);

		// just recycling dome data
		List<Point3D_F64> controlCameraPts = func.getCameraPts();

		// compute the motion
		double improved[] = optimizer.getParameters();
		UtilLepetitEPnP.computeCameraControl(improved,master.nullPts,controlCameraPts);
		motionFit.process(master.controlWorldPts,controlCameraPts);

		solutionMotion.set(motionFit.getMotion());
	}

	/**
	 * Return the value of beta after being optimzied.
	 */
	public double[] getOptimizedBetas() {
		return optimizer.getParameters();
	}

	public Se3_F64 getMotion() {
		return solutionMotion;
	}
}
