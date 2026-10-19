/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.geometry;

import boofcv.chainrule.CROperatorInvSE3;
import boofcv.chainrule.Rodrigues3Numerics;
import boofcv.chainrule.RotationNumerics;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ddogleg.chainrule.JacobianSerialChain;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ddogleg.chainrule.ops.CRErrorHuberNorm;
import org.ddogleg.chainrule.ops.CROperatorRemapParams;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.List;

/// Demonstrates how to fit a 3D plane to a set of points with a robust cost function to mitigate the outliers
/// First it will fit it without a robust cost function and show you the error resulting fit error against
/// points on the plane, then it will add robustness and show you the results.
public class ExampleChainRulePlaneFitChain {
	/// The plane is a coordinate frame the points are moved into. A point's distance from the plane is
	/// then its z, so the chain is: world point -> plane frame -> take z -> error
	static final RotationNumerics ROTATION = new Rodrigues3Numerics();
	static final int DOF = 3 + 3; // translation and rotation of the plane's frame

	public static void main( String[] args ) {
		var helper = new ChainRuleExampleOps();
		var truth = new PlaneNormal3D_F64(0.2, -0.1, 1.5, 0.3, 0.4, 1.0);
		truth.n.normalize();

		List<Point3D_F64> points = helper.createPlane(truth, 1.0, 200);
		helper.addNoise(points, 0.01);
		List<Point3D_F64> inliers = List.copyOf(points);
		helper.addOutliers3D(points, 2.0, 20);

		System.out.println("Least squares:");
		PlaneNormal3D_F64 found = fit(points, false);
		System.out.println("  inliers " + ChainRuleExampleOps.planeError(inliers, found));

		System.out.println("Huber loss:");
		found = fit(points, true);
		System.out.println("  inliers " + ChainRuleExampleOps.planeError(inliers, found));
	}

	/// Fits the plane by minimizing point-to-plane distance, optionally through a robust loss
	static PlaneNormal3D_F64 fit( List<Point3D_F64> points, boolean robust ) {
		var function = new PlaneResiduals(points, robust);

		var minimizer = new LeastSquaresSwitcher<>();
		minimizer.setSolver(new ConfigNonLinearLeastSquares(), true, true);
		minimizer.setFunction(function, function);

		// seeded at the origin looking down z, so the solver has to find the plane
		var param = new double[DOF];
		var solver = minimizer.getSolver();
		solver.initialize(param, 1e-8, 1e-8);
		for (int i = 0; i < 100 && !solver.isConverged(); i++) {
			solver.iterate();
		}

		return toPlane(solver.getParameters());
	}

	/// Point-to-plane distance of every point, as a residual function and its Jacobian
	static class PlaneResiduals implements FunctionNtoM, LeastSquaresSwitcher.ProcessJacobianOut {
		final List<Point3D_F64> points;

		/// world point -> plane frame -> z -> error against a distance of zero
		final JacobianSerialChain<double[]> chain = new JacobianSerialChain<>(new MathCRMatrixD());
		final CROperatorInvSE3 opToPlane = new CROperatorInvSE3(false, ROTATION);
		final CROperatorRemapParams opTakeZ = new CROperatorRemapParams(1, -1, -1, 0);
		final CROperatorBackwardsJac<double[]> opRobustError;

		final CRArray<double[]> input = new CRArray<>(new double[3], 0);
		final CRArray<double[]> plane = new CRArray<>(new double[DOF], 0);
		final CRArray<double[]> output = new CRArray<>(new double[1], 0);

		LeastSquaresSwitcher.JacobianOut jacobian;
		int row;

		PlaneResiduals( List<Point3D_F64> points, boolean robust ) {
			this.points = points;
			this.opRobustError = new CRErrorHuberNorm(1, 0.05);

			chain.setPublishJac(( col, jac ) ->
					jacobian.addMatrixToJacobian(row, col, jac.data, jac.rows, jac.cols));

			// The chain will automatically pass in the output of the previous block as input to argument 0
			int blkPlane = chain.add(opToPlane).in(1, 0).lastBlockId();
			int blkTakeZ = chain.add(opTakeZ).lastBlockId();
			int blkRobust = chain.add(opRobustError).lastBlockId();

			// Optionally, include robust error in the chain
			if (robust)
				chain.chain(blkPlane, blkTakeZ, blkRobust);
			else
				chain.chain(blkPlane, blkTakeZ);
		}

		@Override public void process( double[] param, double[] residuals ) {
			plane.wrap(param, 0);
			opToPlane.setInput(1, plane);
			for (int i = 0; i < points.size(); i++) {
				output.wrap(residuals, i);
				chain.forward(0, pointAt(i), output);
			}
		}

		@Override public void process( double[] param ) {
			plane.wrap(param, 0);
			opToPlane.setInput(1, plane);
			for (int i = 0; i < points.size(); i++) {
				row = i;
				chain.forward(0, pointAt(i), null);
				chain.backwards(0);
			}
		}

		CRArray<double[]> pointAt( int index ) {
			Point3D_F64 p = points.get(index);
			input.a[0] = p.x;
			input.a[1] = p.y;
			input.a[2] = p.z;
			return input;
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			this.jacobian = jacobian;
		}

		/// Nothing to eliminate first: every residual sees every parameter
		@Override public int getSchurSplit() {return DOF;}

		@Override public int getNumOfInputsN() {return DOF;}

		@Override public int getNumOfOutputsM() {return points.size();}
	}

	/// The frame's origin is a point on the plane and its z axis is the normal
	static PlaneNormal3D_F64 toPlane( double[] param ) {
		var planeToWorld = new Se3_F64();
		ROTATION.set(param, 3);
		ROTATION.toMatrix(planeToWorld.R);
		planeToWorld.T.setTo(param[0], param[1], param[2]);

		// the frame's z axis is the third column of its rotation
		var normal = new Vector3D_F64(planeToWorld.R.get(0, 2), planeToWorld.R.get(1, 2), planeToWorld.R.get(2, 2));
		return new PlaneNormal3D_F64(new Point3D_F64(planeToWorld.T.x, planeToWorld.T.y, planeToWorld.T.z), normal);
	}
}
