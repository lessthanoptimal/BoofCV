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
public class GenerateImplBilinearPixel extends CodeGeneratorBase {
	AutoTypeImage image;

	String floatType;
	String f;
	String borderType;

	@Override
	public void generate() throws FileNotFoundException {
		createType(AutoTypeImage.F64);
		createType(AutoTypeImage.F32);
		createType(AutoTypeImage.U8);
		createType(AutoTypeImage.S16);
		createType(AutoTypeImage.S32);
	}

	private void createType( AutoTypeImage type ) throws FileNotFoundException {
		className = "ImplBilinearPixel_"+type.name();
		image = type;

		createFile();
	}

	private void createFile() throws FileNotFoundException {

		borderType = image.isInteger() ? "S32" : image.getAbbreviatedType();
		floatType = !image.isInteger() && image.getNumBits()==64 ? "double" : "float";
		f = !image.isInteger() && image.getNumBits()==64 ? "" : "f";

		printPreamble();
		printTheRest();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);
		out.print("import boofcv.alg.interpolate.BilinearPixelS;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.struct.image." + image.getSingleBandName() + ";\n" +
				"import boofcv.core.image.border.ImageBorder_"+borderType+";\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Implementation of {@link BilinearPixelS} for a specific image type.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link GenerateImplBilinearPixel}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" extends BilinearPixelS<"+image.getSingleBandName()+"> {\n" +
				"\n" +
				"\tpublic "+className+"() {\n" +
				"\t}\n" +
				"\n" +
				"\tpublic "+className+"("+image.getSingleBandName()+" orig) {\n" +
				"\n" +
				"\t\tsetImage(orig);\n" +
				"\t}\n");

	}

	private void printTheRest() {
		String bitWise = image.getBitWise();

		out.print("\t@Override\n" +
				"\tpublic float get_fast(float x, float y) {\n" +
				"\t\tint xt = (int) x;\n" +
				"\t\tint yt = (int) y;\n" +
				"\t\t"+floatType+" ax = x - xt;\n" +
				"\t\t"+floatType+" ay = y - yt;\n" +
				"\n" +
				"\t\tint index = orig.startIndex + yt * stride + xt;\n" +
				"\n" +
				"\t\t"+image.getDataType()+"[] data = orig.data;\n" +
				"\n" +
				"\t\t"+floatType+" val = (1.0"+f+" - ax) * (1.0"+f+" - ay) * (data[index] "+bitWise+"); // (x,y)\n" +
				"\t\tval += ax * (1.0"+f+" - ay) * (data[index + 1] "+bitWise+"); // (x+1,y)\n" +
				"\t\tval += ax * ay * (data[index + 1 + stride] "+bitWise+"); // (x+1,y+1)\n" +
				"\t\tval += (1.0"+f+" - ax) * ay * (data[index + stride] "+bitWise+"); // (x,y+1)\n" +
				"\n" +
				"\t\treturn val;\n" +
				"\t}\n" +
				"\n" +
				"\tpublic float get_border(float x, float y) {\n" +
				"\t\t"+floatType+" xf = ("+floatType+")Math.floor(x);\n" +
				"\t\t"+floatType+" yf = ("+floatType+")Math.floor(y);\n" +
				"\t\tint xt = (int) xf;\n" +
				"\t\tint yt = (int) yf;\n" +
				"\t\t"+floatType+" ax = x - xf;\n" +
				"\t\t"+floatType+" ay = y - yf;\n" +
				"\n" +
				"\t\tImageBorder_"+borderType+" border = (ImageBorder_"+borderType+")this.border;\n" +
				"\n" +
				"\t\t"+floatType+" val = (1.0f - ax) * (1.0f - ay) * border.get(xt,yt); // (x,y)\n" +
				"\t\tval += ax * (1.0f - ay) *  border.get(xt + 1, yt);; // (x+1,y)\n" +
				"\t\tval += ax * ay *  border.get(xt + 1, yt + 1);; // (x+1,y+1)\n" +
				"\t\tval += (1.0f - ax) * ay *  border.get(xt,yt+1);; // (x,y+1)\n" +
				"\n" +
				"\t\treturn val;\n" +
				"\t}\n"+
				"\n" +
				"\t@Override\n" +
				"\tpublic float get(float x, float y) {\n" +
				"\t\tif (x < 0 || y < 0 || x > width-2 || y > height-2)\n" +
				"\t\t\treturn get_border(x,y);\n" +
				"\n" +
				"\t\treturn get_fast(x,y);\n" +
				"\t}\n"+
				"\n" +
				"\t@Override\n" +
				"\tpublic ImageType<"+image.getSingleBandName()+"> getImageType() {\n" +
				"\t\treturn ImageType.single("+image.getSingleBandName()+".class);\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImplBilinearPixel gen = new GenerateImplBilinearPixel();
		gen.generate();
	}
}
