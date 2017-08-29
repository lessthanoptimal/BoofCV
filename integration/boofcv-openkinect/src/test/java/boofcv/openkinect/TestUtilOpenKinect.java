/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.openkinect;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU16;
import org.ddogleg.struct.GrowQueue_I8;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilOpenKinect {

	int width = 320;
	int height = 240;
	Random rand = new Random(234);

	@Test
	public void saveDepth_parseDepth() throws IOException {

		GrayU16 depth = new GrayU16(width,height);
		ImageMiscOps.fillUniform(depth,rand,0,10000);
		GrowQueue_I8 data = new GrowQueue_I8();
		GrayU16 found = new GrayU16(width,height);

		UtilOpenKinect.saveDepth(depth, "temp.depth", data);


		UtilOpenKinect.parseDepth("temp.depth",found,data);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {

				int a = depth.get(j,i);
				int b = found.get(j,i);

				assertEquals(a,b);
			}
		}

		// clean up
		File f = new File("temp.depth");
		assertTrue(f.delete());
	}

}
