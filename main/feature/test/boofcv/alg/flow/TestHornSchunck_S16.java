/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageSInt16;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestHornSchunck_S16 {

	int width = 20;
	int height = 30;

	@Test
	public void process() {
		HornSchunck_S16 alg = new HornSchunck_S16(0.2f,1);

		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);
		ImageSInt16 derivT = new ImageSInt16(width,height);
		ImageFlow output = new ImageFlow(width,height);

		for( int i = 0; i <10; i++ ) {
			derivX.set(5,i,1);
			derivY.set(i,9,1);

			derivT.set(5,i,10);
			derivT.set(i,9,10);
		}

		alg.process(derivX, derivY, derivT, output);

		assertTrue( output.get(5,0).x < -5);
		assertTrue( Math.abs(output.get(5,0).y) < 0.5 );
		assertTrue( output.get(5,1).x < -5);
		assertTrue( Math.abs(output.get(5,1).y) < 0.5 );

		assertTrue( Math.abs(output.get(0,9).x) < 0.5 );
		assertTrue( output.get(0,9).y < -5);
		assertTrue( Math.abs(output.get(1,9).x) < 0.5 );
		assertTrue( output.get(1,9).y < -5);

		assertTrue( output.get(5,9).x < -4);
		assertTrue( output.get(5,9).y < -4);
	}

}
