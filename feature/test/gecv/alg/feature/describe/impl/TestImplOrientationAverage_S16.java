/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.describe.impl;

import gecv.struct.image.ImageSInt16;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationAverage_S16 {

	double angleTol = 0.01;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationTests<ImageSInt16> tests = new GenericOrientationTests<ImageSInt16>();

		ImplOrientationAverage_S16 alg = new ImplOrientationAverage_S16(false);
		alg.setRadius(r);

		tests.setup(angleTol, r*2+1 , alg);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationTests<ImageSInt16> tests = new GenericOrientationTests<ImageSInt16>();

		ImplOrientationAverage_S16 alg = new ImplOrientationAverage_S16(true);
		alg.setRadius(r);

		tests.setup(angleTol, r*2+1 ,alg);
		tests.performAll();
		tests.performWeightTests();
	}
}
