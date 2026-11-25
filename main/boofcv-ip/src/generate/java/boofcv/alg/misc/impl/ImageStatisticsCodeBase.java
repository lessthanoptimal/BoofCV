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

package boofcv.alg.misc.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;
import boofcv.generate.ImageFamily;

import java.io.FileNotFoundException;

/// Code shared by the generators of ImplImageStatistics and ImplImageStatistics_MT. Functions are declared here and
/// their bodies are printed by each generator. The row helpers in ImplImageStatistics hold the inner loops.
public abstract class ImageStatisticsCodeBase extends CodeGeneratorBase {

	protected AutoTypeImage input;

	/// A function which selects one value, e.g. the minimum
	protected static class Select {
		final String name;
		// Condition for 'v' to replace 'output'
		final String conditional;
		// If the absolute value of signed values is selected
		final boolean abs;

		Select( String name, String conditional, boolean abs ) {
			this.name = name;
			this.conditional = conditional;
			this.abs = abs;
		}
	}

	protected static final Select[] SELECTS = new Select[]{
			new Select("min", "v < output", false),
			new Select("max", "v > output", false),
			new Select("minAbs", "v < output", true),
			new Select("maxAbs", "v > output", true)};

	/// Mean of the squared or absolute differences between two images
	protected static final String[] DIFFERENCES = new String[]{"meanDiffSq", "meanDiffAbs"};

	protected static boolean isSquared( String difference ) {
		return difference.equals("meanDiffSq");
	}

	protected static final String[] IMAGE_ROW = new String[]{"int index = img.startIndex + y*img.stride;"};

	@Override public void generateCode() throws FileNotFoundException {
		printPreamble();
		for (AutoTypeImage t : AutoTypeImage.getSpecificTypes()) {
			input = t;
			printRowHelpers();
			printAll();
		}
		out.println("}");
	}

	protected abstract void printPreamble();

	/// Prints the row helpers of the current type
	protected abstract void printRowHelpers();

	/// Body of a select function, after 'initial' has been declared
	///
	/// @param row Name of the row helper
	protected abstract void printSelect( Select select, String row );

	/// Body which adds up the row totals in 'total'
	///
	/// @param totalType Type of 'total'
	/// @param rowStart Statements at the start of row 'y'
	/// @param rowTotal Expression of row 'y' total
	protected abstract void printRowTotals( String totalType, String[] rowStart, String rowTotal );

	/// Body of an integer variance function which sums the values in 'total' and their squares in 'totalSq'
	protected abstract void printIntegerVariance();

	/// Body of a histogram function after the histogram has been zeroed
	///
	/// @param rowCall Statement which counts row 'y' into the histogram called HIST
	protected abstract void printHistogram( String rowCall );

	private void printAll() {
		for (Select s : SELECTS) {
			printSelectFunction(s);
		}
		for (String name : DIFFERENCES) {
			printDifference(name);
		}
		for (ImageFamily f : new ImageFamily[]{ImageFamily.GRAY, ImageFamily.INTERLEAVED}) {
			printSum(f, "sum", "sumRow");
			if (input.isSigned()) {
				printSum(f, "sumAbs", "sumAbsRow");
			}
		}
		printVariance();
		printHistogramFunctions();
	}

