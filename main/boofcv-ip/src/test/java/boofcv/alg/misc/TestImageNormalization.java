/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestImageNormalization {
	Random rand = new Random(2345);
	int width = 40;
	int height = 30;
	// TODO write a speed benchmark

	// TODO go through expected input types exhaustively

	// TODO ensure with and without parameters works

	private Class[] types = new Class[]{GrayU8.class, GrayU16.class, GrayF32.class};

	@Test
	void zeroMeanMaxOne() {
		fail("Implement");
	}

	@Test
	void zeroMeanStdOne() {
		fail("Implement");
	}
}
