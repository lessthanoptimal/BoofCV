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

package boofcv.alg.filter.convolve.noborder;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * Code generator for ConvolveImageStandard_IL.
 *
 * @author Peter Abeles
 */
public class GenerateConvolveImageStandard_IL extends CodeGeneratorBase {

	String kernelType;
	String inputType;
	String outputType;
	String kernelData;
	String inputData;
	String outputData;
	String sumType;
	String typeCast;
	String bitWise;
	boolean hasDivide;

	@Override
	public void generateCode() {
		printPreamble();
		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false);
//		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false, true);
		printAllOps(AutoTypeImage.F64, AutoTypeImage.F64, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.S32, false);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I8, true, true);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.I8, true);
//		printAllOps(AutoTypeImage.U8,AutoTypeImage.I8,false, true);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, true);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I16, true);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.I16, true, true);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, false);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, true);
		out.println("}");
	}

	private void printPreamble() {
		autoSelectName();
		out.print("import boofcv.struct.convolve.*;\n" +
				"import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import java.util.Arrays;\n");
		out.print("//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n");

		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.\n" +
				" * </p>\n" +
				" * \n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using " + getClass().getSimpleName() + ".\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"@Generated({\"" + getClass().getCanonicalName() + "\"})\n" +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\",\"Duplicates\"})\n" +
				"public class " + className + " {\n\n");
	}

	private void printAllOps( AutoTypeImage input, AutoTypeImage output, boolean hasDivide ) {
		printAllOps(input, output, hasDivide, false);
	}

	private void printAllOps( AutoTypeImage input, AutoTypeImage output, boolean hasDivide,
							  boolean justVertical ) {
		typeCast = output.getTypeCastFromSum();
		kernelType = input.getKernelType();
		kernelData = input.getKernelDataType();
		inputType = input.getInterleavedName();
		outputType = output.getInterleavedName();
		inputData = input.getDataType();
		outputData = output.getDataType();
		sumType = input.getSumType();
		bitWise = input.getBitWise();
		this.hasDivide = hasDivide;

		// just for anal retentive formatting
		if (!bitWise.isEmpty())
			bitWise = " " + bitWise;

		if (justVertical) {
			if (hasDivide)
				printVertical_div();
			else
				printVertical_sum();
		} else {
			printHorizontal();
			if (hasDivide)
				printVertical_div();
			else
				printVertical_sum();
			printConvolve2D();
		}
	}

	private void printHorizontal() {
		String paramDiv = hasDivide ? " , int divisor" : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";

		out.print("\tpublic static void horizontal( Kernel1D_" + kernelType + " kernel ,\n" +
				"\t\t\t\t\t\t\t\t   " + inputType + " src, " + outputType + " dst" + paramDiv + " ) {\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\t\tfinal int numBands = src.getNumBands();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\n" +
				"\t\tfinal int endJ = src.width - (kernelWidth - 1);\n");

		String body = "";

		body += "\t\t\tint indexDst = dst.startIndex + i*dst.stride+offset*numBands;\n" +
				"\t\t\tfor (int j = 0; j < endJ; j++) {\n" +
				"\t\t\t\tint indexSrcStart = src.startIndex + i*src.stride + j*numBands;\n" +
				"\t\t\t\tfor (int band = 0; band < numBands; band++) {\n" +
				"\t\t\t\t\tint indexSrc = indexSrcStart + band;\n" +
				"\t\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\t\tfor (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {\n" +
				"\t\t\t\t\t\ttotal += (dataSrc[indexSrc]" + bitWise + ")*dataKer[k];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n";

		printParallel("i", "0", "src.height", body);

		out.print("\t}\n\n");
	}

	private void printVertical_div() {
		String paramDiv = hasDivide ? " , int divisor" : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";

		out.print("\tpublic static void vertical( Kernel1D_" + kernelType + " kernel,\n" +
				"\t\t\t\t\t\t\t\t " + inputType + " src, " + outputType + " dst" + paramDiv + " )\n" +
				"\t{\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\t\tfinal int numBands = src.getNumBands();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n" +
				"\n" +
				"\t\tfinal int yEnd = imgHeight-(kernelWidth-offset-1);\n");

		String body =
				"\t\t\tint indexDst = dst.startIndex+y*dst.stride;\n" +
						"\t\t\tint indexSrcStart = src.startIndex+(y-offset)*src.stride;\n" +
						"\n" +
						"\t\t\tfor (int x = 0; x < imgWidth; x++) {\n" +
						"\t\t\t\tfor (int band = 0; band < numBands; band++) {\n" +
						"\t\t\t\t\tint indexSrc = indexSrcStart + band;\n" +
						"\n" +
						"\t\t\t\t\t" + sumType + " total = 0;\n" +
						"\t\t\t\t\tfor (int k = 0; k < kernelWidth; k++) {\n" +
						"\t\t\t\t\t\ttotal += (dataSrc[indexSrc]" + bitWise + ")*dataKer[k];\n" +
						"\t\t\t\t\t\tindexSrc += src.stride;\n" +
						"\t\t\t\t\t}\n" +
						"\t\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
						"\t\t\t\t}\n" +
						"\t\t\t\tindexSrcStart += numBands;\n" +
						"\t\t\t}\n";

		printParallel("y", "offset", "yEnd", body);
		out.print("\t}\n\n");
	}

	private void printVertical_sum() {
		out.print("\tpublic static void vertical( Kernel1D_" + kernelType + " kernel,\n" +
				"\t\t\t\t\t\t\t\t " + inputType + " src, " + outputType + " dst )\n" +
				"\t{\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\t\tfinal int numBands = src.getNumBands();\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n" +
				"\n" +
				"\t\tfinal int yEnd = imgHeight-(kernelWidth-offset-1);\n" +
				"\t\tfinal int elementsInRow = imgWidth*numBands;\n");

		String body = "";
		body += "\t\t\tfinal int indexDstStart = dst.startIndex+y*dst.stride;\n" +
				"\t\t\tint indexSrcStart = src.startIndex + (y - offset)*src.stride;\n" +
				"\t\t\tArrays.fill(dataDst, indexDstStart, indexDstStart + elementsInRow, (" + outputData + ")0);\n" +
				"\n" +
				"\t\t\tfor (int k = 0; k < kernelWidth; k++) {\n" +
				"\t\t\t\tfinal " + kernelData + " kernelValue = dataKer[k];\n" +
				"\t\t\t\tint indexDst = indexDstStart;\n" +
				"\t\t\t\tint indexSrc = indexSrcStart;\n" +
				"\t\t\t\tint indexSrcEnd = indexSrc + elementsInRow;\n" +
				"\n" +
				"\t\t\t\twhile (indexSrc < indexSrcEnd) {\n" +
				"\t\t\t\t\tdataDst[indexDst++] += " + typeCast + "((dataSrc[indexSrc++]" + bitWise + ")*kernelValue);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tindexSrcStart += src.stride;\n" +
				"\t\t\t}\n";

		printParallel("y", "offset", "yEnd", body);
		out.print("\t}\n\n");
	}

	private void printConvolve2D() {

		String paramDiv = hasDivide ? ", int divisor " : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";
		String performBound = "";

		out.print("\tpublic static void convolve( Kernel2D_" + kernelType + " kernel , " + inputType + " src , " + outputType + " dst " + paramDiv + ")\n" +
				"\t{\n" +
				"\t\tfinal " + kernelData + "[] dataKernel = kernel.data;\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\n" +
				"\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n" +
				"\t\tfinal int numBands = src.getNumBands();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");

		out.print("\n" +
				"\t\tint offsetL = kernel.offset;\n" +
				"\t\tint offsetR = kernel.width-kernel.offset-1;\n");

		String body = "\t\t\tint indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;\n" +
				"\t\t\tfor( int x = offsetL; x < width-offsetR; x++ ) {\n" +
				"\t\t\t\tint indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;\n" +
				"\n" +
				"\t\t\t\tfor (int band = 0; band < numBands; band++) {\n" +
				"\t\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\t\tint indexKer = 0;\n" +
				"\t\t\t\t\tfor (int ki = 0; ki < kernel.width; ki++) {\n" +
				"\t\t\t\t\t\tint indexSrc = indexSrcStart+ki*src.stride + band;\n" +
				"\t\t\t\t\t\tfor (int kj = 0; kj < kernel.width; kj++) {\n" +
				"\t\t\t\t\t\t\ttotal += (dataSrc[indexSrc]" + bitWise + ")* dataKernel[indexKer++];\n" +
				"\t\t\t\t\t\t\tindexSrc += numBands;\n" +
				"\t\t\t\t\t\t}\n" +
				"\t\t\t\t\t}\n" +
				performBound +
				"\t\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n";

		printParallel("y", "offsetL", "height-offsetR", body);

		out.print("\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateConvolveImageStandard_IL gen = new GenerateConvolveImageStandard_IL();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generateCode();
	}
}
