/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.meshing;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PointToPixelTransform_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDepthImageToMeshGridSample extends BoofStandardJUnit {
	CameraPinhole pinhole = new CameraPinhole().fsetK(200, 200, 0, 100, 100, 400, 400);

	/**
	 * Very basic test that checks to see if it can detect the object inside and now blow up
	 */
	@Test void disparitySimpleScene() {
		SimulatePlanarWorld sim = renderPlanarWorld();

		var param = new DisparityParameters();
		param.pinhole.setTo(pinhole);
		param.baseline = 0.3;
		param.disparityMin = 5;
		param.disparityRange = 250;

		GrayF32 depth = sim.getDepthMap();
		GrayF32 disparity = depth.createSameShape();
		depthToDisparityImage(pinhole, param, depth, disparity);

		var alg = new DepthImageToMeshGridSample();
		alg.processDisparity(param, disparity, 2);
		VertexMesh found = alg.getMesh();

		assertTrue(found.vertexes.size() >= 4);
		assertTrue(found.faceVertexes.size() >= 4);
		assertTrue(found.faceOffsets.size() >= 1);
	}

	private void depthToDisparityImage( CameraPinhole pinhole, DisparityParameters param, GrayF32 depth, GrayF32 disparity ) {
		for (int pixY = 0; pixY < depth.height; pixY++) {
			for (int pixX = 0; pixX < depth.width; pixX++) {
				double z = depth.get(pixX, pixY);

				double d;
				if (z == Float.MAX_VALUE) {
					d = param.disparityRange;
				} else {
					// Convert into disparity
					d = (param.baseline*pinhole.fx)/z - param.disparityMin;

					// Make sure it's legal
					if (d < 0) {
						d = param.disparityRange;
					} else if (d > param.disparityRange) {
						d = param.disparityRange;
					}
				}

				disparity.set(pixX, pixY, (float)d);
			}
		}
	}

	@Test void inverseDepthSimpleScene() {
		SimulatePlanarWorld sim = renderPlanarWorld();

		// Convert the depth image into an inverse depth image
		GrayF32 depth = sim.getDepthMap();
		GrayF32 invDepth = depth.createSameShape();
		depth.forEachPixel(( x, y, d ) -> invDepth.set(x, y, 1.0f/d));

		// Convert it into a 3D mesh
		var pixelToNorm = new PointToPixelTransform_F64(new LensDistortionPinhole(pinhole).undistort_F64(true, false));
		var alg = new DepthImageToMeshGridSample();
		alg.processInvDepth(invDepth, pixelToNorm, 1e-3f);
		VertexMesh found = alg.getMesh();

		assertTrue(found.vertexes.size() >= 4);
		assertTrue(found.faceVertexes.size() >= 4);
		assertTrue(found.faceOffsets.size() >= 1);
	}

	@NotNull private SimulatePlanarWorld renderPlanarWorld() {
		// View a planar object that's at an angle. It will look like a trapazoid
		Se3_F64 planeToWorld = SpecialEuclideanOps_F64.eulerXyz(0.2, 0.1, 1, 2.7, 0, 0, null);
		var sim = new SimulatePlanarWorld();
		sim.setCamera(pinhole);
		sim.addSurface(planeToWorld, 1.00, new GrayF32(20, 20));

		sim.render();
		return sim;
	}
}
