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

import boofcv.chainrule.CROperatorCameraPinhole;
import boofcv.chainrule.CROperatorSE3;
import boofcv.chainrule.CRUtils;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.JacobianChainGraph;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ddogleg.chainrule.grid.ConfigGridCoefND;
import org.ddogleg.chainrule.ops.CRErrorVectorSW;
import org.ddogleg.chainrule.ops.CROperatorBilinearPolyND;
import org.ddogleg.chainrule.ops.CROperatorGridBilinearWeights;
import org.ddogleg.chainrule.ops.CROperatorPlus;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

/// Same model as [ExampleChainRuleCameraGridChain] but uses a [JacobianChainGraph] which handles the problem's
/// graph structure natively, avoiding manually passing inputs/outputs around two chains.
public class ExampleChainRuleCameraGridGraph {
	public static void main( String[] args ) {
		var scene = new ChainRuleCameraGridScene();
		var function = new Residuals(scene);

		var minimizer = new LeastSquaresSwitcher<>();
		minimizer.setSolver(new ConfigNonLinearLeastSquares(), true, true);
		minimizer.setFunction(function, function);

		double[] param = scene.createParameters();
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

		final JacobianChainGraph<double[]> graphs = new JacobianChainGraph<>(new MathCRMatrixD());

		final CROperatorSE3 opPose = new CROperatorSE3(false, ChainRuleCameraGridScene.ROTATION);
		final CROperatorCameraPinhole opCamera = new CROperatorCameraPinhole(false, true, -1);
		final CROperatorGridBilinearWeights opWeights = new CROperatorGridBilinearWeights();
		final CROperatorBilinearPolyND opField = new CROperatorBilinearPolyND();
		final CROperatorPlus opPlus = new CROperatorPlus(2);
		final CRErrorVectorSW opError = new CRErrorVectorSW(2, 1.0);

		final int blockPose, blockCamera, blockField;
		final int chainPixel, chainWeights, chainField;
		final int graph;

		final CRArray<double[]> point = new CRArray<>(new double[3], 0);
		final CRArray<double[]> pose = new CRArray<>(new double[ChainRuleCameraGridScene.POSE_DOF], 0);
		final CRArray<double[]> knots = new CRArray<>(new double[0], 0);
		final CRArray<double[]> knotSlice = new CRArray<>();
		final int[] knotCoors = new int[ConfigGridCoefND.NUM_KNOTS];
		final CRArray<double[]> output = new CRArray<>(new double[2], 0);

		LeastSquaresSwitcher.JacobianOut jacobian;
		int row;

		Residuals( ChainRuleCameraGridScene scene ) {
			this.scene = scene;
			opWeights.configure(ChainRuleCameraGridScene.WIDTH, ChainRuleCameraGridScene.HEIGHT, scene.gridShape);
			opField.configure(scene.gridShape);

			var chains = graphs.getChains();
			chains.setPublishJac(( col, jac ) ->
					jacobian.addMatrixToJacobian(row, col, jac.data, jac.rows, jac.cols));

			blockPose = chains.add(opPose).lastBlockId();
			blockCamera = chains.add(opCamera).lastBlockId();
			int blockPlus = chains.add(opPlus).lastBlockId();
			int blockError = chains.add(opError).lastBlockId();
			int blockWeights = chains.add(opWeights).lastBlockId();
			blockField = chains.add(opField).lastBlockId();

			// the pixel's own path, then the field's, which reads the pixel the first one produced
			chainPixel = chains.chain(blockPose, blockCamera, blockPlus, blockError);
			chainWeights = chains.chain(blockWeights);
			chainField = chains.chain(blockField);

			// which knots to read is only known once the pixel exists, so it is chosen here
			chains.setPreForward(blockWeights, ( op ) -> {
				scene.gridShape.knotOffsets(opWeights.col(), opWeights.row(), knots.offset, knotCoors);
				for (int knot = 0; knot < ConfigGridCoefND.NUM_KNOTS; knot++) {
					opField.setInput(CROperatorBilinearPolyND.IN_KNOT_0 + knot,
							knotSlice.wrap(knots.a, knotCoors[knot]));
				}
			});

			graph = graphs.graph()
					// the projected pixel goes to the field's weights and, through the field, back
					// into the sum it is part of
					.depMid(chainPixel, 1, chainWeights, 0, CROperatorGridBilinearWeights.IN_PIXEL)
					.depMid(chainWeights, 0, chainField, 0, CROperatorBilinearPolyND.IN_WEIGHTS)
					.depMid(chainField, 0, chainPixel, 2, CROperatorPlus.IN_B)
					.fixate();

			// the field's knot columns move with the pixel, so they are published as they are
			// read, and are registered once rather than per evaluation
			for (int knot = 0; knot < ConfigGridCoefND.NUM_KNOTS; knot++) {
				final int index = knot;
				chains.setCapture(blockField, CROperatorBilinearPolyND.IN_KNOT_0 + knot,
						( jac ) -> jacobian.addMatrixToJacobian(row, opField.knotOffset(index),
								jac.data, jac.rows, jac.cols));
			}

			opField.setInput(CROperatorBilinearPolyND.IN_VALUE, new CRArray<>(new double[]{0}, 0));

			// The camera is held fixed, so this is the only time it is set
			opCamera.setTo(scene.pinhole);
		}

		@Override public void process( double[] param, double[] out ) {run(param, out);}

		@Override public void process( double[] param ) {run(param, null);}

		void run( double[] param, double[] out ) {
			knots.wrap(param, scene.offsetGrid());
			var chains = graphs.getChains();
			row = 0;
			int index = 0;
			for (int idxView = 0; idxView < scene.views.size(); idxView++) {
				pose.wrap(param, idxView*ChainRuleCameraGridScene.POSE_DOF);
				opPose.setInput(1, pose);
				chains.setInputMap(blockPose, 1, pose.offset);

				for (Point3D_F64 p : scene.target) {
					CRUtils.toArray(p.x, p.y, p.z, point.a, 0);

					Point2D_F64 observed = scene.observations.get(index++);
					opError.setObserved(new double[]{observed.x, observed.y}, 0);

					if (out != null) {
						output.wrap(out, row);
						graphs.forward(graph).input(chainPixel, point).run(output);
					} else {
						graphs.forward(graph).input(chainPixel, point).run(null);
						graphs.backwards(graph);
					}
					row += 2;
				}
			}
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			this.jacobian = jacobian;
		}

		@Override public int getSchurSplit() {return scene.numParameters();}

		@Override public int getNumOfInputsN() {return scene.numParameters();}

		@Override public int getNumOfOutputsM() {return scene.observations.size()*2;}
	}
}
