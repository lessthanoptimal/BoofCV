/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.overhead;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCreateSyntheticOverheadViewS {

	int width = 800;
	int height = 850;
	CameraPinholeRadial param = new CameraPinholeRadial(200,201,0,width/2,height/2,width,height).fsetRadial(0.002,0);

	int overheadW = 500;
	int overheadH = 600;
	double cellSize = 0.05;
	double centerX = 1;
	double centerY = overheadH*cellSize/2.0;

	@Test
	public void checkRender() {
		// Easier to make up a plane in this direction
		Se3_F64 cameraToPlane = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,UtilAngle.degreeToRadian(0), 0, 0, cameraToPlane.getR());
		cameraToPlane.getT().set(0,-5,0);

		Se3_F64 planeToCamera = cameraToPlane.invert(null);

		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		CreateSyntheticOverheadViewS<GrayF32> alg = new CreateSyntheticOverheadViewS<>(interp);

		alg.configure(param,planeToCamera,centerX,centerY,cellSize,overheadW,overheadH);

		GrayF32 input = new GrayF32(width,height);
		ImageMiscOps.fill(input,10);

		GrayF32 output = new GrayF32(overheadW,overheadH);

		alg.process(input,output);

		// check parts that shouldn't be in view
		assertEquals(0,output.get(0,300),1e-8);
		assertEquals(0,output.get(5,0),1e-8);
		assertEquals(0,output.get(5,599),1e-8);

		// check areas that should be in view
		assertEquals(10,output.get(499,300),1e-8);
	}

}
