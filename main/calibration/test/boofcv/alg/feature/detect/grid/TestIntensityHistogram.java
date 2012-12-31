/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.grid;

import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestIntensityHistogram {

	@Test
	public void reset() {
		IntensityHistogram alg = new IntensityHistogram(5,10);
		alg.add(3.3);
		assertTrue(alg.total == 1);
		assertTrue(alg.histogram[1]==1);

		alg.reset();
		assertTrue(alg.total==0);
		assertTrue(alg.histogram[1]==0);
	}

	@Test
	public void add_value() {
		IntensityHistogram alg = new IntensityHistogram(5,10);
		alg.add(3.3);
		alg.add(3.9999);
		alg.add(0);
		assertTrue(alg.total==3);
		assertTrue(alg.histogram[0]==1);
		assertTrue(alg.histogram[1]==2);
		assertTrue(alg.histogram[2]==0);
	}

	@Test
	public void add_F32() {
		ImageFloat32 img = new ImageFloat32(2,3);
		
		img.set(0,0,1);
		img.set(1,2,3);

		BoofTesting.checkSubImage(this,"add_F32",false,img);
	}
	
	public void add_F32( ImageFloat32 img ) {
		IntensityHistogram alg = new IntensityHistogram(5,10);

		alg.add(img);

		assertTrue(alg.total == 6);
		assertTrue(alg.histogram[0] == 5);
		assertTrue(alg.histogram[1] == 1);
	}

	@Test
	public void downSample() {
		IntensityHistogram alg = new IntensityHistogram(10,10);
		alg.add(2.1);
		alg.add(3.3);
		alg.add(4.1);

		IntensityHistogram alg2 = new IntensityHistogram(5,10);

		alg2.downSample(alg);

		assertTrue(alg2.total==3);
		assertTrue(alg2.histogram[1]==2);
		assertTrue(alg2.histogram[2]==1);
	}
}
