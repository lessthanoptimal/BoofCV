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

package boofcv.alg.mvs;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.LookUpImages;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMultiBaselineStereoIndependent extends BoofStandardJUnit {

	boolean visualize = false;

	/**
	 * Wall cameras are parallel to each other and are set up so that when fused they will cover almost the
	 * entire image
	 */
	@Test void simulated_parallel_cameras() {
		Se3_F64 world_to_view1 = eulerXyz(0.8, 0, 0, 0.0, 0, 0, null);
		Se3_F64 world_to_view2 = eulerXyz(-0.8, 0, 0, 0.0, 0, 0, null);
		simulate_constant_disparity(world_to_view1, world_to_view2, 0.99, 0.995);
	}

	/**
	 * The cameras are no longer parallel and if the rectification matrices aren't handled correctly this will fail
	 */
	@Test void simulated_skewed_cameras() {
		Se3_F64 world_to_view1 = eulerXyz(0.4, 0, 0, -0.05, 0, 0, null);
		Se3_F64 world_to_view2 = eulerXyz(-0.4, 0, 0, 0.0, 0.05, 0, null);
		simulate_constant_disparity(world_to_view1, world_to_view2, 0.9, 0.99);
	}

	/**
	 * The plane being viewed and the camera's image plane are parallel causing the disparity to have a constant value
	 * making it easy check for correctness.
	 *
	 * @param tolFilled What fraction of fused image should be filled
	 * @param tolCorrect Out of the filled pixels what fraction need to have the correct disparity
	 */
	void simulate_constant_disparity( Se3_F64 world_to_view1, Se3_F64 world_to_view2,
									  double tolFilled, double tolCorrect ) {
		// Each camera is different.
		List<CameraPinholeBrown> listIntrinsic = new ArrayList<>();
		listIntrinsic.add(new CameraPinholeBrown().fsetK(150, 140, 0, 105, 100, 210, 200).fsetRadial(0.02, -0.003));
		listIntrinsic.add(new CameraPinholeBrown().fsetK(151, 142, 0, 107.5, 102.5, 215, 205).fsetRadial(0.03, -0.001));
		listIntrinsic.add(new CameraPinholeBrown().fsetK(149, 141, 0, 102.5, 107.5, 205, 215).fsetRadial(0.001, 0.003));

		// Create the scene. This will be used as input into MultiViewToFusedDisparity and in the simulator
		var scene = new SceneStructureMetric(true);
		scene.initialize(3, 3, 0);

		scene.setCamera(0, true, listIntrinsic.get(0));
		scene.setCamera(1, true, listIntrinsic.get(1));
		scene.setCamera(2, true, listIntrinsic.get(2));

		// All views are looking head on at the target. The 2nd and third view have been offset to ensure full coverage and that
		// it's incorporating all the views, otherwise there would be large gaps
		scene.setView(0, 0, true, eulerXyz(0, 0, 0, 0.0, 0, 0, null));
		scene.setView(1, 1, true, world_to_view1);
		scene.setView(2, 2, true, world_to_view2);

		var lookup = new MockLookUp();
		var alg = new MultiBaselineStereoIndependent<>(lookup, ImageType.SB_F32);
		// Not mocking disparity because of how complex that would be to pull off. This makes it a bit of an inexact
		// science to ensure fill in
		var configDisp = new ConfigDisparityBMBest5();
		configDisp.errorType = DisparityError.SAD;
		configDisp.texture = 0.05;
		configDisp.disparityMin = 20;
		configDisp.disparityRange = 80;
		alg.stereoDisparity = FactoryStereoDisparity.blockMatchBest5(configDisp, GrayF32.class, GrayF32.class);

		// Textured target that stereo will work well on
		var texture = new GrayF32(100, 100);
		ImageMiscOps.fillUniform(texture, rand, 50, 255);

		SimulatePlanarWorld sim = new SimulatePlanarWorld();
		sim.addSurface(eulerXyz(0, 0, 2, 0, Math.PI, 0, null), 3, texture);

		List<GrayF32> images = new ArrayList<>();
		TIntObjectMap<String> sbaIndexToViewID = new TIntObjectHashMap<>();
		for (int i = 0; i < listIntrinsic.size(); i++) {
			sbaIndexToViewID.put(i, i + "");
			sim.setCamera(listIntrinsic.get(i));
			sim.setWorldToCamera(scene.motions.get(i).motion);
			images.add(sim.render().clone());

			if (visualize)
				ShowImages.showWindow(images.get(images.size() - 1), "Frame " + i);
		}
		lookup.images = images;

		assertTrue(alg.process(scene, 0, DogArray_I32.array(1, 2), sbaIndexToViewID::get));

		GrayF32 found = alg.getFusedDisparity();
		assertEquals(listIntrinsic.get(0).width, found.width);
		assertEquals(listIntrinsic.get(0).height, found.height);

		if (visualize) {
			ShowImages.showWindow(VisualizeImageData.disparity(found, null, 100, 0x00FF00), "Disparity");
			BoofMiscOps.sleep(60_000);
		}

		DisparityParameters param = alg.getFusedParam();

		// Check the results. Since the target fills the view and is a known constant Z away we can that here.
		// however since a real disparity algorithm is being used its inputs will not be perfect
		int totalFilled = 0;
		int totalCorrect = 0;
		for (int y = 0; y < found.height; y++) {
			for (int x = 0; x < found.width; x++) {
				float d = found.get(x, y);
				assertTrue(d >= 0);
				if (d >= param.disparityRange)
					continue;
				double Z = param.baseline*param.pinhole.fx/(d + param.disparityMin);
				if (Math.abs(Z - 2.0) <= 0.1)
					totalCorrect++;
				totalFilled++;
			}
		}

		int N = found.width*found.height;
		assertTrue(N*tolFilled <= totalFilled);
		assertTrue(totalFilled*tolCorrect <= totalCorrect);
	}

	/** In this scene there is only one camera for several views */
	@Test void handleOneCameraManyViews() {
		var scene = new SceneStructureMetric(true);
		scene.initialize(1, 3, 0);
		scene.setCamera(0, true, new CameraPinholeBrown().fsetK(30, 30, 0, 25, 25, 50, 50));
		for (int i = 0; i < 3; i++) {
			scene.setView(i, 0, true, eulerXyz(i, 0, 0, 0, 0, 0, null));
		}

		var alg = new MultiBaselineStereoIndependent<>(ImageType.SB_F32);

		var configDisp = new ConfigDisparityBMBest5();
		configDisp.errorType = DisparityError.SAD;
		configDisp.disparityRange = 5;
		alg.stereoDisparity = FactoryStereoDisparity.blockMatchBest5(configDisp, GrayF32.class, GrayF32.class);
		List<GrayF32> images = new ArrayList<>();
		TIntObjectMap<String> sbaIndexToViewID = new TIntObjectHashMap<>();
		for (int i = 0; i < 3; i++) {
			images.add(new GrayF32(50, 50));
			sbaIndexToViewID.put(i, i + "");
		}
		alg.lookUpImages = new MockLookUp(images);

		// Override so that it will always be happy
		alg.performFusion = new MultiBaselineDisparityMedian() {
			@Override public boolean process( GrayF32 disparity ) {return true;}
		};

		// just see if it blows up
		assertTrue(alg.process(scene, 0, DogArray_I32.array(1, 2), sbaIndexToViewID::get));
	}

	class MockLookUp implements LookUpImages {
		List<GrayF32> images;

		public MockLookUp( List<GrayF32> images ) {this.images = images;}

		public MockLookUp() {}

		@Override public boolean loadShape( String name, ImageDimension shape ) {
			GrayF32 image = images.get(Integer.parseInt(name));
			shape.setTo(image.width, image.height);
			return true;
		}

		@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
			output.setTo((LT)images.get(Integer.parseInt(name)));
			return true;
		}
	}
}
