/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestContourExternalOnly {
	public static GrayU8 TEST1 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,1,0,0,0,1,1},
			 {0,0,0,0,0,0,0,1,0,0,0,1,1},
			 {0,0,0,0,0,0,0,1,0,0,1,1,0},
			 {0,0,0,0,0,0,0,0,1,1,1,1,0},
			 {0,0,1,0,0,0,0,0,1,1,1,0,0},
			 {0,0,1,0,0,0,1,1,1,1,1,0,0},
			 {1,1,1,1,1,1,1,1,1,1,0,0,0},
			 {0,0,0,1,1,1,1,1,0,0,0,0,0}});


	public static GrayU8 TEST2 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			 {0,0,0,1,0,0,0,0,1,0,0,0,0,0},
			 {0,0,1,0,1,0,0,1,0,0,1,0,0,0},
			 {0,0,0,1,0,0,1,0,1,0,1,1,1,0},
			 {0,0,0,0,0,1,0,0,0,1,1,1,1,0},
			 {0,0,0,1,0,1,0,0,0,1,0,0,0,0},
			 {0,0,0,0,0,1,0,1,1,1,0,1,1,0},
			 {0,1,1,1,0,0,1,0,0,1,0,0,1,0},
			 {0,0,0,0,1,1,1,1,1,0,0,0,0,0},
			 {0,0,0,0,0,0,0,0,0,0,0,0,0,0}});

	public static GrayU8 TEST3 = new GrayU8(new byte[][]
			{{0,0,0,0,0},
					{0,1,1,1,0},
					{0,1,1,1,0},
					{0,1,0,1,0},
					{0,1,1,1,0},
					{0,0,0,0,0}});

	public static GrayU8 TEST4 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0},
					{0,0,1,1,1,1,1},
					{0,1,0,1,1,1,1},
					{0,1,1,1,0,1,1},
					{0,1,1,1,1,1,1},
					{0,1,1,1,1,1,1},
					{0,1,1,1,1,1,1},
					{0,0,0,0,0,0,0}});

	@Test
	public void test1() {
		ContourExternalOnly alg = new ContourExternalOnly();

		GrayU8 binary = TEST1.clone();
		GrayS32 labeled = new GrayS32(1,1);

		alg.process(binary,labeled);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(2,contours.size());
	}

	@Test
	public void test2() {
		ContourExternalOnly alg = new ContourExternalOnly();

		GrayU8 binary = TEST2.clone();
		GrayS32 labeled = new GrayS32(1,1);

		alg.process(binary,labeled);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(14,contours.size());
	}

	@Test
	public void test3() {
		ContourExternalOnly alg = new ContourExternalOnly();

		GrayU8 binary = TEST3.clone();
		GrayS32 labeled = new GrayS32(1,1);

		alg.process(binary,labeled);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(1,contours.size());
	}

	@Test
	public void test4() {
		ContourExternalOnly alg = new ContourExternalOnly();

		GrayU8 binary = TEST4.clone();
		GrayS32 labeled = new GrayS32(1,1);

		alg.process(binary,labeled);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(1,contours.size());
	}
}