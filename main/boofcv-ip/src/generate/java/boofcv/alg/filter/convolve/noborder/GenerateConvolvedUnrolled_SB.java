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
 * @author Peter Abeles
 */
public class GenerateConvolvedUnrolled_SB extends CodeGeneratorBase {

	final static int numUnrolled = 5;

	String typeKernel;
	String typeInput;
	String typeOutput;
	String dataKernel;
	String dataInput;
	String dataOutput;
	String bitWise;
	String sumType;
	String workType;
	boolean hasDivisor;
	boolean isInteger;

	String declareHalf;
	String divide;

	public GenerateConvolvedUnrolled_SB() {
		className = null;
	}

	@Override
	public void generateCode() throws FileNotFoundException {
		create(AutoTypeImage.F32,AutoTypeImage.F32,false);
		create(AutoTypeImage.F64,AutoTypeImage.F64,false);
		create(AutoTypeImage.U8,AutoTypeImage.I8,true);
		create(AutoTypeImage.U8,AutoTypeImage.I16,false);
		create(AutoTypeImage.S16,AutoTypeImage.I16,false);
		create(AutoTypeImage.S16,AutoTypeImage.I16,true);
		create(AutoTypeImage.U16,AutoTypeImage.I16,false);
		create(AutoTypeImage.U16,AutoTypeImage.I16,true);
		create(AutoTypeImage.S32,AutoTypeImage.S32,false);
		create(AutoTypeImage.S32,AutoTypeImage.S32,true);
	}

	protected void create( AutoTypeImage inputImg , AutoTypeImage outputImg , boolean divided ) throws FileNotFoundException {
		super.className = null; // need to do this to avoid sanity check
		isInteger = inputImg.isInteger();

		String name = "ConvolveImageUnrolled_SB_"+inputImg.getAbbreviatedType()+"_"+outputImg.getAbbreviatedType();
		String nameConcurrent = "ConvolveImageUnrolled_SB_MT_"+inputImg.getAbbreviatedType()+"_"+outputImg.getAbbreviatedType();
		if( divided ) {
			name += "_Div";
			nameConcurrent += "_Div";
		}

		typeKernel = isInteger ? "S32" : "F"+inputImg.getNumBits();
		typeInput = inputImg.getSingleBandName();
		typeOutput = outputImg.getSingleBandName();
		dataKernel = inputImg.getSumType();
		dataInput = inputImg.getDataType();
		dataOutput = outputImg.getDataType();
		sumType = inputImg.getSumType();
		bitWise = inputImg.getBitWise();
		hasDivisor = divided;
		workType = ("DogArray_"+inputImg.getKernelType()).replace("S32","I32");

		declareHalf = isInteger ? "\t\tfinal " + sumType + " halfDivisor = divisor/2;\n" : "";
		divide = isInteger ? "(total + halfDivisor)/divisor" : "total/divisor";

		createFile(name,nameConcurrent);
	}

	public void createFile( String fileName , String nameConcurrent ) throws FileNotFoundException {
		setOutputFile(fileName);
		printPreamble(nameConcurrent);
		createMaster("horizontal",1,hasDivisor);
		createMaster("vertical",1,hasDivisor);
		createMaster("convolve",2,hasDivisor);

		for (int i = 0; i < numUnrolled; i++) {
			addHorizontal(3 + i * 2,hasDivisor);
		}
		for (int i = 0; i < numUnrolled; i++) {
			// Disabling because it's worth investigating why the Div approach didn't win. It can also be
			// adapted to Vector easier
//			if (hasDivisor)
//				addVerticalDiv(3 +i*2);
//			else
			addVertical(3 + i*2, hasDivisor);
		}
		for (int i = 0; i < numUnrolled; i++) {
			if( hasDivisor )
				addConvolveDiv(3 + i*2 );
			else
				addConvolve(3 + i*2 );
		}

//		if (isInteger) {
//			out.println("\tprivate static void scaleArray( final " + dataInput + "[] dataSrc, final int kernelVal, final int imgWidth, final int[] totalRow, int indexSrc ) {\n" +
//					"\t\tfor (int j = 0; j < imgWidth; j++) {\n" +
//					"\t\t\ttotalRow[j] += (dataSrc[indexSrc++]" + bitWise + ")*kernelVal;\n" +
//					"\t\t}\n" +
//					"\t}\n");
//		}

		out.println("}");
	}

