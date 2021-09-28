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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.struct.border.ImageBorder1D_F32;
import boofcv.struct.border.ImageBorder1D_S32;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * Base class for benchmarking derivative classes
 *
 * @author Peter Abeles
 */
public abstract class CommonBenchmarkDerivative {
	public static int width = 1024;
	public static int height = 768;
	public static boolean border = true;
	public static ImageBorder_S32 borderI32 = new ImageBorder1D_S32(BorderIndex1D_Extend::new);
	public static ImageBorder_F32 borderF32 = new ImageBorder1D_F32(BorderIndex1D_Extend::new);

	public static GrayF32 imgF32;
	public static GrayF32 dx_F32, dy_F32, dxy_F32;
	public static GrayU8 imgI8;
	public static GrayS16 dx_I16, dy_I16, dxy_I16;

	public void setup() {
		imgI8 = new GrayU8(width, height);
		dx_I16 = new GrayS16(width, height);
		dy_I16 = new GrayS16(width, height);
		dxy_I16 = new GrayS16(width, height);
		imgF32 = new GrayF32(width, height);
		dx_F32 = new GrayF32(width, height);
		dy_F32 = new GrayF32(width, height);
		dxy_F32 = new GrayF32(width, height);

		Random rand = new Random(123);
		GImageMiscOps.fillUniform(imgI8, rand, 0, 100);
		GImageMiscOps.fillUniform(imgF32, rand, 0, 100);
	}
}
