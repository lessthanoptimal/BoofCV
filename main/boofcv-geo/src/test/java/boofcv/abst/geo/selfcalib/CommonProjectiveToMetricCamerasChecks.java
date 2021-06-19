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

package boofcv.abst.geo.selfcalib;

import boofcv.BoofTesting;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.selfcalib.CommonThreeViewSelfCalibration;
import boofcv.alg.geo.selfcalib.ResolveSignAmbiguityPositiveDepth;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTuple;
import boofcv.struct.geo.AssociatedTupleN;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
abstract class CommonProjectiveToMetricCamerasChecks extends CommonThreeViewSelfCalibration {
	// assumed image width and height
	protected int imageWidth = 800;
	protected int imageHeight = 600;

	// tolerance for focal length
	protected double focusTol = 20;
	protected double skewTol = 0.01;
	protected double princpleTol = 1.5;

	protected double rotationTol = 1e-2;
	protected double translationTol = 1e-3;

	// Amount of noise applied in noisy3_three_cameras
	protected double noiseSigma = 0.2;

	protected boolean zeroPrinciplePoint = true;

	public abstract ProjectiveToMetricCameras createEstimator( boolean singleCamera );

	@Override
	protected void standardScene() {
		super.standardScene();
		if (!zeroPrinciplePoint)
			return;

		cameraA = cameraB = cameraC = new CameraPinhole(600, 600, 0, 0, 0, imageWidth, imageHeight);
	}

	/**
	 * 3 views with one camera model for all. Perfect observations.
	 */
	@Test void perfect3_one_unique_camera() {
		standardScene();
		simulateScene(0);
		checkScene(false);
	}

	/**
	 * 3 views with three camera model for all. Perfect observations.
	 */
	@Test void perfect3_three_cameras() {
		standardScene();
		cameraA = new CameraPinhole(600, 600, 0, 0, 0, imageWidth, imageHeight);
		cameraB = new CameraPinhole(800, 800, 0, 0, 0, imageWidth, imageHeight);
		cameraC = new CameraPinhole(350, 350, 0, 0, 0, imageWidth, imageHeight);
		simulateScene(0);

		checkScene(false);
	}

	/**
	 * @param singleCamera if true then the algorithm will be told all views come from one camera
	 */
	private void checkScene( boolean singleCamera ) {
		ProjectiveToMetricCameras alg = createEstimator(singleCamera);
		assertTrue(alg.getMinimumViews() >= 1);
		assertTrue(alg.getMinimumViews() <= 3);

		List<ElevateViewInfo> views = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			int cameraIdx = singleCamera ? 0 : i;
			views.add(new ElevateViewInfo(imageWidth, imageHeight, cameraIdx));
		}

		List<DMatrixRMaj> inputCameras = new ArrayList<>();
		inputCameras.add(P2);
		inputCameras.add(P3);

		MetricCameras results = new MetricCameras();

		// Compute the model
		assertTrue(alg.process(views, inputCameras, observationsN, results));

		assertEquals(2, results.motion_1_to_k.size);
		assertEquals(3, results.intrinsics.size);

		// Check results
		checkEquals(cameraA, results.intrinsics.get(0));
		checkEquals(cameraB, results.intrinsics.get(1));
		checkEquals(cameraC, results.intrinsics.get(2));

		BoofTesting.assertEqualsToScaleS(truthView_1_to_i(1), results.motion_1_to_k.get(0), rotationTol, translationTol);
		BoofTesting.assertEqualsToScaleS(truthView_1_to_i(2), results.motion_1_to_k.get(1), rotationTol, translationTol);

