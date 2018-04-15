/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import static boofcv.struct.image.ImageDataType.*;
import static boofcv.struct.image.ImageType.Family.GRAY;
import static boofcv.struct.image.ImageType.Family.INTERLEAVED;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageType {

	@Test
	public void getImageClass(){
		assertTrue(GrayF32.class==ImageType.getImageClass(GRAY, F32));
		assertTrue(GrayF64.class==ImageType.getImageClass(GRAY, F64));
		assertTrue(GrayU8.class==ImageType.getImageClass(GRAY, U8));
		assertTrue(GrayS8.class==ImageType.getImageClass(GRAY, S8));
		assertTrue(GrayU16.class==ImageType.getImageClass(GRAY, U16));
		assertTrue(GrayS16.class==ImageType.getImageClass(GRAY, S16));
		assertTrue(GrayS32.class==ImageType.getImageClass(GRAY, S32));
		assertTrue(GrayS64.class==ImageType.getImageClass(GRAY, S64));

		assertTrue(InterleavedF32.class==ImageType.getImageClass(INTERLEAVED, F32));
		assertTrue(InterleavedF64.class==ImageType.getImageClass(INTERLEAVED, F64));
		assertTrue(InterleavedU8.class==ImageType.getImageClass(INTERLEAVED, U8));
		assertTrue(InterleavedS8.class==ImageType.getImageClass(INTERLEAVED, S8));
		assertTrue(InterleavedU16.class==ImageType.getImageClass(INTERLEAVED, U16));
		assertTrue(InterleavedS16.class==ImageType.getImageClass(INTERLEAVED, S16));
		assertTrue(InterleavedS32.class==ImageType.getImageClass(INTERLEAVED, S32));
		assertTrue(InterleavedS64.class==ImageType.getImageClass(INTERLEAVED, S64));
	}

	@Test
	public void isSameType() {
		ImageType types[] = new ImageType[4];
		types[0] = ImageType.single(GrayU8.class);
		types[1] = ImageType.single(GrayF32.class);
		types[2] = ImageType.pl(3,GrayU8.class);
		types[3] = ImageType.pl(2,GrayU8.class);

		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < types.length; j++) {
				assertEquals(i==j,types[i].isSameType(types[j]));
			}
		}
	}

	@Test
	public void setTo() {
		ImageType a = ImageType.single(GrayU8.class);
		ImageType b = ImageType.pl(2,GrayF32.class);

		assertFalse(a.isSameType(b));
		a.setTo(b);
		assertTrue(a.isSameType(b));
	}
}