	public void printPreamble( String nameConcurrent ) {
		out.print(
				"import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.convolve.*;\n" +
				"import pabeles.concurrency.GrowArray;\n" +
				"import org.jetbrains.annotations.Nullable;\n");
		if (typeInput.compareTo(typeOutput) != 0)
			out.print("import boofcv.struct.image." + typeOutput + ";\n");
		out.print("import boofcv.struct.image." + typeInput + ";\n"+
				"import org.ddogleg.struct."+workType+";\n"+
				"import javax.annotation.Generated;\n" +
				"import boofcv.concurrency.*;\n" +
				"import java.util.Arrays;\n");

		out.println("\n//CONCURRENT_CLASS_NAME "+nameConcurrent);
		out.println("//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;");

		out.print("\n" +
				"/**\n" +
				" * <p>\n" +
				" * Unrolls the convolution kernel to reduce array accessing and save often used variables to the stack.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * Unrolling the image being convolved resulting in an additional 10% performance boost on a Core i7 processor,\n" +
				" * see commented out code below. Due to the added complexity it was decided that this performance boost was\n" +
				" * not worth it. By comparison, unrolling the kernel causes a performance boost between 2 and 3 times.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\",\"Duplicates\"})\n" +
				"public class " + className + " {\n");
	}

	void createMaster(String opName, int kernelDOF , boolean hasDivisor ) {
		String kernel = "Kernel"+kernelDOF+"D_"+typeKernel;

		String workParam = hasDivisor && (opName.equals("convolve")||opName.equals("vertical")) ? ", GrowArray<"+workType+"> work" : "";
		String workVar = hasDivisor && (opName.equals("convolve")||opName.equals("vertical")) ? ", work" : "";

		out.print("\tpublic static boolean " + opName + "( " + kernel + " kernel,\n" +
				"\t\t\t\t\t\t\t\t   " + typeInput + " image, " + typeOutput + " dest");


		if( hasDivisor ) {
			out.print(", int divisor"+workParam+" ) {\n");
		} else {
			out.print(") {\n");
		}

		out.print(
				"\n" +
				"\t\t// Unrolled functions only exist for symmetric kernels with an odd width\n" +
				"\t\tif( kernel.offset != kernel.width/2 || kernel.width%2 == 0 )\n" +
				"\t\t\treturn false;\n" +
				"\n");

		out.print("\t\tswitch (kernel.width) {\n");
		for (int i = 0; i < numUnrolled; i++) {
			int num = 3 + i * 2;
			out.print("\t\t\tcase " + num + ":");
			if( hasDivisor )
				out.print(" " + opName + num + "(kernel, image, dest, divisor"+workVar+");");
			else
				out.print(" " + opName + num + "(kernel, image, dest);");
			out.print(" break;\n");
		}
		out.print("\t\t\tdefault: return false;\n" +
				"\t\t}\n" +
				"\t\treturn true;\n" +
				"\t}\n\n");
	}

