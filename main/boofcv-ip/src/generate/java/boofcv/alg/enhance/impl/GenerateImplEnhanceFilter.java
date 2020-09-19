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

package boofcv.alg.enhance.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplEnhanceFilter extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		sharpen4(AutoTypeImage.U8);
		sharpenBorder4(AutoTypeImage.U8);
		sharpen4(AutoTypeImage.F32);
		sharpenBorder4(AutoTypeImage.F32);
		sharpen8(AutoTypeImage.U8);
		sharpenBorder8(AutoTypeImage.U8);
		sharpen8(AutoTypeImage.F32);
		sharpenBorder8(AutoTypeImage.F32);

		printVarious();

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(
				"import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n"+
				"/**\n" +
				" * <p>\n" +
				" * Filter based functions for image enhancement.\n" +
				" * </p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class ImplEnhanceFilter {\n" +
				"\n");
	}

	private void sharpen4(AutoTypeImage image) {
		String name = image.getSingleBandName();
		String bitwise = image.getBitWise();
		String cast = image.getTypeCastFromSum();
		String sumtype = image.getSumType();

		out.print("\tpublic static void sharpenInner4( "+name+" input , "+name+" output , "+sumtype+" minValue , "+sumtype+" maxValue ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(1,input.height-1,y->{\n" +
				"\t\tfor( int y = 1; y < input.height-1; y++ ) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride + 1;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride + 1;\n" +
				"\n" +
				"\t\t\tfor( int x = 1; x < input.width-1; x++ , indexIn++,indexOut++) {\n" +
				"\n" +
				"\t\t\t\t"+sumtype+" a = 5*(input.data[indexIn] "+bitwise+") - (\n" +
				"\t\t\t\t\t\t(input.data[indexIn-1] "+bitwise+")+(input.data[indexIn+1] "+bitwise+") +\n" +
				"\t\t\t\t\t\t\t\t(input.data[indexIn-input.stride] "+bitwise+") + (input.data[indexIn+input.stride] "+bitwise+"));\n" +
				"\n" +
				"\t\t\t\tif( a > maxValue )\n" +
				"\t\t\t\t\ta = maxValue;\n" +
				"\t\t\t\telse if( a < minValue )\n" +
				"\t\t\t\t\ta = minValue;\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut] = "+cast+"a;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void sharpenBorder4( AutoTypeImage image ) {

		String name = image.getSingleBandName();
		String cast = image.getTypeCastFromSum();
		String sumtype = image.getSumType();

		out.print("\tpublic static void sharpenBorder4( "+name+" input , "+name+" output , "+sumtype+" minValue , "+sumtype+" maxValue ) {\n" +
				"\n" +
				"\t\tint b = input.height-1;\n" +
				"\t\tint c = input.width-1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.width,x->{\n" +
				"\t\tfor( int x = 0; x < input.width; x++ ) {\n" +
				"\t\t\tint indexTop = input.startIndex + x;\n" +
				"\t\t\tint indexBottom = input.startIndex + b*input.stride + x;\n" +
				"\t\t\t"+sumtype+" value = 4*safeGet(input,x,0) - (safeGet(input,x-1,0) + safeGet(input,x+1,0) + safeGet(input,x,1));\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexTop++] = "+cast+"value;\n" +
				"\n" +
				"\t\t\tvalue = 4*safeGet(input,x,b) - (safeGet(input,x-1,b) + safeGet(input,x+1,b) + safeGet(input,x,b-1));\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexBottom++] = "+cast+"value;\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(1,input.height-1,y->{\n" +
				"\t\tfor( int y = 1; y < input.height-1; y++ ) {\n" +
				"\t\t\tint indexLeft = input.startIndex + input.stride*y;\n" +
				"\t\t\tint indexRight = input.startIndex + input.stride*y + c;\n" +
				"\t\t\t"+sumtype+" value = 4*safeGet(input,0,y) - (safeGet(input,1,y) + safeGet(input,0,y-1) + safeGet(input,0,y+1));\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexLeft] = "+cast+"value;\n" +
				"\n" +
				"\t\t\tvalue = 4*safeGet(input,c,y) - (safeGet(input,c-1,y) + safeGet(input,c,y-1) + safeGet(input,c,y+1));\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexRight] = "+cast+"value;\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void sharpen8(AutoTypeImage image) {
		String name = image.getSingleBandName();
		String bitwise = image.getBitWise();
		String cast = image.getTypeCastFromSum();
		String sumtype = image.getSumType();

		out.print("\tpublic static void sharpenInner8( "+name+" input , "+name+" output , "+sumtype+" minValue , "+sumtype+" maxValue ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(1,input.height-1,y->{\n" +
				"\t\tfor( int y = 1; y < input.height-1; y++ ) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride + 1;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride + 1;\n" +
				"\n" +
				"\t\t\tfor( int x = 1; x < input.width-1; x++ , indexIn++,indexOut++) {\n" +
				"\n" +
				"\t\t\t\t"+sumtype+" a11 = input.data[indexIn-input.stride-1] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a12 = input.data[indexIn-input.stride] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a13 = input.data[indexIn-input.stride+1] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a21 = input.data[indexIn-1] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a22 = input.data[indexIn] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a23 = input.data[indexIn+1] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a31 = input.data[indexIn+input.stride-1] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a32 = input.data[indexIn+input.stride] "+bitwise+";\n" +
				"\t\t\t\t"+sumtype+" a33 = input.data[indexIn+input.stride+1] "+bitwise+";\n" +
				"\t\t\t\t\n" +
				"\t\t\t\t"+sumtype+" result = 9*a22 - (a11+a12+a13+a21+a23+a31+a32+a33);\n" +
				"\n" +
				"\t\t\t\tif( result > maxValue )\n" +
				"\t\t\t\t\tresult = maxValue;\n" +
				"\t\t\t\telse if( result < minValue )\n" +
				"\t\t\t\t\tresult = minValue;\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut] = "+cast+"result;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void sharpenBorder8(AutoTypeImage image) {
		String name = image.getSingleBandName();
		String cast = image.getTypeCastFromSum();
		String sumtype = image.getSumType();

		out.print("\tpublic static void sharpenBorder8( "+name+" input , "+name+" output , "+sumtype+" minValue , "+sumtype+" maxValue ) {\n" +
				"\t\tint b = input.height-1;\n" +
				"\t\tint c = input.width-1;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.width,x->{\n" +
				"\t\tfor( int x = 0; x < input.width; x++ ) {\n" +
				"\t\t\tint indexTop = input.startIndex + x;\n" +
				"\t\t\tint indexBottom = input.startIndex + x + b*input.stride;\n" +
				"\n" +
				"\t\t\t"+sumtype+" a11 = safeGet(input,x-1,-1);\n" +
				"\t\t\t"+sumtype+" a12 = safeGet(input,x  ,-1);\n" +
				"\t\t\t"+sumtype+" a13 = safeGet(input,x+1,-1);\n" +
				"\t\t\t"+sumtype+" a21 = safeGet(input,x-1, 0);\n" +
				"\t\t\t"+sumtype+" a22 = safeGet(input,x  , 0);\n" +
				"\t\t\t"+sumtype+" a23 = safeGet(input,x+1, 0);\n" +
				"\t\t\t"+sumtype+" a31 = safeGet(input,x-1, 1);\n" +
				"\t\t\t"+sumtype+" a32 = safeGet(input,x  , 1);\n" +
				"\t\t\t"+sumtype+" a33 = safeGet(input,x+1, 1);\n" +
				"\n" +
				"\t\t\t"+sumtype+" value = 9*a22 - (a11+a12+a13+a21+a23+a31+a32+a33);\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexTop++] = "+cast+"value;\n" +
				"\n" +
				"\t\t\ta11 = safeGet(input,x-1,b-1);\n" +
				"\t\t\ta12 = safeGet(input,x  ,b-1);\n" +
				"\t\t\ta13 = safeGet(input,x+1,b-1);\n" +
				"\t\t\ta21 = safeGet(input,x-1, b);\n" +
				"\t\t\ta22 = safeGet(input,x  , b);\n" +
				"\t\t\ta23 = safeGet(input,x+1, b);\n" +
				"\t\t\ta31 = safeGet(input,x-1,b+1);\n" +
				"\t\t\ta32 = safeGet(input,x  ,b+1);\n" +
				"\t\t\ta33 = safeGet(input,x+1,b+1);\n" +
				"\n" +
				"\t\t\tvalue = 9*a22 - (a11+a12+a13+a21+a23+a31+a32+a33);\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexBottom++] = "+cast+"value;\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(1,input.height-1,y->{\n" +
				"\t\tfor( int y = 1; y < input.height-1; y++ ) {\n" +
				"\t\t\tint indexLeft = input.startIndex + input.stride*y;\n" +
				"\t\t\tint indexRight = input.startIndex + input.stride*y + c;\n" +
				"\t\t\t"+sumtype+" a11 = safeGet(input,-1,y-1);\n" +
				"\t\t\t"+sumtype+" a12 = safeGet(input, 0,y-1);\n" +
				"\t\t\t"+sumtype+" a13 = safeGet(input,+1,y-1);\n" +
				"\t\t\t"+sumtype+" a21 = safeGet(input,-1, y );\n" +
				"\t\t\t"+sumtype+" a22 = safeGet(input, 0, y );\n" +
				"\t\t\t"+sumtype+" a23 = safeGet(input,+1, y );\n" +
				"\t\t\t"+sumtype+" a31 = safeGet(input,-1,y+1);\n" +
				"\t\t\t"+sumtype+" a32 = safeGet(input, 0,y+1);\n" +
				"\t\t\t"+sumtype+" a33 = safeGet(input,+1,y+1);\n" +
				"\n" +
				"\t\t\t"+sumtype+" value = 9*a22 - (a11+a12+a13+a21+a23+a31+a32+a33);\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexLeft] = "+cast+"value;\n" +
				"\n" +
				"\t\t\ta11 = safeGet(input,c-1,y-1);\n" +
				"\t\t\ta12 = safeGet(input, c ,y-1);\n" +
				"\t\t\ta13 = safeGet(input,c+1,y-1);\n" +
				"\t\t\ta21 = safeGet(input,c-1, y );\n" +
				"\t\t\ta22 = safeGet(input, c , y );\n" +
				"\t\t\ta23 = safeGet(input,c+1, y );\n" +
				"\t\t\ta31 = safeGet(input,c-1,y+1);\n" +
				"\t\t\ta32 = safeGet(input, c ,y+1);\n" +
				"\t\t\ta33 = safeGet(input,c+1,y+1);\n" +
				"\n" +
				"\t\t\tvalue = 9*a22 - (a11+a12+a13+a21+a23+a31+a32+a33);\n" +
				"\n" +
				"\t\t\tif( value > maxValue )\n" +
				"\t\t\t\tvalue = maxValue;\n" +
				"\t\t\telse if( value < minValue )\n" +
				"\t\t\t\tvalue = minValue;\n" +
				"\n" +
				"\t\t\toutput.data[indexRight] = "+cast+"value;\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printVarious() {
		out.print("\t/**\n" +
				"\t * Handle outside image pixels by extending the image.\n" +
				"\t */\n" +
				"\tpublic static int safeGet( GrayI input , int x , int y ) {\n" +
				"\t\tif( x < 0 )\n" +
				"\t\t\tx = 0;\n" +
				"\t\telse if( x >= input.width )\n" +
				"\t\t\tx = input.width-1;\n" +
				"\t\tif( y < 0 )\n" +
				"\t\t\ty = 0;\n" +
				"\t\telse if( y >= input.height )\n" +
				"\t\t\ty = input.height-1;\n" +
				"\n" +
				"\t\treturn input.unsafe_get(x,y);\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Handle outside image pixels by extending the image.\n" +
				"\t */\n" +
				"\tpublic static float safeGet( GrayF32 input , int x , int y ) {\n" +
				"\t\tif( x < 0 )\n" +
				"\t\t\tx = 0;\n" +
				"\t\telse if( x >= input.width )\n" +
				"\t\t\tx = input.width-1;\n" +
				"\t\tif( y < 0 )\n" +
				"\t\t\ty = 0;\n" +
				"\t\telse if( y >= input.height )\n" +
				"\t\t\ty = input.height-1;\n" +
				"\n" +
				"\t\treturn input.unsafe_get(x,y);\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplEnhanceFilter app = new GenerateImplEnhanceFilter();
		app.generateCode();
	}
}
