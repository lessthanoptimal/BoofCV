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

package boofcv.struct.image;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageDataType {

	@Test
	public void classToType() {
		assertTrue(ImageDataType.U8 == ImageDataType.classToType(GrayU8.class));
		assertTrue(ImageDataType.S8 == ImageDataType.classToType(GrayS8.class));
		assertTrue(ImageDataType.U16 == ImageDataType.classToType(GrayU16.class));
		assertTrue(ImageDataType.S16 == ImageDataType.classToType(GrayS16.class));
		assertTrue(ImageDataType.S32 == ImageDataType.classToType(GrayS32.class));
		assertTrue(ImageDataType.S64 == ImageDataType.classToType(GrayS64.class));
		assertTrue(ImageDataType.F32 == ImageDataType.classToType(GrayF32.class));
		assertTrue(ImageDataType.F64 == ImageDataType.classToType(GrayF64.class));
		assertTrue(ImageDataType.I8 == ImageDataType.classToType(GrayI8.class));
		assertTrue(ImageDataType.I16 == ImageDataType.classToType(GrayI16.class));
		assertTrue(ImageDataType.I == ImageDataType.classToType(GrayI.class));
		assertTrue(ImageDataType.F == ImageDataType.classToType(GrayF.class));
	}

	@Test
	public void typeToClass() {
		assertTrue(GrayU8.class == ImageDataType.typeToSingleClass(ImageDataType.U8));
		assertTrue(GrayS8.class == ImageDataType.typeToSingleClass(ImageDataType.S8));
		assertTrue(GrayU16.class == ImageDataType.typeToSingleClass(ImageDataType.U16));
		assertTrue(GrayS16.class == ImageDataType.typeToSingleClass(ImageDataType.S16));
		assertTrue(GrayS32.class == ImageDataType.typeToSingleClass(ImageDataType.S32));
		assertTrue(GrayS64.class == ImageDataType.typeToSingleClass(ImageDataType.S64));
		assertTrue(GrayF32.class == ImageDataType.typeToSingleClass(ImageDataType.F32));
		assertTrue(GrayF64.class == ImageDataType.typeToSingleClass(ImageDataType.F64));
		assertTrue(GrayI8.class == ImageDataType.typeToSingleClass(ImageDataType.I8));
		assertTrue(GrayI16.class == ImageDataType.typeToSingleClass(ImageDataType.I16));
		assertTrue(GrayI.class == ImageDataType.typeToSingleClass(ImageDataType.I));
		assertTrue(GrayF.class == ImageDataType.typeToSingleClass(ImageDataType.F));
	}

}
