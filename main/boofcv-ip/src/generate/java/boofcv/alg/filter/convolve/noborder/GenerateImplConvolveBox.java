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

package boofcv.alg.filter.convolve.noborder;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/// Generates ImplConvolveBox. Functions take a range of rows, so ImplConvolveBox_MT can call them on each block
public class GenerateImplConvolveBox extends CodeGeneratorBase {

	AutoTypeImage imageIn;
	AutoTypeImage imageOut;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U8, AutoTypeImage.S32);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.S32, AutoTypeImage.S32);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.println("}");
	}

	public void addFunctions( AutoTypeImage imageIn, AutoTypeImage imageOut ) {
		this.imageIn = imageIn;
		this.imageOut = imageOut;
		printHorizontal();
		printVertical();
	}

	public void printPreamble() {
		autoSelectName();
		out.print("import boofcv.struct.image.*;\n" +
				"import org.ddogleg.struct.DogArray;\n" +
				"import org.ddogleg.struct.DogArray_F32;\n" +
				"import org.ddogleg.struct.DogArray_F64;\n" +
				"import org.ddogleg.struct.DogArray_I16;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * Convolves a box filter across an image. A box filter is equivalent to convolving a kernel with all 1's.\n" +
				" * Pixels outside the image are zero. Each function also has a version for a range of rows, which\n" +
				" * {@link ImplConvolveBox_MT} calls on each block.\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\", \"Duplicates\"})\n" +
				"public class " + className + " {\n\n" +
				"\t/// Floating point running sums along y start over every VERTICAL_RESTART rows, so the results don't depend\n" +
				"\t/// on how rows are split between threads. Fewer rows bound the error of the running sums more tightly but\n" +
				"\t/// recompute more sums\n" +
				"\tpublic static int VERTICAL_RESTART = 64;\n" +
				"\n" +
				"\t/// Number of blocks of VERTICAL_RESTART inner rows\n" +
				"\tpublic static int restartBlocks( int height, int radius ) {\n" +
				"\t\treturn Math.max(1, (height - 2*radius + VERTICAL_RESTART - 1)/VERTICAL_RESTART);\n" +
				"\t}\n" +
				"\n" +
				"\t/// First output row of the block. Block 0 includes the top border and the last block the bottom border\n" +
				"\tpublic static int restartRow( int block, int blocks, int height, int radius ) {\n" +
				"\t\tif (block == 0)\n" +
				"\t\t\treturn 0;\n" +
				"\t\tif (block >= blocks)\n" +
				"\t\t\treturn height;\n" +
				"\t\treturn radius + block*VERTICAL_RESTART;\n" +
				"\t}\n\n");
	}

	/// Primitive type of the vertical pass's row buffers. Outputs stored as short keep short sums, since the low 16
	/// bits of a sum don't depend on overflow
	static String bufferType( AutoTypeImage imageOut, AutoTypeImage imageIn ) {
		return imageOut.getDataType().equals("short") ? "short" : imageIn.getSumType();
	}

	static String bufferArray( String bufferType ) {
		return switch (bufferType) {
			case "short" -> "DogArray_I16";
			case "int" -> "DogArray_I32";
			case "float" -> "DogArray_F32";
			case "double" -> "DogArray_F64";
			default -> throw new IllegalArgumentException(bufferType);
		};
	}

	public void printHorizontal() {
		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();
		String nameIn = imageIn.getSingleBandName(), nameOut = imageOut.getSingleBandName();

		out.print("\t/// Box sum along x. Pixels outside the image are zero\n" +
				"\tpublic static void horizontal( " + nameIn + " input, " + nameOut + " output, int radius ) {\n" +
				"\t\thorizontal(input, output, radius, 0, input.height);\n" +
				"\t}\n\n");

		out.print("\t/// horizontal() of the rows y0 to y1\n" +
				"\tpublic static void horizontal( " + nameIn + " input, " + nameOut + " output, int radius, int y0, int y1 ) {\n" +
				"\t\tfinal int kernelWidth = radius*2 + 1;\n" +
				"\t\tfinal int width = input.width;\n" +
				"\t\tfinal int strideIn = input.stride, strideOut = output.stride;\n" +
				"\n" +
				"\t\t// Border: sum of the pixels inside the image\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tfinal int indexIn = input.startIndex + y*strideIn;\n" +
				"\t\t\tfinal int indexOut = output.startIndex + y*strideOut;\n" +
				"\t\t\tfor (int x = 0; x < width; x++) {\n" +
				"\t\t\t\tif (x >= radius && x < width - radius) {\n" +
				"\t\t\t\t\tx = width - radius - 1;\n" +
				"\t\t\t\t\tcontinue;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tfinal int end = Math.min(width, x + radius + 1);\n" +
				"\t\t\t\tfor (int i = Math.max(0, x - radius); i < end; i++) {\n" +
				"\t\t\t\t\ttotal += input.data[indexIn + i]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.data[indexOut + x] = " + typeCast + "total;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tif (kernelWidth > width)\n" +
				"\t\t\treturn;\n" +
				"\n");
		if (imageIn.isInteger())
			printHorizontalInnerRow(typeCast, sumType, bitWise);
		else
			printHorizontalInnerTwoRows(sumType);
		out.print("\t}\n\n");
	}

	/// Inner image, one row at a time
	private void printHorizontalInnerRow( String typeCast, String sumType, String bitWise ) {
		out.print("\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + strideIn*y;\n" +
				"\t\t\tint indexOut = output.startIndex + strideOut*y + radius;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\tint indexEnd = indexIn + kernelWidth;\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn++) {\n" +
				"\t\t\t\ttotal += input.data[indexIn]" + bitWise + ";\n" +
				"\t\t\t}\n" +
				"\t\t\toutput.data[indexOut++] = " + typeCast + "total;\n" +
				"\n" +
				"\t\t\tindexEnd = indexIn + width - kernelWidth;\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn++) {\n" +
				"\t\t\t\ttotal -= input.data[indexIn - kernelWidth]" + bitWise + ";\n" +
				"\t\t\t\ttotal += input.data[indexIn]" + bitWise + ";\n" +
				"\t\t\t\toutput.data[indexOut++] = " + typeCast + "total;\n" +
				"\t\t\t}\n" +
				"\t\t}\n");
	}

	/// Inner image of floating point images, two rows at a time. total += added - removed puts one addition per pixel
	/// on the dependency chain, and the two rows' chains run in parallel
	private void printHorizontalInnerTwoRows( String sumType ) {
		out.print("\t\tint y = y0;\n" +
				"\t\tfor (; y + 2 <= y1; y += 2) {\n" +
				"\t\t\tint indexIn = input.startIndex + strideIn*y;\n" +
				"\t\t\tint indexOut = output.startIndex + strideOut*y + radius;\n" +
				"\n" +
				"\t\t\t" + sumType + " total0 = 0, total1 = 0;\n" +
				"\t\t\tfor (int i = indexIn; i < indexIn + kernelWidth; i++) {\n" +
				"\t\t\t\ttotal0 += input.data[i];\n" +
				"\t\t\t\ttotal1 += input.data[i + strideIn];\n" +
				"\t\t\t}\n" +
				"\t\t\toutput.data[indexOut] = total0;\n" +
				"\t\t\toutput.data[indexOut + strideOut] = total1;\n" +
				"\n" +
				"\t\t\tfinal int indexEnd = indexIn + width;\n" +
				"\t\t\tfor (int i = indexIn + kernelWidth; i < indexEnd; i++) {\n" +
				"\t\t\t\tindexOut++;\n" +
				"\t\t\t\ttotal0 += input.data[i] - input.data[i - kernelWidth];\n" +
				"\t\t\t\ttotal1 += input.data[i + strideIn] - input.data[i + strideIn - kernelWidth];\n" +
				"\t\t\t\toutput.data[indexOut] = total0;\n" +
				"\t\t\t\toutput.data[indexOut + strideOut] = total1;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tif (y < y1) {\n" +
				"\t\t\tint indexIn = input.startIndex + strideIn*y;\n" +
				"\t\t\tint indexOut = output.startIndex + strideOut*y + radius;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\tfor (int i = indexIn; i < indexIn + kernelWidth; i++) {\n" +
				"\t\t\t\ttotal += input.data[i];\n" +
				"\t\t\t}\n" +
				"\t\t\toutput.data[indexOut] = total;\n" +
				"\n" +
				"\t\t\tfinal int indexEnd = indexIn + width;\n" +
				"\t\t\tfor (int i = indexIn + kernelWidth; i < indexEnd; i++) {\n" +
				"\t\t\t\ttotal += input.data[i] - input.data[i - kernelWidth];\n" +
				"\t\t\t\toutput.data[++indexOut] = total;\n" +
				"\t\t\t}\n" +
				"\t\t}\n");
	}

	public void printVertical() {
		String nameIn = imageIn.getSingleBandName(), nameOut = imageOut.getSingleBandName();
		String bitWise = imageIn.getBitWise();
		String buffer = bufferType(imageOut, imageIn);
		String rowsType = "DogArray<" + bufferArray(buffer) + ">";
		String outData = imageOut.getDataType();
		// explicit casts, since compound assignments to short hide the narrowing
		String castOut = outData.equals("short") ? "(short)" : "";
		String castBuffer = buffer.equals("short") ? "(short)" : "";
		// U16 is copied into short buffers too: the low 16 bits of the sum are the same for signed values
		boolean copyInput = imageIn.getDataType().equals(buffer);
		String convert = buffer.equals("short") ? "(short)(input.data[indexIn + i]" + bitWise + ")" : "input.data[indexIn + i]" + bitWise;

		String update;
		if (buffer.equals("short"))
			update = "\t\t\t\ttotals[i] = (short)(totals[i] - removed[i] + added[i]);\n";
		else if (imageIn.isInteger())
			update = "\t\t\t\ttotals[i] = totals[i] - removed[i] + added[i];\n";
		else
			update = "\t\t\t\t" + buffer + " total = totals[i] - removed[i];\n" +
					"\t\t\t\ttotals[i] = total + added[i];\n";

		out.print("\t/// Box sum along y. Pixels outside the image are zero\n" +
				"\t///\n" +
				"\t/// @param rows Work space for the row buffers\n" +
				"\tpublic static void vertical( " + nameIn + " input, " + nameOut + " output, int radius, " + rowsType + " rows ) {\n" +
				"\t\tvertical(input, output, radius, 0, input.height, rows);\n" +
				"\t}\n\n");

		out.print("\t/// vertical() of the output rows y0 to y1. " + (imageIn.isInteger() ?
				"The running sums start at the first inner row." :
				"The running sums start at y0 and every VERTICAL_RESTART rows, see restartRow()") + "\n" +
				"\tpublic static void vertical( " + nameIn + " input, " + nameOut + " output, int radius, int y0, int y1,\n" +
				"\t\t\t\t\t\t\t\t " + rowsType + " rows ) {\n" +
				"\t\tfinal int width = input.width, height = input.height;\n" +
				"\t\tfinal " + outData + "[] dataOut = output.data;\n" +
				"\n" +
				"\t\t// Border: sum of the rows inside the image\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tif (y >= radius && y < height - radius) {\n" +
				"\t\t\t\ty = height - radius - 1;\n" +
				"\t\t\t\tcontinue;\n" +
				"\t\t\t}\n" +
				"\t\t\tfinal int indexOut = output.startIndex + y*output.stride;\n" +
				"\t\t\tfor (int i = 0; i < width; i++) {\n" +
				"\t\t\t\tdataOut[indexOut + i] = 0;\n" +
				"\t\t\t}\n" +
				"\t\t\tfinal int end = Math.min(height, y + radius + 1);\n" +
				"\t\t\tfor (int k = Math.max(0, y - radius); k < end; k++) {\n" +
				"\t\t\t\tfinal int indexIn = input.startIndex + k*input.stride;\n" +
				"\t\t\t\tfor (int i = 0; i < width; i++) {\n" +
				"\t\t\t\t\tdataOut[indexOut + i] = " + castOut + "(dataOut[indexOut + i] + (input.data[indexIn + i]" + bitWise + "));\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfinal int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);\n");
		if (imageIn.isInteger())
			out.print("\t\tif (inner0 < inner1)\n" +
					"\t\t\tverticalInner(input, output, radius, inner0, inner1, rows);\n");
		else
			out.print("\t\tfor (int start = inner0; start < inner1; ) {\n" +
					"\t\t\t// next multiple of VERTICAL_RESTART rows after the first inner row\n" +
					"\t\t\tint end = radius + ((start - radius)/VERTICAL_RESTART + 1)*VERTICAL_RESTART;\n" +
					"\t\t\tend = Math.min(end, inner1);\n" +
					"\t\t\tverticalInner(input, output, radius, start, end, rows);\n" +
					"\t\t\tstart = end;\n" +
					"\t\t}\n");
		out.print("\t}\n\n");

		out.print("\t/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a\n" +
				"\t/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized\n" +
				"\tprivate static void verticalInner( " + nameIn + " input, " + nameOut + " output, int radius, int y0, int y1,\n" +
				"\t\t\t\t\t\t\t\t\t   " + rowsType + " rows ) {\n" +
				"\t\tfinal int kernelWidth = radius*2 + 1;\n" +
				"\t\tfinal int n = input.width;\n" +
				"\n" +
				"\t\t// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals\n" +
				"\t\tfinal int first = y0 - radius;\n" +
				"\t\tfinal int ringSize = kernelWidth + 1;\n" +
				"\t\trows.resize(ringSize + 1);\n" +
				"\t\tfor (int k = 0; k < rows.size; k++) {\n" +
				"\t\t\trows.get(k).resize(n);\n" +
				"\t\t}\n" +
				"\t\tfinal " + buffer + "[] totals = rows.get(ringSize).data;\n" +
				"\n" +
				"\t\tfor (int k = 0; k < kernelWidth; k++) {\n" +
				"\t\t\tcopyRow(input, n, first + k, rows.get(k).data);\n" +
				"\t\t}\n" +
				"\t\tSystem.arraycopy(rows.get(0).data, 0, totals, 0, n);\n" +
				"\t\tfor (int k = 1; k < kernelWidth; k++) {\n" +
				"\t\t\tfinal " + buffer + "[] row = rows.get(k).data;\n" +
				"\t\t\tfor (int i = 0; i < n; i++) {\n" +
				"\t\t\t\ttotals[i] = " + castBuffer + "(totals[i] + row[i]);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tSystem.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);\n" +
				"\n" +
				"\t\tfor (int y = y0 + 1; y < y1; y++) {\n" +
				"\t\t\tfinal " + buffer + "[] removed = rows.get((y - radius - 1 - first)%ringSize).data;\n" +
				"\t\t\tfinal " + buffer + "[] added = rows.get((y + radius - first)%ringSize).data;\n" +
				"\t\t\tcopyRow(input, n, y + radius, added);\n" +
				"\t\t\tfor (int i = 0; i < n; i++) {\n" +
				update +
				"\t\t\t}\n" +
				"\t\t\tSystem.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);\n" +
				"\t\t}\n" +
				"\t}\n\n");

		// One copyRow() for each input type and buffer type
		String key = nameIn + buffer;
		if (printedCopyRow.contains(key))
			return;
		printedCopyRow.add(key);
		out.print("\t/// Copies the first n pixels of row y into 'row'\n" +
				"\tprivate static void copyRow( " + nameIn + " input, int n, int y, " + buffer + "[] row ) {\n" +
				"\t\tfinal int indexIn = input.startIndex + y*input.stride;\n");
		if (copyInput)
			out.print("\t\tSystem.arraycopy(input.data, indexIn, row, 0, n);\n");
		else
			out.print("\t\tfor (int i = 0; i < n; i++) {\n" +
					"\t\t\trow[i] = " + convert + ";\n" +
					"\t\t}\n");
		out.print("\t}\n\n");
	}

	java.util.Set<String> printedCopyRow = new java.util.HashSet<>();

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplConvolveBox generator = new GenerateImplConvolveBox();
		generator.setModuleName("boofcv-ip");
		generator.generateCode();
	}
}
