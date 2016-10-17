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

import boofcv.alg.feature.orientation.GenericOrientationImageTests;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.struct.image.GrayU8;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationImageAverage_U8 {
	double angleTol = 0.1;// had to up tolerance for limited resolution of UInt8 images
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationImageTests<GrayU8> tests = new GenericOrientationImageTests<>();

		OrientationImageAverage<GrayU8> alg = new ImplOrientationImageAverage_U8(1.0/2.0,r);

		tests.setup(angleTol, r*2+1 , alg,GrayU8.class);
		tests.performAll();
	}
}
