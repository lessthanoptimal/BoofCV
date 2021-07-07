/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestTldNonMaximalSuppression extends BoofStandardJUnit {

	/**
	 * Just tests the connections graph generated in process()
	 */
	@Test void process() {
		TldNonMaximalSuppression alg = new TldNonMaximalSuppression(0.5);

		DogArray<TldRegion> regions = new DogArray<>(TldRegion::new);
		regions.grow().rect.setTo(0,100,10,120);
		regions.grow().rect.setTo(2,3,8,33);
		regions.grow().rect.setTo(0,100,9,119);
		regions.grow().rect.setTo(0,100,2,102);
		regions.get(0).confidence = 100;
		regions.get(1).confidence = 200;
		regions.get(2).confidence = 300;
		regions.get(3).confidence = 400;

		DogArray<TldRegion> output = new DogArray<>(TldRegion::new);

		alg.process(regions,output);
		assertEquals(output.size(), 3);

		DogArray<TldNonMaximalSuppression.Connections> conn = alg.getConnections();

		assertFalse(conn.data[0].maximum);
		assertTrue(conn.data[1].maximum);
		assertTrue(conn.data[2].maximum);
		assertTrue(conn.data[3].maximum);
	}
}
