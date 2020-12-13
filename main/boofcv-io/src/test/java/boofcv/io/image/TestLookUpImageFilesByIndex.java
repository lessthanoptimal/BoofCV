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

package boofcv.io.image;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestLookUpImageFilesByIndex extends BoofStandardJUnit {
	@Test void loadShape() {
		List<String> paths = new ArrayList<>();
		paths.add("0");
		paths.add("1");
		paths.add("2");

		var alg = new DummyLookupFileByIndex(paths);
		var dimension = new ImageDimension();
		assertTrue(alg.loadShape("1", dimension));

		assertEquals(10, dimension.width);
		assertEquals(15, dimension.height);

		assertFalse(alg.loadShape("10", dimension));
	}

	@Test void loadImage() {
		List<String> paths = new ArrayList<>();
		paths.add("0");
		paths.add("1");
		paths.add("2");

		var alg = new DummyLookupFileByIndex(paths);
		var image = new GrayU8(1, 1);
		assertTrue(alg.loadImage("1", image));

		assertEquals(10, image.width);
		assertEquals(15, image.height);

		assertFalse(alg.loadImage("10", image));
	}

	private static class DummyLookupFileByIndex extends LookUpImageFilesByIndex {
		public List<String> loaded = new ArrayList<>();

		public DummyLookupFileByIndex( List<String> paths ) {
			super(paths,(path,out)-> out.reshape(10,15));
			setLoader((path,out)->{loaded.add(path);out.reshape(10,15);});
		}
	}
}
