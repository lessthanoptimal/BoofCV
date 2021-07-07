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

package boofcv.alg.transform.pyramid.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplPyramidOps extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printAll(AutoTypeImage.F32);
		printAll(AutoTypeImage.U8);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.alg.interpolate.InterpolatePixelS;\n" +
				"import boofcv.struct.image.GrayF32;\n" +
				"import boofcv.struct.image.GrayU8;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Image type specific implementations of functions in {@link boofcv.alg.transform.pyramid.PyramidOps}.\n" +
				" * </p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class "+className+" {\n\n");

	}

	public void printAll( AutoTypeImage imageIn ) {
		printScaleUp(imageIn);
		printScaleDown2(imageIn);
	}

	private void printScaleUp(AutoTypeImage imageIn) {

		String imageName = imageIn.getSingleBandName();
		String typeCast = AutoTypeImage.F32 == imageIn ? "" : "("+imageIn.getDataType()+")";
		String floatType = imageIn.getNumBits() <= 32 ? "float" : "double";

		out.print("\t/**\n" +
				"\t * Scales an image up using interpolation\n" +
				"\t */\n" +
				"\tpublic static void scaleImageUp("+imageName+" input , "+imageName+" output , int scale ,\n" +
				"\t\t\t\t\t  InterpolatePixelS<"+imageName+"> interp ) {\n" +
				"\t\toutput.reshape(input.width*scale,input.height*scale);\n" +
				"\n" +
				"\t\t"+floatType+" fdiv = 1/("+floatType+")scale;\n" +
				"\t\tinterp.setImage(input);\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, output.height, y -> {\n" +
				"\t\tfor (int y = 0; y < output.height; y++) {\n" +
				"\t\t\t"+floatType+" inputY = y*fdiv;\n" +
				"\t\t\tint indexOutput = output.getIndex(0,y);\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < output.width; x++) {\n" +
				"\t\t\t\t"+floatType+" inputX = x*fdiv;\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOutput++] = "+typeCast+"interp.get(inputX,inputY);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printScaleDown2(AutoTypeImage imageIn) {

		String imageName = imageIn.getSingleBandName();

		out.print("\t/**\n" +
				"\t * Scales down the input by a factor of 2. Every other pixel along both axises is skipped.\n" +
				"\t */\n" +
				"\tpublic static void scaleDown2( "+imageName+" input , "+imageName+" output ) {\n" +
				"\t\t\n" +
				"\t\toutput.reshape(input.width / 2, input.height / 2);\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, output.height, y -> {\n" +
				"\t\tfor (int y = 0; y < output.height; y++) {\n" +
				"\t\t\tint indexInput = 2*y*input.stride;\n" +
				"\t\t\tint indexOutput = y*output.stride;\n" +
				"\t\t\tfor (int x = 0; x < output.width; x++,indexInput+=2) {\n" +
				"\t\t\t\toutput.data[indexOutput++] = input.data[indexInput];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplPyramidOps app = new GenerateImplPyramidOps();
		app.generateCode();
	}
}
