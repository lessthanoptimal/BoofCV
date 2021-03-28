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

import boofcv.abst.disparity.StereoDisparity;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.misc.LookUpImages;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.junit.jupiter.api.Test;

import static boofcv.misc.BoofMiscOps.uniform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestSparseSceneToDenseCloud extends BoofStandardJUnit {

	int width = 50;
	int height = 40;

	/**
	 * Very basic test which mostly checks to see if it blows up. A dummy class replaces stereo to ensure it runs fast
	 * and the number of times its called is recorded as a sanity check.
	 */
	@Test void simpleScenario() {
		DummyLookUp lookup = new DummyLookUp();
		DummyStereo stereo = new DummyStereo();

		SparseSceneToDenseCloud<GrayU8> alg = FactorySceneReconstruction.sparseSceneToDenseCloud(null, ImageType.SB_U8);
		// Reduce tolerances to ensure that nothing is pruned
		alg.getGenerateGraph().minimumCommonFeaturesFrac = 0.1;
//		alg.getGenerateGraph().setVerbose(System.out, null);
		alg.getMultiViewStereo().minimumQuality3D = 0.01;
		alg.getMultiViewStereo().maximumCenterOverlap = 1.0; // default value is too aggressive
		alg.getMultiViewStereo().setStereoDisparity(stereo);
//		alg.getMultiViewStereo().setVerbose(System.out, null);

		var scene = new SceneStructureMetric(false);
		TIntObjectMap<String> viewIdx_to_imageID = new TIntObjectHashMap<>();

		createSparseScene(scene, viewIdx_to_imageID);

		// Invoke the code being tested
		alg.process(scene, viewIdx_to_imageID, lookup);

		// Each view should be used in a stereo pair at least once
		assertTrue(stereo.total >= 7);

		// should be a significant number of points in the cloud. The test number is arbitrary.
		assertTrue(alg.getCloud().size() > 100);

		// these two should be the same, although the colors won't be interesting
		assertEquals(alg.getCloud().size(), alg.getColorRgb().size);
	}

	/**
	 * Creates a scene where not every image will be connected. The images move in a line along the x-axis
	 */
	private void createSparseScene( SceneStructureMetric scene, TIntObjectMap<String> viewIdx_to_imageID ) {
		int pointsPerView = 10;
		scene.initialize(8, 8, 8*pointsPerView);
		for (int i = 0; i < 8; i++) {
			viewIdx_to_imageID.put(i, "" + (i + 1));
			scene.setView(i, i, true, new Se3_F64());
			scene.setCamera(i, true, new CameraPinhole(200, 200, 0, 150, 150, 300, 300));
			scene.motions.get(i).motion.T.x = -i;
		}

		// Create a set of points that are visible and overlapping with other neighboring views
		for (int imageIdx0 = 0; imageIdx0 < 8; imageIdx0++) {
			int imageIdx1 = Math.min(8, imageIdx0 + 2);

			double x = imageIdx0;
			for (int i = 0; i < pointsPerView; i++) {
				int pointIdx = imageIdx0*pointsPerView + i;
				scene.setPoint(pointIdx, x + uniform(-.1, .1, rand), uniform(-.1, .1, rand), uniform(2, 3, rand));
				SceneStructureCommon.Point p = scene.points.get(pointIdx);
				for (int j = imageIdx0; j < imageIdx1; j++) {
					p.views.add(j);
				}
			}
		}
	}

	public class DummyStereo implements StereoDisparity<GrayU8, GrayF32> {
		int total = 0;

		@Override public void process( GrayU8 imageLeft, GrayU8 imageRight ) {total++;}

		@Override public GrayF32 getDisparity() {
			var image = new GrayF32(width, height);
			GImageMiscOps.fill(image, 10);
			return image;
		}

		// @formatter:off
		@Override public int getDisparityMin() {return 5;}
		@Override public int getDisparityRange() {return 100;}
		@Override public int getInvalidValue() {return 10;}
		@Override public int getBorderX() {return 0;}
		@Override public int getBorderY() {return 0;}
		@Override public ImageType<GrayU8> getInputType() {return ImageType.SB_U8;}
		@Override public Class<GrayF32> getDisparityType() {return GrayF32.class;}
		// @formatter:on
	}

	public class DummyLookUp implements LookUpImages {
		@Override public boolean loadShape( String name, ImageDimension shape ) {
			shape.setTo(width, height);
			return true;
		}

		@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
			if (output instanceof Planar)
				((Planar)output).reshape(width, height, 3);
			else
				output.reshape(width, height);
			return true;
		}
	}
}
