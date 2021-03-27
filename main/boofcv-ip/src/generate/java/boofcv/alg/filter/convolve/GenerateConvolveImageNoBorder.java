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

package boofcv.alg.filter.convolve;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * NOTE: There is a tinny bit of manual work required. Need to comment out a few lines to unroll
 *
 * @author Peter Abeles
 */
public class GenerateConvolveImageNoBorder extends CodeGeneratorBase {

	AutoTypeImage inputType, outputType;
	String kernelType;
	String inputName, outputName, typeIn, typeOut, sumType;

	int totalFunctions = 0;

	@Override
	public void generateCode() {
		printPreamble();
		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false, false);
		printAllOps(AutoTypeImage.F64, AutoTypeImage.F64, false, false);
		printAllOps(AutoTypeImage.U8,  AutoTypeImage.I16, false, false);
		printAllOps(AutoTypeImage.U8,  AutoTypeImage.S32, false, false);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I8,  true,  true);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, false, false);
		printAllOps(AutoTypeImage.U8,  AutoTypeImage.I8,  true,  false);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, true,  false);
		printAllOps(AutoTypeImage.U16, AutoTypeImage.I16, true,  false);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.I16, true,  true);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, false, false);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, true,  false);

		out.println("}");

		System.out.println("Total functions generated "+totalFunctions);
	}

	private void printPreamble() {
		out.print(
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.alg.filter.convolve.noborder.*;\n" +
				"import pabeles.concurrency.GrowArray;\n" +
				"import boofcv.struct.convolve.*;\n" +
				"import boofcv.struct.image.*;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"\n"+
				"import javax.annotation.Generated;\n");

		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Provides functions for convolving 1D and 2D kernels across an image, excluding the image border. 1D kernels can either\n" +
				" * be convolved along each row or column in the image. No checks are done for overflow or underflow.\n" +
				" * </p>\n" +
				" * <p>\n" +
				" * When convolving with division the convolution is computed as usual, but then the result is divided by\n" +
				" * the divisor. This is typically done when performing convolution inside of integer images to normalize\n" +
				" * it by the sum of all the elements in the convolution kernel.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * Image Edges: There is no general purpose way for handling convolutions along the image edges. Therefore unless\n" +
				" * the whole kernel can be convolved image borders are skipped. In special cases where there is a clear way to\n" +
				" * handle image edges specialized functions are provided.\n" +
				" * </p>\n" +
				generateDocString("Peter Abekes") +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\", \"rawtypes\"})\n" +
				"public class "+className+" {\n\n");
	}

	private void printAllOps(AutoTypeImage input, AutoTypeImage output, boolean hasDivide,
							 boolean justVertical )
	{
		this.inputType = input;
		this.outputType = output;
		kernelType = input.getKernelType();
		typeIn = input.name();
		typeOut = output.name();
		sumType = input.getSumType();

		inputName = input.getSingleBandName();
		outputName = output.getSingleBandName();

		if( justVertical ) {
			printFunction("vertical", true, hasDivide);
			inputName = input.getInterleavedName();
			outputName = output.getInterleavedName();
			printFunction("vertical", false, hasDivide);
		} else {
			printFunction("horizontal", true, hasDivide);
			printFunction("vertical", true, hasDivide);
			printFunction("convolve", true, hasDivide);

			inputName = input.getInterleavedName();
			outputName = output.getInterleavedName();
			printFunction("horizontal", false, hasDivide);
			printFunction("vertical", false, hasDivide);
			printFunction("convolve", false, hasDivide);
		}
	}

	private void printFunction( String name , boolean singleBand , boolean hasDivide ) {

		totalFunctions++;

		String divideArg = hasDivide ? ", int divisor" : "";
		String divideSuf = hasDivide ? "_Div" : "";
		String divideInput = hasDivide ? ", divisor" : "";
		String workspaceArg = hasDivide && singleBand && (name.equals("convolve") || name.equals("vertical"))
				? "@Nullable GrowArray<DogArray_I32> work" : "";
		String workspaceInput = workspaceArg.length()==0 ? "" : ", work";

		// Not all permutations of unrolled are generated
		boolean unrolled = inputName.substring(5).equals(outputName.substring(5));
		unrolled |= (inputName.equals("GrayU8") && outputName.equals("GrayI16"));

		String dimen = name.equals("convolve") ? "2D" : "1D";
//		String docName = name.equals("convolve") ? "" : " "+name;

//		out.print(
//				"\t/**\n" +
//				"\t * Performs a"+docName+" "+dimen+" convolution across the image. The "+name+" border is not processed.\n" +
//				"\t *\n" +
//				"\t * @param input The original image. Not modified.\n" +
//				"\t * @param output Where the resulting image is written to. Modified.\n" +
//				"\t * @param kernel The kernel that is being convolved. Not modified.\n");
//		if( hasDivide )
//			out.print("\t * @param divisor The value that the convolved image is divided by.\n");
//		out.print("\t */\n" );
		out.print(functionSignature(1,"public static void",name,"Kernel"+dimen+"_"+kernelType+" kernel",
				inputName+" input", outputName+" output"+divideArg,workspaceArg));
		out.print("\t\tInputSanityCheck.checkSameShape(input, output);\n" +
				"\n");
		out.print("\t\tif (BoofConcurrency.USE_CONCURRENT) {\n");
		if( singleBand ) {
			if (unrolled)
				out.print("\t\t\tif (!ConvolveImageUnrolled_SB_MT_"+typeIn+"_"+typeOut+divideSuf+"."+name+"(kernel, input, output"+divideInput+workspaceInput+"))\n\t");
			out.print("\t\t\tConvolveImageStandard_SB_MT."+name+"(kernel, input, output"+divideInput+workspaceInput+");\n");
		} else {
			out.print("\t\t\tConvolveImageStandard_IL_MT."+name+"(kernel, input, output"+divideInput+");\n");
		}
		out.print("\t\t} else {\n");
		if( singleBand ) {
			if (unrolled)
				out.print("\t\t\tif (!ConvolveImageUnrolled_SB_"+typeIn+"_"+typeOut+divideSuf+"."+name+"(kernel, input, output"+divideInput+workspaceInput+"))\n\t");
			out.print("\t\t\tConvolveImageStandard_SB."+name+"(kernel, input, output"+divideInput+workspaceInput+");\n");
		} else {
			out.print("\t\t\tConvolveImageStandard_IL."+name+"(kernel, input, output"+divideInput+");\n");
		}
		out.print("\t\t}\n");

		out.print("\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		var gen = new GenerateConvolveImageNoBorder();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generate();
	}
}
