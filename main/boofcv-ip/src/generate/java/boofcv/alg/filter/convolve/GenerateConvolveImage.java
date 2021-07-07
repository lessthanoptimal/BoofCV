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

/**
 * NOTE: There is a tinny bit of manual work required. need to comment out a few lines to unroll
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GenerateConvolveImage extends CodeGeneratorBase {

	String kernelType;
	String borderName;
	String inputName, outputName, typeIn, typeOut, sumType;
	AutoTypeImage inputType;
	int totalFunctions = 0;

	@Override
	public void generateCode() {
		printPreamble();
		printAllOps(AutoTypeImage.F32, AutoTypeImage.F32, false);
//		printAllOps(AutoTypeImage.F64, AutoTypeImage.F64, false);
		printAllOps(AutoTypeImage.U8,  AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.U8,  AutoTypeImage.S32, false);
		printAllOps(AutoTypeImage.S16, AutoTypeImage.I16, false);
//		printAllOps(AutoTypeImage.U16, AutoTypeImage.I16, false);
		printAllOps(AutoTypeImage.S32, AutoTypeImage.S32, false);

		out.println("}");

		System.out.println("Total functions generated "+totalFunctions);
	}

	private void printPreamble() {
		out.print(
				"import boofcv.alg.InputSanityCheck;\n" +
						"import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_IL;\n" +
						"import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;\n" +
						"import boofcv.struct.border.ImageBorder_F32;\n" +
						"import boofcv.struct.border.ImageBorder_IL_F32;\n" +
						"import boofcv.struct.border.ImageBorder_IL_S32;\n" +
						"import boofcv.struct.border.ImageBorder_S32;\n" +
						"import boofcv.struct.convolve.Kernel1D_F32;\n" +
						"import boofcv.struct.convolve.Kernel1D_S32;\n" +
						"import boofcv.struct.convolve.Kernel2D_F32;\n" +
						"import boofcv.struct.convolve.Kernel2D_S32;\n" +
						"import boofcv.struct.image.*;\n");
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Convolves a kernel across an image and handles the image border using the specified method.\n" +
				" * </p>\n" +
				" * <p>Automatically generated by "+getClass().getSimpleName()+". DO NOT MODIFY</p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\", \"Duplicates\"})\n" +
				"public class "+className+" {\n\n");
	}

	private void printAllOps(AutoTypeImage input, AutoTypeImage output,
							 boolean justVertical )
	{
		inputType = input;
		kernelType = input.getKernelType();
		typeIn = input.name();
		typeOut = output.name();
		sumType = input.getSumType();

		inputName = input.getSingleBandName();
		outputName = output.getSingleBandName();
		borderName = "ImageBorder_";

		if( justVertical ) {
			printFunction("vertical", true);
			inputName = input.getInterleavedName();
			outputName = output.getInterleavedName();
			borderName = "ImageBorder_IL_";
			printFunction("vertical", false);
		} else {
			printFunction("horizontal", true);
			printFunction("vertical", true);
			printFunction("convolve", true);

			inputName = input.getInterleavedName();
			outputName = output.getInterleavedName();
			borderName = "ImageBorder_IL_";
			printFunction("horizontal", false);
			printFunction("vertical", false);
			printFunction("convolve", false);
		}
	}

	private void printFunction(  String name , boolean singleBand ) {

		totalFunctions++;

		String dimen = name.equals("convolve") ? "2D" : "1D";
		String docName = name.equals("convolve") ? "" : " "+name;

		String nativeName = Character.toUpperCase(name.charAt(0)) + name.substring(1);

		String suffice = singleBand ? "SB" : "IL";
		String suffice2 = singleBand ? "" : "B";

		out.print(
				"\t/**\n" +
				"\t * Performs a"+docName+" "+dimen+" convolution across the image.\n" +
				"\t *\n" +
				"\t * @param input The original image. Not modified.\n" +
				"\t * @param output Where the resulting image is written to. Modified.\n" +
				"\t * @param kernel The kernel that is being convolved. Not modified.\n" +
				"\t * @param border How the image borders are handled.\n" +
				"\t */\n" );

		String kernelName = borderName+kernelType+ (inputType.isInteger() ? "<"+inputName+">" : "");

		out.print("\tpublic static void "+name+"(Kernel"+dimen+"_"+kernelType+" kernel,\n" +
				"\t\t\t\t\t\t\t\t  "+inputName+" input, "
				+outputName+" output , "+kernelName+" border ) {\n" +
				"\t\tInputSanityCheck.checkSameShape"+suffice2+"(input, output);\n" +
				"\n" +
				"\t\tboolean processed = BOverrideConvolveImage.invokeNative"+nativeName+"(kernel,input,output,border);\n" +
				"\n" +
				"\t\tif( !processed ) {\n" +
				"\t\t\tborder.setImage(input);\n" +
				"\t\t\tConvolveImageNoBorder."+name+"(kernel,input,output);\n" +
				"\t\t\tConvolveJustBorder_General_"+suffice+"."+name+"(kernel, border,output);\n" +
				"\t\t}\n" +
				"\t}\n\n"
		);
	}

	public static void main(String[] args) {
		GenerateConvolveImage gen = new GenerateConvolveImage();
		gen.generateCode();
	}
}
