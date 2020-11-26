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

package boofcv.alg.mvs;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDisparityParameters extends BoofStandardJUnit {
	@Test void setTo() {
		var src = new DisparityParameters();
		src.pinhole.fsetK(1,2,3,4,5,6,9);
		src.rotateToRectified.set(0,0,2);
		src.baseline = 10;
		src.disparityRange = 102;
		src.disparityMin = 8;

		var dst = new DisparityParameters();
		dst.setTo(src);

		assertTrue(src.pinhole.isEquals(dst.pinhole, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isEquals(src.rotateToRectified, dst.rotateToRectified));
		assertEquals(src.baseline, dst.baseline);
		assertEquals(src.disparityRange, dst.disparityRange);
		assertEquals(src.disparityMin, dst.disparityMin);
	}
}
