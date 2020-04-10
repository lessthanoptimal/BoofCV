/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.mls.ImageDeformPointMLS_F32;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BMemoryImageDeformPointMLS {
	public static void main(String[] args) {
		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32(TypeDeformMLS.RIGID);

		int N = 20_000;
		int size = 1000;

		long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		for (int trial = 0; trial < 200; trial++) {
			alg.reset();
			alg.configure(size,size,30,30);
			Random rand = new Random(2345);
			for (int i = 0; i < N; i++) {
				float sx = rand.nextFloat()*(size-1);
				float sy = rand.nextFloat()*(size-1);
				float dx = sx + (rand.nextFloat()-0.5f)*20f;
				float dy = sy + (rand.nextFloat()-0.5f)*20f;
				dx = BoofMiscOps.bound(dx,0,size-1);
				dy = BoofMiscOps.bound(dy,0,size-1);

				alg.add(sx,sy,dx,dy);
			}
			alg.fixateUndistorted();
			alg.fixateDistorted();

			Point2D_F32 out = new Point2D_F32();
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					alg.compute(x,y,out);
				}
			}
			long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
			long memory = afterUsedMem-beforeUsedMem;
			System.out.printf("Memory Usage %6.2f MB\n",memory/1024.0/1024.0);
		}

	}
}
