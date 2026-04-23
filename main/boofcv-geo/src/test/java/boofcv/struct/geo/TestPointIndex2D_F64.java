/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.MapPrintFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPointIndex2D_F64 extends BoofStandardJUnit {
	@Test void formatMap() {
		var a = new PointIndex2D_F64(1.2345, 2, 34);
		String found = a.formatMap(new MapPrintFormat().withPrecision(2));
		assertEquals("{p: {x: 1.23, y: 2}, index: 34}", found);
	}
}
