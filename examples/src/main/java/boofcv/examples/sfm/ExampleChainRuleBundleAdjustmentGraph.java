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
import boofcv.chainrule.CROperatorSE3;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.JacobianChainGraph;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ddogleg.chainrule.ops.CRErrorVectorSW;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.io.IOException;

/// The same bundle adjustment as [ExampleBundleAdjustment], but the model and its Jacobian come from chain rule
/// operators instead of BoofCV's hand written bundle adjustment code. For each observation of point j in view i
///
///     pixel = Snavely(camera_i, R_i·X_j + T_i),   residual = observed − pixel
///
/// The operators are strung together by hand into a [JacobianChainGraph]. Every value here is read once, so the
/// graph holds a single chain. [ExampleChainRuleBundleAdjustmentSym] writes the same model as an expression.
public class ExampleChainRuleBundleAdjustmentGraph {
	public static void main( String[] args ) throws IOException {
		var scene = new ChainRuleBundleScene();
		scene.optimize(new Residuals(scene));
	}

	/// Residuals of every observation and their Jacobian
	static class Residuals implements FunctionNtoM, LeastSquaresSwitcher.ProcessJacobianOut {
		final ChainRuleBundleScene scene;

		final JacobianChainGraph<double[]> graphs = new JacobianChainGraph<>(new MathCRMatrixD());

		// X' = R·X + T, the point in the camera's frame
		final CROperatorSE3 opPose = new CROperatorSE3(false, ChainRuleBundleScene.ROTATION);
		// pixel = Snavely(camera, X')
		final CROperatorCameraSnavely opCamera = new CROperatorCameraSnavely();
		// residual = observed − pixel
		final CRErrorVectorSW opError = new CRErrorVectorSW(2, 1.0);

		final int blockPose, blockCamera, chain, graph;

		final CRArray<double[]> point = new CRArray<>();
		final CRArray<double[]> pose = new CRArray<>();
		final CRArray<double[]> camera = new CRArray<>();
		final CRArray<double[]> observed = new CRArray<>(new double[2], 0);
		final CRArray<double[]> output = new CRArray<>(new double[2], 0);

		LeastSquaresSwitcher.JacobianOut jacobian;
		int row, pointColumn;

		Residuals( ChainRuleBundleScene scene ) {
			this.scene = scene;
			var chains = graphs.getChains();

			// ∂r/∂x for inputs mapped to parameters, the pose and the camera
			chains.setPublishJac(( col, jac ) -> jacobian.addMatrixToJacobian(row, col, jac.data, jac.rows, jac.cols));

			// point → X' → pixel → residual
			blockPose = chains.add(opPose).lastBlockId();
			blockCamera = chains.add(opCamera).lastBlockId();
			int blockError = chains.add(opError).lastBlockId();
			chain = chains.chain(blockPose, blockCamera, blockError);

			// The point is the chain's input rather than a mapped parameter, so ∂r/∂X is captured and written to
			// the point's columns
			chains.setCapture(blockPose, 0, ( jac ) ->
					jacobian.addMatrixToJacobian(row, pointColumn, jac.data, jac.rows, jac.cols));

			graph = graphs.graph().chain(chain).fixate();
		}

		/// r(x)
		@Override public void process( double[] param, double[] out ) {run(param, out);}

		/// J(x) = ∂r/∂x
		@Override public void process( double[] param ) {run(param, null);}

		/// Points each operator at this observation's parameters, then runs forwards or differentiates
		void run( double[] param, double[] out ) {
			var chains = graphs.getChains();
			scene.forEachObservation(( row, pointIdx, motion, cameraIdx, pixel ) -> {
				this.row = row;
				pointColumn = scene.pointColumn(pointIdx);
				point.wrap(param, pointColumn);

				// Inputs other than the chain's own are handed to their operator, along with which column of
				// the Jacobian they belong to
				opPose.setInput(1, pose.wrap(param, scene.poseColumn(motion)));
				chains.setInputMap(blockPose, 1, pose.offset);
				opCamera.setInput(CROperatorCameraSnavely.IN_CAMERA, camera.wrap(param, scene.cameraColumn(cameraIdx)));
				chains.setInputMap(blockCamera, CROperatorCameraSnavely.IN_CAMERA, camera.offset);

				observed.a[0] = pixel.x;
				observed.a[1] = pixel.y;
				opError.setInput(1, observed);

				if (out != null) {
					graphs.forward(graph).input(chain, point).run(output.wrap(out, row));
				} else {
					graphs.forward(graph).input(chain, point).run(null);
					graphs.backwards(graph);
				}
			});
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			this.jacobian = jacobian;
		}

		@Override public int getSchurSplit() {return scene.poseColumn(0);}

		@Override public int getNumOfInputsN() {return scene.numParameters();}

		@Override public int getNumOfOutputsM() {return scene.numResiduals();}
	}
}
