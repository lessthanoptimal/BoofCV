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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSelectNBestFeatures {

	/**
	 * The value of N is less than the number of features
	 */
	@Test
	public void testExtra() {

		GrayF32 intensity = new GrayF32(10,20);
		intensity.set(5,10,-3);
		intensity.set(4,10,-3.5f);
		intensity.set(5,11,0);
		intensity.set(8,8,10);

		QueueCorner corners = new QueueCorner();
		corners.add(5,10);
		corners.add(4,10);
		corners.add(5,11);
		corners.add(8,8);

		SelectNBestFeatures alg = new SelectNBestFeatures(20);
		alg.setN(3);
		alg.process(intensity,corners,true);

		QueueCorner found = alg.getBestCorners();

		assertEquals(3,found.size);
		assertEquals(8,found.get(0).x);
		assertEquals(8,found.get(0).y);

		// same test, but with negative features
		alg.process(intensity,corners,false);

		found = alg.getBestCorners();

		assertEquals(3,found.size);
		assertEquals(4,found.get(0).x);
		assertEquals(10,found.get(0).y);
	}

	/**
	 * The size of N is less than the number of points
	 */
	@Test
	public void testTooLittle() {

		GrayF32 intensity = new GrayF32(10,20);
		intensity.set(5,10,-3);
		intensity.set(4,10,-3.5f);
		intensity.set(5,11,0);
		intensity.set(8,8,10);

		QueueCorner corners = new QueueCorner();
		corners.add(5,10);
		corners.add(4,10);
		corners.add(5,11);
		corners.add(8,8);

		SelectNBestFeatures alg = new SelectNBestFeatures(20);
		alg.setN(20);
		alg.process(intensity,corners,true);

		QueueCorner found = alg.getBestCorners();

		assertEquals(4,found.size);
	}


}
