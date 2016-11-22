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

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateNearestNeighborPixel_IL extends CodeGeneratorBase {
	AutoTypeImage image;

	String floatType;
	String f;
	String borderType;

	@Override
	public void generate() throws FileNotFoundException {
//		createType(AutoTypeImage.F32); This is a special case
		createType(AutoTypeImage.S16);
		createType(AutoTypeImage.S32);
		createType(AutoTypeImage.U8);
		createType(AutoTypeImage.U16);
	}

	private void createType( AutoTypeImage type ) throws FileNotFoundException {
		className = "NearestNeighborPixel_IL_"+type.name();
		image = type;

		createFile();
	}

	private void createFile() throws FileNotFoundException {

		borderType = "IL_"+(image.isInteger() ? "S32" : image.getAbbreviatedType());
		floatType = !image.isInteger() && image.getNumBits()==64 ? "double" : "float";
		f = !image.isInteger() && image.getNumBits()==64 ? "" : "f";

		printPreamble();
		printTheRest();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);
		out.print("import boofcv.alg.interpolate.NearestNeighborPixelMB;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.struct.image." + image.getInterleavedName() + ";\n" +
				"import boofcv.core.image.border.ImageBorder_" + borderType + ";\n");
		out.println();
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Performs nearest neighbor interpolation to extract values between pixels in an image.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link "+getClass().getSimpleName()+"}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" extends NearestNeighborPixelMB<"+image.getInterleavedName()+"> {\n" +
				"\n" +
				"\tprivate "+image.getSumType()+" pixel[] = new "+image.getSumType()+"[3];" +
				"\n" +
				"\tpublic "+className+"() {\n" +
				"\t}\n" +
				"\n" +
				"\tpublic "+className+"("+image.getInterleavedName()+" orig) {\n" +
				"\n" +
				"\t\tsetImage(orig);\n" +
				"\t}\n");

	}

	private void printTheRest() {

		String sumToFloat = image.getNumBits() == 64 ? "(float)" : "";

		out.print("\t@Override\n" +
				"\tpublic void setImage("+image.getInterleavedName()+" image) {\n" +
				"\t\tsuper.setImage(image);\n" +
				"\t\tint N = image.getImageType().getNumBands();\n" +
				"\t\tif( pixel.length != N )\n" +
				"\t\t\tpixel = new "+image.getSumType()+"[ N ];\n" +
				"\t}\n\n");

		out.print("\t\t@Override\n" +
				"\tpublic void get(float x, float y, float[] values) {\n" +
				"\t\tif (x < 0 || y < 0 || x > width-1 || y > height-1 ) {\n" +
				"\t\t\t((ImageBorder_"+borderType+")border).get((int) Math.floor(x), (int) Math.floor(y), pixel);\n" +
				"\t\t\tfor (int i = 0; i < pixel.length; i++) {\n" +
				"\t\t\t\tvalues[i] = "+sumToFloat+"pixel[i];\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tget_fast(x,y,values);\n" +
				"\t\t}\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic void get_fast(float x, float y, float[] values) {\n" +
				"\t\tint xx = (int)x;\n" +
				"\t\tint yy = (int)y;\n" +
				"\n" +
				"\t\torig.unsafe_get(xx,yy,pixel);\n" +
				"\t\tfor (int i = 0; i < pixel.length; i++) {\n" +
				"\t\t\tvalues[i] = "+sumToFloat+"pixel[i];\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateNearestNeighborPixel_IL gen = new GenerateNearestNeighborPixel_IL();
		gen.generate();
	}
}
