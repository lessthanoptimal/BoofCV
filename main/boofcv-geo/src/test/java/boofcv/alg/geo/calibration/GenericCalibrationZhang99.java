/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.distort.SphereToNarrowPixel_F64;
import boofcv.alg.geo.calibration.cameras.Zhang99Camera;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class GenericCalibrationZhang99<CM extends CameraModel> extends BoofStandardJUnit {

	protected double reprojectionTol = 1e-3;

	/**
	 * Create a set of observations from a known grid, give it the observations and see if it can
	 * reconstruct the known parameters.
	 */
	@Test void fullTest() {
		fullTest(false);
		fullTest(true);
	}

	void fullTest( boolean partial ) {
		for (CameraConfig config : createCamera(rand)) {
			CalibInputs inputs = createInputs(config.model, 3, rand);

			// remove points for partial visibility of a target
			if (partial) {
				for (int i = 0; i < inputs.observations.size(); i++) {
					CalibrationObservation o = inputs.observations.get(i);
					for (int j = 0; j < 5; j++) {
						o.points.remove(rand.nextInt(o.points.size()));
					}
				}
			}

			Zhang99Camera zhangCamera = createGenerator(config, inputs.layout);

			CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(
					inputs.layout, zhangCamera);

			// estimate camera parameters using full non-linear methods
			alg.process(inputs.observations);

			// verify results using errors
			List<ImageResults> errors = alg.computeErrors();
			for (int i = 0; i < errors.size(); i++) {
				assertEquals(0, errors.get(i).meanError, reprojectionTol);
			}
		}
	}

	/**
	 * See how well it computes an initial guess at the parameters given perfect inputs
	 */
	@Test public void linearEstimate() {
		for (CameraConfig config : createCameraForLinearTests(rand)) {
			CalibInputs inputs = createInputs(config.model, 3, rand);
			Zhang99Camera zhangCamera = createGenerator(config, inputs.layout);

			var alg = new CalibrationPlanarGridZhang99(inputs.layout, zhangCamera);
			alg.setZeroSkew(true);

			alg.linearEstimate(inputs.observations);

			SceneStructureMetric structure = alg.getStructure();

			CM found = (CM)zhangCamera.getCameraModel(structure.getCameras().get(0).model);

			checkIntrinsicOnly(config.model, found, 0.01, 0.1, 0.1);
		}
	}

	/**
	 * Creates a camera model from the specified configuration
	 */
	public abstract Zhang99Camera createGenerator( CameraConfig config, List<Point2D_F64> layout );

	/**
	 * Extracts a pinhole camera model from the configuration
	 */
	public abstract DMatrixRMaj cameraToK( CameraConfig config );

	public class CameraConfig {
		public CM model;
	}

	/**
	 * Test nonlinear optimization with perfect inputs
	 */
	@Test void optimizedParam_perfect() {
		optimizedParam_noisy(0.0);
	}

	/**
	 * Test nonlinear optimization with a bit of noise
	 */
	@Test void optimizedParam_noisy() {
		optimizedParam_noisy(50);
	}

	void optimizedParam_noisy( double noiseMagnitude) {
		for (CameraConfig config : createCamera(rand)) {
			CalibInputs inputs = createInputs(config.model, 3, rand);

			Zhang99Camera zhangCamera = createGenerator(config, inputs.layout);

			// Add noise to the initial pinhole camera estimate
			DMatrixRMaj K = cameraToK(config);

			K.data[0] += (rand.nextDouble()-0.5)*noiseMagnitude;
			K.data[4] += (rand.nextDouble()-0.5)*noiseMagnitude;

			var alg = new CalibrationPlanarGridZhang99(inputs.layout, zhangCamera);
			alg.convertIntoBundleStructure(inputs.worldToViews, K, inputs.homographies, inputs.observations);
			assertTrue(alg.performBundleAdjustment());

			// verify results using errors
			List<ImageResults> errors = alg.computeErrors();
			for (int i = 0; i < errors.size(); i++) {
				assertEquals(0, errors.get(i).meanError, 0.01);
			}
		}
	}

	public static class CalibInputs {
		List<Point2D_F64> layout = new ArrayList<>();
		List<DMatrixRMaj> homographies = new ArrayList<>();
		List<Se3_F64> worldToViews = new ArrayList<>();
		List<CalibrationObservation> observations = new ArrayList<>();
	}

	public static CalibInputs createInputs( CameraModel camera, int numViews, Random rand ) {
		CalibInputs ret = new CalibInputs();

		// project to distorted pixels. Support all wide and narrow FOV cameras
		Point3Transform2_F64 p2p;
		try {
			p2p = LensDistortionFactory.wide(camera).distortStoP_F64();
		} catch (IllegalArgumentException e) {
			Point2Transform2_F64 n2p = LensDistortionFactory.narrow(camera).distort_F64(false, true);
			p2p = new SphereToNarrowPixel_F64(n2p);
		}

		// 3D point in world and view reference frames
		var worldP = new Point3D_F64();
		var viewP = new Point3D_F64();

		ret.layout = GenericCalibrationGrid.standardLayout();

		var computeHomography = new Zhang99ComputeTargetHomography(ret.layout);

		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			// randomly generate a view location
			double rotX = (rand.nextDouble() - 0.5)*UtilAngle.radian(30);
			double rotY = (rand.nextDouble() - 0.5)*UtilAngle.radian(30);
			double rotZ = (rand.nextDouble() - 0.5)*UtilAngle.radian(180);

			double x = rand.nextGaussian()*5;
			double y = rand.nextGaussian()*5;
			double z = rand.nextGaussian()*5 + 300;

			Se3_F64 worldToView = SpecialEuclideanOps_F64.eulerXyz(x, y, z, rotX, rotY, rotZ, null);

			var obs = new CalibrationObservation(camera.width, camera.height);
			ret.worldToViews.add(worldToView);
			ret.observations.add(obs);

			// render pixel observations
			for (int i = 0; i < ret.layout.size(); i++) {
				worldP.setTo(ret.layout.get(i).x, ret.layout.get(i).y, 0);
				worldToView.transform(worldP, viewP);
				PointIndex2D_F64 pixel = new PointIndex2D_F64();

				p2p.compute(viewP.x, viewP.y, viewP.z, pixel.p);
				pixel.index = i;
				obs.points.add(pixel);
			}

			assertTrue(computeHomography.computeHomography(obs));
			ret.homographies.add(computeHomography.getHomography());
		}

		return ret;
	}

	/**
	 * Standard testing parameters. Should be solvable with non-linear refinement.
	 */
	public abstract List<CameraConfig> createCamera( Random rand );

	/**
	 * These parameters are intended to be easy for the linear estimator to estimate.
	 */
	public abstract List<CameraConfig> createCameraForLinearTests( Random rand );

	@Test void applyDistortion() {
		Point2D_F64 n = new Point2D_F64(0.05, -0.1);
		double[] radial = new double[]{0.1};
		double t1 = 0.034, t2 = 0.34;

		Point2D_F64 distorted = new Point2D_F64();

		double r2 = n.x*n.x + n.y*n.y;
		distorted.x = n.x + radial[0]*r2*n.x + 2*t1*n.x*n.y + t2*(r2 + 2*n.x*n.x);
		distorted.y = n.y + radial[0]*r2*n.y + t1*(r2 + 2*n.y*n.y) + 2*t2*n.x*n.y;

		CalibrationPlanarGridZhang99.applyDistortion(n, radial, t1, t2);

		assertEquals(distorted.x, n.x, 1e-8);
		assertEquals(distorted.y, n.y, 1e-8);
	}

	protected abstract void checkIntrinsicOnly( CM expected,
												CM found,
												double tolK, double tolD, double tolT );
}