	void addHorizontal(int num, boolean hasDivisor ) {
		String typeCast = generateTypeCast();

		out.print("\tpublic static void horizontal" + num + "( Kernel1D_" + typeKernel + " kernel , "
				+ typeInput + " image, " + typeOutput + " dest ");

		if( hasDivisor ) {
			out.print(", int divisor )\n");
		} else {
			out.print(")\n");
		}

		out.print("\t{\n" +
				"\t\tfinal " + dataInput + "[] dataSrc = image.data;\n" +
				"\t\tfinal " + dataOutput + "[] dataDst = dest.data;\n" +
				"\n");
		for (int i = 0; i < num; i++) {
			out.printf("\t\tfinal " + dataKernel + " k%d = kernel.data[%d];\n", i + 1, i);
		}
		out.print(
				"\n"+
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\n" +
				"\t\tfinal int width = image.getWidth();\n"+
				(hasDivisor ? declareHalf : ""));

		String body ="\t\t\tint indexDst = dest.startIndex + i*dest.stride+radius;\n" +
				"\t\t\tint j = image.startIndex + i*image.stride - radius;\n" +
				"\t\t\tfinal int jEnd = j+width-radius;\n" +
				"\n" +
				"\t\t\tfor( j += radius; j < jEnd; j++ ) {\n" +
				"\t\t\t\tint indexSrc = j;\n" +
				"\t\t\t\t" + sumType + " total = (dataSrc[indexSrc++]" + bitWise + ")*k1;\n";
		for (int i = 1; i < num - 1; i++) {
			body += String.format("\t\t\t\ttotal += (dataSrc[indexSrc++]" + bitWise + ")*k%d;\n", i + 1);
		}
		body += String.format("\t\t\t\ttotal += (dataSrc[indexSrc]" + bitWise + ")*k%d;\n", num);
		body += "\n";
		if( hasDivisor ) {
			body += "\t\t\t\tdataDst[indexDst++] = " + typeCast + "(" + divide + ");\n";
		} else {
			body += "\t\t\t\tdataDst[indexDst++] = " + typeCast + "total;\n";
		}
		body += "\t\t\t}\n";

		printParallel("i","0","image.height",body);

		out.print("\t}\n\n");
	}

	void addVertical(int num, boolean hasDivisor) {
		String typeCast = generateTypeCast();

		out.print("\tpublic static void vertical" + num + "( Kernel1D_" + typeKernel + " kernel, "
				 + typeInput + " src, " + typeOutput + " dst ");
		if( hasDivisor && !isInteger)
			out.print(", int divisor )\n");
		else if( hasDivisor && isInteger)
			out.print(", int divisor, @Nullable GrowArray<DogArray_I32> workspaces )\n");
		else
			out.print(")\n");

		out.print("\t{\n" +
				"\t\tfinal " + dataInput + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + dataOutput + "[] dataDst = dst.data;\n" +
				"\n");
		for (int i = 0; i < num; i++) {
			out.printf("\t\tfinal " + dataKernel + " k%d = kernel.data[%d];\n", i + 1, i);
		}

		out.print("\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n" +
				(hasDivisor ? declareHalf : "") +
				"\n" +
				"\t\tfinal int yEnd = imgHeight - radius;\n");

		String body = "\t\t\tint indexDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tint i = src.startIndex + (y - radius)*src.stride;\n" +
				"\t\t\tfinal int iEnd = i + imgWidth;\n" +
				"\n" +
				"\t\t\tfor (; i < iEnd; i++) {\n" +
				"\t\t\t\tint indexSrc = i;\n" +
				"\n" +
				"\t\t\t\t" + sumType + " total = (dataSrc[indexSrc]"+bitWise+") * k1;\n";
		for (int i = 1; i < num; i++) {
			body += "\t\t\t\tindexSrc += src.stride;\n";
			body += String.format("\t\t\t\ttotal += (dataSrc[indexSrc]" + bitWise + ")*k%d;\n", i + 1);
		}
		body += "\n";
		if( hasDivisor )
			body += "\t\t\t\tdataDst[indexDst++] = " + typeCast + "(" + divide + ");\n";
		else
			body += "\t\t\t\tdataDst[indexDst++] = " + typeCast + "total;\n";
		body += "\t\t\t}\n";

		printParallel("y","radius","yEnd",body);

		out.print("\t}\n\n");
	}

