/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

public class GenerateImplConvolveMean extends CodeGeneratorBase {

	AutoTypeImage imageIn;
	AutoTypeImage imageOut;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I8);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.println("}");
	}

	public void addFunctions( AutoTypeImage imageIn, AutoTypeImage imageOut ) throws FileNotFoundException {
		this.imageIn = imageIn;
		this.imageOut = imageOut;
		printHorizontalBorder();
		printHorizontal();
		printVerticalBorder();
		printVertical();
	}

	public void printPreamble() {
		out.print(
				"import boofcv.misc.BoofMiscOps;\n" +
						"import boofcv.struct.image.*;\n" +
						"import javax.annotation.Generated;\n" +
						"import boofcv.concurrency.*;\n" +
						"import org.ddogleg.struct.DogArray_F32;\n" +
						"import org.ddogleg.struct.DogArray_F64;\n" +
						"import org.ddogleg.struct.DogArray_I32;\n" +
						"import org.jetbrains.annotations.Nullable;\n" +
						"import pabeles.concurrency.GrowArray;\n" +
						"\n" +
						"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n");

		out.print(
				"\n" +
						"/**\n" +
						" * <p>\n" +
						" * Convolves a mean filter across the image. The mean value of all the pixels are computed inside the kernel.\n" +
						" * </p>\n" +
						generateDocString("Peter Abeles") +
						"@SuppressWarnings({\"ForLoopReplaceableByForEach\",\"Duplicates\"})\n" +
						"public class " + className + " {\n\n");
	}

	public void printHorizontalBorder() {
		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String divide = imageIn.isInteger() ? typeCast + "((total + count/2)/count)" : "total/count";

		out.print("\tpublic static void horizontalBorder( " + imageIn.getSingleBandName() + " input, " + imageOut.getSingleBandName() + " output, int offset, int length ) {\n");
		out.print("\t\tfinal " + imageIn.getDataType() + "[] dataSrc = input.data;\n" +
				"\t\tfinal " + imageIn.getDataType() + "[] dataDst = output.data;\n" +
				"\n" +
				"\t\tfinal int offsetR = length - offset - 1;\n" +
				"\n" +
				"\t\tfinal int width = input.getWidth();\n" +
				"\t\tfinal int height = input.getHeight();\n");

		String body = "\t\t\tint indexDest = output.startIndex + y*output.stride;\n" +
				"\t\t\tint j = input.startIndex + y*input.stride;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\tint count = length - offset;\n" +
				"\t\t\tint jEnd = j + count;\n" +
				"\t\t\tif (offset > 0) {\n" +
				"\t\t\t\tfor (int indexSrc = j; indexSrc < jEnd; indexSrc++) {\n" +
				"\t\t\t\t\ttotal += dataSrc[indexSrc]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDest++] = " + divide + ";\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\twhile (++count < length) {\n" +
				"\t\t\t\ttotal += dataSrc[jEnd++]" + bitWise + ";\n" +
				"\t\t\t\tdataDst[indexDest++] = " + divide + ";\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tjEnd = j + width;\n" +
				"\t\t\tcount = offset + offsetR;\n" +
				"\t\t\tj += width - count;\n" +
				"\t\t\tindexDest += width - count;\n" +
				"\t\t\ttotal = 0;\n" +
				"\t\t\tif (offsetR > 0) {\n" +
				"\t\t\t\tfor (int indexSrc = j; indexSrc < jEnd; indexSrc++) {\n" +
				"\t\t\t\t\ttotal += dataSrc[indexSrc]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDest++] = " + divide + ";\n" +
				"\t\t\t}\n" +
				"\t\t\twhile (--count > offset) {\n" +
				"\t\t\t\ttotal -= dataSrc[j++]" + bitWise + ";\n" +
				"\t\t\t\tdataDst[indexDest++] = " + divide + ";\n" +
				"\t\t\t}\n";

		printParallel("y", "0", "input.height", body);
		out.print("\t}\n\n");
	}

	public void printHorizontal() {

		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String declareHalf = imageIn.isInteger() ? "\t\tfinal " + sumType + " halfDivisor = divisor/2;\n" : "";
		String divide = imageIn.isInteger() ? "(total + halfDivisor)/divisor" : "total/divisor";

		out.print("\tpublic static void horizontal( " + imageIn.getSingleBandName() + " input," + imageOut.getSingleBandName() + " output, int offset, int length ) {\n" +
				"\t\tfinal " + sumType + " divisor = length;\n" +
				declareHalf);
		String body = "";

		body += "\t\t\tint indexIn = input.startIndex + input.stride*y;\n" +
				"\t\t\tint indexOut = output.startIndex + output.stride*y + offset;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\n" +
				"\t\t\tint indexEnd = indexIn + length;\n" +
				"\t\t\t\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn++) {\n" +
				"\t\t\t\ttotal += input.data[indexIn] " + bitWise + ";\n" +
				"\t\t\t}\n" +
				"\t\t\toutput.data[indexOut++] = " + typeCast + "(" + divide + ");\n" +
				"\n" +
				"\t\t\tindexEnd = indexIn + input.width - length;\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn++) {\n" +
				"\t\t\t\ttotal -= input.data[indexIn - length] " + bitWise + ";\n" +
				"\t\t\t\ttotal += input.data[indexIn] " + bitWise + ";\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut++] = " + typeCast + "(" + divide + ");\n" +
				"\t\t\t}\n";
		printParallel("y", "0", "input.height", body);
		out.print("\t}\n\n");
	}

	public void printVerticalBorder() {
		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String divide = imageIn.isInteger() ? typeCast + "((totals[x - x0] + count/2)/count)" : "totals[x - x0]/count";

		String workType = ("DogArray_" + imageIn.getKernelType()).replace("S32", "I32");

		out.print("\tpublic static void verticalBorder( " + imageIn.getSingleBandName() + " input, " + imageOut.getSingleBandName() + " output, " +
				"int offset, int length, @Nullable GrowArray<" + workType + "> workspaces ) {\n");
		out.print("\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, " + workType + "::new);\n" +
				"\t\tfinal " + workType + " work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal " + imageIn.getDataType() + "[] dataSrc = input.data;\n" +
				"\t\tfinal " + imageIn.getDataType() + "[] dataDst = output.data;\n" +
				"\n" +
				"\t\tfinal int offsetR = length - offset - 1;\n" +
				"\n" +
				"\t\tfinal int width = input.getWidth();\n" +
				"\t\tfinal int height = input.getHeight();\n");

		String body = "";
		body += "\t\t" + sumType + "[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);\n" +
				"\n" +
				"\t\t// Image Top\n" +
				"\t\tfor (int count = length - offset; count < length; count++) {\n" +
				"\t\t\tfinal int indexInRow = input.startIndex + x0;\n" +
				"\t\t\tif (count == length - offset) {\n" +
				"\t\t\t\tint indexIn = indexInRow;\n" +
				"\n" +
				"\t\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\t\ttotals[x - x0] = dataSrc[indexIn++]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tfor (int y = 1; y < count; y++) {\n" +
				"\t\t\t\t\tindexIn = indexInRow + y*input.stride;\n" +
				"\n" +
				"\t\t\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\t\t\ttotals[x - x0] += dataSrc[indexIn++]" + bitWise + ";\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tint indexIn0 = indexInRow + (count - 1)*input.stride;\n" +
				"\t\t\t\tint end = indexIn0 + x1 - x0;\n" +
				"\t\t\t\tfor (int i = indexIn0; i < end; i++) {\n" +
				"\t\t\t\t\ttotals[i - indexIn0] += dataSrc[i]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\tint indexOut = output.startIndex + (count - (length - offset))*output.stride;\n" +
				"\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\tdataDst[indexOut + x] = " + divide + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t// Image Bottom\n" +
				"\t\tfor (int yStart = height - length + 1; yStart < height - offset; yStart++) {\n" +
				"\t\t\tfinal int indexInRow = input.startIndex + x0;\n" +
				"\t\t\tif (yStart == height - length + 1) {\n" +
				"\t\t\t\tint indexIn = indexInRow + yStart*input.stride;\n" +
				"\n" +
				"\t\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\t\ttotals[x - x0] = dataSrc[indexIn++]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tfor (int y = yStart + 1; y < height; y++) {\n" +
				"\t\t\t\t\tindexIn = indexInRow + y*input.stride;\n" +
				"\n" +
				"\t\t\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\t\t\ttotals[x - x0] += dataSrc[indexIn++]" + bitWise + ";\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tint indexIn0 = indexInRow + (yStart - 1)*input.stride;\n" +
				"\t\t\t\tint indexIn1 = indexIn0 + x1 - x0;\n" +
				"\n" +
				"\t\t\t\tfor (int i = indexIn0; i < indexIn1; i++) {\n" +
				"\t\t\t\t\ttotals[i - indexIn0] -= dataSrc[i]" + bitWise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tint count = height - yStart;\n" +
				"\t\t\tint indexOut = output.startIndex + (yStart + offset)*output.stride;\n" +
				"\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\tdataDst[indexOut + x] = " + divide + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n";

		printParallelBlock("x0", "x1", "0", "input.width", "length", body);
		out.print("\t}\n\n");
	}

	public void printVertical() {
		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String declareHalf = imageIn.isInteger() ? "\t\tfinal " + sumType + " halfDivisor = divisor/2;\n" : "";
		String divide = imageIn.isInteger() ? "(total + halfDivisor)/divisor" : "total/divisor";

		String workType = ("DogArray_" + imageIn.getKernelType()).replace("S32", "I32");

		out.print("\tpublic static void vertical( " + imageIn.getSingleBandName() + " input, " +
				imageOut.getSingleBandName() + " output, int offset, int length, @Nullable GrowArray<" + workType + "> workspaces ) {\n" +
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, " + workType + "::new);\n" +
				"\t\tfinal " + workType + " work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal int backStep = length*input.stride;\n" +
				"\t\tfinal int offsetEnd = length - offset - 1;\n" +
				"\n" +
				"\t\tfinal " + sumType + " divisor = length;\n" +
				declareHalf +
				"\n" +
				"\t\t// To reduce cache misses it is processed along rows instead of going down columns, which is\n" +
				"\t\t// more natural for a vertical convolution. For parallel processes this requires building\n" +
				"\t\t// a book keeping array for each thread.\n");

		String body = "";

		body += "\t\t" + sumType + "[] totals = BoofMiscOps.checkDeclare(work, input.width, false);\n" +
				"\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\tint indexIn = input.startIndex + (y0 - offset)*input.stride + x;\n" +
				"\t\t\tint indexOut = output.startIndex + output.stride*y0 + x;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\tint indexEnd = indexIn + input.stride*length;\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn += input.stride) {\n" +
				"\t\t\t\ttotal += input.data[indexIn] " + bitWise + ";\n" +
				"\t\t\t}\n" +
				"\t\t\ttotals[x] = total;\n" +
				"\t\t\toutput.data[indexOut] = " + typeCast + "(" + divide + ");\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// change the order it is processed in to reduce cache misses\n" +
				"\t\tfor (int y = y0 + 1; y < y1; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + (y + offsetEnd)*input.stride;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++, indexIn++, indexOut++) {\n" +
				"\t\t\t\t" + sumType + " total = totals[x] - (input.data[indexIn - backStep]" + bitWise + ");\n" +
				"\t\t\t\ttotals[x] = total += input.data[indexIn]" + bitWise + ";\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut] = " + typeCast + "(" + divide + ");\n" +
				"\t\t\t}\n" +
				"\t\t}\n";

		printParallelBlock("y0", "y1", "offset", "output.height - offsetEnd", "length", body);

		out.print("\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var generator = new GenerateImplConvolveMean();
		generator.setModuleName("boofcv-ip");
		generator.generate();
	}
}
