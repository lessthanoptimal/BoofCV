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
import boofcv.struct.image.GrayS32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationAverage_S32 {

	double angleTol = 0.1;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationGradientTests<GrayS32> tests = new GenericOrientationGradientTests<>();

		ImplOrientationAverage_S32 alg = new ImplOrientationAverage_S32(1.0/2.0,false);
		alg.setSampleRadius(r);

		tests.setup(angleTol, r*2+1 , alg);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationGradientTests<GrayS32> tests = new GenericOrientationGradientTests<>();

		ImplOrientationAverage_S32 alg = new ImplOrientationAverage_S32(1.0/2.0,true);
		alg.setSampleRadius(r);

		tests.setup(angleTol, r*2+1 ,alg);
		tests.performAll();

	}
}
