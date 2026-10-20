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

package boofcv.generate;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TestUnit extends BoofStandardJUnit {
	@Test void scales() {
		assertEquals(1000.0, Unit.MILLIMETER.convert(1, Unit.MICROMETER), 1e-9);
		assertEquals(5280.0, Unit.MILE.convert(1, Unit.FOOT), 1e-9);
		assertEquals(1000.0, Unit.INCH.convert(1, Unit.MIL), 1e-9);
		assertEquals(2.54, Unit.INCH.convert(1, Unit.CENTIMETER), 1e-12);
	}

	@Test void lookup() {
		assertEquals(Unit.MILE, Unit.lookup("mi"));
		assertEquals(Unit.MILE, Unit.lookup("MILE"));
		assertEquals(Unit.MICROMETER, Unit.lookup("um"));
		assertEquals(Unit.UNKNOWN, Unit.lookup("ml"));
	}

	@Test void valuesKnown() {
		Unit[] known = Unit.valuesKnown();
		assertEquals(Unit.values().length - 1, known.length);
		for (Unit u : known) {
			assertNotEquals(Unit.UNKNOWN, u);
		}
	}
}
