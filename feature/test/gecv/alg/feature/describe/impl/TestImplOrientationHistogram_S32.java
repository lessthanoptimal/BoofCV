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

import gecv.struct.image.ImageSInt32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationHistogram_S32 {

	int N = 10;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationTests<ImageSInt32> tests = new GenericOrientationTests<ImageSInt32>();

		ImplOrientationHistogram_S32 alg = new ImplOrientationHistogram_S32(N,false);
		alg.setRadius(r);

		tests.setup(2.0*Math.PI/N, r*2+1 , alg);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationTests<ImageSInt32> tests = new GenericOrientationTests<ImageSInt32>();

		ImplOrientationHistogram_S32 alg = new ImplOrientationHistogram_S32(N,true);
		alg.setRadius(r);

		tests.setup(2.0*Math.PI/N, r*2+1 ,alg);
		tests.performAll();

	}


}
