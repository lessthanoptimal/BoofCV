/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.impl;

import boofcv.misc.AutoTypeImage;
import boofcv.misc.CodeGeneratorBase;
import boofcv.misc.CodeGeneratorUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Generator for bilinear interpolation
 *
 * @author Peter Abeles
 */
public class GenerateBilinearRectangle extends CodeGeneratorBase {

	String className;

	PrintStream out;
	AutoTypeImage image;

	@Override
	public void generate() throws FileNotFoundException {
		createType(AutoTypeImage.F32);
		createType(AutoTypeImage.U8);
		createType(AutoTypeImage.S16);
	}

	private void createType( AutoTypeImage type ) throws FileNotFoundException {
		className = "BilinearRectangle_"+type.name();
		image = type;

		createFile();
	}

	private void createFile() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(className + ".java"));
		printPreamble();
		printTheRest();
		out.println("}");
	}

	private void printPreamble() {
		out.print(CodeGeneratorUtil.copyright);
		out.print("package boofcv.alg.interpolate.impl;\n");
		out.println();
		out.print("import boofcv.alg.interpolate.InterpolateRectangle;\n" +
				"import boofcv.struct.image."+image.getSingleBandName()+";\n");
		if( image.getSingleBandName().compareTo("GrayF32") != 0 )
			out.println("import boofcv.struct.image.GrayF32;");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Performs bilinear interpolation to extract values between pixels in an image.\n" +
				" * Image borders are detected and handled appropriately.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link GenerateBilinearRectangle}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" implements InterpolateRectangle<"+image.getSingleBandName()+"> {\n" +
				"\n" +
				"\tprivate "+image.getSingleBandName()+" orig;\n" +
				"\n" +
				"\tprivate "+image.getDataType()+" data[];\n" +
				"\tprivate int stride;\n" +
				"\n" +
				"\tpublic "+className+"("+image.getSingleBandName()+" image) {\n" +
				"\t\tsetImage(image);\n" +
				"\t}\n" +
				"\n" +
				"\tpublic "+className+"() {\n" +
				"\t}\n\n");
	}
	
	private void printTheRest() {

		String bitWise = image.getBitWise();

		out.print("\t@Override\n" +
				"\tpublic void setImage("+image.getSingleBandName()+" image) {\n" +
				"\t\tthis.orig = image;\n" +
				"\t\tthis.data = orig.data;\n" +
				"\t\tthis.stride = orig.getStride();\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic "+image.getSingleBandName()+" getImage() {\n" +
				"\t\treturn orig;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic void region(float tl_x, float tl_y, GrayF32 output ) {\n" +
				"\t\tif( tl_x < 0 || tl_y < 0 || tl_x + output.width > orig.width || tl_y + output.height > orig.height ) {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Region is outside of the image\");\n" +
				"\t\t}\n" +
				"\t\tint xt = (int) tl_x;\n" +
				"\t\tint yt = (int) tl_y;\n" +
				"\t\tfloat ax = tl_x - xt;\n" +
				"\t\tfloat ay = tl_y - yt;\n" +
				"\n" +
				"\t\tfloat bx = 1.0f - ax;\n" +
				"\t\tfloat by = 1.0f - ay;\n" +
				"\n" +
				"\t\tfloat a0 = bx * by;\n" +
				"\t\tfloat a1 = ax * by;\n" +
				"\t\tfloat a2 = ax * ay;\n" +
				"\t\tfloat a3 = bx * ay;\n" +
				"\n" +
				"\t\tint regWidth = output.width;\n" +
				"\t\tint regHeight = output.height;\n" +
				"\t\tfinal float results[] = output.data;\n" +
				"\t\tboolean borderRight = false;\n" +
				"\t\tboolean borderBottom = false;\n" +
				"\n" +
				"\t\t// make sure it is in bounds or if its right on the image border\n" +
				"\t\tif (xt + regWidth >= orig.width || yt + regHeight >= orig.height) {\n" +
				"\t\t\tif( (xt + regWidth > orig.width || yt + regHeight > orig.height) )\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"requested region is out of bounds\");\n" +
				"\t\t\tif( xt+regWidth == orig.width ) {\n" +
				"\t\t\t\tregWidth--;\n" +
				"\t\t\t\tborderRight = true;\n" +
				"\t\t\t}\n" +
				"\t\t\tif( yt+regHeight == orig.height ) {\n" +
				"\t\t\t\tregHeight--;\n" +
				"\t\t\t\tborderBottom = true;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// perform the interpolation while reducing the number of times the image needs to be accessed\n" +
				"\t\tfor (int i = 0; i < regHeight; i++) {\n" +
				"\t\t\tint index = orig.startIndex + (yt + i) * stride + xt;\n" +
				"\t\t\tint indexResults = output.startIndex + i*output.stride;\n" +
				"\n" +
				"\t\t\tfloat XY = data[index]"+bitWise+";\n" +
				"\t\t\tfloat Xy = data[index + stride]"+bitWise+";\n" +
				"\n" +
				"\t\t\tint indexEnd = index + regWidth;\n" +
				"\t\t\t// for( int j = 0; j < regWidth; j++, index++ ) {\n" +
				"\t\t\tfor (; index < indexEnd; index++) {\n" +
				"\t\t\t\tfloat xY = data[index + 1]"+bitWise+";\n" +
				"\t\t\t\tfloat xy = data[index + stride + 1]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\tfloat val = a0 * XY + a1 * xY + a2 * xy + a3 * Xy;\n" +
				"\n" +
				"\t\t\t\tresults[indexResults++] = val;\n" +
				"\t\t\t\tXY = xY;\n" +
				"\t\t\t\tXy = xy;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t\n" +
				"\t\t// if touching the image border handle the special case\n" +
				"\t\tif( borderBottom || borderRight )\n" +
				"\t\t\thandleBorder(output, xt, yt, ax, ay, bx, by, regWidth, regHeight, results, borderRight, borderBottom);\n" +
				"\t}\n" +
				"\n" +
				"\tprivate void handleBorder( GrayF32 output,\n" +
				"\t\t\t\t\t\t\t  int xt, int yt,\n" +
				"\t\t\t\t\t\t\t  float ax, float ay, float bx, float by,\n" +
				"\t\t\t\t\t\t\t  int regWidth, int regHeight, float[] results,\n" +
				"\t\t\t\t\t\t\t  boolean borderRight, boolean borderBottom) {\n" +
				"\n" +
				"\t\tif( borderRight ) {\n" +
				"\t\t\tfor( int y = 0; y < regHeight; y++ ) {\n" +
				"\t\t\t\tint index = orig.startIndex + (yt + y) * stride + xt + regWidth;\n" +
				"\t\t\t\tint indexResults = output.startIndex + y*output.stride + regWidth;\n" +
				"\n" +
				"\t\t\t\tfloat XY = data[index]"+bitWise+";\n" +
				"\t\t\t\tfloat Xy = data[index + stride]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\tresults[indexResults] = by*XY + ay*Xy;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tif( borderBottom ) {\n" +
				"\t\t\t\toutput.set(regWidth,regHeight, orig.get(xt+ regWidth,yt+regHeight));\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tfloat XY = orig.get(xt+ regWidth,yt+regHeight-1);\n" +
				"\t\t\t\tfloat Xy = orig.get(xt+ regWidth,yt+regHeight);\n" +
				"\n" +
				"\t\t\t\toutput.set(regWidth,regHeight-1, by*XY + ay*Xy);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tif( borderBottom ) {\n" +
				"\t\t\tfor( int x = 0; x < regWidth; x++ ) {\n" +
				"\t\t\t\tint index = orig.startIndex + (yt + regHeight) * stride + xt + x;\n" +
				"\t\t\t\tint indexResults = output.startIndex + regHeight *output.stride + x;\n" +
				"\n" +
				"\t\t\t\tfloat XY = data[index]"+bitWise+";\n" +
				"\t\t\t\tfloat Xy = data[index + 1]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\tresults[indexResults] = bx*XY + ax*Xy;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tif( !borderRight ) {\n" +
				"\t\t\t\tfloat XY = orig.get(xt+regWidth-1,yt+ regHeight);\n" +
				"\t\t\t\tfloat Xy = orig.get(xt+regWidth, regHeight);\n" +
				"\n" +
				"\t\t\t\toutput.set(regWidth-1, regHeight, by*XY + ay*Xy);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateBilinearRectangle gen = new GenerateBilinearRectangle();
		gen.generate();
	}
}
