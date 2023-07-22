/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.io.wrapper.images;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class TestLoadFileImageSequence extends BoofStandardJUnit {

	private String imagePath = getClass().getResource("/boofcv/io/image/wrapper/images/").getFile();

	/**
	 * See if it loads the expected number of files.
	 */
	@Test
	void basicLoadTest() {

		var alg = new LoadFileImageSequence<>(ImageType.single(GrayF32.class),
				imagePath,"png");

		assertSame(alg.getImageType().getFamily(), ImageType.Family.GRAY);
		assertSame(ImageDataType.F32, alg.getImageType().getDataType());
		assertFalse(alg.isLoop());

		int total = 0;
		while( alg.hasNext() ) {
			total++;
			GrayF32 image = alg.next();
			assertEquals(100,image.width);
			assertEquals(100,image.height);

			BufferedImage buff = alg.getGuiImage();
			assertEquals(100,buff.getWidth());
			assertEquals(100,buff.getHeight());
		}

		assertEquals(3,total);
	}

		/**
	 * See if it loads the expected number of files.
	 */
	@Test
	void checkLoop() {
		LoadFileImageSequence<GrayF32> alg = new LoadFileImageSequence<>(ImageType.single(GrayF32.class),
				imagePath,"png");
		alg.setLoop(true);

		assertTrue(alg.isLoop());

		int total = 0;
		while( alg.hasNext() && total < 6 ) {
			total++;
			GrayF32 image = alg.next();
			assertEquals(100,image.width);
			assertEquals(100,image.height);

			BufferedImage buff = alg.getGuiImage();
			assertEquals(100,buff.getWidth());
			assertEquals(100,buff.getHeight());
		}

		assertEquals(6,total);
	}
}
