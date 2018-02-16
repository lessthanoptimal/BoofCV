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

package boofcv.abst.filter.binary;

import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericBinaryContourFinder {

	public static GrayU8 TEST3 = new GrayU8(new byte[][]
					{{0,0,0,0,0},
					 {0,0,1,0,0},
					 {0,1,1,1,0},
					 {0,1,0,1,0},
					 {0,1,1,1,0},
					 {0,0,0,0,0}});

	protected abstract BinaryContourFinder create();

	@Test
	public void checkDefaults() {
		BinaryContourFinder alg = create();

		assertEquals(ConnectRule.FOUR,alg.getConnectRule());
		assertTrue(alg.isSaveInternalContours());
		assertEquals(0,alg.getMinContour());
		assertEquals(Integer.MAX_VALUE,alg.getMaxContour());
	}

	@Test
	public void minContour() {
		GrayU8 input = TEST3.clone();
		GrayS32 labeled = input.createSameShape(GrayS32.class);

		BinaryContourFinder alg = create();

		alg.setMinContour(1000);
		alg.process(input,labeled);
		assertEquals(1,alg.getContours().size());
		checkExternalSize(alg,0,0);

	}

	@Test
	public void maxContour() {
		GrayU8 input = TEST3.clone();
		GrayS32 labeled = input.createSameShape(GrayS32.class);

		BinaryContourFinder alg = create();

		alg.setMaxContour(1);
		alg.process(input,labeled);

		assertEquals(1,alg.getContours().size());
		checkExternalSize(alg,0,0);
	}

	@Test
	public void connectRule() {
		GrayU8 input = TEST3.clone();
		GrayS32 labeled = input.createSameShape(GrayS32.class);

		BinaryContourFinder alg = create();

		alg.process(input,labeled);
		checkExternalSize(alg,0,10);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(input,labeled);
		checkExternalSize(alg,0,8);
	}

	@Test
	public void saveInternal() {
		GrayU8 input = TEST3.clone();
		GrayS32 labeled = input.createSameShape(GrayS32.class);

		BinaryContourFinder alg = create();

		alg.process(input,labeled);
		checkInternalSize(alg,0,0,8);

		alg.setSaveInnerContour(false);
		alg.process(input,labeled);
		checkInternalSize(alg,0,0,0);
	}

	private void checkExternalSize( BinaryContourFinder alg , int which , int expected )
	{
		FastQueue<Point2D_I32> points = new FastQueue<>(Point2D_I32.class,true);
		alg.loadContour(alg.getContours().get(which).externalIndex,points);
		assertEquals(expected,points.size);
	}

	private void checkInternalSize( BinaryContourFinder alg , int blob, int which , int expected )
	{
		FastQueue<Point2D_I32> points = new FastQueue<>(Point2D_I32.class,true);
		alg.loadContour(alg.getContours().get(blob).internalIndexes.get(which),points);
		assertEquals(expected,points.size);
	}

}