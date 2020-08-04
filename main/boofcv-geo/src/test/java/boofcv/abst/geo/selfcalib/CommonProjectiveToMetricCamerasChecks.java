/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.selfcalib.CommonThreeViewSelfCalibration;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofTesting;
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

	protected boolean zeroPrinciplePoint = true;

	public abstract ProjectiveToMetricCameras createEstimator();

	@Override
	protected void standardScene() {
		super.standardScene();
		if( !zeroPrinciplePoint )
			return;

		cameraA = cameraB = cameraC = new CameraPinhole(600,600,0,0,0,imageWidth,imageHeight);
	}

	/**
	 * 3 views with one camera model for all. Perfect observations.
	 */
	@Test
	void perfect3_one_unique_camera() {
		standardScene();
		simulateScene(0);
		checkScene();
	}

	/**
	 * 3 views with three camera model for all. Perfect observations.
	 */
	@Test
	void perfect3_three_cameras() {
		standardScene();
		cameraA = new CameraPinhole(600,600,0,0,0,imageWidth,imageHeight);
		cameraB = new CameraPinhole(800,800,0,0,0,imageWidth,imageHeight);
		cameraC = new CameraPinhole(350,350,0,0,0,imageWidth,imageHeight);
		simulateScene(0);

		checkScene();
	}

	private void checkScene() {
		ProjectiveToMetricCameras alg = createEstimator();
		assertTrue(alg.getMinimumViews()>=1);
		assertTrue(alg.getMinimumViews()<=3);

		List<ImageDimension> dimensions = new ArrayList<>();
		List<DMatrixRMaj> inputCameras = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			dimensions.add( new ImageDimension(imageWidth,imageHeight));
		}
		inputCameras.add(P2);
		inputCameras.add(P3);

		MetricCameras results = new MetricCameras();

		// Compute the model
		assertTrue(alg.process(dimensions,inputCameras,observationsN,results) );

		assertEquals(2,results.motion_1_to_k.size);
		assertEquals(3,results.intrinsics.size);

		// Check results
		checkEquals(cameraA,results.intrinsics.get(0));
		checkEquals(cameraB,results.intrinsics.get(1));
		checkEquals(cameraC,results.intrinsics.get(2));

		BoofTesting.assertEqualsToScaleS(truthView_1_to_i(1), results.motion_1_to_k.get(0), rotationTol, translationTol);
		BoofTesting.assertEqualsToScaleS(truthView_1_to_i(2), results.motion_1_to_k.get(1), rotationTol, translationTol);
	}

	/**
	 * 3 views with three camera model for all. Slightly noisy.
	 */
	@Test
	void noisy3_three_cameras() {
		standardScene();
		cameraA = new CameraPinhole(600,600,0,0,0,imageWidth,imageHeight);
		cameraB = new CameraPinhole(800,800,0,0,0,imageWidth,imageHeight);
		cameraC = new CameraPinhole(350,350,0,0,0,imageWidth,imageHeight);
		simulateScene(0.2);

		translationTol = 0.1;
		rotationTol = 0.1;
		skewTol = 0.2;
		focusTol = 30;
		checkScene();
	}

	/**
	 * The implicit camera was added. it should fail
	 */
	@Test
	void unexpected_number_of_cameras() {
		standardScene();
		simulateScene(0);

		List<ImageDimension> dimensions = new ArrayList<>();
		List<DMatrixRMaj> inputCameras = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			dimensions.add( new ImageDimension(imageWidth,imageHeight));
		}
		inputCameras.add(P2); // extra camera here
		inputCameras.add(P2);
		inputCameras.add(P3);

		ProjectiveToMetricCameras alg = createEstimator();
		assertThrows(RuntimeException.class, ()->
				alg.process(dimensions,inputCameras,observationsN,new MetricCameras()));
	}

	/**
	 * Incorrect number of input dimensions
	 */
	@Test
	void unexpected_number_of_dimensions() {
		standardScene();
		simulateScene(0);

		List<ImageDimension> dimensions = new ArrayList<>();
		List<DMatrixRMaj> inputCameras = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			dimensions.add( new ImageDimension(imageWidth,imageHeight));
		}
		inputCameras.add(P2);
		inputCameras.add(P3);

		ProjectiveToMetricCameras alg = createEstimator();
		assertThrows(RuntimeException.class, ()->
				alg.process(dimensions,inputCameras,observationsN,new MetricCameras()));
	}

	public void checkEquals(CameraPinhole expected , CameraPinhole found ) {
		assertEquals(expected.fx, found.fx, focusTol);
		assertEquals(expected.fy, found.fy, focusTol);
		assertEquals(expected.skew, found.skew, skewTol);
		assertEquals(expected.cx, found.cx, princpleTol);
		assertEquals(expected.cy, found.cy, princpleTol);
	}
}