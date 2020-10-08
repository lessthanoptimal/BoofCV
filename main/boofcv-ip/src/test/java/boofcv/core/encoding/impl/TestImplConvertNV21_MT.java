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
class TestImplConvertNV21_MT extends CompareIdenticalFunctions {

	private int width = 105;
	private int height = 100;


	TestImplConvertNV21_MT() {
		super(ImplConvertNV21_MT.class, ImplConvertNV21.class);
	}

	@Test
	void performTests() {
		performTests(8);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		byte[] nv21 = new byte[width*height*2];
		rand.nextBytes(nv21);

		Class[] type = candidate.getParameterTypes();

		ImageBase output ;

		String name = candidate.getName();
		if( name.startsWith("nv21ToGray") ) {
			if(type[1] == GrayU8.class ) {
				output = new GrayU8(width,height);
			} else {
				output = new GrayF32(width,height);
			}
		} else if( name.startsWith("nv21ToPlanarRgb")) {
			if( name.endsWith("U8")) {
				output = new Planar<>(GrayU8.class,width,height,3);
			} else {
				output = new Planar<>(GrayF32.class,width,height,3);
			}
		} else if( name.startsWith("nv21ToPlanarYuv")) {
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

		return new Object[][]{{nv21, output}};
	}
}

