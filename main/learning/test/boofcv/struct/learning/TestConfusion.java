/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.learning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConfusion {
	@Test
	public void computeAccuracy() {
		Confusion c = new Confusion(2);

		c.matrix.set(0,0,0.25);
		c.matrix.set(0,1,0.75);
		c.matrix.set(1,1,0.85);
		c.matrix.set(1,0,0.15);

		double accuracy = (0.25+0.85)/2.0;
		assertEquals(accuracy,c.computeAccuracy(),1e-8);
	}
}
