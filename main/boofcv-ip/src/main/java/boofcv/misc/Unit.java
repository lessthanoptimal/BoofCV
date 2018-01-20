/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

/**
 * Set of standard units of measure, conversions between them, and their abbreviations
 *
 * @author Peter Abeles
 */
public enum Unit {
	MILLIMETER(0.001,"mm"),
	CENTIMETER(0.01,"cm"),
	METER(1,"m"),
	KILOMETER(1000,"km"),
	INCH(0.0254,"in"),
	FOOT(0.3048,"ft"),
	YARD(0.9144,"yd"),
	MILE(1852,"ml");

	public double unitToMeter;
	public String abbreviation;

	Unit(double unitToMeter, String abbreviation) {
		this.unitToMeter = unitToMeter;
		this.abbreviation = abbreviation;
	}

	/**
	 * Sees if the specified work matches any of the units full name or short name.
	 */
	public static Unit lookup( String word ) {
		for( Unit unit : values() ) {
			if( unit.toString().compareToIgnoreCase(word) == 0 ) {
				return unit;
			} else if( unit.getAbbreviation().compareToIgnoreCase(word) == 0 ) {
				return unit;
			}
		}

		return null;
	}

	public double convert( double value , Unit to ) {
		return value*Unit.conversion(this,to);
	}

	public double conversion( Unit to ) {
		return Unit.conversion(this,to);
	}

	public static double conversion(Unit from , Unit to ) {
		return from.unitToMeter/to.unitToMeter;
	}

	public double getUnitToMeter() {
		return unitToMeter;
	}

	public String getAbbreviation() {
		return abbreviation;
	}
}
