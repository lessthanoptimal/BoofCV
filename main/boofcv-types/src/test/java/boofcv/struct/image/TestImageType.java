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

import static boofcv.struct.image.ImageDataType.*;
import static boofcv.struct.image.ImageType.Family.GRAY;
import static boofcv.struct.image.ImageType.Family.INTERLEAVED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class TestImageType extends BoofStandardJUnit {

	@Test void getImageClass() {
		assertSame(GrayF32.class, ImageType.getImageClass(GRAY, F32));
		assertSame(GrayF64.class, ImageType.getImageClass(GRAY, F64));
		assertSame(GrayU8.class, ImageType.getImageClass(GRAY, U8));
		assertSame(GrayS8.class, ImageType.getImageClass(GRAY, S8));
		assertSame(GrayU16.class, ImageType.getImageClass(GRAY, U16));
		assertSame(GrayS16.class, ImageType.getImageClass(GRAY, S16));
		assertSame(GrayS32.class, ImageType.getImageClass(GRAY, S32));
		assertSame(GrayS64.class, ImageType.getImageClass(GRAY, S64));

		assertSame(InterleavedF32.class, ImageType.getImageClass(INTERLEAVED, F32));
		assertSame(InterleavedF64.class, ImageType.getImageClass(INTERLEAVED, F64));
		assertSame(InterleavedU8.class, ImageType.getImageClass(INTERLEAVED, U8));
		assertSame(InterleavedS8.class, ImageType.getImageClass(INTERLEAVED, S8));
		assertSame(InterleavedU16.class, ImageType.getImageClass(INTERLEAVED, U16));
		assertSame(InterleavedS16.class, ImageType.getImageClass(INTERLEAVED, S16));
		assertSame(InterleavedS32.class, ImageType.getImageClass(INTERLEAVED, S32));
		assertSame(InterleavedS64.class, ImageType.getImageClass(INTERLEAVED, S64));
	}

	@Test void isSameType() {
		ImageType[] types = new ImageType[4];
		types[0] = ImageType.single(GrayU8.class);
		types[1] = ImageType.single(GrayF32.class);
		types[2] = ImageType.pl(3, GrayU8.class);
		types[3] = ImageType.pl(2, GrayU8.class);

		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < types.length; j++) {
				assertEquals(i == j, types[i].isSameType(types[j]));
			}
		}
	}

	@Test void setTo() {
		ImageType a = ImageType.single(GrayU8.class);
		ImageType b = ImageType.pl(2, GrayF32.class);

		assertFalse(a.isSameType(b));
		a.setTo(b);
		assertTrue(a.isSameType(b));
	}
}
