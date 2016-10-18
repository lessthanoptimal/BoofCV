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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.GenericOrientationGradientTests;
import boofcv.struct.image.GrayS16;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationSlidingWindow_S16 {
	int N = 10;
	int r = 3;
	double window = Math.PI/3.0;

	@Test
	public void standardUnweighted() {
		GenericOrientationGradientTests<GrayS16> tests = new GenericOrientationGradientTests<>();

		ImplOrientationSlidingWindow_S16 alg = new ImplOrientationSlidingWindow_S16(r,N,window,false);
		alg.setObjectRadius(r);

		tests.setup(2.0*Math.PI/N, r*2+1 , alg);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationGradientTests<GrayS16> tests = new GenericOrientationGradientTests<>();

		ImplOrientationSlidingWindow_S16 alg = new ImplOrientationSlidingWindow_S16(r,N,window,true);
		alg.setObjectRadius(r);

		tests.setup(2.0*Math.PI/N, r*2+1 ,alg);
		tests.performAll();
	}
}
