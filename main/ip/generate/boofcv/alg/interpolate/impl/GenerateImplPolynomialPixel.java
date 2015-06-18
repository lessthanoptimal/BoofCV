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
 * Code generator for polynomial interpolation
 *
 * @author Peter Abeles
 */
public class GenerateImplPolynomialPixel extends CodeGeneratorBase {
	AutoTypeImage image;
	String borderType;

	@Override
	public void generate() throws FileNotFoundException {
		createType(AutoTypeImage.F32);
		createType(AutoTypeImage.I);
	}

	private void createType( AutoTypeImage type ) throws FileNotFoundException {
		className = "ImplPolynomialPixel_"+type.name();
		image = type;
		borderType = type.isInteger() ? "I32" : type.getAbbreviatedType();

		createFile();
	}

	private void createFile() throws FileNotFoundException {
		printPreamble();
		printTheRest();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		setOutputFile(className);
		out.print("import boofcv.alg.interpolate.PolynomialPixel;\n" +
				"import boofcv.struct.image.*;\n" +
				"import boofcv.core.image.border.ImageBorder_"+borderType+";\n");
		out.println();
		out.print("/**\n" +
				" * <p>\n" +
				" * Implementation of {@link PolynomialPixel}.\n" +
				" * </p>\n" +
				" * <p>\n" +
				" * NOTE: This code was automatically generated using {@link "+getClass().getSimpleName()+"}.\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" extends PolynomialPixel<"+image.getSingleBandName()+"> {\n" +
				"\n" +
				"\tpublic "+className+"(int maxDegree, float min, float max) {\n" +
				"\t\tsuper(maxDegree, min, max);\n" +
				"\t}\n\n");

	}

	private void printTheRest() {
		out.print("\t@Override\n" +
						"\tpublic float get(float x, float y) {\n" +
						"\t\tif( x < 0 || y < 0 || x > image.width-1 || y > image.height-1 )\n" +
						"\t\t\treturn get_border(x,y);\n" +
						"\t\tint width = image.getWidth();\n" +
						"\t\tint height = image.getHeight();\n" +
						"\n" +
						"\t\tfinal int xt = (int) x;\n" +
						"\t\tfinal int yt = (int) y;\n" +
						"\n" +
						"\t\t// offM makes sure even numbered M will bound the test point with samples\n" +
						"\t\tint x0 = xt - M/2 + offM;\n" +
						"\t\tint x1 = x0 + M;\n" +
						"\t\tint y0 = yt - M/2 + offM;\n" +
						"\t\tint y1 = y0 + M;\n" +
						"\n" +
						"\t\tif( x0 < 0 ) { x0 = 0;}\n" +
						"\t\tif( x1 > width) {x1 = width;}\n" +
						"\n" +
						"\t\tif( y0 < 0 ) { y0 = 0;}\n" +
						"\t\tif( y1 > height) {y1 = height;}\n" +
						"\n" +
						"\t\tfinal int horizM = x1-x0;\n" +
						"\t\tfinal int vertM = y1-y0;\n" +
						"\n" +
						"\t\tinterp1D.setInput(horiz,horizM);\n" +
						"\t\tfor( int i = 0; i < vertM; i++ ) {\n" +
						"\t\t\tfor( int j = 0; j < horizM; j++ ) {\n" +
						"\t\t\t\thoriz[j] = image.get(j+x0,i+y0);\n" +
						"\t\t\t}\n" +
						"\t\t\tvert[i]=interp1D.process(x-x0,0,horizM-1);\n" +
						"\t\t}\n" +
						"\t\tinterp1D.setInput(vert,vertM);\n" +
						"\n" +
						"\t\tfloat ret = interp1D.process(y-y0,0,vertM-1);\n" +
						"\n" +
						"\t\t// because it is fitting polynomials it can go above and below max values.\n" +
						"\t\tif( ret > max ) {\n" +
						"\t\t\tret = max;\n" +
						"\t\t} else if( ret < min ) {\n" +
						"\t\t\tret = min;\n" +
						"\t\t}\n" +
						"\t\treturn ret;\n" +
						"\t}\n" +
						"\n" +
						"\t@Override\n" +
						"\tpublic float get_fast(float x, float y) {\n" +
						"\t\tint xt = (int) x;\n" +
						"\t\tint yt = (int) y;\n" +
						"\n" +
						"\t\tint x0 = xt - M/2 + offM;\n" +
						"\t\tint y0 = yt - M/2 + offM;\n" +
						"\n" +
						"\t\tinterp1D.setInput(horiz,horiz.length);\n" +
						"\t\tfor( int i = 0; i < M; i++ ) {\n" +
						"\t\t\tfor( int j = 0; j < M; j++ ) {\n" +
						"\t\t\t\thoriz[j] = image.get(j+x0,i+y0);\n" +
						"\t\t\t}\n" +
						"\t\t\tvert[i]=interp1D.process(x-x0,0,M-1);\n" +
						"\t\t}\n" +
						"\t\tinterp1D.setInput(vert,vert.length);\n" +
						"\n" +
						"\t\tfloat ret = interp1D.process(y-y0,0,M-1);\n" +
						"\n" +
						"\t\t// because it is fitting polynomials it can go above or below max or min values.\n" +
						"\t\tif( ret > max ) {\n" +
						"\t\t\tret = max;\n" +
						"\t\t} else if( ret < min ) {\n" +
						"\t\t\tret = min;\n" +
						"\t\t}\n" +
						"\t\treturn ret;\n" +
						"\t}\n" +
						"\n" +
						"\tpublic float get_border(float x, float y) {\n" +
						"\t\tint xt = (int) Math.floor(x);\n" +
						"\t\tint yt = (int) Math.floor(y);\n" +
						"\n" +
						"\t\tint x0 = xt - M/2 + offM;\n" +
						"\t\tint y0 = yt - M/2 + offM;\n" +
						"\n" +
						"\t\tImageBorder_"+borderType+" border = (ImageBorder_"+borderType+")this.border;\n" +
						"\n" +
						"\t\tinterp1D.setInput(horiz,horiz.length);\n" +
						"\t\tfor( int i = 0; i < M; i++ ) {\n" +
						"\t\t\tfor( int j = 0; j < M; j++ ) {\n" +
						"\t\t\t\thoriz[j] = border.get(j+x0,i+y0);\n" +
						"\t\t\t}\n" +
						"\t\t\tvert[i]=interp1D.process(x-x0,0,M-1);\n" +
						"\t\t}\n" +
						"\t\tinterp1D.setInput(vert,vert.length);\n" +
						"\n" +
						"\t\tfloat ret = interp1D.process(y-y0,0,M-1);\n" +
						"\n" +
						"\t\t// because it is fitting polynomials it can go above or below max or min values.\n" +
						"\t\tif( ret > max ) {\n" +
						"\t\t\tret = max;\n" +
						"\t\t} else if( ret < min ) {\n" +
						"\t\t\tret = min;\n" +
						"\t\t}\n" +
						"\t\treturn ret;\n" +
						"\t}\n" +
						"\t@Override\n" +
						"\tpublic ImageType<"+image.getSingleBandName()+"> getImageType() {\n" +
						"\t\treturn ImageType.single("+image.getSingleBandName()+".class);\n" +
						"\t}\n\n"
		);
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImplPolynomialPixel gen = new GenerateImplPolynomialPixel();
		gen.generate();
	}
}
