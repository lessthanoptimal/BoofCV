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
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.ops.CRErrorHuberNorm;
import org.ddogleg.chainrule.sym.Sym;
import org.ddogleg.chainrule.sym.SymFunction;
import org.ddogleg.chainrule.sym.SymGraphBuilder;
import org.ddogleg.chainrule.sym.SymOps;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.List;

/// [ExampleChainRulePlaneFitChain] written symbolically: the same plane fit, with the chain described as
/// an expression instead of assembled block by block. What the caller supplies is unchanged — a
/// point per residual — so only the wiring differs.
public class ExampleChainRulePlaneFitSym {
	static final RotationNumerics ROTATION = new Rodrigues3Numerics();
	static final int DOF = 3 + 3;

	public static void main( String[] args ) {
		var helper = new ChainRuleExampleOps();
		var truth = new PlaneNormal3D_F64(0.2, -0.1, 1.5, 0.3, 0.4, 1.0);
		truth.n.normalize();

		List<Point3D_F64> points = helper.createPlane(truth, 1.0, 200);
		helper.addNoise(points, 0.01);
		List<Point3D_F64> inliers = List.copyOf(points);
		helper.addOutliers3D(points, 2.0, 20);

		System.out.println("Least squares:");
		System.out.println("  inliers " + ChainRuleExampleOps.planeError(inliers, fit(points, false)));

		System.out.println("Huber loss:");
		System.out.println("  inliers " + ChainRuleExampleOps.planeError(inliers, fit(points, true)));
	}

	static PlaneNormal3D_F64 fit( List<Point3D_F64> points, boolean robust ) {
		var function = new PlaneResiduals(points, robust);

		var minimizer = new LeastSquaresSwitcher<>();
		minimizer.setSolver(new ConfigNonLinearLeastSquares(), true, true);
		minimizer.setFunction(function, function);

		var solver = minimizer.getSolver();
		solver.initialize(new double[DOF], 1e-8, 1e-8);
		for (int i = 0; i < 100 && !solver.isConverged(); i++) {
			solver.iterate();
		}

		return ExampleChainRulePlaneFitChain.toPlane(solver.getParameters());
	}

	static class PlaneResiduals implements FunctionNtoM, LeastSquaresSwitcher.ProcessJacobianOut {
		final List<Point3D_F64> points;

		final SymFunction residuals;
		final Sym symPoint = Sym.input("point", 3);
		final Sym symPlane = Sym.param("plane", DOF);
		final CRArray<double[]> hPoint, hPlane;
		final CRArray<double[]> output = new CRArray<>(new double[1], 0);

		PlaneResiduals( List<Point3D_F64> points, boolean robust ) {
			this.points = points;

			// ============ the model: distance from the plane, as an expression ============
			Sym local = Sym.of(new CROperatorInvSE3(false, ROTATION)).in0(symPoint).in1(symPlane);
			// Signed distance from the plane
			Sym distance = SymOps.remap(1, local, -1, -1, 0);
			// Optionally, use robust loss to reshape the distance to give less weight towards outliers.
			Sym error = robust ? Sym.of(new CRErrorHuberNorm(1, 0.05)).in0(distance) : distance;
			// =============================================================================

			residuals = new SymGraphBuilder().build(error);
			hPoint = residuals.feed(symPoint);
			hPlane = residuals.feed(symPlane, new double[DOF]);
		}

		/// Forward function
		@Override public void process( double[] param, double[] out ) {
			bind(param);
			for (int i = 0; i < points.size(); i++) {
				output.wrap(out, i);
				point(i);
				residuals.run(output);
			}
		}

		/// Jacobian
		@Override public void process( double[] param ) {
			bind(param);
			for (int i = 0; i < points.size(); i++) {
				point(i);
				residuals.jacobianRow = i;
				residuals.runJacobian();
			}
		}

		/// The parameter array is the plane terminal's storage, so its column is where it starts
		void bind( double[] param ) {
			residuals.feed(symPlane, param).offset = 0;
			residuals.mapParamColumns();
		}

		void point( int index ) {
			Point3D_F64 p = points.get(index);
			hPoint.a[0] = p.x;
			hPoint.a[1] = p.y;
			hPoint.a[2] = p.z;
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			residuals.jacobianTo(jacobian);
		}

		@Override public int getSchurSplit() {return DOF;}

		@Override public int getNumOfInputsN() {return DOF;}

		@Override public int getNumOfOutputsM() {return points.size();}
	}
}
