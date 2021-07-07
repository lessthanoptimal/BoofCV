/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
public class GenerateImplInterpolatePixelConvolution extends CodeGeneratorBase {

	AutoTypeImage inputType;
	String borderType;

	public GenerateImplInterpolatePixelConvolution() {
		super.className = "off";
	}

	@Override
	public void generateCode() throws FileNotFoundException {
		createFile(AutoTypeImage.F32);
		createFile(AutoTypeImage.S16);
		createFile(AutoTypeImage.U8);
	}

	private void createFile( AutoTypeImage inputType ) throws FileNotFoundException {
		this.inputType = inputType;
		borderType = inputType.isInteger() ? "S32" : inputType.getAbbreviatedType();

		this.className = null;
		setOutputFile("ImplInterpolatePixelConvolution_"+inputType.getAbbreviatedType());

		printPreamble(className);

		printFuncs();

		out.print("}\n");
	}

	private void printPreamble( String fileName ) {
		out.print("import boofcv.alg.interpolate.InterpolatePixelS;\n" +
				"import boofcv.struct.convolve.KernelContinuous1D_F32;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.struct.image.*;\n" +
				"import boofcv.struct.border.ImageBorder;\n" +
				"import boofcv.struct.border.ImageBorder_"+borderType+";\n" +
				"\n"+
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Performs interpolation by convolving a continuous-discrete function across the image. Borders are handled by\n" +
				" * re-normalizing. It is assumed that the kernel will sum up to one. This is particularly\n" +
				" * important for the unsafe_get() function which does not re-normalize.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
				"public class "+fileName+" implements InterpolatePixelS<"+inputType.getSingleBandName()+">  {\n" +
				"\n" +
				"\t// used to read outside the image border\n" +
				"\tprivate ImageBorder_"+borderType+" border;\n" +
				"\t// kernel used to perform interpolation\n" +
				"\tprivate final KernelContinuous1D_F32 kernel;\n" +
				"\t// input image\n" +
				"\tprivate "+inputType.getSingleBandName()+" image;\n" +
				"\t// minimum and maximum allowed pixel values\n" +
				"\tprivate final float min,max;\n" +
				"\n" +
				"\tpublic "+fileName+"(KernelContinuous1D_F32 kernel , float min , float max ) {\n" +
				"\t\tthis.kernel = kernel;\n" +
				"\t\tthis.min = min;\n" +
				"\t\tthis.max = max;\n" +
				"\t}\n\n");
	}

