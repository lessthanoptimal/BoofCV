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

package boofcv.alg.cloud;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestDisparityToColorPointCloud extends BoofStandardJUnit {

	int width = 500;
	int height = 500;

	/**
	 * Simple test to see if it crashes. Very little validation of results is done
	 */
	@Test
	void doesItCrash() {
		double baseline = 1.0;

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(500.0,500,0,250,250);
		DMatrixRMaj rectifiedR = CommonOps_DDRM.identity(3);
		Point2Transform2_F64 rectifiedToColor = new DoNothing2Transform2_F64();
		int disparityMin = 2;
		int disparityRange = 100;

		var alg = new DisparityToColorPointCloud();

		alg.configure(baseline,K,rectifiedR,rectifiedToColor,disparityMin,disparityRange);

		var disparity = new GrayF32(width,height);
		var color = new DisparityToColorPointCloud.ColorImage() {
			@Override
			public boolean isInBounds(int x, int y) {
				return true;
			}
			@Override
			public int getRGB(int x, int y) {
				return 0xFF00FF;
			}
		};

		var output = new DogArray<>(Point3dRgbI_F64::new);
		alg.process(disparity,color,PointCloudWriter.wrapF64RGB(output));

		assertEquals(width*height,output.size);
		for (int i = 0; i < output.size; i++) {
			assertEquals(0xFF00FF, output.get(i).rgb);
		}
	}


}
