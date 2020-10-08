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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestImplXCornerAbeles2019Intensity extends BoofStandardJUnit {

	int width = 30;
	int height = 20;

	/**
	 * Compare it to a naive implementation
	 */
	@Test
	void compareToNaive() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = input.createSameShape();
		GrayF32 expected = input.createSameShape();

		ImageMiscOps.fillUniform(input,rand,-1,1);
		ImageMiscOps.fillUniform(found,rand,-1,1);
		ImageMiscOps.fillUniform(expected,rand,-1,1);

		ImplXCornerAbeles2019Intensity.process(input,found);
		naive(input,expected);

		BoofTesting.assertEquals(expected,found, 1e-8);
	}

	void naive( GrayF32 input, GrayF32 output ) {
		ImageMiscOps.fill(output,0);

		int r = 3;
		for (int y = r; y < input.height-r; y++) {
			for (int x = r; x < input.width-r; x++) {
				float v00 = input.get(x  , y-3 );
				float v01 = input.get(x+1, y-3 );
				float v02 = input.get(x+2, y-2 );
				float v03 = input.get(x+3, y-1 );
				float v04 = input.get(x+3, y   );
				float v05 = input.get(x+3, y+1 );
				float v06 = input.get(x+2, y+2 );
				float v07 = input.get(x+1, y+3 );
				float v08 = input.get(x  , y+3 );
				float v09 = input.get(x-1, y+3 );
				float v10 = input.get(x-2, y+2 );
				float v11 = input.get(x-3, y+1 );
				float v12 = input.get(x-3, y   );
				float v13 = input.get(x-3, y-1 );
				float v14 = input.get(x-2, y-2 );
				float v15 = input.get(x-1, y-3 );

				float a = (v15+v00+v01);
				float b = (v03+v04+v05);
				float c = (v07+v08+v09);
				float d = (v11+v12+v13);

				float e = (v01+v02+v03);
				float f = (v05+v06+v07);
				float g = (v09+v10+v11);
				float h = (v13+v14+v15);

				output.set(x,y, Math.max(score(a,b,c,d), score(e,f,g,h)));
			}
		}
	}
	private static float score(float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		return a*c + b*d;
	}

}
