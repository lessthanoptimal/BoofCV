/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class TestImplMedianSortNaive_MT extends CompareIdenticalFunctions {
	public TestImplMedianSortNaive_MT() {
		super(ImplMedianSortNaive_MT.class, ImplMedianSortNaive.class);
	}

	@Test void performTests() {
		super.performTests(4);
	}

	@Override protected Object[][] createInputParam( Method candidate, Method validation ) {

		Class[] types = candidate.getParameterTypes();
		ImageBase input;
		if (ImageGray.class.isAssignableFrom(types[0])) {
			if( types[0] == ImageGray.class)
				input = new GrayF32(31,34);
			else
				input = GeneralizedImageOps.createImage(types[0], 31, 34, 2);
		} else
			input = new Planar<>(GrayU8.class,31,34,2);
		ImageBase output = input.createSameShape();

		GImageMiscOps.fillUniformSmart(input,rand,-150,150);

		return new Object[][]{{input,output,2,3,null}};
	}
}
