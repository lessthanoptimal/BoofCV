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
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.alg.geo.selfcalib.CommonThreeViewSelfCalibration;
import boofcv.alg.geo.selfcalib.MetricCameraTriple;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTriple;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonGenerateMetricCameraTripleChecks extends CommonThreeViewSelfCalibration {

	// Number of randomized trials it will do during testing
	protected int totalTrials = 50;

	// assumed image width and height
	protected int imageWidth = 800;
	protected int imageHeight = 600;

	// tolerance for focal length
	protected double focusTol = 20;
	protected double skewTol = 0.01;
	protected double princpleTol = 1.5;

	protected boolean zeroPrinciplePoint = true;

	protected double minimumFractionSuccess = 1.0;

	private int extraObservations = 0;

	public abstract ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo> createGenerator();

	@Override
	protected void standardScene() {
		super.standardScene();
		if (!zeroPrinciplePoint)
			return;

		cameraA = cameraB = cameraC = new CameraPinhole(600, 600, 0, 0, 0, imageWidth, imageHeight);
	}

	@Test
	void perfect_one_camera_minimum() {
		standardScene();
		simulateScene(0);

		checkScene();
	}

	@Test
	void perfect_three_cameras() {
		standardScene();
		cameraA = new CameraPinhole(600, 600, 0, 0, 0, 800, 600);
		cameraB = new CameraPinhole(800, 800, 0, 0, 0, 800, 600);
		cameraC = new CameraPinhole(350, 350, 0, 0, 0, 800, 600);
		simulateScene(0);

		checkScene();
	}

	private void checkScene() {
		ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo> alg = createGenerator();
		assertEquals(3, alg.getNumberOfViews());
		for (int i = 0; i < 3; i++) {
			alg.setView(i, new ElevateViewInfo(800, 600, i));
		}

		assertTrue(alg.getMinimumPoints() > 0);

		var found = new MetricCameraTriple();
		List<AssociatedTriple> selected = new ArrayList<>();

		DogArray_I32 indexes = DogArray_I32.range(0, numFeatures);

		int countSuccess = 0;
		for (int trial = 0; trial < totalTrials; trial++) {
			// Randomly select different points each trial
			PrimitiveArrays.shuffle(indexes.data, 0, numFeatures, rand);
			selected.clear();
			for (int i = 0; i < alg.getMinimumPoints() + extraObservations; i++) {
				selected.add(observations3.get(indexes.get(i)));
			}

			// Compute the model
			if (!alg.generate(selected, found)) {
				continue;
			}
			countSuccess++;

//			System.out.println("Trial = "+trial);

			// Check results
			checkEquals(cameraA, found.view1);
			checkEquals(cameraB, found.view2);
			checkEquals(cameraC, found.view3);

			BoofTesting.assertEqualsToScaleS(truthView_1_to_i(1), found.view_1_to_2, 1e-2, 1e-3);
			BoofTesting.assertEqualsToScaleS(truthView_1_to_i(2), found.view_1_to_3, 1e-2, 1e-3);
		}
//		System.out.println("Passed "+countSuccess+" / "+totalTrials);
		assertTrue(minimumFractionSuccess*totalTrials <= countSuccess,
				"Failed " + (totalTrials - countSuccess) + " min " + (minimumFractionSuccess*totalTrials - countSuccess));
	}

	@Test
	void perfect_more_than_minimum() {
		standardScene();
		simulateScene(0);
		extraObservations = 30;
		checkScene();
	}

	public void checkEquals( CameraPinhole expected, CameraPinhole found ) {
		assertEquals(expected.fx, found.fx, focusTol);
		assertEquals(expected.fy, found.fy, focusTol);
		assertEquals(expected.skew, found.skew, skewTol);
		assertEquals(expected.cx, found.cx, princpleTol);
		assertEquals(expected.cy, found.cy, princpleTol);
	}
}
