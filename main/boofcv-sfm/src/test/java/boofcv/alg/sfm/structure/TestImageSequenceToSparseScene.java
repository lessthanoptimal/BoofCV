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

package boofcv.alg.sfm.structure;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.sfm.ConfigSequenceToSparseScene;
import boofcv.factory.sfm.FactorySceneReconstruction;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.LookUpImagesByIndex;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"SameParameterValue", "IntegerDivisionInFloatingPointContext"})
class TestImageSequenceToSparseScene extends BoofStandardJUnit {
	boolean visualize = false;

	/**
	 * This unit test just ensures that exceptions are not thrown.
	 */
	@Test void does_it_blow_up() {
		var config = new ConfigSequenceToSparseScene();
		// let's make it run faster
		config.pairwise.ransacF.iterations = 30;
		config.pairwise.ransacH.iterations = 30;
		config.projective.ransac.iterations = 30;

		ImageSequenceToSparseScene<GrayU8> alg = FactorySceneReconstruction.
				sequenceToSparseScene(config, ImageType.SB_U8);

		// Create the input
		List<GrayF32> frames = renderSceneSequence(6);
		var lookup = new LookUpImagesByIndex<>(frames);
		List<String> imageIDs = new ArrayList<>();
		for (int i = 0; i < frames.size(); i++) {
			imageIDs.add(i+"");
		}

		// Run it
		assertTrue(alg.process(imageIDs, lookup));

//		System.out.printf("track %.1f\n",alg.getTimeTrackingMS());
//		System.out.printf("Pairwise %.1f\n",alg.getTimePairwiseMS());
//		System.out.printf("Metric %.1f\n",alg.getTimeMetricMS());
//		System.out.printf("Refine %.1f\n",alg.getTimeRefineMS());

		// Minimal sanity check
		SceneStructureMetric scene = alg.getSceneStructure();
		assertEquals(frames.size(), scene.views.size);
		assertEquals(frames.size(), scene.cameras.size);
		assertEquals(frames.size(), scene.motions.size);

		fail("Replace with a perfect point tracker and get rid of rendered scene");
	}

	/**
	 * Render an actual 3D scene in an attempt to stop it from aborting early and more fully testing it
	 */
	List<GrayF32> renderSceneSequence(int length) {
		List<GrayF32> images = new ArrayList<>();

		var textureA = new GrayF32(100, 100);
		ImageMiscOps.fillUniform(textureA, rand, 50, 255);

		var textureB = new GrayF32(100, 100);
		ImageMiscOps.fillUniform(textureB, rand, 50, 255);

		SimulatePlanarWorld sim = new SimulatePlanarWorld();
		sim.addSurface(eulerXyz(-2.0, 0, 2.5, 0, Math.PI, 0, null), 3, textureA);
		sim.addSurface(eulerXyz(1.0, 0, 1.5, 0.05, Math.PI, 0, null), 3, textureB);

		int w = 240;
		int h = 200;
		var intrinsic = new CameraPinholeBrown().fsetK(w/3,w/3,0,w/2,h/2, w, h);

		for (int i = 0; i < length; i++) {
			sim.setCamera(intrinsic);
			sim.setWorldToCamera(SpecialEuclideanOps_F64.eulerXyz(i*0.15,0,0,0,0,0,null));
			images.add(sim.render().clone());

			if (visualize)
				ShowImages.showWindow(images.get(images.size() - 1), "Frame " + i);
		}

		if (visualize) {
			BoofMiscOps.sleep(40_000);
		}

		return images;
	}

}