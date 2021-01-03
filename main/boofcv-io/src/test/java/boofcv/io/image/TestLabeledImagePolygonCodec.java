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

import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLabeledImagePolygonCodec extends BoofStandardJUnit {
	@Test void encode_decode() throws IOException {
		List<PolygonRegion> expected = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			expected.add(createRandomRegion());
		}

		var output = new ByteArrayOutputStream();
		LabeledImagePolygonCodec.encode(expected, 100, 120, output);

		var input = new ByteArrayInputStream(output.toByteArray());
		var dimension = new ImageDimension();
		var found = new DogArray<>(PolygonRegion::new);
		LabeledImagePolygonCodec.decode(input, dimension, found);

		assertEquals(100, dimension.width);
		assertEquals(120, dimension.height);
		assertEquals(expected.size(), found.size);
		for (int i = 0; i < found.size; i++) {
			PolygonRegion e = expected.get(i);
			PolygonRegion f = found.get(i);

			assertEquals(e.regionID, f.regionID);
			assertEquals(e.polygon.size(), f.polygon.size());
			e.polygon.vertexes.forIdx(( idx, o ) -> assertEquals(0.0, o.distance(f.polygon.get(idx))));
		}
	}

	private PolygonRegion createRandomRegion() {
		var region = new PolygonRegion();
		int N = rand.nextInt(10) + 3;
		for (int i = 0; i < N; i++) {
			Point2D_F64 p = region.polygon.vertexes.grow();
			p.setTo(rand.nextGaussian(), rand.nextGaussian());
		}
		region.regionID = rand.nextInt(20);
		return region;
	}
}
