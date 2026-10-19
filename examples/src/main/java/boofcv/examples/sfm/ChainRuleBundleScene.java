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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.EvaluateBundleScene;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSnavely;
import boofcv.chainrule.CROperatorSE3;
import boofcv.chainrule.Rodrigues3Numerics;
import boofcv.chainrule.RotationNumerics;
import boofcv.io.UtilIO;
import boofcv.io.geo.CodecBundleAdjustmentInTheLarge;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.optimization.LeastSquaresSwitcher;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.io.File;
import java.io.IOException;

/// The "Bundle Adjustment in the Large" problem used by [ExampleBundleAdjustment], with its parameters laid
/// out for a chain rule Jacobian. Used by [ExampleChainRuleBundleAdjustmentSym] and
/// [ExampleChainRuleBundleAdjustmentGraph].
///
/// Parameters are every 3D point (X, Y, Z), then every view's pose (T, Rodrigues), then every camera
/// (f, k1, k2). Points come first because the Schur complement eliminates them.
public class ChainRuleBundleScene {
	/// Rotations are encoded as a Rodrigues vector, as in the data set
	public static final RotationNumerics ROTATION = new Rodrigues3Numerics();
	public static final int POINT_DOF = 3;
	public static final int POSE_DOF = 6;
	public static final int CAMERA_DOF = 3;

	public final SceneStructureMetric scene;
	public final SceneObservations observations;

	public ChainRuleBundleScene() throws IOException {
		var parser = new CodecBundleAdjustmentInTheLarge();
		parser.parse(new File(UtilIO.pathExample("sfm/problem-16-22106-pre.txt")));
		scene = parser.scene;
		observations = parser.observations;
	}

	public int pointColumn( int point ) {return point*POINT_DOF;}

	public int poseColumn( int motion ) {return scene.points.size*POINT_DOF + motion*POSE_DOF;}

	public int cameraColumn( int camera ) {return poseColumn(scene.motions.size) + camera*CAMERA_DOF;}

	public int numParameters() {return cameraColumn(scene.cameras.size);}

	/// Copies the scene into a parameter vector
	public double[] encode() {
		var param = new double[numParameters()];
		for (int i = 0; i < scene.points.size; i++) {
			System.arraycopy(scene.points.get(i).coordinate, 0, param, pointColumn(i), POINT_DOF);
		}
		var se3 = new CROperatorSE3(false, ROTATION);
		for (int i = 0; i < scene.motions.size; i++) {
			se3.encode(scene.motions.get(i).parent_to_view, param, poseColumn(i));
		}
		for (int i = 0; i < scene.cameras.size; i++) {
			((BundlePinholeSnavely)scene.cameras.get(i).model).getIntrinsic(param, cameraColumn(i));
		}
		return param;
	}

	/// Copies a parameter vector back into the scene
	public void decode( double[] param ) {
		for (int i = 0; i < scene.points.size; i++) {
			System.arraycopy(param, pointColumn(i), scene.points.get(i).coordinate, 0, POINT_DOF);
		}
		for (int i = 0; i < scene.motions.size; i++) {
			var viewFromParent = scene.motions.get(i).parent_to_view;
			int col = poseColumn(i);
			viewFromParent.T.setTo(param[col], param[col + 1], param[col + 2]);
			ROTATION.set(param, col + 3);
			ROTATION.toMatrix(viewFromParent.R);
		}
		for (int i = 0; i < scene.cameras.size; i++) {
			((BundlePinholeSnavely)scene.cameras.get(i).model).setIntrinsic(param, cameraColumn(i));
		}
	}

	/// Visits every observation, in the order its residuals are stored
	public void forEachObservation( ObservationVisitor visitor ) {
		var pixel = new Point2D_F64();
		int row = 0;
		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			SceneStructureMetric.View view = scene.views.get(viewIdx);
			SceneObservations.View obs = observations.views.get(viewIdx);
			for (int i = 0; i < obs.size(); i++) {
				obs.getPixel(i, pixel);
				visitor.visit(row, obs.getPointId(i), view.parent_to_view, view.camera, pixel);
				row += 2;
			}
		}
	}

	public int numResiduals() {return observations.getObservationCount()*2;}

	/// Optimizes the scene with the same solver settings as [ExampleBundleAdjustment], then reports how well it fit
	public <F extends FunctionNtoM & LeastSquaresSwitcher.ProcessJacobianOut> void optimize( F function ) {
		System.out.println("Optimizing " + numParameters() + " parameters with " +
				observations.getObservationCount() + " observations\n");

		var config = new ConfigNonLinearLeastSquares();
		config.type = ConfigNonLinearLeastSquares.Type.LEVENBERG_MARQUARDT;
		config.lm.dampeningInitial = 1e-3;
		config.lm.hessianScaling = false;

		var minimizer = new LeastSquaresSwitcher<>();
		// sparse, with the points eliminated through a Schur complement
		minimizer.setSolver(config, true, true);
		minimizer.setFunction(function, function);

		var evaluator = new EvaluateBundleScene();
		evaluator.addInliers(1.0, 2.0, 3.0, 5.0, 10.0, 20.0);
		evaluator.evaluate(scene, observations);
		evaluator.printSummary(System.out);
		System.out.println();

		var solver = minimizer.getSolver();
		solver.setVerbose(System.out, 0);
		long startTime = System.currentTimeMillis();
		solver.initialize(encode(), 1e-6, 1e-6);
		double errorBefore = solver.getFunctionValue();
		for (int i = 0; i < 50 && !solver.iterate(); i++) {}
		decode(solver.getParameters());

		evaluator.evaluate(scene, observations);
		evaluator.printSummary(System.out);
		System.out.println();
		System.out.printf("Error reduced by %.1f%%\n", (100.0*(errorBefore/solver.getFunctionValue() - 1.0)));
		System.out.println(BoofMiscOps.milliToHuman(System.currentTimeMillis() - startTime));
	}

	@FunctionalInterface
	public interface ObservationVisitor {
		/// @param row First of the observation's two residual rows
		void visit( int row, int point, int motion, int camera, Point2D_F64 pixel );
	}
}
