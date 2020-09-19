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
public class GenerateImplIntegralImageConvolve extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

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
				"import static boofcv.alg.transform.ii.impl.ImplIntegralImageOps.block_zero;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Compute the integral image for different types of input images.\n" +
				" * </p>\n" +
				" * \n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n\n");
	}

	private void singleInput(AutoTypeImage image) {
		printConvolve(image,image);
		printConvolveBorder(image,image);
	}

	private void printConvolve( AutoTypeImage imageIn , AutoTypeImage imageOut) {
		out.print("\tpublic static void convolve( "+imageIn.getSingleBandName()+" integral ,\n" +
				"\t\t\t\t\t\t\t\t IntegralKernel kernel,\n" +
				"\t\t\t\t\t\t\t\t "+imageOut.getSingleBandName()+" output )\n" +
				"\t{\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, integral.height, y -> {\n" +
				"\t\tfor( int y = 0; y < integral.height; y++ ) {\n" +
				"\t\t\tfor( int x = 0; x < integral.width; x++ ) {\n" +
				"\t\t\t\t"+imageIn.getSumType()+" total = 0;\n" +
				"\t\t\t\tfor( int i = 0; i < kernel.blocks.length; i++ ) {\n" +
				"\t\t\t\t\tImageRectangle b = kernel.blocks[i];\n" +
				"\t\t\t\t\ttotal += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*kernel.scales[i];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.set(x,y,total);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printConvolveBorder(AutoTypeImage imageIn , AutoTypeImage imageOut) {
		String sumType = imageIn.getSumType();
		out.print("\tpublic static void convolveBorder( "+imageIn.getSingleBandName()+" integral ,\n" +
				"\t\t\t\t\t\t\t\t\t   IntegralKernel kernel,\n" +
				"\t\t\t\t\t\t\t\t\t   "+imageOut.getSingleBandName()+" output , int borderX , int borderY )\n" +
				"\t{\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, integral.width, x -> {\n" +
				"\t\tfor( int x = 0; x < integral.width; x++ ) {\n" +
				"\t\t\tfor( int y = 0; y < borderY; y++ ) {\n" +
				"\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\tfor( int i = 0; i < kernel.blocks.length; i++ ) {\n" +
				"\t\t\t\t\tImageRectangle b = kernel.blocks[i];\n" +
				"\t\t\t\t\ttotal += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*kernel.scales[i];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.set(x,y,total);\n" +
				"\t\t\t}\n" +
				"\t\t\tfor( int y = integral.height-borderY; y < integral.height; y++ ) {\n" +
				"\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\tfor( int i = 0; i < kernel.blocks.length; i++ ) {\n" +
				"\t\t\t\t\tImageRectangle b = kernel.blocks[i];\n" +
				"\t\t\t\t\ttotal += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*kernel.scales[i];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.set(x,y,total);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\tint endY = integral.height-borderY;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(borderY, endY, y -> {\n" +
				"\t\tfor( int y = borderY; y < endY; y++ ) {\n" +
				"\t\t\tfor( int x = 0; x < borderX; x++ ) {\n" +
				"\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\tfor( int i = 0; i < kernel.blocks.length; i++ ) {\n" +
				"\t\t\t\t\tImageRectangle b = kernel.blocks[i];\n" +
				"\t\t\t\t\ttotal += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*kernel.scales[i];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.set(x,y,total);\n" +
				"\t\t\t}\n" +
				"\t\t\tfor( int x = integral.width-borderX; x < integral.width; x++ ) {\n" +
				"\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\tfor( int i = 0; i < kernel.blocks.length; i++ ) {\n" +
				"\t\t\t\t\tImageRectangle b = kernel.blocks[i];\n" +
				"\t\t\t\t\ttotal += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*kernel.scales[i];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.set(x,y,total);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplIntegralImageConvolve app = new GenerateImplIntegralImageConvolve();
		app.generateCode();
	}
}