		// The largest translation should be close to 1.0
		double largestTranslation = 0;
		for (Se3_F64 m : results.motion_1_to_k.toList()) {
			largestTranslation = Math.max(largestTranslation, m.T.norm());
		}
		assertEquals(1.0, largestTranslation, 0.001);
	}

	/**
	 * 3 views with three camera model for all. Slightly noisy.
	 */
	@Test void noisy3_three_cameras() {
		standardScene();
		cameraA = new CameraPinhole(600, 600, 0, 0, 0, imageWidth, imageHeight);
		cameraB = new CameraPinhole(800, 800, 0, 0, 0, imageWidth, imageHeight);
		cameraC = new CameraPinhole(350, 350, 0, 0, 0, imageWidth, imageHeight);
		simulateScene(noiseSigma);

		translationTol = 0.1;
		rotationTol = 0.1;
		skewTol = skewTol*10;
		focusTol = 30;
		checkScene(false);
	}

	@Test void noisy_one_camera_three_views() {
		standardScene();
		cameraA = new CameraPinhole(600, 600, 0, 0, 0, imageWidth, imageHeight);
		cameraB = cameraA;
		cameraC = cameraA;
		simulateScene(noiseSigma);

		translationTol = 0.1;
		rotationTol = 0.1;
		skewTol = skewTol*10;
		focusTol = 30;
		checkScene(true);
	}

	/**
	 * In this situation a scene was created where points appeared behind the camera. Taken from real data
	 */
	@Test void real_world_case0() {
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4, true,
				71.2714309, -1.50598476, -354.50553, -.052935998,
				-1.28683386, 39.1891727, 672.658283, -.994592935,
				.00056663, -.019338274, 70.0397946, -.000445996);

		DMatrixRMaj P3 = new DMatrixRMaj(3, 4, true,
				32.4647875, -1.02054892, -241.805355, -.054715714,
				-1.8370892, -.061992654, .486096194, -1.00684043,
				.000185405, -.010046842, 31.8668685, -.000209807);

		DogArray<AssociatedTuple> observations = new DogArray<>(() -> new AssociatedTupleN(3));

		// These are in front of both cameras
		add(-47.208221435546875, -14.024078369140625, -49.9302978515625, 36.35797119140625, -50.079071044921875, 77.59286499023438, observations);
		add(-203.9057159423828, 70.39932250976562, -207.64544677734375, 124.38552856445312, -206.31866455078125, 172.38186645507812, observations);
		add(-362.7781524658203, -218.54442596435547, -361.6542053222656, -160.6702880859375, -363.30285263061523, -107.35969543457031, observations);
		add(-154.99310302734375, 3.35784912109375, -158.14512634277344, 55.362579345703125, -157.7862548828125, 100.77597045898438, observations);
		add(-170.89407348632812, -181.27266693115234, -172.10398864746094, -127.54672241210938, -174.48524475097656, -81.65957641601562, observations);
		add(41.3905029296875, 170.15188598632812, 39.365081787109375, 221.3468017578125, 43.634307861328125, 261.2353515625, observations);
		add(-350.1354789733887, -229.5992660522461, -349.162899017334, -171.76145935058594, -351.1237335205078, -118.83564758300781, observations);
		add(-50.12109375, -14.451873779296875, -52.87139892578125, 35.835052490234375, -53.014801025390625, 77.25506591796875, observations);
		add(-250.23069763183594, -212.5504379272461, -250.5589599609375, -156.41912841796875, -252.87100219726562, -107.27978515625, observations);

		// These are behind at least one camera
		add(154.89532470703125, -21.821807861328125, 151.21435546875, 41.2327880859375, 151.974365234375, 93.64697265625, observations);
		add(226.85003662109375, -95.77021789550781, 221.5345458984375, -35.9564208984375, 219.90155029296875, 12.154052734375, observations);
		add(237.870361328125, -46.12437438964844, 232.88519287109375, 13.570709228515625, 232.98577880859375, 61.028564453125, observations);
		add(162.7314453125, -165.1600341796875, 156.9556884765625, -99.56578063964844, 154.2447509765625, -45.94012451171875, observations);
		add(283.9959716796875, -147.1155242919922, 276.13848876953125, -86.35987854003906, 273.4132080078125, -40.23883056640625, observations);
		add(135.57574462890625, -232.8561019897461, 129.67437744140625, -163.39407348632812, 125.60736083984375, -107.20663452148438, observations);
		add(-21.8720703125, -162.5299530029297, -24.70025634765625, -101.63801574707031, -27.263427734375, -50.05320739746094, observations);
		add(62.40008544921875, -173.78022003173828, 59.92376708984375, -105.06491088867188, 56.91351318359375, -45.15827941894531, observations);
		add(-63.860626220703125, -259.0756492614746, -65.89141845703125, -195.2255096435547, -69.55535888671875, -142.1841278076172, observations);

		List<ElevateViewInfo> views = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			views.add(new ElevateViewInfo(800, 600, i));
		}
		List<DMatrixRMaj> inputCameras = new ArrayList<>();
		inputCameras.add(P2);
		inputCameras.add(P3);

		var results = new MetricCameras();

		ProjectiveToMetricCameras alg = createEstimator(false);
		assertTrue(alg.process(views, inputCameras, observations.toList(), results));

		// Yes internally most implementations run this function, but the number of invalid was > 0 before
		var checkMatches = new ResolveSignAmbiguityPositiveDepth();
		checkMatches.process(observations.toList(), results);
		assertFalse(checkMatches.signChanged);
		assertEquals(0, checkMatches.bestInvalid);
	}

	void add( double x1, double y1, double x2, double y2, double x3, double y3, DogArray<AssociatedTuple> observations ) {
		AssociatedTuple a = observations.grow();
		a.set(0, x1, y1);
		a.set(1, x2, y2);
		a.set(2, x3, y3);
	}

	/**
	 * The implicit camera was added. it should fail
	 */
	@Test void unexpected_number_of_cameras() {
		standardScene();
		simulateScene(0);

		List<ElevateViewInfo> views = new ArrayList<>();
		List<DMatrixRMaj> inputCameras = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			views.add(new ElevateViewInfo(imageWidth, imageHeight, i));
		}
		inputCameras.add(P2); // extra camera here
		inputCameras.add(P2);
		inputCameras.add(P3);

		ProjectiveToMetricCameras alg = createEstimator(false);
		assertThrows(RuntimeException.class, () ->
				alg.process(views, inputCameras, observationsN, new MetricCameras()));
	}

	/**
	 * Incorrect number of input dimensions
	 */
	@Test void unexpected_number_of_dimensions() {
		standardScene();
		simulateScene(0);

		List<ElevateViewInfo> views = new ArrayList<>();
		List<DMatrixRMaj> inputCameras = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			views.add(new ElevateViewInfo(imageWidth, imageHeight, i));
		}
		inputCameras.add(P2);
		inputCameras.add(P3);

		ProjectiveToMetricCameras alg = createEstimator(false);
		assertThrows(RuntimeException.class, () ->
				alg.process(views, inputCameras, observationsN, new MetricCameras()));
	}

	public void checkEquals( CameraPinhole expected, CameraPinhole found ) {
		assertEquals(expected.fx, found.fx, focusTol);
		assertEquals(expected.fy, found.fy, focusTol);
		assertEquals(expected.skew, found.skew, skewTol);
		assertEquals(expected.cx, found.cx, princpleTol);
		assertEquals(expected.cy, found.cy, princpleTol);
	}
}
