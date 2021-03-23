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
 * Code generator for ConvolveImageStandard_SB.
 *
 * @author Peter Abeles
 */
public class GenerateConvolveImageStandard_SB extends CodeGeneratorBase {

	String kernelType;
	String inputType;
	String outputType;
	String kernelData;
	String inputData;
	String outputData;
	String sumType;
	String typeCast;
	String bitWise;
	String workType;
	boolean hasDivide;

	@Override
	public void generateCode() throws FileNotFoundException {
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
		out.print("import pabeles.concurrency.GrowArray;\n" +
				"import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.convolve.*;\n" +
				"import boofcv.struct.image.*;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"import java.util.Arrays;\n");
		out.println();
		out.print("//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n");

		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
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
		inputType = input.getSingleBandName();
		outputType = output.getSingleBandName();
		kernelData = input.getKernelDataType();
		inputData = input.getDataType();
		outputData = output.getDataType();
		sumType = input.getSumType();
		bitWise = input.getBitWise();
		workType = ("DogArray_" + input.getKernelType()).replace("S32", "I32");
		this.hasDivide = hasDivide;

		// just for anal retentive formatting
		if (!bitWise.isEmpty())
			bitWise = " " + bitWise;

		if (justVertical) {
			if (hasDivide)
				printVertical_div();
			else
				printVertical();
		} else {
			printHorizontal();
			if (hasDivide)
				printVertical_div();
			else
				printVertical();
			if (hasDivide)
				printConvolve2D_div();
			else
				printConvolve2D();
		}
	}

	private void printHorizontal() {
		String paramDiv = hasDivide ? " , int divisor" : "";
		String totalDiv = hasDivide ? "((total + halfDivisor)/divisor)" : "total";

		out.print("\tpublic static void horizontal( Kernel1D_" + kernelType + " kernel, " +
				inputType + " src, " + outputType + " dst" + paramDiv + " ) {\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");
		out.print("\n" +
				"\t\tfinal int width = src.getWidth();\n");

		String body = "\t\t\tint indexDst = dst.startIndex + i*dst.stride + offset;\n" +
				"\t\t\tint j = src.startIndex + i*src.stride;\n" +
				"\t\t\tfinal int jEnd = j + width - (kernelWidth - 1);\n" +
				"\n" +
				"\t\t\tfor (; j < jEnd; j++) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexSrc = j;\n" +
				"\t\t\t\tfor (int k = 0; k < kernelWidth; k++) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc++]" + bitWise + ")*dataKer[k];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n";

		printParallel("i", "0", "src.height", body);

		out.print("\t}\n\n");
	}

	private void printVertical_div() {
		out.print("\tpublic static void vertical( Kernel1D_" + kernelType + " kernel,\n" +
				"\t\t\t\t\t\t\t\t " + inputType + " src, " + outputType + " dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {\n" +
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);\n" +
				"\t\tfinal DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\t\tfinal int halfDivisor = divisor/2;\n" +
				"\t\tfinal double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??\n" +
				"\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n" +
				"\t\tfinal int yEnd = imgHeight - (kernelWidth - offset - 1);\n");

		String body = "";
		body += "\t\t" + sumType + "[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tfor (int k = 0; k < kernelWidth; k++) {\n" +
				"\t\t\t\tfinal int kernelValue = dataKer[k];\n" +
				"\t\t\t\tint indexSrc = src.startIndex + (y - offset + k)*src.stride;\n" +
				"\t\t\t\tfor (int i = 0; i < imgWidth; i++) {\n" +
				"\t\t\t\t\ttotalRow[i] += ((dataSrc[indexSrc++]" + bitWise + ")*kernelValue);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tint indexDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int i = 0; i < imgWidth; i++) {\n" +
				"\t\t\t\tdataDst[indexDst++] = (" + outputData + ")((totalRow[i] + halfDivisor)*divisionHack);\n" +
				"\t\t\t}\n" +
				"\t\t\tArrays.fill(totalRow,0,imgWidth,0);\n" +
				"\t\t}\n";

		printParallelBlock("y0", "y1", "offset", "yEnd", null, body);

		out.print("\t}\n\n");
	}

	/**
	 * If a divisor isn't needed then the image can be processed in a way which minimizes cache misses. It's assumed
	 * the output image can sum up without overflowing. This would be an issue even if an int is used to sum.
	 */
	private void printVertical() {
		out.print("\tpublic static void vertical( Kernel1D_" + kernelType + " kernel, " + inputType + " src, " + outputType + " dst ) {\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dst.data;\n" +
				"\t\tfinal " + kernelData + "[] dataKer = kernel.data;\n" +
				"\n" +
				"\t\tfinal int offset = kernel.getOffset();\n" +
				"\t\tfinal int kernelWidth = kernel.getWidth();\n" +
				"\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n" +
				"\t\tfinal int yEnd = imgHeight - (kernelWidth - offset - 1);\n");

		String body = "";
		body += "\t\t\tfinal int indexDstStart = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tArrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (" + outputData + ")0);\n" +
				"\n" +
				"\t\t\tfor (int k = 0; k < kernelWidth; k++) {\n" +
				"\t\t\t\tfinal int iStart = src.startIndex + (y - offset + k)*src.stride;\n" +
				"\t\t\t\tfinal int iEnd = iStart + imgWidth;\n" +
				"\t\t\t\tint indexDst = indexDstStart;\n" +
				"\t\t\t\t" + kernelData + " kernelValue = dataKer[k];\n" +
				"\t\t\t\tfor (int i = iStart; i < iEnd; i++) {\n" +
				"\t\t\t\t\tdataDst[indexDst++] += " + typeCast + "((dataSrc[i]" + bitWise + ")*kernelValue);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n";

		printParallel("y", "offset", "yEnd", body);

		out.print("\t}\n\n");
	}

