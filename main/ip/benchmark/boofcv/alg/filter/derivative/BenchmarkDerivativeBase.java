/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Base class for benchmarking derivative classes
 * 
 * @author Peter Abeles
 */
public abstract class BenchmarkDerivativeBase {
	public static int imgWidth = 640;
	public static int imgHeight = 480;
	public static long TEST_TIME = 1000;
	public static boolean border = true;
	public static ImageBorder_I32 borderI32 = new ImageBorder1D_I32(BorderIndex1D_Extend.class);
	public static ImageBorder_F32 borderF32 = new ImageBorder1D_F32(BorderIndex1D_Extend.class);

	public static ImageFloat32 imgFloat32;
	public static ImageFloat32 derivX_F32;
	public static ImageFloat32 derivY_F32;
	public static ImageFloat32 derivXY_F32;
	public static ImageUInt8 imgInt8;
	public static ImageSInt16 derivX_I16;
	public static ImageSInt16 derivY_I16;

	public static ImageSInt16 derivXY_I16;


	public abstract void profile_I8();
	public abstract void profile_F32();

	public void process() {
		imgInt8 = new ImageUInt8(imgWidth,imgHeight);
		derivX_I16 = new ImageSInt16(imgWidth,imgHeight);
		derivY_I16 = new ImageSInt16(imgWidth,imgHeight);
		derivXY_I16 = new ImageSInt16(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		derivX_F32 = new ImageFloat32(imgWidth,imgHeight);
		derivY_F32 = new ImageFloat32(imgWidth,imgHeight);
		derivXY_F32 = new ImageFloat32(imgWidth,imgHeight);

		Random rand = new Random(123);
		GImageMiscOps.fillUniform(imgInt8, rand, 0, 100);
		GImageMiscOps.fillUniform(imgFloat32,rand,0,100);
		
		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println("               border = "+border);
		System.out.println();
		System.out.println("             ImageUInt8");
		System.out.println();

		profile_I8();

		System.out.println("\n             ImageFloat32");
		System.out.println();

		profile_F32();

	}
}
