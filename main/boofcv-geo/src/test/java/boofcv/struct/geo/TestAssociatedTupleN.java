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

import boofcv.testing.StandardStructChecks;
import georegression.struct.point.Point2D_F64;
import org.ejml.MapPrintFormat;
import org.ejml.MatrixPrintFormat;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAssociatedTupleN extends StandardStructChecks {

	// Need to override since there is no default constructor
	@Override
	protected Object createNew( Class<?> type )
			throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		if (type == AssociatedTupleN.class)
			return new AssociatedTupleN(3);
		return super.createNew(type);
	}

	@Test void format_Matrix() {
		var p = new AssociatedTupleN(new Point2D_F64(1.234, 2), new Point2D_F64(3, 4));
		String found = p.format(new MatrixPrintFormat().withPrecision(2));
		assertEquals("[{1.23, 2},\n{3, 4}]", found);
	}

	@Test void formatMap() {
		var p = new AssociatedTupleN(new Point2D_F64(1.234, 2), new Point2D_F64(3, 4));
		String found = p.formatMap(new MapPrintFormat().withPrecision(2));
		assertEquals("[{x: 1.23, y: 2},\n{x: 3, y: 4}]", found);
	}
}
