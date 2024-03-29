/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to specify the size of different standard pieces of paper
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PaperSize {
	public static PaperSize FIT_CONTENT = new PaperSize("CONTENT", 0, 0, Unit.UNKNOWN);
	public static PaperSize A0 = new PaperSize("A0", 841, 1189, Unit.MILLIMETER);
	public static PaperSize A1 = new PaperSize("A1", 594, 841, Unit.MILLIMETER);
	public static PaperSize A2 = new PaperSize("A2", 420, 594, Unit.MILLIMETER);
	public static PaperSize A3 = new PaperSize("A3", 297, 420, Unit.MILLIMETER);
	public static PaperSize A4 = new PaperSize("A4", 210, 297, Unit.MILLIMETER);
	public static PaperSize LEGAL = new PaperSize("LEGAL", 8.5, 14.0, Unit.INCH);
	public static PaperSize LETTER = new PaperSize("LETTER", 8.5, 11.0, Unit.INCH);

	public PaperSize( String name, double width, double height, Unit unit ) {
		this(width, height, unit);
		this.name = name;
	}

	public PaperSize( double width, double height, Unit unit ) {
		this.width = width;
		this.height = height;
		this.unit = unit;
	}

	/** What the name of this document is */
	@Getter public String name;

	/** Units the size is specified in */
	@Getter public Unit unit;

	/** Length of the document along x-axis */
	@Getter public double width;

	/** Length of the document along y-axis */
	@Getter public double height;

	public static List<PaperSize> values() {
		return values;
	}

	private static final List<PaperSize> values = new ArrayList<>();

	static {
		values.add(A0);
		values.add(A1);
		values.add(A2);
		values.add(A3);
		values.add(A4);
		values.add(LEGAL);
		values.add(LETTER);
	}

	/** Sees if the specified string matches any of the document's name. */
	public static @Nullable PaperSize lookup( String word ) {
		for (int i = 0; i < values.size(); i++) {
			PaperSize paper = values.get(i);
			if (paper.name.compareToIgnoreCase(word) == 0) {
				return paper;
			}
		}

		return null;
	}

	/** Returns the document's width in the specified units */
	public double convertWidth( Unit outputUnit ) {
		return this.unit.convert(width, outputUnit);
	}

	/** Returns the document's height in the specified units */
	public double convertHeight( Unit outputUnit ) {
		return this.unit.convert(height, outputUnit);
	}

	@Override
	public String toString() {
		String output = "";
		if (name != null)
			output += name + " ";
		output += String.format("%4.1f x %4.1f", width, height);
		if (unit != null)
			output += "  " + unit.abbreviation;
		return output;
	}
}
