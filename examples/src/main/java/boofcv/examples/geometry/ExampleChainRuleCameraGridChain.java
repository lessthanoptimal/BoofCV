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
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.JacobianSerialChain;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ddogleg.chainrule.grid.ConfigGridCoefND;
import org.ddogleg.chainrule.ops.CRErrorVectorSW;
import org.ddogleg.chainrule.ops.CROperatorBilinearPolyND;
import org.ddogleg.chainrule.ops.CROperatorGridBilinearWeights;
import org.ddogleg.chainrule.ops.CROperatorPlus;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

/// A camera looks at a planar target through a lens that shifts every pixel a little. Fit the pose
/// of each view and the shift field: `pixel = u + D(u)`, where `u` is where the pinhole projected
/// the point.
///
/// `u` is read twice — by the field, and by the sum the field's output is added to. A serial chain
/// is a line, so it cannot say that. Here the two paths are two chains, and everything they share
/// is the caller's problem: the pixel is passed across by hand, and the pose derivative arriving
/// along both paths has to be summed by hand before it is published. [ExampleChainRuleCameraGridGraph] is
/// the same model with that work handed to the library.
public class ExampleChainRuleCameraGridChain {
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

		/// Every operator lives in one chain object. What differs is how they are strung together:
		/// one chain for the pixel's path, another for the field's, since the pixel feeds both the
		/// field and the sum the field's output goes into
		final JacobianSerialChain<double[]> chains = new JacobianSerialChain<>(new MathCRMatrixD());

		/// point -> view -> pinhole. Ends where the field has to be read
		final CROperatorSE3 opPose = new CROperatorSE3(false, ChainRuleCameraGridScene.ROTATION);
		final CROperatorCameraPinhole opCamera = new CROperatorCameraPinhole(false, true, -1);

		/// pixel -> weights -> field
		final CROperatorGridBilinearWeights opWeights = new CROperatorGridBilinearWeights();
		final CROperatorBilinearPolyND opField = new CROperatorBilinearPolyND();

		/// pixel + shift -> error. Ends in an error operator like the other two examples, so the
		/// residual comes out of a forward pass and its sign is the operator's business
		final CROperatorPlus opPlus = new CROperatorPlus(2);
		final CRErrorVectorSW opError = new CRErrorVectorSW(2, 1.0);

		final int blockPose, blockField, blockPlus;
		final int chainPixel, chainField, chainSum;

		final CRArray<double[]> point = new CRArray<>(new double[3], 0);
		final CRArray<double[]> pose = new CRArray<>(new double[ChainRuleCameraGridScene.POSE_DOF], 0);
		final CRArray<double[]> pixel = new CRArray<>(new double[2], 0);
		final CRArray<double[]> shift = new CRArray<>(new double[2], 0);
		final CRArray<double[]> observedPixel = new CRArray<>(new double[2], 0);
		final CRArray<double[]> residual = new CRArray<>(new double[2], 0);
		final CRArray<double[]> knots = new CRArray<>(new double[0], 0);
		final CRArray<double[]> knotSlice = new CRArray<>();
		final int[] knotCoors = new int[ConfigGridCoefND.NUM_KNOTS];

		/// The pieces of the chain rule the caller has to put together itself
		final MathCRMatrixD math = new MathCRMatrixD();
		final CRMatrix<double[]> dPixelDPose = CRMatrix.createD(2, ChainRuleCameraGridScene.POSE_DOF);
		final CRMatrix<double[]> dShiftDPixel = CRMatrix.createD(2, 2);
		final CRMatrix<double[]> dResidualDPixel = CRMatrix.createD(2, 2);
		final CRMatrix<double[]> dResidualDShift = CRMatrix.createD(2, 2);
		/// used by the knot captures and then by the pose, which never overlap
		final CRMatrix<double[]> work = CRMatrix.createD(2, 2);
		final CRMatrix<double[]> poseTotal = CRMatrix.createD(2, ChainRuleCameraGridScene.POSE_DOF);

		LeastSquaresSwitcher.JacobianOut jacobian;
		int row, poseColumn;

