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

package boofcv.alg.feature.detect.chess;

import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDetectChessboardCornersPyramid extends GenericChessboardCornersChecks {

	@Override
	public List<ChessboardCorner> process(GrayF32 image) {
		DetectChessboardCornersPyramid<GrayF32,GrayF32> alg = new DetectChessboardCornersPyramid<>(GrayF32.class);
		alg.setPyramidTopSize(50);
		alg.process(image);
		return alg.getCorners().toList();
	}

	@Test
	void constructPyramid() {
		DetectChessboardCornersPyramid<GrayF32,GrayF32> alg = new DetectChessboardCornersPyramid<>(GrayF32.class);

		// zero is no pyramid, full resolution only
		alg.setPyramidTopSize(0);

		alg.constructPyramid(new GrayF32(500,400));
		assertEquals(1,alg.pyramid.size());
		assertEquals(500,alg.pyramid.get(0).width);
		assertEquals(400,alg.pyramid.get(0).height);

		// now it should create a pyramid
		alg.setPyramidTopSize(100);
		alg.constructPyramid(new GrayF32(500,400));
		assertEquals(3,alg.pyramid.size());
		int divisor = 1;
		for (int level = 0; level < 3; level++) {
			assertEquals(500/divisor,alg.pyramid.get(level).width);
			assertEquals(400/divisor,alg.pyramid.get(level).height);
			divisor *= 2;
		}
	}
}