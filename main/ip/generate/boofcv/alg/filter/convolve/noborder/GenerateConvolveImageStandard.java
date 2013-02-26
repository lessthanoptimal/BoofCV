/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.AutoTypeImage;
import boofcv.misc.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * Code generator for {@link ConvolveImageStandard}.
 *
 * @author Peter Abeles
 */
public class GenerateConvolveImageStandard extends CodeGeneratorBase {
	String className = "ConvolveImageStandard";

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
	boolean hasBound;

	@Override
	public void generate()throws FileNotFoundException {
		printPreamble();
		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false, false);
		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false, true);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.I16, false, false);
		printAllOps(AutoTypeImage.U8, AutoTypeImage.S32, false, false);
		printAllOps(AutoTypeImage.S16,AutoTypeImage.I16,false, false);
		printAllOps(AutoTypeImage.U8,AutoTypeImage.I8,true, false);
		printAllOps(AutoTypeImage.U8,AutoTypeImage.I8,false, true);
		printAllOps(AutoTypeImage.S16,AutoTypeImage.I16,true, false);
		printAllOps(AutoTypeImage.S32,AutoTypeImage.S32,false, false);
		printAllOps(AutoTypeImage.S32,AutoTypeImage.S32,true, false);

		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);
		out.print("import boofcv.struct.convolve.Kernel1D_F32;\n" +
				"import boofcv.struct.convolve.Kernel1D_I32;\n" +
				"import boofcv.struct.convolve.Kernel2D_F32;\n" +
				"import boofcv.struct.convolve.Kernel2D_I32;\n" +
				"import boofcv.struct.image.*;\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.\n" +
				" * </p>\n" +
				" * \n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link GenerateConvolveImageStandard}.\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\"})\n" +
				"public class " + className + " {\n\n");
	}

	private void printAllOps(AutoTypeImage input, AutoTypeImage output, boolean hasDivide, boolean hasBound)
	{
		boolean isInteger = input.isInteger();

		typeCast = output.getTypeCastFromSum();
		kernelType = isInteger ? "I32" : "F32";
		inputType = input.getImageName();
		outputType = output.getImageName();
		kernelData = isInteger ? "int" : "float";
		inputData = input.getDataType();
		outputData = output.getDataType();
		sumType = isInteger ? "int" : "float";
		bitWise = input.getBitWise();
		this.hasDivide = hasDivide;
		this.hasBound = hasBound;

		if( !hasBound ) {
			printHorizontal();
			printVerticle();
		}
		printConvolve2D();
	}

	private void printHorizontal() {
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
				"\t\t\tint j = image.startIndex + i*image.stride - radius;\n" +
				"\t\t\tfinal int jEnd = j+width-radius;\n" +
				"\n" +
				"\t\t\tfor( j += radius; j < jEnd; j++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = j;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ") * dataKer[k];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printVerticle() {
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
				"\t\t\tint i = image.startIndex + (y-radius)*image.stride;\n" +
				"\t\t\tfinal int iEnd = i+imgWidth-xBorder;\n" +
				"\n" +
				"\t\t\tfor( i += xBorder; i < iEnd; i++ ) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = i;\n" +
				"\t\t\t\tfor( int k = 0; k < kernelWidth; k++ ) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* dataKer[k];\n" +
				"\t\t\t\t\tindexSrc += image.stride;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printConvolve2D() {

		String paramDiv = hasDivide ? ", int divisor " : "";
		String totalDiv = hasDivide ? "(total/divisor)" : "total";
		String paramBound = hasBound ? ", "+sumType+" minValue , "+sumType+" maxValue " : "";
		String performBound = "";

		if( hasBound ) {
			performBound = "\n" +
					"\t\t\t\tif( total < minValue )\n" +
					"\t\t\t\t\ttotal = minValue;\n" +
					"\t\t\t\telse if( total > maxValue )\n" +
					"\t\t\t\t\ttotal = maxValue;\n" +
					"\n";
		}


		out.print("\tpublic static void convolve( Kernel2D_" + kernelType + " kernel , " + inputType + " src , " + outputType + " dest " + paramDiv + paramBound + ")\n" +
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
				performBound +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main(String args[]) throws FileNotFoundException {
		GenerateConvolveImageStandard gen = new GenerateConvolveImageStandard();
		gen.generate();
	}
}