		Residuals( ChainRuleCameraGridScene scene ) {
			this.scene = scene;
			opWeights.configure(ChainRuleCameraGridScene.WIDTH, ChainRuleCameraGridScene.HEIGHT, scene.gridShape);
			opField.configure(scene.gridShape);

			opCamera.setTo(scene.pinhole);
			opField.setInput(CROperatorBilinearPolyND.IN_VALUE, new CRArray<>(new double[]{0}, 0));

			blockPose = chains.add(opPose).in(1, 0).lastBlockId();
			int blockCamera = chains.add(opCamera).lastBlockId();
			int blockWeights = chains.add(opWeights).lastBlockId();
			blockField = chains.add(opField).lastBlockId();
			blockPlus = chains.add(opPlus).lastBlockId();
			int blockError = chains.add(opError).lastBlockId();

			chainPixel = chains.chain(blockPose, blockCamera);
			chainField = chains.chain(blockWeights, blockField);
			chainSum = chains.chain(blockPlus, blockError);

			// the pose is the only thing published this way, and it goes into scratch rather than
			// into the Jacobian, since this chain has only half of its derivative
			chains.setPublishJac(( col, jac ) -> dPixelDPose.setTo(jac));

			// how the field's output moves with the pixel it was read at. Asking for the Jacobian of
			// a chain's input is what makes it get computed
			chains.setCapture(blockWeights, CROperatorGridBilinearWeights.IN_PIXEL,
					( jac ) -> dShiftDPixel.setTo(jac));

			// how the residual moves with each half of the sum. These carry the sign of the error,
			// so nothing here has to know which way round it is defined
			chains.setCapture(blockPlus, CROperatorPlus.IN_A, ( jac ) -> dResidualDPixel.setTo(jac));
			chains.setCapture(blockPlus, CROperatorPlus.IN_B, ( jac ) -> dResidualDShift.setTo(jac));

			// the field's knots depend on where the pixel landed, so they are chosen per observation
			chains.setPreForward(blockWeights, ( op ) -> {
				scene.gridShape.knotOffsets(opWeights.col(), opWeights.row(), knots.offset, knotCoors);
				for (int knot = 0; knot < ConfigGridCoefND.NUM_KNOTS; knot++) {
					opField.setInput(CROperatorBilinearPolyND.IN_KNOT_0 + knot,
							knotSlice.wrap(knots.a, knotCoors[knot]));
				}
			});
			// a knot reaches the residual only through the field, so its column is finished once
			// the field's derivative is taken through the sum
			for (int knot = 0; knot < ConfigGridCoefND.NUM_KNOTS; knot++) {
				final int index = knot;
				chains.setCapture(blockField, CROperatorBilinearPolyND.IN_KNOT_0 + knot,
						( jac ) -> {
							work.reshape(jac.rows, jac.cols);
							math.mult(dResidualDShift, jac, work);
							jacobian.addMatrixToJacobian(row, opField.knotOffset(index),
									work.data, work.rows, work.cols);
						});
			}
		}

		@Override public void process( double[] param, double[] out ) {
			forEach(param, out);
		}

		@Override public void process( double[] param ) {
			forEach(param, null);
		}

		void forEach( double[] param, double[] out ) {
			knots.wrap(param, scene.offsetGrid());

			row = 0;
			int index = 0;
			for (int idxView = 0; idxView < scene.views.size(); idxView++) {
				poseColumn = idxView*ChainRuleCameraGridScene.POSE_DOF;
				pose.wrap(param, poseColumn);
				opPose.setInput(1, pose);
				chains.setInputMap(blockPose, 1, poseColumn);

				for (Point3D_F64 p : scene.target) {
					CRUtils.toArray(p.x, p.y, p.z, point.a, 0);

					// where the pinhole put it, then what the field adds there
					chains.forward(chainPixel, point, pixel);
					chains.forward(chainField, pixel, shift);

					// the sum's other side is not part of any chain, so it is handed over here
					opPlus.setInput(CROperatorPlus.IN_B, shift);
					Point2D_F64 observed = scene.observations.get(index);
					CRUtils.toArray(observed.x, observed.y, observedPixel.a, 0);
					opError.setInput(1, observedPixel);

					if (out != null) {
						residual.wrap(out, row);
						chains.forward(chainSum, pixel, residual);
					} else {
						differentiate();
					}
					index++;
					row += 2;
				}
			}
		}

		/// The pose reaches the residual twice: straight through the projection, and again through
		/// the field the projected pixel was read at. Neither chain knows about the other, so the
		/// two derivatives are found separately and combined here — the bookkeeping a graph does
		/// on its own
		void differentiate() {
			// the sum first, since what it says about the residual is what the other two get
			// multiplied by. Each call runs its own forward pass, so nothing depends on what a
			// previous one left in the shared buffer
			chains.backwardsF(chainSum, pixel);    // d(residual)/d(pixel) and d(residual)/d(shift)
			chains.backwardsF(chainField, pixel);  // d(shift)/d(pixel), and the knot columns
			chains.backwardsF(chainPixel, point);  // d(pixel)/d(pose)

			// the pose moves the residual twice over: straight through the projection, and again
			// through the field the projected pixel was read at
			work.reshape(2, 2);
			math.mult(dResidualDShift, dShiftDPixel, work);
			math.addTo(work, dResidualDPixel);
			math.mult(work, dPixelDPose, poseTotal);

			jacobian.addMatrixToJacobian(row, poseColumn, poseTotal.data, 2, ChainRuleCameraGridScene.POSE_DOF);
		}

		@Override public void setJacobianOut( LeastSquaresSwitcher.JacobianOut jacobian ) {
			this.jacobian = jacobian;
		}

		@Override public int getSchurSplit() {return scene.numParameters();}

		@Override public int getNumOfInputsN() {return scene.numParameters();}

		@Override public int getNumOfOutputsM() {return scene.observations.size()*2;}
	}
}
