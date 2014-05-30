/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

		floatType = !image.isInteger() && image.getNumBits()==64 ? "double" : "float";
		f = !image.isInteger() && image.getNumBits()==64 ? "" : "f";

		printPreamble();
		printTheRest();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);
		out.print("import boofcv.alg.interpolate.BilinearPixel;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.struct.image."+image.getSingleBandName()+";\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Implementation of {@link BilinearPixel} for a specific image type.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link GenerateImplBilinearPixel}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" extends BilinearPixel<"+image.getSingleBandName()+"> {\n" +
				"\n" +
				"\tpublic "+className+"() {\n" +
				"\t}\n" +
				"\n" +
				"\tpublic "+className+"("+image.getSingleBandName()+" orig) {\n" +
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
				"\t@Override\n" +
				"\tpublic float get(float x, float y) {\n" +
				"\t\tif (x < 0 || y < 0 || x > width-1 || y > height-1)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Point is outside of the image \"+x+\" \"+y);\n" +
				"\n" +
				"\t\tint xt = (int) x;\n" +
				"\t\tint yt = (int) y;\n" +
				"\n" +
				"\t\t"+floatType+" ax = x - xt;\n" +
				"\t\t"+floatType+" ay = y - yt;\n" +
				"\n" +
				"\t\tint index = orig.startIndex + yt * stride + xt;\n" +
				"\n" +
				"\t\t// allows borders to be interpolated gracefully by double counting appropriate pixels\n" +
				"\t\tint dx = xt == width - 1 ? 0 : 1;\n" +
				"\t\tint dy = yt == height - 1 ? 0 : stride;\n" +
				"\n" +
				"\t\t"+image.getDataType()+"[] data = orig.data;\n" +
				"\n" +
				"\t\t"+floatType+" val = (1.0"+f+" - ax) * (1.0"+f+" - ay) * (data[index] "+bitWise+"); // (x,y)\n" +
				"\t\tval += ax * (1.0"+f+" - ay) * (data[index + dx] "+bitWise+"); // (x+1,y)\n" +
				"\t\tval += ax * ay * (data[index + dx + dy] "+bitWise+"); // (x+1,y+1)\n" +
				"\t\tval += (1.0"+f+" - ax) * ay * (data[index + dy] "+bitWise+"); // (x,y+1)\n" +
				"\n" +
				"\t\treturn val;\n" +
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
