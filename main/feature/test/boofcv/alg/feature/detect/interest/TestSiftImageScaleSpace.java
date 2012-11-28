/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSiftImageScaleSpace {

	Random rand = new Random(234);

	@Test
	public void computeScaleSigma() {

		SiftImageScaleSpace alg = new SiftImageScaleSpace(2,5,1.6f,false);

		assertEquals( 1.6 , alg.computeScaleSigma(0,0) , 1e-4);
		assertEquals( 3.2 , alg.computeScaleSigma(0,1) , 1e-4 );
		assertEquals(4.8, alg.computeScaleSigma(0, 2), 1e-4);

		// compute total gaussian blur from previous set taken at level 2
		double prev = 2*1.6;
		// Each level still has 1.6, but at 1/2 the resolution
		double next1 = Math.sqrt( prev*prev + 4*1.6*1.6 );
		double next2 = Math.sqrt( prev*prev + 4*3.2*3.2 );

		assertEquals( next1 , alg.computeScaleSigma(1,0) , 1e-4);
		assertEquals( next2 , alg.computeScaleSigma(1,1) , 1e-4 );
	}

	@Test
	public void downSample() {
		checkDownSample(20,30);
		checkDownSample(19, 29);
	}

	private void checkDownSample( int w , int h ) {
		ImageFloat32 input = new ImageFloat32(w,h);
		ImageFloat32 output = new ImageFloat32(w/2,h/2);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		SiftImageScaleSpace.downSample(input, output);

		for( int i = 0; i < output.height; i++ ) {
			for( int j = 0; j < output.width; j++ ) {
				assertTrue(input.get(j * 2+1, i * 2+1) == output.get(j, i));
			}
		}
	}

	@Test
	public void upSample() {
		checUpSample(20, 30);
		checUpSample(19, 29);
	}

	private void checUpSample( int w , int h ) {
		ImageFloat32 input = new ImageFloat32(w,h);
		ImageFloat32 output = new ImageFloat32(w*2,h*2);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		SiftImageScaleSpace.upSample(input, output);

		for( int i = 0; i < output.height; i++ ) {
			for( int j = 0; j < output.width; j++ ) {
				assertTrue(input.get(j/2, i/2) == output.get(j, i));
			}
		}
	}
}
