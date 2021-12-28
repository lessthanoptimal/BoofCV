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

package boofcv.generate;

import java.util.Objects;

/**
 * Specifies how long and in what units something is.
 *
 * @author Peter Abeles
 */
public class LengthUnit {
	public double length;
	public Unit unit;

	public LengthUnit( String string ) {
		unit = Unit.UNKNOWN;
		for (Unit u : Unit.values()) { // lint:forbidden ignore_line
			if (string.endsWith(u.abbreviation)) {
				unit = u;
				string = string.substring(0, string.length() - u.abbreviation.length());
				break;
			}
		}
		length = Double.parseDouble(string);
	}

	public Unit getUnit() {
		return Objects.requireNonNull(unit);
	}

	public double convert( Unit target ) {
		if (unit == null)
			return length;
		else
			return unit.convert(length, target);
	}

	@Override
	public String toString() {
		if (unit == null)
			return length + "";
		else
			return length + unit.abbreviation;
	}
}
