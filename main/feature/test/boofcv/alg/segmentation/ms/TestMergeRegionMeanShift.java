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

package boofcv.alg.segmentation.ms;

import boofcv.struct.image.GrayS32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
* @author Peter Abeles
*/
public class TestMergeRegionMeanShift {
	@Test
	public void basicAll() {
		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);

		GrayS32 pixelToRegion = new GrayS32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,1,
				 2,0,0,1,
				 2,0,1,1,
				 0,0,3,1};

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.data = new int[]{1,2,3,4};
		regionMemberCount.size = 4;

		FastQueue<float[]> regionColor = createList(5,1,6,4);
		FastQueue<Point2D_I32> modeLocation = new FastQueue<>(Point2D_I32.class, true);
		modeLocation.grow().set(0,0);
		modeLocation.grow().set(3,3);
		modeLocation.grow().set(0,1);
		modeLocation.grow().set(2, 3);

		alg.process(pixelToRegion, regionMemberCount, regionColor, modeLocation);

		GrayS32 expectedP2R = new GrayS32(4,4);
		expectedP2R.data = new int[]
				{0,0,0,1,
				 0,0,0,1,
				 0,0,1,1,
				 0,0,2,1};

		int expectedCount[] = new int[]{4,2,4};

		for( int i = 0; i < expectedP2R.data.length; i++ )
			assertEquals(expectedP2R.data[i],pixelToRegion.data[i]);

		for( int i = 0; i < expectedCount.length; i++ )
			assertEquals(expectedCount[i],regionMemberCount.data[i]);
	}

	private FastQueue<float[]> createList( int ...colors ) {
		FastQueue<float[]> ret = new FastQueue<float[]>(float[].class,true) {
			@Override
			protected float[] createInstance() {
				return new float[1];
			}
		};

		for( int i = 0; i < colors.length; i++ ) {
			ret.grow()[0] = colors[i];
		}
		return ret;
	}
}
