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

import java.io.FileNotFoundException;

/// Generates ImplImageStatistics, which has the row helpers that ImplImageStatistics_MT also uses
public class GenerateImplImageStatistics extends ImageStatisticsCodeBase {

	@Override protected void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import java.util.Arrays;\n" +
				"\n" +
				"/**\n" +
				" * Computes statistical properties of pixels inside an image. Each function's inner loop over a row is\n" +
				" * in a helper which {@link ImplImageStatistics_MT} shares.\n" +
				generateDocString() +
				"public class " + className + " {\n\n");
	}

	@Override protected void printRowHelpers() {
		for (Select s : SELECTS) {
			printSelectRowHelper(s);
		}
		for (String name : DIFFERENCES) {
			printDifferenceRowHelper(name, isSquared(name));
		}
		printSumRowHelper("sumRow", value("data", "index"));
		if (input.isSigned())
			printSumRowHelper("sumAbsRow", "Math.abs(" + value("data", "index") + ")");
		printVarianceRowHelper();
		printHistogramRowHelpers();
	}

	@Override protected void printSelect( Select select, String row ) {
		String sumType = input.getSumType();
		out.print("\t\t" + sumType + " output = initial;\n" +
				"\t\tfor (int y = 0; y < rows; y++) {\n" +
				"\t\t\toutput = " + row + "(array, startIndex + y*stride, columns, output);\n" +
				"\t\t}\n" +
				"\t\treturn output;\n");
	}

	@Override protected void printRowTotals( String totalType, String[] rowStart, String rowTotal ) {
		out.print("\t\t" + totalType + " total = 0;\n" +
				"\t\tfor (int y = 0; y < rows; y++) {\n" +
				statements(rowStart, "\t\t\t") +
				"\t\t\ttotal += " + rowTotal + ";\n" +
				"\t\t}\n");
	}

	@Override protected void printIntegerVariance() {
		out.print(varianceRows("\t\t", "0", "rows", ""));
	}

	@Override protected void printHistogram( String rowCall ) {
		out.print("\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\t" + rowCall.replace("HIST", "histogram") + ";\n" +
				"\t\t}\n");
	}

	private void printSelectRowHelper( Select s ) {
		String sumType = input.getSumType();
		out.print("\tstatic " + sumType + " " + helper(s.name + "Row") + "( " + input.getDataType() + "[] array, int index, int columns, " + sumType + " output ) {\n" +
				"\t\tfinal int end = index + columns;\n" +
				"\t\tfor (; index < end; index++) {\n" +
				"\t\t\t" + sumType + " v = " + selectValue(s, "index") + ";\n" +
				"\t\t\tif (" + s.conditional + ")\n" +
				"\t\t\t\toutput = v;\n" +
				"\t\t}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printDifferenceRowHelper( String name, boolean squared ) {
		String dataType = input.getDataType();
		String rowType = rowType(squared);
		String diffType = !input.isInteger() ? input.getSumType() : input.getNumBits() >= 32 ? "long" : "int";
		String toLong = input.isInteger() && input.getNumBits() == 32 ? "(long)" : "";
		String operation = squared ? square("difference", rowType) : "Math.abs(difference)";

		out.print("\tstatic " + rowType + " " + helper(name + "Row") + "( " + dataType + "[] dataA, int indexA, " + dataType + "[] dataB, int indexB, int columns ) {\n" +
				"\t\tfinal int end = indexA + columns;\n" +
				"\t\t" + rowType + " total = 0;\n" +
				"\t\tfor (; indexA < end; indexA++, indexB++) {\n" +
				"\t\t\t" + diffType + " difference = " + toLong + value("dataA", "indexA") + " - " + value("dataB", "indexB") + ";\n" +
				"\t\t\ttotal += " + operation + ";\n" +
				"\t\t}\n" +
				"\t\treturn total;\n" +
				"\t}\n\n");
	}

	private void printSumRowHelper( String name, String element ) {
		String rowType = rowType(false);
		out.print("\tstatic " + rowType + " " + helper(name) + "( " + input.getDataType() + "[] data, int index, int columns ) {\n" +
				"\t\tfinal int end = index + columns;\n" +
				"\t\t" + rowType + " total = 0;\n" +
				"\t\tfor (; index < end; index++) {\n" +
				"\t\t\ttotal += " + element + ";\n" +
				"\t\t}\n" +
				"\t\treturn total;\n" +
				"\t}\n\n");
	}

	/// Floating point rows return the sum of squared differences from the mean. Integer variance sums its rows inline
	private void printVarianceRowHelper() {
		if (input.isInteger())
			return;
		String dataType = input.getDataType();
		String sumType = input.getSumType();
		out.print("\tstatic " + sumType + " " + helper("varianceRow") + "( " + dataType + "[] data, int index, int columns, " + sumType + " mean ) {\n" +
				"\t\tfinal int end = index + columns;\n" +
				"\t\t" + sumType + " total = 0;\n" +
				"\t\tfor (; index < end; index++) {\n" +
				"\t\t\t" + sumType + " d = data[index] - mean;\n" +
				"\t\t\ttotal += d*d;\n" +
				"\t\t}\n" +
				"\t\treturn total;\n" +
				"\t}\n\n");
	}

	/// Histogram bin of the element at 'index'
	private String histogramBin( String scale ) {
		String v = input.isInteger() && input.getNumBits() != 64 ? value("data", "index") : "data[index]";
		if (scale.isEmpty())
			return input.isInteger() && input.getNumBits() != 64 ? v + " - minValue" : "(int)(" + v + " - minValue)";
		return "(int)(histLength*(" + v + " - minValue)/rangeValue)";
	}

	private void printHistogramRowHelpers() {
		String sumType = input.getSumType();
		String dataType = input.getDataType();
		out.print("\tstatic void " + helper("histogramRow") + "( " + dataType + "[] data, int index, int columns, " + sumType + " minValue, int[] histogram ) {\n" +
				"\t\tfinal int end = index + columns;\n" +
				"\t\tfor (; index < end; index++) {\n" +
				"\t\t\thistogram[" + histogramBin("") + "]++;\n" +
				"\t\t}\n" +
				"\t}\n\n");
		out.print("\tstatic void " + helper("histogramScaledRow") + "( " + dataType + "[] data, int index, int columns, " + sumType + " minValue,\n" +
				"\t\t\t\t\t\t\t\t\t\t" + sumType + " histLength, " + sumType + " rangeValue, int[] histogram ) {\n" +
				"\t\tfinal int end = index + columns;\n" +
				"\t\tfor (; index < end; index++) {\n" +
				"\t\t\thistogram[" + histogramBin("scaled") + "]++;\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var gen = new GenerateImplImageStatistics();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generate();
	}
}
