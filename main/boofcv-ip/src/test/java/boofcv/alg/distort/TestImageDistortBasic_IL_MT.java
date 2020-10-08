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

package boofcv.alg.distort;

import boofcv.BoofTesting;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestImageDistortBasic_IL_MT extends BoofStandardJUnit {
	int width = 60,height=80;
	private static final int NUM_BANDS = 2;

	@Test
	void compare_all() {
		InterleavedF32 input = new InterleavedF32(width,height,NUM_BANDS);
		InterleavedF32 output_ST = new InterleavedF32(width,height,NUM_BANDS);
		InterleavedF32 output_MT = new InterleavedF32(width,height,NUM_BANDS);
		GImageMiscOps.fillUniform(input,rand,0,150);

		InterpolatePixelMB<InterleavedF32> interpolate = FactoryInterpolation.createPixelMB(
				0, 255, InterpolationType.BILINEAR, BorderType.EXTENDED, ImageType.il(NUM_BANDS,InterleavedF32.class));

		ImageDistortBasic_IL alg_ST = new ImageDistortBasic_IL(new AssignPixelValue_MB.F32(),interpolate);
		ImageDistortBasic_IL_MT alg_MT = new ImageDistortBasic_IL_MT(new AssignPixelValue_MB.F32(),interpolate);

		alg_ST.setModel(new Transform());
		alg_ST.apply(input,output_ST);

		alg_MT.setModel(new Transform());
		alg_MT.apply(input,output_MT);

		BoofTesting.assertEquals(output_ST,output_MT, UtilEjml.TEST_F32);
	}

	@Test
	void compare_mask() {
		InterleavedF32 input = new InterleavedF32(width,height,NUM_BANDS);
		InterleavedF32 output_ST = new InterleavedF32(width,height,NUM_BANDS);
		InterleavedF32 output_MT = new InterleavedF32(width,height,NUM_BANDS);
		GImageMiscOps.fillUniform(input,rand,0,150);

		GrayU8 mask = new GrayU8(width,height);
		GImageMiscOps.fillUniform(input,rand,0,1);

		InterpolatePixelMB<InterleavedF32> interpolate = FactoryInterpolation.createPixelMB(
				0, 255, InterpolationType.BILINEAR, BorderType.EXTENDED, ImageType.il(NUM_BANDS,InterleavedF32.class));

		ImageDistortBasic_IL alg_ST = new ImageDistortBasic_IL(new AssignPixelValue_MB.F32(),interpolate);
		ImageDistortBasic_IL_MT alg_MT = new ImageDistortBasic_IL_MT(new AssignPixelValue_MB.F32(),interpolate);

		alg_ST.setModel(new TestImageDistortBasic_IL_MT.Transform());
		alg_ST.apply(input,output_ST,mask);

		alg_MT.setModel(new TestImageDistortBasic_IL_MT.Transform());
		alg_MT.apply(input,output_MT,mask);

		BoofTesting.assertEquals(output_ST,output_MT, UtilEjml.TEST_F32);
	}

	public static class Transform implements PixelTransform<Point2D_F32> {

		@Override
		public void compute(int x, int y, Point2D_F32 output) {
			output.x = x+y*0.0001f;
			output.y = y*0.999f;
		}

		@Override
		public PixelTransform<Point2D_F32> copyConcurrent() {
			return new Transform();
		}
	}
}