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

package boofcv.alg.structure;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.factory.structure.ConfigSequenceToSparseScene;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.io.image.LookUpImageListByIndex;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.PointTrackerPerfectCloud;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"SameParameterValue"})
class TestImageSequenceToSparseScene extends BoofStandardJUnit {
	final static int width = 400;
	final static int height = 400;

	/**
	 * Given perfect data does it return correct results?
	 */
	@Test void perfect() {
		var config = new ConfigSequenceToSparseScene();
		// let's make it run faster
		config.pairwise.score.ransacF.iterations = 30;
		config.pairwise.score.typeInliers.ransacH.iterations = 30;
		config.projective.ransac.iterations = 30;

		ImageSequenceToSparseScene<GrayF32> alg = FactorySceneReconstruction.
				sequenceToSparseScene(config, ImageType.SB_F32);

		alg.tracker = new DummyTracker();

//		alg.generatePairwise.setVerbose(System.out, null);
//		alg.metricFromPairwise.setVerbose(System.out, null);
//		alg.metricFromPairwise.getInitProjective().setVerbose(System.out, null);
//		alg.setVerbose(System.out, null);

		// Create the input
		List<GrayF32> frames = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			frames.add(new GrayF32(width, height));
		}

		var lookup = new LookUpImageListByIndex<>(frames);
		List<String> imageIDs = new ArrayList<>();
		for (int i = 0; i < frames.size(); i++) {
			imageIDs.add(i + "");
		}

		// Run it
		assertTrue(alg.process(imageIDs, lookup));

//		System.out.printf("track %.1f\n", alg.getTimeTrackingMS());
//		System.out.printf("Pairwise %.1f\n", alg.getTimePairwiseMS());
//		System.out.printf("Metric %.1f\n", alg.getTimeMetricMS());
//		System.out.printf("Refine %.1f\n", alg.getTimeRefineMS());

		// Minimal sanity check
		SceneStructureMetric scene = alg.getSceneStructure();
		assertEquals(frames.size(), scene.views.size);
		assertEquals(frames.size(), scene.cameras.size);
		assertEquals(frames.size(), scene.motions.size);

		// sanity check the solution
		for (int i = 0; i < scene.cameras.size; i++) {
			assertEquals(150.0, ((BundlePinholeSimplified)scene.cameras.get(i).model).f, 1e-4);
		}
	}

	/** Custom tracker that moves every frame */
	class DummyTracker extends PointTrackerPerfectCloud<GrayF32> {
		public DummyTracker() {
			cloud = UtilPoint3D_F64.random(new Point3D_F64(1.0, 0, 2), -2, 2, -1, 1, -1, 1, 1000, rand);
			setCamera(new CameraPinhole(150, 150, 0, 200, 200,
					TestImageSequenceToSparseScene.width, TestImageSequenceToSparseScene.height));
		}

		@Override public void process( GrayF32 image ) {
			world_to_view.T.x += (frameID + 1)*0.1;

			// need to add these small motions to converge to a correct solution
			world_to_view.T.y = BoofMiscOps.uniform(-0.1, 0.1, rand);
			world_to_view.T.z = BoofMiscOps.uniform(-0.1, 0.1, rand);
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
					rand.nextGaussian()*0.02, rand.nextGaussian()*0.02, rand.nextGaussian()*0.02, world_to_view.R);

			super.process(image);
		}
	}
}
