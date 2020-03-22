/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
 *
 * @author Peter Abeles
 */
public class GenerateConvolveImageMean extends CodeGeneratorBase {
	AutoTypeImage src;
	AutoTypeImage dst;

	@Override
	public void generate() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I8);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.println("}");
	}

	public void addFunctions( AutoTypeImage imageIn , AutoTypeImage imageOut ) {
		this.src = imageIn;
		this.dst = imageOut;
		horizontal(src.getSingleBandName(),dst.getSingleBandName());
		vertical(src.getSingleBandName(),dst.getSingleBandName());
		horizontalBorder(src.getSingleBandName(),dst.getSingleBandName());
		verticalBorder(src.getSingleBandName(),dst.getSingleBandName());
	}

	private void printPreamble() {
		out.print("import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;\n" +
				"import boofcv.alg.filter.convolve.noborder.ImplConvolveMean;\n" +
				"import boofcv.alg.filter.convolve.noborder.ImplConvolveMean_MT;\n" +
				"import boofcv.alg.filter.convolve.normalized.ConvolveNormalized_JustBorder_SB;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.concurrency.DWorkArrays;\n" +
				"import boofcv.concurrency.FWorkArrays;\n" +
				"import boofcv.concurrency.IWorkArrays;\n" +
				"import boofcv.factory.filter.kernel.FactoryKernel;\n" +
				"import boofcv.struct.border.*;\n" +
				"import boofcv.struct.convolve.*;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"import javax.annotation.Nullable;\n" +
				"\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Convolves a mean filter across the image.  The mean value of all the pixels are computed inside the kernel.\n" +
				" * </p>\n" +
				" *\n" +
				generateDocString() +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				generatedAnnotation() +
				"public class "+className+" {\n\n");
	}

	private void horizontal( String srcName , String dstName ) {
		String suffix = src.getKernelType();
		String normalized = src.isInteger() ? "" : " , true";

		out.print("\t/**\n" +
				"\t * Performs a horizontal 1D mean box filter. Borders are handled by reducing the box size.\n" +
				"\t *\n" +
				"\t * @param input The input image. Not modified.\n" +
				"\t * @param output Where the resulting image is written to. Modified.\n" +
				"\t * @param offset Start offset from pixel coordinate\n" +
				"\t * @param length How long the mean filter is\n" +
				"\t */\n" +
				"\tpublic static void horizontal("+ srcName+" input, "+ dstName+" output, int offset, int length) {\n" +
				"\t\toutput.reshape(input);\n" +
				"\n" +
				"\t\tif( BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length) )\n" +
				"\t\t\treturn;\n" +
				"\n" +
				"\t\tKernel1D_"+suffix+" kernel = FactoryKernel.table1D_"+suffix+"(offset, length"+normalized+");\n" +
				"\t\tif (length > input.width) {\n" +
				"\t\t\tConvolveImageNormalized.horizontal(kernel, input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tConvolveNormalized_JustBorder_SB.horizontal(kernel, input, output);\n" +
				"\t\t\tif(BoofConcurrency.USE_CONCURRENT) {\n" +
				"\t\t\t\tImplConvolveMean_MT.horizontal(input, output, offset, length);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tImplConvolveMean.horizontal(input, output, offset, length);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void vertical( String srcName , String dstName ) {
		String workArray = src.getLetterSum()+"WorkArrays";
		String suffix = src.getKernelType();
		String normalized = src.isInteger() ? "" : " , true";

		out.print("\t/**\n" +
				"\t * Performs a vertical 1D mean box filter. Borders are handled by reducing the box size.\n" +
				"\t *\n" +
				"\t * @param input The input image. Not modified.\n" +
				"\t * @param output Where the resulting image is written to. Modified.\n" +
				"\t * @param offset Start offset from pixel coordinate\n" +
				"\t * @param length How long the mean filter is\n" +
				"\t * @param work (Optional) Storage for work array\n" +
				"\t */\n" +
				"\tpublic static void vertical("+srcName+" input, "+dstName+" output, int offset, int length, @Nullable "+workArray+" work) {\n" +
				"\t\toutput.reshape(input);\n" +
				"\n" +
				"\t\tif( BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length) )\n" +
				"\t\t\treturn;\n" +
				"\n" +
				"\t\tKernel1D_"+suffix+" kernel = FactoryKernel.table1D_"+suffix+"(offset, length"+normalized+");\n" +
				"\t\tif (length > input.height) {\n" +
				"\t\t\tConvolveImageNormalized.vertical(kernel, input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tConvolveNormalized_JustBorder_SB.vertical(kernel, input, output);\n" +
				"\t\t\tif(BoofConcurrency.USE_CONCURRENT) {\n" +
				"\t\t\t\tImplConvolveMean_MT.vertical(input, output, offset, length, work);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tImplConvolveMean.vertical(input, output, offset, length, work);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void horizontalBorder( String srcName, String dstName ) {

		String suffix = src.getKernelType();
		String normalized = src.isInteger() ? "" : " , true";
		String divisor = src.isInteger() ? ", kernel.computeSum()" : "";
		String borderSuffix = src.isInteger() ? suffix+"<"+srcName+">" : suffix;

		out.print("\t/**\n" +
				"\t * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.\n" +
				"\t *\n" +
				"\t * @param input The input image. Not modified.\n" +
				"\t * @param output Where the resulting image is written to. Modified.\n" +
				"\t * @param offset Start offset from pixel coordinate\n" +
				"\t * @param binput Used to process image borders. If null borders are not processed.\n" +
				"\t * @param length How long the mean filter is\n" +
				"\t */\n" +
				"\tpublic static void horizontal("+srcName+" input, "+dstName+" output, int offset, int length, @Nullable ImageBorder_"+borderSuffix+" binput) {\n" +
				"\t\toutput.reshape(input.width,output.height);\n" +
				"\n" +
				"\t\tif( binput != null ) {\n" +
				"\t\t\tbinput.setImage(input);\n" +
				"\t\t\tKernel1D_"+suffix+" kernel = FactoryKernel.table1D_"+suffix+"(offset, length"+normalized+");\n" +
				"\t\t\tConvolveJustBorder_General_SB.horizontal(kernel, binput, output"+divisor+");\n" +
				"\t\t}\n" +
				"\t\tif (length <= input.width) {\n" +
				"\t\t\tif(BoofConcurrency.USE_CONCURRENT) {\n" +
				"\t\t\t\tImplConvolveMean_MT.horizontal(input, output, offset, length);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tImplConvolveMean.horizontal(input, output, offset, length);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void verticalBorder( String srcName, String dstName ) {
		String workArray = src.getLetterSum()+"WorkArrays";
		String suffix = src.getKernelType();
		String normalized = src.isInteger() ? "" : " , true";
		String divisor = src.isInteger() ? ", kernel.computeSum()" : "";
		String borderSuffix = src.isInteger() ? suffix+"<"+srcName+">" : suffix;

		out.print("\t/**\n" +
				"\t * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.\n" +
				"\t *\n" +
				"\t * @param input The input image. Not modified.\n" +
				"\t * @param output Where the resulting image is written to. Modified.\n" +
				"\t * @param offset Start offset from pixel coordinate\n" +
				"\t * @param binput Used to process image borders. If null borders are not processed.\n" +
				"\t * @param work (Optional) Storage for work array\n" +
				"\t */\n" +
				"\tpublic static void vertical("+srcName+" input, "+dstName+" output, int offset, int length, @Nullable ImageBorder_"+borderSuffix+" binput, @Nullable "+workArray+" work) {\n" +
				"\t\toutput.reshape(input);\n" +
				"\n" +
				"\t\tif( binput != null ) {\n" +
				"\t\t\tbinput.setImage(input);\n" +
				"\t\t\tKernel1D_"+suffix+" kernel = FactoryKernel.table1D_"+suffix+"(offset, length"+normalized+");\n" +
				"\t\t\tConvolveJustBorder_General_SB.vertical(kernel, binput, output"+divisor+");\n" +
				"\t\t}\n" +
				"\t\tif (length <= input.height) {\n" +
				"\t\t\tif(BoofConcurrency.USE_CONCURRENT) {\n" +
				"\t\t\t\tImplConvolveMean_MT.vertical(input, output, offset, length, work);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tImplConvolveMean.vertical(input, output, offset, length, work);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		GenerateConvolveImageMean gen = new GenerateConvolveImageMean();
		gen.generate();
	}
}
