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

package boofcv.struct.image;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
public class TestImageDataType extends BoofStandardJUnit {

	@Test void classToType() {
		assertSame(ImageDataType.U8, ImageDataType.classToType(GrayU8.class));
		assertSame(ImageDataType.S8, ImageDataType.classToType(GrayS8.class));
		assertSame(ImageDataType.U16, ImageDataType.classToType(GrayU16.class));
		assertSame(ImageDataType.S16, ImageDataType.classToType(GrayS16.class));
		assertSame(ImageDataType.S32, ImageDataType.classToType(GrayS32.class));
		assertSame(ImageDataType.S64, ImageDataType.classToType(GrayS64.class));
		assertSame(ImageDataType.F32, ImageDataType.classToType(GrayF32.class));
		assertSame(ImageDataType.F64, ImageDataType.classToType(GrayF64.class));
		assertSame(ImageDataType.I8, ImageDataType.classToType(GrayI8.class));
		assertSame(ImageDataType.I16, ImageDataType.classToType(GrayI16.class));
		assertSame(ImageDataType.I, ImageDataType.classToType(GrayI.class));
		assertSame(ImageDataType.F, ImageDataType.classToType(GrayF.class));
	}

	@Test void typeToClass() {
		assertSame(GrayU8.class, ImageDataType.typeToSingleClass(ImageDataType.U8));
		assertSame(GrayS8.class, ImageDataType.typeToSingleClass(ImageDataType.S8));
		assertSame(GrayU16.class, ImageDataType.typeToSingleClass(ImageDataType.U16));
		assertSame(GrayS16.class, ImageDataType.typeToSingleClass(ImageDataType.S16));
		assertSame(GrayS32.class, ImageDataType.typeToSingleClass(ImageDataType.S32));
		assertSame(GrayS64.class, ImageDataType.typeToSingleClass(ImageDataType.S64));
		assertSame(GrayF32.class, ImageDataType.typeToSingleClass(ImageDataType.F32));
		assertSame(GrayF64.class, ImageDataType.typeToSingleClass(ImageDataType.F64));
		assertSame(GrayI8.class, ImageDataType.typeToSingleClass(ImageDataType.I8));
		assertSame(GrayI16.class, ImageDataType.typeToSingleClass(ImageDataType.I16));
		assertSame(GrayI.class, ImageDataType.typeToSingleClass(ImageDataType.I));
		assertSame(GrayF.class, ImageDataType.typeToSingleClass(ImageDataType.F));
	}
}
