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

/// Generates ImplImageStatistics_MT. Rows are processed by the row helpers in ImplImageStatistics
public class GenerateImplImageStatistics_MT extends ImageStatisticsCodeBase {

	@Override protected void printPreamble() {
		out.print("import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.struct.image.*;\n" +
				"import org.ddogleg.struct.DogArray_I64;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"import java.util.Arrays;\n" +
				"\n" +
				"import static boofcv.alg.misc.impl.ImplImageStatistics.*;\n" +
				"\n" +
				"/**\n" +
				" * Concurrent implementation of {@link ImplImageStatistics}, with identical results. Rows are processed by\n" +
				" * its row helpers.\n" +
				generateDocString() +
				"public class " + className + " {\n\n");
	}

	@Override protected void printRowHelpers() {}

	@Override protected void printSelect( Select select, String row ) {
		out.print("\t\treturn BoofConcurrency.select_" + primitiveSuffix(input.getSumType()) + "(0, rows, initial, y ->\n" +
				"\t\t\t\t" + row + "(array, startIndex + y*stride, columns, initial), ( v, output ) -> " + select.conditional + ");\n");
	}

	@Override protected void printRowTotals( String totalType, String[] rowStart, String rowTotal ) {
		out.print("\t\tfinal " + totalType + " total = BoofConcurrency.sum_" + primitiveSuffix(totalType) + "(0, rows, y -> {\n" +
				statements(rowStart, "\t\t\t") +
				"\t\t\treturn " + rowTotal + ";\n" +
				"\t\t});\n");
	}

	@Override protected void printIntegerVariance() {
		out.print("\t\tfinal long total;\n" +
				"\t\tfinal long totalSq;\n" +
				"\t\tfinal DogArray_I64 work = BoofConcurrency.takeArray_I64();\n" +
				"\t\ttry {\n" +
				"\t\t\twork.resize(2);\n" +
				"\t\t\twork.fill(0);\n" +
				"\t\t\tfinal long[] totals = work.data;\n" +
				"\t\t\tBoofConcurrency.loopBlocks(0, rows, ( y0, y1 ) -> {\n" +
				varianceRows("\t\t\t\t", "y0", "y1", "block") +
				"\t\t\t\tsynchronized (totals) {\n" +
				"\t\t\t\t\ttotals[0] += blockTotal;\n" +
				"\t\t\t\t\ttotals[1] += blockTotalSq;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t});\n" +
				"\t\t\ttotal = totals[0];\n" +
				"\t\t\ttotalSq = totals[1];\n" +
				"\t\t} finally {\n" +
				"\t\t\tBoofConcurrency.releaseArray_I64();\n" +
				"\t\t}\n");
	}

	@Override protected void printHistogram( String rowCall ) {
		out.print("\t\tBoofConcurrency.sumBlocks(0, input.height, histogram, ( h, y0, y1 ) -> {\n" +
				"\t\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\t\t" + rowCall.replace("HIST", "h") + ";\n" +
				"\t\t\t}\n" +
				"\t\t});\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var gen = new GenerateImplImageStatistics_MT();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generate();
	}
}
