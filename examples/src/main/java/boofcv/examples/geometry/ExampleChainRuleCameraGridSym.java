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

import boofcv.chainrule.CRUtils;
import boofcv.chainrule.SymGeoOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.JacobianChainGraph;
import org.ddogleg.chainrule.sym.*;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

/// Same model as [ExampleChainRuleCameraGridChain], but it uses a symbolic graph that handles the bookkeeping that
/// [JacobianChainGraph] forces you to do manually. Simplest to implement
public class ExampleChainRuleCameraGridSym {
	public static void main( String[] args ) {
		var scene = new ChainRuleCameraGridScene();
		var function = new Residuals(scene);

		var minimizer = new LeastSquaresSwitcher<>();
		minimizer.setSolver(new ConfigNonLinearLeastSquares(), true, true);
		minimizer.setFunction(function, function);

		double[] param = scene.createParameters(); // the grid starts at zero: no lens shift at all
		System.out.println("before " + ChainRuleExampleOps.pixelError(scene.observations, scene.predict(param)));

		var solver = minimizer.getSolver();
		solver.initialize(param, 1e-12, 1e-12);
		for (int i = 0; i < 100 && !solver.isConverged(); i++) {
			solver.iterate();
		}

		String after = ChainRuleExampleOps.pixelError(scene.observations, scene.predict(solver.getParameters()));
		System.out.println("after  " + after);
	}

	static class Residuals implements FunctionNtoM, LeastSquaresSwitcher.ProcessJacobianOut {
		final ChainRuleCameraGridScene scene;

		final SymFunction residuals;
		final Sym symPoint = Sym.input("point", 3);
		final Sym symObserved = Sym.input("observed", 2);
		final Sym symPose = Sym.param("pose", ChainRuleCameraGridScene.POSE_DOF);
		/// held at its true value, so a constant rather than something with a column
		final Sym symPinhole;
		final SymGrid grid;

		final CRArray<double[]> hPoint, hObserved, hPose;
		final CRArray<double[]> output = new CRArray<>(new double[2], 0);

		Residuals( ChainRuleCameraGridScene scene ) {
			this.scene = scene;
			symPinhole = Sym.constant(scene.pinhole.fx, scene.pinhole.fy, scene.pinhole.cx, scene.pinhole.cy);
			grid = SymOps.grid("shift", scene.gridShape,
					ChainRuleCameraGridScene.WIDTH, ChainRuleCameraGridScene.HEIGHT);

			// ================== the model, in four lines ==================
			Sym inView = SymGeoOps.se3(ChainRuleCameraGridScene.ROTATION, symPose, symPoint);
			Sym u = SymGeoOps.pinhole(true, -1, symPinhole, inView);
			Sym shifted = SymOps.plus(2, u, grid.at(u).of(Sym.constant(0.0)));
			Sym error = SymOps.residual(1.0, shifted, symObserved);
			// =============================================================

			residuals = new SymGraphBuilder().build(error);
			hPoint = residuals.feed(symPoint);
			hObserved = residuals.feed(symObserved);
			hPose = residuals.feed(symPose, new double[ChainRuleCameraGridScene.POSE_DOF]);
			residuals.feed(grid.knots, new double[scene.gridShape.dof()]);
		}

		@Override public void process( double[] param, double[] out ) {
			run(param, out);
		}

		@Override public void process( double[] param ) {
			run(param, null);
		}

		/// The loop is the same either way: bind what moved, then run forwards or differentiate
		void run( double[] param, double[] out ) {
			residuals.feed(grid.knots, param).offset = scene.offsetGrid();

			int row = 0;
			for (int idxView = 0; idxView < scene.views.size(); idxView++) {
				// one small pose param re-bound per view, never a param over every pose at once: the
				// backwards pass is dense in a node's width, so the wide version costs width-squared
				residuals.feed(symPose, param).offset = idxView*ChainRuleCameraGridScene.POSE_DOF;
				residuals.mapParamColumns();

				for (Point3D_F64 p : scene.target) {
					CRUtils.toArray(p.x, p.y, p.z, hPoint.a, 0);

					Point2D_F64 observed = scene.observations.get(row/2);
					CRUtils.toArray(observed.x, observed.y, hObserved.a, 0);

					if (out != null) {
						output.wrap(out, row);
						residuals.run(output);
					} else {
						residuals.jacobianRow = row;
						residuals.runJacobian();
					}
					row += 2;
				}
			}
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			residuals.jacobianTo(jacobian);
		}

		@Override public int getSchurSplit() {return scene.numParameters();}

		@Override public int getNumOfInputsN() {return scene.numParameters();}

		@Override public int getNumOfOutputsM() {return scene.observations.size()*2;}
	}
}
