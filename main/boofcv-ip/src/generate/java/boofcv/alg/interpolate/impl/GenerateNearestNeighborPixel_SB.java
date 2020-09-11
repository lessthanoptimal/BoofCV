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

package boofcv.alg.interpolate.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateNearestNeighborPixel_SB extends CodeGeneratorBase {
	AutoTypeImage image;

	String floatType;
	String f;
	String borderType;

	public GenerateNearestNeighborPixel_SB() {
		super.className = "off";
	}

	@Override
	public void generateCode() throws FileNotFoundException {
		createType(AutoTypeImage.F32);
		createType(AutoTypeImage.S16);
		createType(AutoTypeImage.S32);
		createType(AutoTypeImage.U8);
		createType(AutoTypeImage.U16);
	}

	private void createType( AutoTypeImage type ) throws FileNotFoundException {
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
		className = null;
		setOutputFile("NearestNeighborPixel_"+image.name());
		out.print(
				"import boofcv.alg.interpolate.NearestNeighborPixelS;\n" +
				"import boofcv.alg.interpolate.InterpolatePixelS;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.struct.image." + image.getSingleBandName() + ";\n" +
				"import boofcv.struct.border.ImageBorder_"+borderType+";\n" +
				"\n"+
				"import javax.annotation.Generated;\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Performs nearest neighbor interpolation to extract values between pixels in an image.\n" +
				" * </p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" extends NearestNeighborPixelS<"+image.getSingleBandName()+"> {\n" +
				"\n" +
				"\tprivate "+image.getDataType()+"[] data;" +
				"\n" +
				"\tpublic "+className+"() {}\n" +
				"\n" +
				"\tpublic "+className+"("+image.getSingleBandName()+" orig) {\n" +
				"\t\tsetImage(orig);\n" +
				"\t}\n");
	}

	private void printTheRest() {
		String bitWise = image.getBitWise();

		out.print("\t@Override\n" +
				"\tpublic void setImage("+image.getSingleBandName()+" image) {\n" +
				"\t\tsuper.setImage(image);\n" +
				"\t\tthis.data = orig.data;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic float get_fast(float x, float y) {\n" +
				"\t\treturn data[ orig.startIndex + ((int)y)*stride + (int)x]"+bitWise+";\n" +
				"\t}\n" +
				"\n" +
				"\tpublic float get_border(float x, float y) {\n" +
				"\t\treturn ((ImageBorder_"+borderType+")border).get((int)Math.floor(x),(int)Math.floor(y));\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic float get(float x, float y) {\n" +
				"\t\tif (x < 0 || y < 0 || x > width-1 || y > height-1 )\n" +
				"\t\t\treturn get_border(x,y);\n" +
				"\t\tint xx = (int)x;\n" +
				"\t\tint yy = (int)y;\n" +
				"\n" +
				"\t\treturn data[ orig.startIndex + yy*stride + xx]"+bitWise+";\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic InterpolatePixelS<"+image.getSingleBandName()+"> copy() {\n" +
				"\t\t"+className+" out = new "+className+"();\n" +
				"\t\tout.setBorder(border);\n" +
				"\t\treturn out;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic ImageType<"+image.getSingleBandName()+"> getImageType() {\n" +
				"\t\treturn ImageType.single("+image.getSingleBandName()+".class);\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateNearestNeighborPixel_SB gen = new GenerateNearestNeighborPixel_SB();
		gen.parseArguments(args);
		gen.generateCode();
	}
}
