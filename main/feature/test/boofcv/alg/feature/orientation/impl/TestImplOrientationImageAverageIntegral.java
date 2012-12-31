/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.orientation.GenericOrientationIntegralTests;
import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.sparse.GradientValue_F32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationImageAverageIntegral {
	double angleTol = 0.01;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationIntegralTests<ImageFloat32> tests = new GenericOrientationIntegralTests<ImageFloat32>();

		OrientationIntegralBase<ImageFloat32,GradientValue_F32> alg = 
				new ImplOrientationAverageGradientIntegral(r,1,2,0,ImageFloat32.class);

		tests.setup(angleTol, r*2+1+2 , alg,ImageFloat32.class);
		tests.checkSubImages();
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationIntegralTests<ImageFloat32> tests = new GenericOrientationIntegralTests<ImageFloat32>();

		OrientationIntegralBase<ImageFloat32,GradientValue_F32> alg =
				new ImplOrientationAverageGradientIntegral(r,1,2,-1,ImageFloat32.class);

		tests.setup(angleTol, r*2+1+2 ,alg,ImageFloat32.class);
		tests.performAll();
	}
}
