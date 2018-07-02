/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestEstimateSceneUnordered extends GenericSceneStructureChecks {
	@Test
	public void easy_uncalibrated() {
		fail("implement");
	}

	@Test
	public void easy_calibrated() {
		fail("implement");
	}

	@Test
	public void add() {
		fail("implement");
	}

	@Test
	public void convertToOutput() {
		fail("implement");
	}

	@Test
	public void countFeaturesWith3D() {
		fail("implement");
	}

	@Test
	public void determinePose() {
		fail("implement");
	}

	@Test
	public void triangulateNoLocation() {
		fail("implement");
	}

	@Test
	public void defineCoordinateSystem() {
		fail("implement");
	}

	@Test
	public void selectOriginNode() {
		fail("implement");
	}

	@Test
	public void selectCoordinateBase() {
		fail("implement");
	}

	@Test
	public void triangulateInitialSeed() {
		fail("implement");
	}

	@Test
	public void connectViews_calibrated()
	{
		createWorld(2,3);


		fail("implement");
	}

	@Test
	public void fitEpipolar() {
		createWorld(2,3);

		List<Point3D_F64> worldPoints = new ArrayList<>();
		findViewable(new int[]{0,1},worldPoints);
		Se3_F64 camera_a_to_b = cameraAtoB(0,1);

		List<Point2D_F64> pointsA = new ArrayList<>();
		List<Point2D_F64> pointsB = new ArrayList<>();
		renderObservations(0,false,worldPoints,pointsA);
		renderObservations(1,false,worldPoints,pointsB);

		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class,true);
		for (int i = 0; i < pointsA.size(); i++) {
			matches.grow().setAssociation(i,i,0);
		}

		EstimateSceneUnordered<?> alg = new EstimateSceneUnordered<>();
		alg.calibrated = true;
		alg.declareModelFitting();

		EstimateSceneUnordered.CameraMotion edge = new EstimateSceneUnordered.CameraMotion();
		alg.fitEpipolar(matches,pointsA,pointsB,alg.ransacEssential,edge);

		assertTrue(edge.features.size() >= matches.size*0.95 );
		assertFalse(matches.contains(edge.features.get(0))); // it should be a copy and not have the same instance

		Se3_F64 found_a_to_b = alg.ransacEssential.getModelParameters();

		camera_a_to_b.T.normalize();
		found_a_to_b.T.normalize();

		assertTrue( camera_a_to_b.T.distance(found_a_to_b.T) < 1e-4 );
		assertTrue(MatrixFeatures_DDRM.isIdentical(camera_a_to_b.R,found_a_to_b.R,1e-3));
	}

	@Test
	public void reset() {
		fail("implement");
	}
}