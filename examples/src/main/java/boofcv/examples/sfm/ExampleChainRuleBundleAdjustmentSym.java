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

package boofcv.examples.sfm;

import boofcv.chainrule.CROperatorCameraSnavely;
import boofcv.chainrule.SymGeoOps;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.sym.Sym;
import org.ddogleg.chainrule.sym.SymFunction;
import org.ddogleg.chainrule.sym.SymGraphBuilder;
import org.ddogleg.chainrule.sym.SymOps;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.io.IOException;

/// The same bundle adjustment as [ExampleBundleAdjustment], but the model and its Jacobian come from chain rule
/// operators instead of BoofCV's hand written bundle adjustment code. For each observation of point j in view i
///
///     pixel = Snavely(camera_i, R_i·X_j + T_i),   residual = observed − pixel
///
/// and the model is written as an expression. The library works out how the operators connect and how
/// derivatives flow between them. [ExampleChainRuleBundleAdjustmentGraph] builds the same model from chains.
public class ExampleChainRuleBundleAdjustmentSym {
	public static void main( String[] args ) throws IOException {
		var scene = new ChainRuleBundleScene();
		scene.optimize(new Residuals(scene));
	}

	/// Residuals of every observation and their Jacobian
	static class Residuals implements FunctionNtoM, LeastSquaresSwitcher.ProcessJacobianOut {
		final ChainRuleBundleScene scene;

		final Sym symPoint = Sym.param("point", ChainRuleBundleScene.POINT_DOF);
		final Sym symPose = Sym.param("pose", ChainRuleBundleScene.POSE_DOF);
		final Sym symCamera = Sym.param("camera", ChainRuleBundleScene.CAMERA_DOF);
		final Sym symObserved = Sym.input("observed", 2);

		final SymFunction function;
		final CRArray<double[]> observed;
		final CRArray<double[]> output = new CRArray<>(new double[2], 0);

		Residuals( ChainRuleBundleScene scene ) {
			this.scene = scene;

			// ================== the model ==================
			// X' = R·X + T, the point in the camera's frame
			Sym inView = SymGeoOps.se3(ChainRuleBundleScene.ROTATION, symPose, symPoint);
			// pixel = Snavely(camera, X')
			Sym pixel = Sym.of(new CROperatorCameraSnavely()).in0(inView).in1(symCamera);
			// residual = observed − pixel
			Sym error = SymOps.residual(1.0, pixel, symObserved);
			// ===============================================

			function = new SymGraphBuilder().build(error);
			observed = function.feed(symObserved);
		}

		/// r(x)
		@Override public void process( double[] param, double[] out ) {run(param, out);}

		/// J(x) = ∂r/∂x
		@Override public void process( double[] param ) {run(param, null);}

		/// The loop is the same either way: point the parameters at this observation, then run forwards or
		/// differentiate
		void run( double[] param, double[] out ) {
			scene.forEachObservation(( row, point, motion, camera, pixel ) -> {
				function.feed(symPoint, param).offset = scene.pointColumn(point);
				function.feed(symPose, param).offset = scene.poseColumn(motion);
				function.feed(symCamera, param).offset = scene.cameraColumn(camera);
				function.mapParamColumns();
				observed.a[0] = pixel.x;
				observed.a[1] = pixel.y;

				if (out != null) {
					output.wrap(out, row);
					function.run(output);
				} else {
					function.jacobianRow = row;
					function.runJacobian();
				}
			});
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			function.jacobianTo(jacobian);
		}

		@Override public int getSchurSplit() {return scene.poseColumn(0);}

		@Override public int getNumOfInputsN() {return scene.numParameters();}

		@Override public int getNumOfOutputsM() {return scene.numResiduals();}
	}
}
