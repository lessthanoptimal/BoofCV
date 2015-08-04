/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateImplBilinearPixel_IL extends CodeGeneratorBase {
	AutoTypeImage image;

	String floatType;
	String floatTypeCast;
	String f;
	String borderType;
	String imageName;

	@Override
	public void generate() throws FileNotFoundException {
		createType(AutoTypeImage.F64);
		createType(AutoTypeImage.F32);
		createType(AutoTypeImage.U8);
		createType(AutoTypeImage.S16);
		createType(AutoTypeImage.S32);
	}

	private void createType( AutoTypeImage type ) throws FileNotFoundException {
		className = "ImplBilinearPixel_IL_"+type.name();
		image = type;

		imageName = type.getInterleavedName();
		createFile();
	}

	private void createFile() throws FileNotFoundException {

		borderType = image.isInteger() ? "S32" : image.getAbbreviatedType();
		floatType = !image.isInteger() && image.getNumBits()==64 ? "double" : "float";
		f = !image.isInteger() && image.getNumBits()==64 ? "" : "f";

		if( !floatType.equals("float")) {
			floatTypeCast = "(float)";
		} else {
			floatTypeCast = "";
		}

		printPreamble();
		printGetFast();
		printGetBorder();
		printTheRest();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);

		String sumType = image.getSumType();

		out.print("import boofcv.alg.interpolate.BilinearPixelMB;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.struct.image." + image.getInterleavedName() + ";\n" +
				"import boofcv.core.image.border.ImageBorder_IL_" + borderType + ";\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Implementation of {@link BilinearPixelMB} for a specific image type.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using " + getClass().getSimpleName() + ".\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class " + className + " extends BilinearPixelMB<" + imageName + "> {\n" +
				"\t"+sumType+" temp0[];\n" +
				"\t"+sumType+" temp1[];\n" +
				"\t"+sumType+" temp2[];\n" +
				"\t"+sumType+" temp3[];\n" +
				"\n" +
				"\tpublic " + className + "(int numBands) {\n" +
				"\t\tthis.temp0 = new "+sumType+"[numBands];\n" +
				"\t\tthis.temp1 = new "+sumType+"[numBands];\n" +
				"\t\tthis.temp2 = new "+sumType+"[numBands];\n" +
				"\t\tthis.temp3 = new "+sumType+"[numBands];\n" +
				"\t}\n" +
				"\n" +
				"\tpublic " + className + "(" + imageName + " orig) {\n" +
				"\t\tthis(orig.getNumBands());\n" +
				"\t\tsetImage(orig);\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic void setImage(" + imageName + " image) {\n" +
				"\t\tif( image.getNumBands() != temp0.length )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Number of bands doesn't match\");\n" +
				"\t\tsuper.setImage(image);\n" +
				"\t}\n");
	}

	private void printGetFast() {
		String bitWise = image.getBitWise();

		out.print("\t@Override\n" +
				"\tpublic void get_fast(float x, float y, float[] values) {\n" +
				"\t\tint xt = (int) x;\n" +
				"\t\tint yt = (int) y;\n" +
				"\t\t" + floatType + " ax = x - xt;\n" +
				"\t\t" + floatType + " ay = y - yt;\n" +
				"\n" +
				"\t\tfinal int numBands = orig.numBands;\n" +
				"\t\tint index = orig.startIndex + yt * stride + xt*numBands;\n" +
				"\n" +
				"\t\t" + image.getDataType() + "[] data = orig.data;\n" +
				"\n" +
				"\t\t// computing this just once doesn't seem to change speed very much.  Keeping it here to avoid trying\n" +
				"\t\t// it again in the future\n" +
				"\t\t" + floatType + " a00 = (1.0f - ax) * (1.0f - ay);\n" +
				"\t\t" + floatType + " a10 = ax * (1.0f - ay);\n" +
				"\t\t" + floatType + " a11 = ax * ay;\n" +
				"\t\t" + floatType + " a01 = (1.0f - ax) * ay;\n" +
				"\n" +
				"\t\tfor( int i = 0; i < numBands; i++ ) {\n" +
				"\t\t\tint indexBand = index+i;\n" +
				"\t\t\t" + floatType + " val = a00 * (data[indexBand]" + bitWise + " );                // (x,y)\n" +
				"\t\t\tval += a10 * (data[indexBand + numBands ]" + bitWise + " );         // (x+1,y)\n" +
				"\t\t\tval += a11 * (data[indexBand + numBands + stride]" + bitWise + " ); // (x+1,y+1)\n" +
				"\t\t\tval += a01 * (data[indexBand + stride]" + bitWise + " );            // (x,y+1)\n" +
				"\n" +
				"\t\t\tvalues[i] = " + floatTypeCast + "val;\n" +
				"\t\t}\n" +
				"\t}\n" +
				"\n");
	}

	private void printGetBorder() {

		String sumToFloat = image.getSumType().equals("float") ? "" : "(float)";

		out.print("\tpublic void get_border(float x, float y, float[] values) {\n" +
				"\t\tfloat xf = (float)Math.floor(x);\n" +
				"\t\tfloat yf = (float)Math.floor(y);\n" +
				"\t\tint xt = (int) xf;\n" +
				"\t\tint yt = (int) yf;\n" +
				"\t\tfloat ax = x - xf;\n" +
				"\t\tfloat ay = y - yf;\n" +
				"\n" +
				"\t\tImageBorder_IL_"+borderType+" border = (ImageBorder_IL_"+borderType+")this.border;\n" +
				"\t\tborder.get(xt   , yt  , temp0);\n" +
				"\t\tborder.get(xt+1 , yt  , temp1);\n" +
				"\t\tborder.get(xt+1 , yt+1, temp2);\n" +
				"\t\tborder.get(xt   , yt+1, temp3);\n" +
				"\n" +
				"\t\tfinal int numBands = orig.numBands;\n" +
				"\n" +
				"\t\tfor( int i = 0; i < numBands; i++ ) {\n" +
				"\t\t\tfloat val = (1.0f - ax) * (1.0f - ay) * "+sumToFloat+"temp0[i]; // (x,y)\n" +
				"\t\t\tval += ax * (1.0f - ay) * "+sumToFloat+"temp1[i];               // (x+1,y)\n" +
				"\t\t\tval += ax * ay * "+sumToFloat+"temp2[i];                        // (x+1,y+1)\n" +
				"\t\t\tval += (1.0f - ax) * ay * "+sumToFloat+"temp3[i];               // (x,y+1)\n" +
				"\n" +
				"\t\t\tvalues[i] = val;\n" +
				"\t\t}\n" +
				"\t}\n" +
				"\n");
	}

	private void printTheRest() {
		out.print("\t@Override\n" +
				"\tpublic void get(float x, float y, float[] values) {\n" +
				"\t\tif (x < 0 || y < 0 || x > width-2 || y > height-2)\n" +
				"\t\t\tget_border(x,y,values);\n" +
				"\t\telse\n" +
				"\t\t\tget_fast(x, y, values);\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic ImageType<"+imageName+"> getImageType() {\n" +
				"\t\treturn orig.getImageType();\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImplBilinearPixel_IL gen = new GenerateImplBilinearPixel_IL();
		gen.generate();
	}
}
