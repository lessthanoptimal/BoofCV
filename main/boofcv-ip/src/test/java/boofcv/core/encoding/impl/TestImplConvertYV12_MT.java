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

package boofcv.core.encoding.impl;

import boofcv.struct.image.*;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

@SuppressWarnings("rawtypes")
class TestImplConvertYV12_MT extends CompareIdenticalFunctions {

	private final int width = 105;
	private final int height = 100;

	TestImplConvertYV12_MT() {
		super(ImplConvertYV12_MT.class, ImplConvertYV12.class);
	}

	@Test
	void performTests() {
		performTests(4);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		byte[] yv12 = new byte[width*height*2];
		rand.nextBytes(yv12);

		Class[] type = candidate.getParameterTypes();

		ImageBase output ;

		String name = candidate.getName();
		if( name.startsWith("yv12ToPlanar")) {
			if( name.endsWith("U8")) {
				output = new Planar<>(GrayU8.class,width,height,3);
			} else {
				output = new Planar<>(GrayF32.class,width,height,3);
			}
		} else {
			if( type[1] == InterleavedU8.class ) {
				output = new InterleavedU8(width,height,3);
			} else {
				output = new InterleavedF32(width,height,3);
			}
		}

		return new Object[][]{{yv12, output}};
	}
}

