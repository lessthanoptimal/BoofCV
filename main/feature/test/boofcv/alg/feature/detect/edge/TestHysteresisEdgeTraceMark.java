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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHysteresisEdgeTraceMark extends CommonHysteresisEdgeTrace {

	@Test
	public void test0() {
		standardTest(0);
	}

	@Test
	public void test1() {
		standardTest(1);
	}

	@Test
	public void test2() {
		GrayS8 dir = direction(2);
		GrayU8 out = new GrayU8(dir.width,dir.height);

		HysteresisEdgeTraceMark alg = new HysteresisEdgeTraceMark();

		alg.process(intensity(2),dir,3,5,out);
		assertEquals(3, ImageStatistics.sum(out));

		alg.process(intensity(2),dir,2,5,out);
		assertEquals(4, ImageStatistics.sum(out));
	}

	@Test
	public void test3() {
		standardTest(3);
	}

	@Test
	public void test4() {
		standardTest(4);
	}

	private void standardTest( int which ) {
		GrayF32 inten = intensity(which);
		GrayS8 dir = direction(which);
		GrayU8 out = new GrayU8(inten.width,inten.height);

		HysteresisEdgeTraceMark alg = new HysteresisEdgeTraceMark();

		alg.process(inten,dir,2,5,out);

		BoofTesting.assertEquals(expected(which),out,0);
	}
}