	private void printConvolve2D() {

		String paramDiv = hasDivide ? ", int divisor" : "";
		String totalDiv = hasDivide ? "((total+halfDivisor)/divisor)" : "total";
		String performBound = "";

		out.print("\tpublic static void convolve( Kernel2D_" + kernelType + " kernel, " + inputType + " src, " + outputType + " dest" + paramDiv + " ) {\n" +
				"\t\tfinal " + kernelData + "[] dataKernel = kernel.data;\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\n" +
				"\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n");
		if (hasDivide)
			out.print("\t\tfinal int halfDivisor = divisor/2;\n");

		out.print("\n" +
				"\t\tint offsetL = kernel.offset;\n" +
				"\t\tint offsetR = kernel.width - kernel.offset - 1;\n");

		String body = "";
		body += "\t\t\tint indexDst = dest.startIndex + y*dest.stride + offsetL;\n" +
				"\t\t\tfor (int x = offsetL; x < width - offsetR; x++) {\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tint indexKer = 0;\n" +
				"\t\t\t\tfor (int ki = 0; ki < kernel.width; ki++) {\n" +
				"\t\t\t\t\tint indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;\n" +
				"\t\t\t\t\tfor (int kj = 0; kj < kernel.width; kj++) {\n" +
				"\t\t\t\t\t\ttotal += (dataSrc[indexSrc + kj]" + bitWise + ")*dataKernel[indexKer++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				performBound +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + totalDiv + ";\n" +
				"\t\t\t}\n";

		printParallel("y", "offsetL", "height - offsetR", body);

		out.print("\t}\n\n");
	}

	private void printConvolve2D_div() {
		out.print("\tpublic static void convolve( Kernel2D_" + kernelType + " kernel, " + inputType + " src, " +
				outputType + " dest, int divisor, @Nullable GrowArray<" + workType + "> workspaces ) {\n" +
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, " + workType + "::new);\n" +
				"\t\tfinal " + workType + " work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal " + kernelData + "[] dataKernel = kernel.data;\n" +
				"\t\tfinal " + inputData + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + outputData + "[] dataDst = dest.data;\n" +
				"\n" +
				"\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n" +
				"\t\tfinal int halfDivisor = divisor/2;\n" +
				"\n" +
				"\t\tint offsetL = kernel.offset;\n" +
				"\t\tint offsetR = kernel.width - kernel.offset - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height - offsetR,kernel.width, workspaces, (work,y0,y1) -> {\n" +
				"\t\tfinal int y0 = offsetL, y1 = height - offsetR;\n" +
				"\t\t" + sumType + " totalRow[] = BoofMiscOps.checkDeclare(work, src.width, false);\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint indexSrcRow = src.startIndex + (y - offsetL)*src.stride - offsetL;\n" +
				"\t\t\tfor (int x = offsetL; x < width - offsetR; x++) {\n" +
				"\t\t\t\tint indexSrc = indexSrcRow + x;\n" +
				"\n" +
				"\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\tfor (int k = 0; k < kernel.width; k++) {\n" +
				"\t\t\t\t\ttotal += (dataSrc[indexSrc++]" + bitWise + ")*dataKernel[k];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\ttotalRow[x] = total;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// rest of the convolution rows are an addition\n" +
				"\t\t\tfor (int i = 1; i < kernel.width; i++) {\n" +
				"\t\t\t\tindexSrcRow = src.startIndex + (y + i - offsetL)*src.stride - offsetL;\n" +
				"\t\t\t\tint indexKer = i*kernel.width;\n" +
				"\n" +
				"\t\t\t\tfor (int x = offsetL; x < width - offsetR; x++) {\n" +
				"\t\t\t\t\tint indexSrc = indexSrcRow + x;\n" +
				"\n" +
				"\t\t\t\t\t" + sumType + " total = 0;\n" +
				"\t\t\t\t\tfor (int k = 0; k < kernel.width; k++) {\n" +
				"\t\t\t\t\t\ttotal += (dataSrc[indexSrc++]" + bitWise + ")*dataKernel[indexKer + k];\n" +
				"\t\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t\ttotalRow[x] += total;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\tint indexDst = dest.startIndex + y*dest.stride + offsetL;\n" +
				"\t\t\tfor (int x = offsetL; x < width - offsetR; x++) {\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + "((totalRow[x] + halfDivisor)/divisor);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_INLINE });\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var gen = new GenerateConvolveImageStandard_SB();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generateCode();
	}
}
