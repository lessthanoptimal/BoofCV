/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image.border;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.border.ImageBorderWrapped;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImageBorderWrapped {
	Random rand = BoofTesting.createRandom(1);

	@Nested
	public class CheckS32 {
		GrayU8 image = new GrayU8(10,12);
		GrayU8 border = new GrayU8(14,18);

		public CheckS32() {
			ImageMiscOps.fillUniform(image,rand,0,200);
			ImageMiscOps.fillUniform(border,rand,0,200);
		}

		@Test
		void checkInner() {
			ImageBorderWrapped.S32<GrayU8> alg = createAlg();

			assertEquals(image.get(0,0),alg.get(0,0));
			assertEquals(image.get(9,11),alg.get(9,11));
		}

		@Test
		void checkBorder() {
			ImageBorderWrapped.S32<GrayU8> alg = createAlg();

			assertEquals(border.get(1,1),alg.get(-1,-2));
			assertEquals(border.get(12,15),alg.get(10,12));
		}

		private ImageBorderWrapped.S32<GrayU8> createAlg() {
			ImageBorderWrapped.S32<GrayU8> alg = new ImageBorderWrapped.S32<>();
			alg.setImage(image);
			alg.offsetX = 2;
			alg.offsetY = 3;
			alg.borderImage = border;
			return alg;
		}
	}

	@Nested
	public class CheckF32 {
		GrayF32 image = new GrayF32(10,12);
		GrayF32 border = new GrayF32(14,18);

		public CheckF32() {
			ImageMiscOps.fillUniform(image,rand,0,200);
			ImageMiscOps.fillUniform(border,rand,0,200);
		}

		@Test
		void checkInner() {
			ImageBorderWrapped.F32 alg = createAlg();

			assertEquals(image.get(0,0),alg.get(0,0));
			assertEquals(image.get(9,11),alg.get(9,11));
		}

		@Test
		void checkBorder() {
			ImageBorderWrapped.F32 alg = createAlg();

			assertEquals(border.get(1,1),alg.get(-1,-2));
			assertEquals(border.get(12,15),alg.get(10,12));
		}

		private ImageBorderWrapped.F32 createAlg() {
			var alg = new ImageBorderWrapped.F32();
			alg.setImage(image);
			alg.offsetX = 2;
			alg.offsetY = 3;
			alg.borderImage = border;
			return alg;
		}
	}
}