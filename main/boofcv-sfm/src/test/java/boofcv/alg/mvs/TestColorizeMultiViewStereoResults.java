/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.misc.LookUpImages;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestColorizeMultiViewStereoResults extends BoofStandardJUnit {

	int width = 20;
	int height = 15;

	int count = 0;

	/**
	 * Two images contribute to the point cloud. Each image has a different color so that the source of the RGB
	 * value can be easily found.
	 */
	@Test void simple_processMvsCloud() {
		// scene with two views that are identical
		var scene = new SceneStructureMetric(false);
		scene.initialize(1, 2, 0);
		scene.setCamera(0, true, new CameraPinhole(200, 200, 0, width/2, height/2, 0, 0));
		scene.setView(0, 0, true, SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0, 0, 0, null));
		scene.setView(1, 0, true, SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0, 0, 0, null));

		// Create two views
		var mvs = new MultiViewStereoFromKnownSceneStructure<>(new MockLookUp(), ImageType.SB_U8);
		mvs.listCenters.add(new MultiViewStereoFromKnownSceneStructure.ViewInfo());
		mvs.listCenters.get(0).metric = scene.views.get(0);
		mvs.listCenters.get(0).relations = new StereoPairGraph.Vertex();
		mvs.listCenters.get(0).relations.id = "10";
		mvs.listCenters.get(0).relations.indexSba = 0;

		mvs.listCenters.add(new MultiViewStereoFromKnownSceneStructure.ViewInfo());
		mvs.listCenters.get(1).metric = scene.views.get(1);
		mvs.listCenters.get(1).relations = new StereoPairGraph.Vertex();
		mvs.listCenters.get(1).relations.id = "15";
		mvs.listCenters.get(1).relations.indexSba = 1;

		// One point for each view. Both points are in the image center
		mvs.disparityCloud.viewPointIdx.setTo(0, 1, 2);
		mvs.disparityCloud.cloud.grow().setTo(0, 0, 1);
		mvs.disparityCloud.cloud.grow().setTo(0, 0, 1);

		var alg = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.SB_U8(), new MockLookUp());
		alg.processMvsCloud(scene, mvs, ( idx, r, g, b ) -> {
			// we can assume the first view is called first, but that's not strictly required to be correct
			int expected = idx == 0 ? 10 : 15;
			assertEquals(expected, r);
			assertEquals(expected, g);
			assertEquals(expected, b);
			count++;
		});

		// make sure the functions were called
		assertEquals(2, count);
	}

	/**
	 * Two points seen in the two views. Which view is first depends on the point. See if the expected point
	 * has the expected color.
	 */
	@Test void simple_processScenePoints() {
		// scene with two views that are identical
		var scene = new SceneStructureMetric(true);
		scene.initialize(1, 2, 2);
		scene.setCamera(0, true, new CameraPinhole(200, 200, 0, width/2, height/2, 0, 0));
		scene.setView(0, 0, true, SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0, 0, 0, null));
		scene.setView(1, 0, true, SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0, 0, 0, null));

		// only difference between the points are the order of their views
		scene.setPoint(0, 0.01, -0.01, 2.0, 0.99);
		scene.setPoint(1, 0.01, -0.01, 2.0, 0.99);
		scene.points.get(0).views.addAll(DogArray_I32.array(0, 1));
		scene.points.get(1).views.addAll(DogArray_I32.array(1, 0));

		var alg = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.SB_U8(), new MockLookUp());
		alg.processScenePoints(scene, ( idx ) -> (5*idx + 10) + "", ( idx, r, g, b ) -> {
			// we can assume the first view is called first, but that's not strictly required to be correct
			int expected = idx == 0 ? 10 : 15;
			assertEquals(expected, r);
			assertEquals(expected, g);
			assertEquals(expected, b);
			count++;
		});

		assertEquals(2, count);
	}

	class MockLookUp implements LookUpImages {
		@Override public boolean loadShape( String name, ImageDimension shape ) {
			shape.setTo(width, height);
			return true;
		}

		@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
			output.reshape(width, height);
			int value = Integer.parseInt(name);
			GImageMiscOps.fill(output, value);
			return true;
		}
	}
}