	/**
	 * This doesn't seem to be getting optimized as well. For r=1 it's much faster. Cost increases linearly. While
	 * for the other it has constant runtime independent of kernel length, which is really odd.
	 */
	void addVerticalDiv( int num) {
		out.print("\tpublic static void vertical"+num+"( Kernel1D_"+typeKernel+" kernel, "+typeInput+" src, "+
				typeOutput+" dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {\n" +
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);\n" +
				"\t\tfinal DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal "+dataInput+"[] dataSrc = src.data;\n" +
				"\t\tfinal "+dataOutput+"[] dataDst = dst.data;\n" +
				"\n");
		for (int i = 0; i < num; i++) {
			out.printf("\t\tfinal " + dataKernel + " k%d = kernel.data[%d];\n", i + 1, i);
		}
		out.print("\n" +
				"\t\tfinal int offset = kernel.getRadius();\n" +
				"\n" +
				"\t\tfinal int imgWidth = dst.getWidth();\n" +
				"\t\tfinal int imgHeight = dst.getHeight();\n" +
				"\t\tfinal int halfDivisor = divisor/2;\n" +
				"\t\tfinal double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??\n" +
				"\n" +
				"\t\tfinal int yEnd = imgHeight-offset;\n");
		String body = "";
		body += "\t\tint[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint indexSrc = src.startIndex + (y - offset)*src.stride;\n";
		for (int i = 0; i < num; i++) {
			body += String.format("\t\t\tscaleArray(dataSrc, k%d, imgWidth, totalRow, indexSrc); indexSrc += src.stride;\n",i+1);
		}

		body += "\n" +
				"\t\t\tint indexDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int j = 0; j < imgWidth; j++) {\n" +
				"\t\t\t\tdataDst[indexDst++] = ("+dataOutput+")((totalRow[j] + halfDivisor)*divisionHack);\n" +
				"\t\t\t}\n" +
				"\t\t\tArrays.fill(totalRow,0,imgWidth,0);\n" +
				"\t\t}\n";

		printParallelBlock("y0", "y1", "offset", "yEnd", null, body);

