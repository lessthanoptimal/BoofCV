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

public class GenerateConvolvedUnrolled_IL extends CodeGeneratorBase {
	public static final int[] KERNEL_WIDTHS = {3, 5, 7};
	public static final int[] NUM_BANDS = {2, 3, 4};

	String kernelType;
	String inputType;
	String outputType;
	String inputData;
	String outputData;
	String kernelData;
	String sumType;
	String typeCast;
	String bitWise;
	boolean hasDivide;

	@Override
	public void generateCode() throws FileNotFoundException {
		setOutputFile("ConvolveImageUnrolled_IL");
		printPreamble();

		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false);
		printAllOps(AutoTypeImage.F64, AutoTypeImage.F64, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.S32, false);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I8, true, true);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.I8, true);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, true);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I16, true);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.I16, true, true);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, false);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, true);

		printSupport();
		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.convolve.*;\n" +
				"import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n");

		out.println("\n//CONCURRENT_CLASS_NAME ConvolveImageUnrolled_IL_MT");
		out.println("//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;");

		out.print("\n" +
				"/**\n" +
				" * <p>\n" +
				" * Unrolls convolution kernels for interleaved images with two, three, or four bands.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\",\"Duplicates\"})\n" +
				"public class " + className + " {\n");
	}

	private void printAllOps( AutoTypeImage input, AutoTypeImage output, boolean hasDivide ) {
		printAllOps(input, output, hasDivide, false);
	}

	private void printAllOps( AutoTypeImage input, AutoTypeImage output, boolean hasDivide, boolean justVertical ) {
		kernelType = input.getKernelType();
		inputType = input.getInterleavedName();
		outputType = output.getInterleavedName();
		inputData = input.getDataType();
		outputData = output.getDataType();
		kernelData = input.getKernelDataType();
		sumType = input.getSumType();
		typeCast = output.getTypeCastFromSum();
		bitWise = input.getBitWise();
		this.hasDivide = hasDivide;

		if (!bitWise.isEmpty())
			bitWise = " " + bitWise;

		if (justVertical) {
			printMaster("vertical", 1);
			for (int kernelWidth : KERNEL_WIDTHS) {
				for (int numBands : NUM_BANDS) {
					printVertical(kernelWidth, numBands);
				}
			}
		} else {
			printMaster("horizontal", 1);
			printMaster("vertical", 1);
			printMaster("convolve", 2);

			for (int kernelWidth : KERNEL_WIDTHS) {
				for (int numBands : NUM_BANDS) {
					printHorizontal(kernelWidth, numBands);
					printVertical(kernelWidth, numBands);
					printConvolve(kernelWidth, numBands);
				}
			}
		}
	}

	private void printMaster( String opName, int kernelDOF ) {
		String kernel = "Kernel" + kernelDOF + "D_" + kernelType;
		String divisorArg = hasDivide ? ", int divisor" : "";
		String divisorInput = hasDivide ? ", divisor" : "";

		out.print(functionSignature(1, "public static boolean", opName, kernel + " kernel",
				inputType + " src", outputType + " dst" + divisorArg));
		out.print("\t\tif (!isSupported(kernel, src, dst))\n" +
				"\t\t\treturn false;\n" +
				"\n" +
				"\t\tswitch (src.getNumBands()) {\n");
		for (int numBands : NUM_BANDS) {
			out.print("\t\t\tcase " + numBands + ":\n" +
					"\t\t\t\tswitch (kernel.width) {\n");
			for (int kernelWidth : KERNEL_WIDTHS) {
				out.print("\t\t\t\t\tcase " + kernelWidth + ": " + methodName(opName, kernelWidth, numBands) +
						"(kernel, src, dst" + divisorInput + "); break;\n");
			}
			out.print("\t\t\t\t\tdefault: return false;\n" +
					"\t\t\t\t}\n" +
					"\t\t\t\tbreak;\n");
		}
		out.print("\t\t\tdefault: return false;\n" +
				"\t\t}\n" +
				"\t\treturn true;\n" +
				"\t}\n\n");
	}

	private void printHorizontal( int kernelWidth, int numBands ) {
		String divisorArg = hasDivide ? ", int divisor" : "";

		printPrivateSignature(methodName("horizontal", kernelWidth, numBands), "Kernel1D_" + kernelType + " kernel",
				inputType + " src", outputType + " dst", divisorArg);
		printArrayReferences();
		printKernel1DValues(kernelWidth);
		out.print("\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int imgWidth = src.getWidth();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");

		String body = "\t\t\tint indexDst = dst.startIndex + y*dst.stride + radius*" + numBands + ";\n" +
				"\t\t\tfor (int x = radius; x < imgWidth - radius; x++) {\n" +
				"\t\t\t\tint indexSrc = src.startIndex + y*src.stride + (x - radius)*" + numBands + ";\n";

		for (int band = 0; band < numBands; band++) {
			body += "\n" +
					"\t\t\t\t{\n" +
					"\t\t\t\t\t" + sumType + " total = " + horizontalTerm(band, 0, numBands, 1) + ";\n";
			for (int i = 1; i < kernelWidth; i++) {
				body += "\t\t\t\t\ttotal += " + horizontalTerm(band, i, numBands, i + 1) + ";\n";
			}
			body += "\t\t\t\t\tdataDst[indexDst++] = " + outputExpression() + ";\n" +
					"\t\t\t\t}\n";
		}
		body += "\t\t\t}\n";

		printParallel("y", "0", "src.height", body);
		out.print("\t}\n\n");
	}

	private void printVertical( int kernelWidth, int numBands ) {
		String divisorArg = hasDivide ? ", int divisor" : "";

		printPrivateSignature(methodName("vertical", kernelWidth, numBands), "Kernel1D_" + kernelType + " kernel",
				inputType + " src", outputType + " dst", divisorArg);
		printArrayReferences();
		printKernel1DValues(kernelWidth);
		out.print("\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\t\tfinal int yEnd = imgHeight - radius;\n");

		String body = "\t\t\tint indexDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < imgWidth; x++) {\n" +
				"\t\t\t\tint indexSrc = src.startIndex + (y - radius)*src.stride + x*" + numBands + ";\n";

		for (int band = 0; band < numBands; band++) {
			body += "\n" +
					"\t\t\t\t{\n" +
					"\t\t\t\t\t" + sumType + " total = " + verticalTerm(band, 0, 1) + ";\n";
			for (int i = 1; i < kernelWidth; i++) {
				body += "\t\t\t\t\ttotal += " + verticalTerm(band, i, i + 1) + ";\n";
			}
			body += "\t\t\t\t\tdataDst[indexDst++] = " + outputExpression() + ";\n" +
					"\t\t\t\t}\n";
		}
		body += "\t\t\t}\n";

		printParallel("y", "radius", "yEnd", body);
		out.print("\t}\n\n");
	}

	private void printConvolve( int kernelWidth, int numBands ) {
		String divisorArg = hasDivide ? ", int divisor" : "";

		printPrivateSignature(methodName("convolve", kernelWidth, numBands), "Kernel2D_" + kernelType + " kernel",
				inputType + " src", outputType + " dst", divisorArg);
		printArrayReferences();
		for (int i = 0; i < kernelWidth*kernelWidth; i++) {
			out.printf("\t\tfinal " + kernelData + " k%d = kernel.data[%d];\n", i + 1, i);
		}
		out.print("\n" +
				"\t\tfinal int imgWidth = src.getWidth();\n" +
				"\t\tfinal int imgHeight = src.getHeight();\n" +
				"\t\tfinal int radius = kernel.getRadius();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\t\tfinal int yEnd = imgHeight - radius;\n");

		String body = "\t\t\tint indexDst = dst.startIndex + y*dst.stride + radius*" + numBands + ";\n" +
				"\t\t\tfor (int x = radius; x < imgWidth - radius; x++) {\n" +
				"\t\t\t\tint indexSrc = src.startIndex + (y - radius)*src.stride + (x - radius)*" + numBands + ";\n";

		for (int band = 0; band < numBands; band++) {
			body += "\n" +
					"\t\t\t\t{\n" +
					"\t\t\t\t\t" + sumType + " total = " + convolveTerm(kernelWidth, numBands, band, 0, 0, 1) + ";\n";
			int kernelIndex = 2;
			for (int ky = 0; ky < kernelWidth; ky++) {
				for (int kx = 0; kx < kernelWidth; kx++) {
					if (ky == 0 && kx == 0)
						continue;
					body += "\t\t\t\t\ttotal += " + convolveTerm(kernelWidth, numBands, band, ky, kx, kernelIndex++) + ";\n";
				}
			}
			body += "\t\t\t\t\tdataDst[indexDst++] = " + outputExpression() + ";\n" +
					"\t\t\t\t}\n";
		}
		body += "\t\t\t}\n";

		printParallel("y", "radius", "yEnd", body);
		out.print("\t}\n\n");
	}

	private void printArrayReferences() {
		out.print("\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\n");
	}

	private void printPrivateSignature( String name, String... arguments ) {
		out.print("\tprivate static void " + name + "( " + arguments[0]);
		for (int i = 1; i < arguments.length; i++) {
			String argument = arguments[i];
			if (argument.isEmpty())
				continue;
			if (argument.startsWith(", "))
				argument = argument.substring(2);
			out.print(",\n\t\t\t\t\t\t\t   " + argument);
		}
		out.print(" ) {\n");
	}

	private void printKernel1DValues( int kernelWidth ) {
		for (int i = 0; i < kernelWidth; i++) {
			out.printf("\t\tfinal " + kernelData + " k%d = kernel.data[%d];\n", i + 1, i);
		}
	}

	private String horizontalTerm( int band, int kernelIndex, int numBands, int kernelName ) {
		return "(" + source("indexSrc", kernelIndex*numBands + band) + bitWise + ")*k" + kernelName;
	}

	private String verticalTerm( int band, int kernelIndex, int kernelName ) {
		String index = "indexSrc";
		if (kernelIndex > 0)
			index += " + " + kernelIndex + "*src.stride";
		if (band > 0)
			index += " + " + band;
		return "(" + source(index, 0) + bitWise + ")*k" + kernelName;
	}

	private String convolveTerm( int kernelWidth, int numBands, int band, int ky, int kx, int kernelName ) {
		String index = "indexSrc";
		if (ky > 0)
			index += " + " + ky + "*src.stride";
		int pixelOffset = kx*numBands + band;
		if (pixelOffset > 0)
			index += " + " + pixelOffset;
		return "(" + source(index, 0) + bitWise + ")*k" + kernelName;
	}

	private String source( String index, int offset ) {
		if (offset == 0)
			return "dataSrc[" + index + "]";
		return "dataSrc[" + index + " + " + offset + "]";
	}

	private String outputExpression() {
		if (hasDivide)
			return typeCast + "((total + halfDivisor)/divisor)";
		return typeCast + "total";
	}

	private String methodName( String opName, int kernelWidth, int numBands ) {
		String divide = hasDivide ? "_Div" : "";
		return opName + kernelWidth + "_" + inputType.substring("Interleaved".length()) + "_" +
				outputType.substring("Interleaved".length()) + divide + "_B" + numBands;
	}

	private void printSupport() {
		out.print("\tprivate static boolean isSupported( KernelBase kernel, ImageInterleaved src, ImageInterleaved dst ) {\n" +
				"\t\tif (kernel.offset != kernel.width/2 || kernel.width%2 == 0)\n" +
				"\t\t\treturn false;\n" +
				"\t\tif (src.getNumBands() != dst.getNumBands())\n" +
				"\t\t\treturn false;\n" +
				"\t\tint numBands = src.getNumBands();\n" +
				"\t\treturn numBands >= 2 && numBands <= 4;\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var generator = new GenerateConvolvedUnrolled_IL();
		generator.setModuleName("boofcv-ip");
		generator.parseArguments(args);
		generator.generateCode();
	}
}
