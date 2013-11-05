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

package boofcv.struct.image;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageDataType {

	@Test
	public void classToType() {
		assertTrue(ImageDataType.U8 == ImageDataType.classToType(ImageUInt8.class));
		assertTrue(ImageDataType.S8 == ImageDataType.classToType(ImageSInt8.class));
		assertTrue(ImageDataType.U16 == ImageDataType.classToType(ImageUInt16.class));
		assertTrue(ImageDataType.S16 == ImageDataType.classToType(ImageSInt16.class));
		assertTrue(ImageDataType.S32 == ImageDataType.classToType(ImageSInt32.class));
		assertTrue(ImageDataType.S64 == ImageDataType.classToType(ImageSInt64.class));
		assertTrue(ImageDataType.F32 == ImageDataType.classToType(ImageFloat32.class));
		assertTrue(ImageDataType.F64 == ImageDataType.classToType(ImageFloat64.class));
		assertTrue(ImageDataType.I8 == ImageDataType.classToType(ImageInt8.class));
		assertTrue(ImageDataType.I16 == ImageDataType.classToType(ImageInt16.class));
		assertTrue(ImageDataType.I == ImageDataType.classToType(ImageInteger.class));
		assertTrue(ImageDataType.F == ImageDataType.classToType(ImageFloat.class));
	}

	@Test
	public void typeToClass() {
		assertTrue(ImageUInt8.class == ImageDataType.typeToClass(ImageDataType.U8));
		assertTrue(ImageSInt8.class == ImageDataType.typeToClass(ImageDataType.S8));
		assertTrue(ImageUInt16.class == ImageDataType.typeToClass(ImageDataType.U16));
		assertTrue(ImageSInt16.class == ImageDataType.typeToClass(ImageDataType.S16));
		assertTrue(ImageSInt32.class == ImageDataType.typeToClass(ImageDataType.S32));
		assertTrue(ImageSInt64.class == ImageDataType.typeToClass(ImageDataType.S64));
		assertTrue(ImageFloat32.class == ImageDataType.typeToClass(ImageDataType.F32));
		assertTrue(ImageFloat64.class == ImageDataType.typeToClass(ImageDataType.F64));
		assertTrue(ImageInt8.class == ImageDataType.typeToClass(ImageDataType.I8));
		assertTrue(ImageInt16.class == ImageDataType.typeToClass(ImageDataType.I16));
		assertTrue(ImageInteger.class == ImageDataType.typeToClass(ImageDataType.I));
		assertTrue(ImageFloat.class == ImageDataType.typeToClass(ImageDataType.F));
	}

}