	private void printFuncs() {

		String bitWise = inputType.getBitWise();

		out.print( "\t@Override\n" +
				"\tpublic void setBorder(ImageBorder<"+inputType.getSingleBandName()+"> border) {\n" +
				"\t\tthis.border = (ImageBorder_"+borderType+")border;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic void setImage("+inputType.getSingleBandName()+" image ) {\n" +
				"\t\tif( border != null )\n" +
				"\t\t\tborder.setImage(image);\n" +
				"\t\tthis.image = image;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic "+inputType.getSingleBandName()+" getImage() {\n" +
				"\t\treturn image;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic float get(float x, float y) {\n" +
				"\n" +
				"\t\tif( x < 0 || y < 0 || x > image.width-1 || y > image.height-1 )\n" +
				"\t\t\treturn get_border(x,y);\n" +
				"\n" +
				"\t\tint xx = (int)x;\n" +
				"\t\tint yy = (int)y;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int width = kernel.getWidth();\n" +
				"\n" +
				"\t\tint x0 = xx - radius;\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\n" +
				"\t\tint y0 = yy - radius;\n" +
				"\t\tint y1 = y0 + width;\n" +
				"\n" +
				"\t\tif( x0 < 0 ) x0 = 0;\n" +
				"\t\tif( x1 > image.width ) x1 = image.width;\n" +
				"\n" +
				"\t\tif( y0 < 0 ) y0 = 0;\n" +
				"\t\tif( y1 > image.height ) y1 = image.height;\n" +
				"\n" +
				"\t\tfloat value = 0;\n" +
				"\t\tfloat totalWeightY = 0;\n" +
				"\t\tfor( int i = y0; i < y1; i++ ) {\n" +
				"\t\t\tint indexSrc = image.startIndex + i*image.stride + x0;\n" +
				"\t\t\tfloat totalWeightX = 0;\n" +
				"\t\t\tfloat valueX = 0;\n" +
				"\t\t\tfor( int j = x0; j < x1; j++ ) {\n" +
				"\t\t\t\tfloat w = kernel.compute(j-x);\n" +
				"\t\t\t\ttotalWeightX += w;\n" +
				"\t\t\t\tvalueX += w * (image.data[ indexSrc++ ]"+bitWise+");\n" +
				"\t\t\t}\n" +
				"\t\t\tfloat w = kernel.compute(i-y);\n" +
				"\t\t\ttotalWeightY +=  w;\n" +
				"\t\t\tvalue += w*valueX/totalWeightX;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tvalue /= totalWeightY;\n" +
				"\t\t\n" +
				"\t\tif( value > max )\n" +
				"\t\t\treturn max;\n" +
				"\t\telse if( value < min )\n" +
				"\t\t\treturn min;\n" +
				"\t\telse\n" +
				"\t\t\treturn value;\n" +
				"\t}\n" +
				"\n" +
				"\tpublic float get_border(float x, float y) {\n" +
				"\t\tint xx = (int)Math.floor(x);\n" +
				"\t\tint yy = (int)Math.floor(y);\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int width = kernel.getWidth();\n" +
				"\n" +
				"\t\tint x0 = xx - radius;\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\n" +
				"\t\tint y0 = yy - radius;\n" +
				"\t\tint y1 = y0 + width;\n" +
				"\n" +
				"\t\tfloat value = 0;\n" +
				"\t\tfor( int i = y0; i < y1; i++ ) {\n" +
				"\t\t\tfloat valueX = 0;\n" +
				"\t\t\tfor( int j = x0; j < x1; j++ ) {\n" +
				"\t\t\t\tfloat w = kernel.compute(j-x);\n" +
				"\t\t\t\tvalueX += w * border.get(j,i);\n" +
				"\t\t\t}\n" +
				"\t\t\tfloat w = kernel.compute(i-y);\n" +
				"\t\t\tvalue += w*valueX;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( value > max )\n" +
				"\t\t\treturn max;\n" +
				"\t\telse if( value < min )\n" +
				"\t\t\treturn min;\n" +
				"\t\telse\n" +
				"\t\t\treturn value;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic float get_fast(float x, float y) {\n" +
				"\t\tint xx = (int)x;\n" +
				"\t\tint yy = (int)y;\n" +
				"\n" +
				"\t\tfinal int radius = kernel.getRadius();\n" +
				"\t\tfinal int width = kernel.getWidth();\n" +
				"\n" +
				"\t\tint x0 = xx - radius;\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\n" +
				"\t\tint y0 = yy - radius;\n" +
				"\t\tint y1 = y0 + width;\n" +
				"\n" +
				"\t\tfloat value = 0;\n" +
				"\t\tfor( int i = y0; i < y1; i++ ) {\n" +
				"\t\t\tint indexSrc = image.startIndex + i*image.stride + x0;\n" +
				"\t\t\tfloat valueX = 0;\n" +
				"\t\t\tfor( int j = x0; j < x1; j++ ) {\n" +
				"\t\t\t\tfloat w = kernel.compute(j-x);\n" +
				"\t\t\t\tvalueX += w * (image.data[ indexSrc++ ]"+bitWise+");\n" +
				"\t\t\t}\n" +
				"\t\t\tfloat w = kernel.compute(i-y);\n" +
				"\t\t\tvalue += w*valueX;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( value > max )\n" +
				"\t\t\treturn max;\n" +
				"\t\telse if( value < min )\n" +
				"\t\t\treturn min;\n" +
				"\t\telse\n" +
				"\t\t\treturn value;\n"+
				"\t}\n"+
				"\t@Override\n" +
				"\tpublic boolean isInFastBounds(float x, float y) {\n" +
				"\t\tfloat r = kernel.getRadius();\n" +
				"\t\t\n" +
				"\t\treturn (x-r >= 0 && y-r >= 0 && x+r < image.width && y+r <image.height);\n" +
				"\t}\n"+
				"\t@Override\n" +
				"\tpublic int getFastBorderX() {\n" +
				"\t\treturn kernel.getRadius();\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic int getFastBorderY() {\n" +
				"\t\treturn kernel.getRadius();\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic ImageBorder<"+inputType.getSingleBandName()+"> getBorder() {\n" +
				"\t\treturn border;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic InterpolatePixelS<"+inputType.getSingleBandName()+"> copy() {\n" +
				"\t\tvar out = new "+className+"(kernel,min,max);\n" +
				"\t\tout.setBorder(border);\n" +
				"\t\treturn out;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic ImageType<"+inputType.getSingleBandName()+"> getImageType() {\n" +
				"\t\treturn ImageType.single("+inputType.getSingleBandName()+".class);\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplInterpolatePixelConvolution app = new GenerateImplInterpolatePixelConvolution();
		app.parseArguments(args);
		app.generateCode();
	}
}
