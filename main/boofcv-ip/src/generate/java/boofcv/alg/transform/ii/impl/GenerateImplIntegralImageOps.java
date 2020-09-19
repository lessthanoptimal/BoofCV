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

package boofcv.alg.transform.ii.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateImplIntegralImageOps extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printTwoInput(AutoTypeImage.F32, AutoTypeImage.F32);
		printTwoInput(AutoTypeImage.F64, AutoTypeImage.F64);
		printTwoInput(AutoTypeImage.U8, AutoTypeImage.S32);
		printTwoInput(AutoTypeImage.S32, AutoTypeImage.S32);
		printTwoInput(AutoTypeImage.S64, AutoTypeImage.S64);
		singleInput(AutoTypeImage.F32);
		singleInput(AutoTypeImage.S32);
		singleInput(AutoTypeImage.F64);
		singleInput(AutoTypeImage.S64);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.alg.transform.ii.IntegralKernel;\n" +
				"import boofcv.struct.ImageRectangle;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Compute the integral image for different types of input images.\n" +
				" * </p>\n" +
				" * \n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n\n");
	}

	private void printTwoInput( AutoTypeImage imageIn , AutoTypeImage imageOut ) {
		printTransform(imageIn,imageOut);

	}

	private void singleInput(AutoTypeImage image) {
		printConvolveSparse(image);
		printBlockUnsafe(image);
		printBlockZero(image);
	}

	private void printTransform( AutoTypeImage imageIn , AutoTypeImage imageOut ) {

		String sumType = imageOut.getSumType();
		String bitWise = imageIn.getBitWise();
		String typeCast = imageOut.getTypeCastFromSum();

		out.print("\tpublic static void transform( final "+imageIn.getSingleBandName()+" input , final "+imageOut.getSingleBandName()+" transformed )\n" +
				"\t{\n" +
				"\t\tint indexSrc = input.startIndex;\n" +
				"\t\tint indexDst = transformed.startIndex;\n" +
				"\t\tint end = indexSrc + input.width;\n" +
				"\n" +
				"\t\t"+sumType+" total = 0;\n" +
				"\t\tfor( ; indexSrc < end; indexSrc++ ) {\n" +
				"\t\t\ttransformed.data[indexDst++] = "+typeCast+"total += input.data[indexSrc]"+bitWise+";\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor( int y = 1; y < input.height; y++ ) {\n" +
				"\t\t\tindexSrc = input.startIndex + input.stride*y;\n" +
				"\t\t\tindexDst = transformed.startIndex + transformed.stride*y;\n" +
				"\t\t\tint indexPrev = indexDst - transformed.stride;\n" +
				"\n" +
				"\t\t\tend = indexSrc + input.width;\n" +
				"\n" +
				"\t\t\ttotal = 0;\n" +
				"\t\t\tfor( ; indexSrc < end; indexSrc++ ) {\n" +
				"\t\t\t\ttotal +=  input.data[indexSrc]"+bitWise+";\n" +
				"\t\t\t\ttransformed.data[indexDst++] = transformed.data[indexPrev++] + total;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printConvolveSparse(AutoTypeImage image ) {
		String sumType = image.getSumType();

		out.print("\tpublic static "+sumType+" convolveSparse( "+image.getSingleBandName()+" integral , IntegralKernel kernel , int x , int y )\n" +
				"\t{\n" +
				"\t\t"+sumType+" ret = 0;\n" +
				"\t\tint N = kernel.getNumBlocks();\n" +
				"\n" +
				"\t\tfor( int i = 0; i < N; i++ ) {\n" +
				"\t\t\tImageRectangle r = kernel.blocks[i];\n" +
				"\t\t\tret += block_zero(integral,x+r.x0,y+r.y0,x+r.x1,y+r.y1)*kernel.scales[i];\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn ret;\n" +
				"\t}\n\n");
	}

	private void printBlockUnsafe( AutoTypeImage image ) {
		String sumType = image.getSumType();
		String bitWise = image.getBitWise();

		out.print("\tpublic static " + sumType + " block_unsafe( " + image.getSingleBandName() + " integral , int x0 , int y0 , int x1 , int y1 )\n" +
				"\t{\n" +
				"\t\t" + sumType + " br = integral.data[ integral.startIndex + y1*integral.stride + x1 ]" + bitWise + ";\n" +
				"\t\t" + sumType + " tr = integral.data[ integral.startIndex + y0*integral.stride + x1 ]" + bitWise + ";\n" +
				"\t\t" + sumType + " bl = integral.data[ integral.startIndex + y1*integral.stride + x0 ]" + bitWise + ";\n" +
				"\t\t" + sumType + " tl = integral.data[ integral.startIndex + y0*integral.stride + x0 ]" + bitWise + ";\n" +
				"\n" +
				"\t\treturn br-tr-bl+tl;\n" +
				"\t}\n\n");
	}

	private void printBlockZero( AutoTypeImage image ) {
		String sumType = image.getSumType();
		String bitWise = image.getBitWise();

		out.print("\tpublic static " + sumType + " block_zero( " + image.getSingleBandName() + " integral , int x0 , int y0 , int x1 , int y1 )\n" +
				"\t{\n" +
				"\t\tx0 = Math.min(x0,integral.width-1);\n" +
				"\t\ty0 = Math.min(y0,integral.height-1);\n" +
				"\t\tx1 = Math.min(x1,integral.width-1);\n" +
				"\t\ty1 = Math.min(y1,integral.height-1);\n" +
				"\n" +
				"\t\t" + sumType + " br=0,tr=0,bl=0,tl=0;\n" +
				"\n" +
				"\t\tif( x1 >= 0 && y1 >= 0)\n" +
				"\t\t\tbr = integral.data[ integral.startIndex + y1*integral.stride + x1 ]" + bitWise + ";\n" +
				"\t\tif( y0 >= 0 && x1 >= 0)\n" +
				"\t\t\ttr = integral.data[ integral.startIndex + y0*integral.stride + x1 ]" + bitWise + ";\n" +
				"\t\tif( x0 >= 0 && y1 >= 0)\n" +
				"\t\t\tbl = integral.data[ integral.startIndex + y1*integral.stride + x0 ]" + bitWise + ";\n" +
				"\t\tif( x0 >= 0 && y0 >= 0)\n" +
				"\t\t\ttl = integral.data[ integral.startIndex + y0*integral.stride + x0 ]" + bitWise + ";\n" +
				"\n" +
				"\t\treturn br-tr-bl+tl;\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplIntegralImageOps app = new GenerateImplIntegralImageOps();
		app.generateCode();
	}
}