	/// Suffix of the BoofConcurrency function for a primitive type, e.g. I32 for int
	protected static String primitiveSuffix( String type ) {
		switch (type) {
			case "int": return "I32";
			case "long": return "I64";
			case "float": return "F32";
			case "double": return "F64";
			default: throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/// Name of a row helper for the current image type
	protected String helper( String name ) {
		return name + "_" + input.getAbbreviatedType();
	}

	/// Type a row's total is summed in. Integer rows use an int when the row can't overflow it
	protected String rowType( boolean squared ) {
		if (!input.isInteger())
			return input.getSumType();
		if (input.getNumBits() >= 32)
			return "long";
		if (squared && !(input.getNumBits() == 8 && !input.isSigned()))
			return "long";
		return "int";
	}

	/// Type of the image's total. Integer totals are a long so that they don't overflow
	protected String totalType() {
		return input.isInteger() ? "long" : input.getSumType();
	}

	/// Value of an element, with unsigned integers converted to their positive value
	protected String value( String array, String index ) {
		String bitWise = input.getBitWise();
		return bitWise.isEmpty() ? array + "[" + index + "]" : "(" + array + "[" + index + "] " + bitWise + ")";
	}

	/// Value of an element which a select function compares
	protected String selectValue( Select select, String index ) {
		if (select.abs && input.isSigned())
			return "Math.abs(array[" + index + "])";
		return "array[" + index + "] " + input.getBitWise();
	}

	/// Square of an integer which is promoted to a long first if the row is summed in a long
	protected String square( String v, String rowType ) {
		boolean promote = input.isInteger() && input.getNumBits() < 64 && rowType.equals("long");
		return (promote ? "(long)" : "") + v + "*" + v;
	}

	protected static String statements( String[] lines, String indent ) {
		var builder = new StringBuilder();
		for (String line : lines) {
			builder.append(indent).append(line).append('\n');
		}
		return builder.toString();
	}

	/// Sums of the values and of their squares in rows 'y0' to 'y1', declared as 'total' and 'totalSq' with
	/// 'prefix' in front, e.g. "block" for blockTotal
	protected String varianceRows( String indent, String y0, String y1, String prefix ) {
		String total = prefix.isEmpty() ? "total" : prefix + "Total";
		String totalSq = total + "Sq";
		String rowType = rowType(false);
		String rowTypeSq = rowType(true);
		String valueType = input.getNumBits() == 64 ? "long" : "int";
		String i = indent;
		return i + "long " + total + " = 0;\n" +
				i + "long " + totalSq + " = 0;\n" +
				i + "for (int y = " + y0 + "; y < " + y1 + "; y++) {\n" +
				i + "\tint index = img.startIndex + y*img.stride;\n" +
				i + "\tfinal int end = index + columns;\n" +
				i + "\t" + rowType + " rowTotal = 0;\n" +
				i + "\t" + rowTypeSq + " rowTotalSq = 0;\n" +
				i + "\tfor (; index < end; index++) {\n" +
				i + "\t\t" + valueType + " v = " + value("img.data", "index") + ";\n" +
				i + "\t\trowTotal += v;\n" +
				i + "\t\trowTotalSq += " + square("v", rowTypeSq) + ";\n" +
				i + "\t}\n" +
				i + "\t" + total + " += rowTotal;\n" +
				i + "\t" + totalSq + " += rowTotalSq;\n" +
				i + "}\n";
	}

	private static String imageSetup( String columns ) {
		return "\t\tfinal int rows = img.height;\n" +
				"\t\tfinal int columns = " + columns + ";\n";
	}

	private void printSelectFunction( Select s ) {
		String sumType = input.getSumType();
		String name = s.name + (input.isSigned() ? "" : "U");
		out.print("\tpublic static " + sumType + " " + name + "( " + input.getDataType() + "[] array, int startIndex, int rows, int columns, int stride ) {\n" +
				"\t\tfinal " + sumType + " initial = " + selectValue(s, "startIndex") + ";\n");
		printSelect(s, helper(s.name + "Row"));
		out.print("\t}\n\n");
	}

	/// Prints a function which adds up one total per row. Integer totals are exact. Floating point row totals are
	/// added in row order, so the concurrent result is identical to the single threaded one
	///
	/// @param declaration Function's declaration up to and including its last argument
	/// @param setup Code before the rows are summed. Must declare 'rows'
	/// @param result Returned expression of the image's 'total'
	private void printRowTotalsFunction( String declaration, String setup, String[] rowStart, String rowTotal,
										 String result ) {
		out.print(declaration + " ) {\n" + setup);
		printRowTotals(totalType(), rowStart, rowTotal);
		out.print("\t\treturn " + result + ";\n" +
				"\t}\n\n");
	}

	private void printDifference( String functionName ) {
		String dataType = input.getDataType();
		String name = functionName + (input.isSigned() ? "" : "U");
		printRowTotalsFunction("\tpublic static double " + name + "( " + dataType + "[] dataA, int startIndexA, int strideA,\n" +
						"\t\t\t\t\t\t\t\t\t" + dataType + "[] dataB, int startIndexB, int strideB,\n" +
						"\t\t\t\t\t\t\t\t\tint rows, int columns", "",
				new String[]{},
				helper(functionName + "Row") + "(dataA, startIndexA + y*strideA, dataB, startIndexB + y*strideB, columns)",
				"total/(double)(rows*columns)");
	}

	private void printSum( ImageFamily family, String name, String row ) {
		String columns = family == ImageFamily.INTERLEAVED ? "img.width*img.numBands" : "img.width";
		printRowTotalsFunction("\tpublic static " + totalType() + " " + name + "( " + input.getImageName(family) + " img",
				imageSetup(columns), IMAGE_ROW, helper(row) + "(img.data, index, columns)", "total");
	}

	/// Integer images sum the values and their squares exactly, then compute the variance around the mean from them.
	/// The rows are summed inline, since a row helper can only return one of the two totals
	private void printVariance() {
		String sumType = input.isInteger() ? "double" : input.getSumType();
		String declaration = "\tpublic static " + sumType + " variance( " + input.getSingleBandName() + " img, " +
				sumType + " mean";

		if (!input.isInteger()) {
			printRowTotalsFunction(declaration, imageSetup("img.width"), IMAGE_ROW,
					helper("varianceRow") + "(img.data, index, columns, mean)", "total/(img.width*img.height)");
			return;
		}

		out.print(declaration + " ) {\n" + imageSetup("img.width"));
		printIntegerVariance();
		out.print("\t\tfinal double N = rows*(double)columns;\n" +
				"\t\treturn (totalSq - 2.0*mean*total)/N + mean*mean;\n" +
				"\t}\n\n");
	}

	private void printHistogramFunctions() {
		String sumType = input.getSumType();
		out.print("\tpublic static void histogram( " + input.getSingleBandName() + " input, " + sumType +
				" minValue, int[] histogram ) {\n" +
				"\t\tArrays.fill(histogram, 0);\n");
		printHistogram(helper("histogramRow") + "(input.data, input.startIndex + y*input.stride, input.width, minValue, HIST)");
		out.print("\t}\n\n");

		out.print("\tpublic static void histogramScaled( " + input.getSingleBandName() + " input, " +
				sumType + " minValue, " + sumType + " maxValue, int[] histogram ) {\n" +
				"\t\tArrays.fill(histogram, 0);\n" +
				"\t\tfinal " + sumType + " histLength = histogram.length;\n" +
				"\t\tfinal " + sumType + " rangeValue = maxValue - minValue + 1;\n");
		printHistogram(helper("histogramScaledRow") + "(input.data, input.startIndex + y*input.stride, input.width, minValue, histLength, rangeValue, HIST)");
		out.print("\t}\n\n");
	}
}