		out.print("\t}\n\n");
	}

	void addConvolve(int num ) {
		String typeCast = generateTypeCast();

		out.print("\tpublic static void convolve" + num + "( Kernel2D_" + typeKernel + " kernel, " + typeInput + " src, " + typeOutput + " dest)\n");

		out.print("\t{\n" +
				"\t\tfinal " + dataInput + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + dataOutput + "[] dataDst = dest.data;\n" +
				"\n");

		out.print("\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n" +
				"\n" +
				"\t\tfinal int kernelRadius = kernel.getRadius();\n");

		String body ="\n" +
					 "\t\t\t// first time through the value needs to be set\n";
		for( int i = 0; i < num; i++ ) {
			body += "\t\t\t" + sumType + " k" + (i + 1) + " = kernel.data[" + i + "];\n";
		}

		body += "\n" +
				"\t\t\tint indexDst = dest.startIndex + y*dest.stride+kernelRadius;\n" +
				"\t\t\tint indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;\n" +
				"\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\tint indexSrc = indexSrcRow + x;\n" +
				"\n" +
				"\t\t\t\t" + sumType + " total = 0;\n";
		for( int i = 0; i < num-1; i++ ) {
			body += "\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ")* k" + (i + 1) + ";\n";
		}
		body += "\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* k" + num + ";\n";
		body += "\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + "total;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// rest of the convolution rows are an addition\n" +
				"\t\t\tfor( int i = 1; i < " + num + "; i++ ) {\n" +
				"\t\t\t\tindexDst = dest.startIndex + y*dest.stride+kernelRadius;\n" +
				"\t\t\t\tindexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;\n" +
				"\t\t\t\t\n";
		for( int i = 0; i < num; i++ ) {
			body += "\t\t\t\tk" + (i + 1) + " = kernel.data[i*" + num + " + " + i + "];\n";
		}
		body += "\n" +
				"\t\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\t\tint indexSrc = indexSrcRow+x;\n" +
				"\n" +
				"\t\t\t\t\t" + sumType + " total = 0;\n";
		for( int i = 0; i < num-1; i++ ) {
			body += "\t\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ")* k" + (i + 1) + ";\n";
		}
		body += "\t\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* k" + num + ";\n";
		body += "\n" +
				"\t\t\t\t\tdataDst[indexDst++] += " + typeCast + "total;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n";

		printParallel("y","kernelRadius","height-kernelRadius",body);

		out.print("\t}\n\n");
	}

	void addConvolveDiv(int num ) {
		String typeCast = generateTypeCast();

		out.print("\tpublic static void convolve" + num + "( Kernel2D_" + typeKernel
				+ " kernel, " + typeInput + " src, " + typeOutput + " dest , int divisor , @Nullable GrowArray<"+workType+"> workspaces ) {\n");

		out.print(
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, "+workType+"::new);\n" +
				"\t\tfinal "+workType+" work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal " + dataInput + "[] dataSrc = src.data;\n" +
				"\t\tfinal " + dataOutput + "[] dataDst = dest.data;\n" +
				"\n");
		out.print("\t\tfinal int width = src.getWidth();\n" +
				"\t\tfinal int height = src.getHeight();\n" +
				declareHalf +
				"\n" +
				"\t\tfinal int kernelRadius = kernel.getRadius();\n" +
				"\t\tfinal int kernelWidth = 2*kernelRadius+1;\n");
		String body = "";
		body += "\t\t"+sumType+" totalRow[] = BoofMiscOps.checkDeclare(work,src.width,false);\n";
		body += "\t\tfor( int y = y0; y < y1; y++ ) {\n";
		body += "\n" +
				"\t\t\t// first time through the value needs to be set\n";
		for( int i = 0; i < num; i++ ) {
			body += "\t\t\t"+sumType+" k"+(i+1)+" = kernel.data["+i+"];\n";
		}
		body += "\t\t\tint indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;\n" +
				"\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\tint indexSrc = indexSrcRow + x;\n" +
				"\n" +
				"\t\t\t\t"+sumType+" total = 0;\n";
		for( int i = 0; i < num-1; i++ ) {
			body += "\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ")* k" + (i + 1) + ";\n";
		}
		body += "\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* k" + num + ";\n";
		body += "\n" +
				"\t\t\t\ttotalRow[x] = total;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// rest of the convolution rows are an addition\n" +
				"\t\t\tfor( int i = 1; i < " + num + "; i++ ) {\n" +
				"\t\t\t\tindexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;\n" +
				"\t\t\t\t\n";
		for( int i = 0; i < num; i++ ) {
			body += "\t\t\t\tk" + (i + 1) + " = kernel.data[i*" + num + " + " + i + "];\n";
		}
		body += "\n" +
				"\t\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\t\tint indexSrc = indexSrcRow+x;\n" +
				"\n" +
				"\t\t\t\t\t" + sumType + " total = 0;\n";
		for( int i = 0; i < num-1; i++ ) {
			body += "\t\t\t\t\ttotal += (dataSrc[indexSrc++] " + bitWise + ")* k" + (i + 1) + ";\n";
		}
		body += "\t\t\t\t\ttotal += (dataSrc[indexSrc] " + bitWise + ")* k" + num + ";\n";
		body += "\n" +
				"\t\t\t\t\ttotalRow[x] += total;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\tint indexDst = dest.startIndex + y*dest.stride+kernelRadius;\n" +
				"\t\t\tfor( int x = kernelRadius; x < width-kernelRadius; x++ ) {\n" +
				"\t\t\t\tdataDst[indexDst++] = " + typeCast + "((totalRow[x]+halfDivisor)/ divisor);\n" +
				"\t\t\t}\n" +
				"\t\t}\n";

		printParallelBlock("y0","y1","kernelRadius","height-kernelRadius","kernelWidth", body);

		out.print("\t}\n\n");
	}

	private String generateTypeCast() {
		return sumType.compareTo(dataOutput) == 0 ? "" : "( " + dataOutput + " )";
	}

	public static void main(String[] args) throws FileNotFoundException {
		var generator = new GenerateConvolvedUnrolled_SB();
		generator.setModuleName("boofcv-ip");
		generator.parseArguments(args);
		generator.generateCode();
	}
}
