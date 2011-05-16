/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve.noborder;

import gecv.misc.CodeGeneratorUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Code generator for {@link ConvolveImageStandard}.
 *
 * @author Peter Abeles
 */
public class GenerateConvolveStandard {
	String className = "ConvolveImageStandard";

	PrintStream out;

	public GenerateConvolveStandard() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(className + ".java"));
	}

	public void generate() {
		printPreamble();
		printAllOps("F32", "ImageFloat32", "ImageFloat32",
				"float", "float", "float",
				"float", "", "", false);
		printAllOps("I32", "ImageUInt8", "ImageInt16",
				"int", "byte", "short",
				"int", "(short)", "& 0xFF", false);
		printAllOps("I32", "ImageUInt8", "ImageSInt32",
				"int", "byte", "int",
				"int", "", "& 0xFF", false);
		printAllOps("I32", "ImageSInt16", "ImageInt16",
				"int", "short", "short",
				"int", "(short)", "", false);
		printAllOps("I32", "ImageUInt8", "ImageInt8",
				"int", "byte", "byte",
				"int", "(byte)", "& 0xFF", true);
		printAllOps("I32", "ImageSInt16", "ImageInt16",
				"int", "short", "short",
				"int", "(short)", "", true);
		out.println("}");
	}

	private void printPreamble() {
		out.print(CodeGeneratorUtil.copyright);
		out.print("package gecv.alg.filter.convolve.noborder;\n");
		out.println();
		out.print("import gecv.struct.convolve.Kernel1D_F32;\n" +
				"import gecv.struct.convolve.Kernel1D_I32;\n" +
				"import gecv.struct.convolve.Kernel2D_F32;\n" +
				"import gecv.struct.convolve.Kernel2D_I32;\n" +
				"import gecv.struct.image.ImageFloat32;\n" +
				"import gecv.struct.image.ImageSInt32;\n" +
				"import gecv.struct.image.ImageSInt16;\n" +
				"import gecv.struct.image.ImageInt16;\n" +
				"import gecv.struct.image.ImageInt8;\n" +
				"import gecv.struct.image.ImageUInt8;\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.\n" +
				" * </p>\n" +
				" * \n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link GenerateConvolveStandard}.\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\"})\n" +
				"public class " + className + " {\n\n");
	}

	private void printAllOps(String kernelType, String inputType, String outputType,
							 String kernelData, String inputData, String outputData,
							 String sumType, String typeCast, String bitWise, boolean hasDivide) {
		printHorizontal(kernelType, inputType, outputType, kernelData, inputData, outputData, sumType, typeCast, bitWise, hasDivide);
		printVerticle(kernelType, inputType, outputType, kernelData, inputData, outputData, sumType, typeCast, bitWise, hasDivide);
		printConvolve2D(kernelType, inputType, outputType, kernelData, inputData, outputData, sumType, typeCast, bitWise, hasDivide);
	}

	private void printHorizontal(String kernelType, String inputType, String outputType,
								 String kernelData, String inputData, String outputData,
								 String sumType, String typeCast, String bitWise, boolean hasDivide) {
		String paramDiv = hasDivide ? " int divisor," : "";
		String totalDiv = hasDivide ? "(total/divisor)" : "total";

		out.print("\tpublic static void horizontal( Kernel1D_" + kernelType + " kernel ,\n");
		out.print("\t\t\t\t\t\t\t\t  " + inputType + " image, " + outputType + " dest," + paramDiv + "\n");
		out.print("\t\t\t\t\t\t\t\t  boolean includeBorder) {\n" +
				"\t\tfinal " + inputData + "[] dataSrc = image.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\n" +
				"\t\tfinal int yBorder = includeBorder ? 0 : radius;\n" +
				"\n" +
				"\t\tfinal int width = image.getWidth();\n" +
				"\t\tfinal int height = image.getHeight()-yBorder;\n" +
				"\n" +
				"\t\tfor( int i = yBorder; i < height; i++ ) {\n" +
				"\t\t\tint indexDst = dest.startIndex + i*dest.stride+radius;\n" +
				"\t\t\tint j = image.startIndex+ i*image.stride;\n" +
				"\t\t\tfinal int jEnd = j+width-radius;\n" +
				"\n" +
				"\t\t\tfor( j += radius; j < jEnd; j++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = j-radius;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ") * dataKer[k];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printVerticle(String kernelType, String inputType, String outputType,
							   String kernelData, String inputData, String outputData,
							   String sumType, String typeCast, String bitWise, boolean hasDivide) {
		String paramDiv = hasDivide ? " int divisor," : "";
		String totalDiv = hasDivide ? "(total/divisor)" : "total";

		out.print("\tpublic static void vertical( Kernel1D_" + kernelType + " kernel,\n" +
				"\t\t\t\t\t\t\t\t " + inputType + " image, " + outputType + " dest," + paramDiv + "\n" +
				"\t\t\t\t\t\t\t\t boolean includeBorder)\n" +
				"\t{\n" +
				"\t\tfinal " + inputData + "[] dataSrc = image.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\n" +
				"\t\tfinal int imgWidth = dest.getWidth();\n" +
				"\t\tfinal int imgHeight = dest.getHeight();\n" +
				"\n" +
				"\t\tfinal int yEnd = imgHeight-radius;\n" +
				"\n" +
				"\t\tfinal int xBorder = includeBorder ? 0 : radius;\n" +
				"\n" +
				"\t\tfor( int y = radius; y < yEnd; y++ ) {\n" +
				"\t\t\tint indexDst = dest.startIndex+y*dest.stride+xBorder;\n" +
				"\t\t\tint i = image.startIndex+y*image.stride;\n" +
				"\t\t\tfinal int iEnd = i+imgWidth-xBorder;\n" +
				"\n" +
				"\t\t\tfor( i += xBorder; i < iEnd; i++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = i-radius*image.stride;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* dataKer[k];\n" +
				"\t\t\t\t\tindexSrc += image.stride;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printConvolve2D(String kernelType, String inputType, String outputType,
								 String kernelData, String inputData, String outputData,
								 String sumType, String typeCast, String bitWise, boolean hasDivide) {

		String paramDiv = hasDivide ? ", int divisor " : "";
		String totalDiv = hasDivide ? "(total/divisor)" : "total";
		out.print("\tpublic static void convolve( Kernel2D_" + kernelType + " kernel , " + inputType + " src , " + outputType + " dest " + paramDiv + ")\n" +
				"\t{\n" +
				"\t\tfinal " + kernelData + "[] dataKernel = kernel.data;\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\n" +
				"\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n" +
				"\n" +
				"\t\tint kernelRadius = kernel.width/2;\n" +
				"\n" +
				"\t\tfor( int y = kernelRadius; y < height-kernelRadius; y++ ) {\n" +
				"\t\t\tint indexDst = dest.startIndex + y*dest.stride+kernelRadius;\n" +
				"\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexKer = 0;\n" +
				"\t\t\t\tfor( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {\n" +
				"\t\t\t\t\tint indexSrc = src.startIndex+(y+ki)*src.stride+ x;\n" +
				"\t\t\t\t\tfor( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {\n" +
				"\t\t\t\t\t\ttotal += (dataSrc[indexSrc+kj] " + bitWise + " )* dataKernel[indexKer++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main(String args[]) throws FileNotFoundException {
		GenerateConvolveStandard gen = new GenerateConvolveStandard();
		gen.generate();
	}
}
