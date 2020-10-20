/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplAverageDownSample2 extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		down(AutoTypeImage.U8,AutoTypeImage.I8);
		down(AutoTypeImage.S8,AutoTypeImage.I8);
		down(AutoTypeImage.U16,AutoTypeImage.I16);
		down(AutoTypeImage.S16,AutoTypeImage.I16);
		down(AutoTypeImage.S32,AutoTypeImage.S32);
		down(AutoTypeImage.F32,AutoTypeImage.F32);
		down(AutoTypeImage.F64,AutoTypeImage.F64);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p>Implementation of {@link AverageDownSampleOps} specialized for square regions of width 2.</p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n");
	}

	private void down( AutoTypeImage input , AutoTypeImage output ) {

		String sumType = input.getSumType();
		String cast = input.getTypeCastFromSum();
		String bitwise = input.getBitWise();
		String computeAve4 = input.isInteger() ? "((total+2)/4)" : "(total/4)";
		String computeAve2 = input.isInteger() ? "((total+1)/2)" : "(total/2)";

		out.print("\tpublic static void down( "+input.getSingleBandName()+" input , "+output.getSingleBandName()+" output ) {\n" +
				"\t\tint maxY = input.height - input.height%2;\n" +
				"\t\tint maxX = input.width - input.width%2;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, maxY, 2, y -> {\n" +
				"\t\tfor( int y = 0; y < maxY; y += 2 ) {\n" +
				"\t\t\tint indexOut = output.startIndex + (y/2)*output.stride;\n" +
				"\n" +
				"\t\t\tint indexIn0 = input.startIndex + y*input.stride;\n" +
				"\t\t\tint indexIn1 = indexIn0 + input.stride;\n" +
				"\n" +
				"\t\t\tfor( int x = 0; x < maxX; x += 2 ) {\n" +
				"\t\t\t\t"+sumType+" total = input.data[ indexIn0++ ]"+bitwise+";\n" +
				"\t\t\t\ttotal += input.data[ indexIn0++ ]"+bitwise+";\n" +
				"\t\t\t\ttotal += input.data[ indexIn1++ ]"+bitwise+";\n" +
				"\t\t\t\ttotal += input.data[ indexIn1++ ]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\toutput.data[ indexOut++ ] = "+cast+computeAve4+";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\tif( maxX != input.width ) {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, maxY, 2, y -> {\n" +
				"\t\t\tfor( int y = 0; y < maxY; y += 2 ) {\n" +
				"\t\t\t\tint indexOut = output.startIndex + (y/2)*output.stride + output.width-1;\n" +
				"\n" +
				"\t\t\t\tint indexIn0 = input.startIndex + y*input.stride + maxX;\n" +
				"\t\t\t\tint indexIn1 = indexIn0 + input.stride;\n" +
				"\n" +
				"\t\t\t\t"+sumType+" total = input.data[ indexIn0 ]"+bitwise+";\n" +
				"\t\t\t\ttotal += input.data[ indexIn1 ]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\toutput.data[ indexOut ] = "+cast+computeAve2+";\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( maxY != input.height ) {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, maxX, 2, x -> {\n" +
				"\t\t\tfor( int x = 0; x < maxX; x += 2 ) {\n" +
				"\t\t\t\tint indexOut = output.startIndex + (output.height-1)*output.stride+x/2;\n" +
				"\t\t\t\tint indexIn0 = input.startIndex + (input.height-1)*input.stride+x;\n" +
				"\t\t\t\t"+sumType+" total = input.data[ indexIn0++ ]"+bitwise+";\n" +
				"\t\t\t\ttotal += input.data[ indexIn0++ ]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\toutput.data[ indexOut++ ] = "+cast+computeAve2+";\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( maxX != input.width && maxY != input.height ) {\n" +
				"\t\t\tint indexOut = output.startIndex + (output.height-1)*output.stride + output.width-1;\n" +
				"\t\t\tint indexIn = input.startIndex + (input.height-1)*input.stride + input.width-1;\n" +
				"\n" +
				"\t\t\toutput.data[indexOut] = input.data[ indexIn ];\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplAverageDownSample2 app = new GenerateImplAverageDownSample2();
		app.generateCode();
	}
}
