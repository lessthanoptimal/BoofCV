/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLookUpImageListByIndex extends BoofStandardJUnit {
	List<GrayU8> images = new ArrayList<>();

	public TestLookUpImageListByIndex() {
		for (int i = 0; i < 4; i++) {
			images.add(new GrayU8(10+i,5));
		}
	}

	@Test void loadShape() {
		var alg = new LookUpImageListByIndex<>(images);
		var found = new ImageDimension();
		for (int i = 0; i < 4; i++) {
			alg.loadShape(""+i,found);
			assertEquals(10+i,found.width);
			assertEquals(5,found.height);
		}
	}

	@Test void loadImage() {
		var found = new GrayF32(0,0);
		var alg = new LookUpImageListByIndex<>(images);
		alg.loadImage("1",found);
		assertEquals(11,found.width);
		assertEquals(5,found.height);
	}
}
