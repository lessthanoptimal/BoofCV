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
public class GenerateImplAverageDownSampleN extends CodeGeneratorBase {

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
				" * <p>Implementation of {@link AverageDownSampleOps} specialized for square regions of width N.</p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n");
	}

	private void down( AutoTypeImage input , AutoTypeImage output ) {

		String sumType = input.getSumType();
		String cast = input.getTypeCastFromSum();
		String bitwise = input.getBitWise();

		String declareHalf = input.isInteger() ?  "\t\t\t"+sumType+" N_half = N/2;\n" : "";
		String updateHalf = input.isInteger() ?  "\t\t\t"+sumType+" N_half = N/2;\n" : "";
		String computeAve;

		if( input.isInteger() ) {
			if( input.isSigned() ) {
				computeAve = "total >= 0 ? "+cast+"((total+N_half)/N) : "+cast+"((total-N_half)/N)";
			} else {
				computeAve = cast+"((total+N_half)/N)";
			}
		} else {
			computeAve = cast+"(total/N)";
		}


		out.print(
				"\tpublic static void down( "+input.getSingleBandName()+" input , int sampleWidth , "+output.getSingleBandName()+" output ) {\n" +
				"\t\tint maxY = input.height - input.height%sampleWidth;\n" +
				"\t\tint maxX = input.width - input.width%sampleWidth;\n" +
				"\n" +
				"\t\t{\n" +
				"\t\t\t"+sumType+" N = sampleWidth*sampleWidth;\n" +
				declareHalf +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, maxY, sampleWidth, y -> {\n" +
				"\t\t\tfor( int y = 0; y < maxY; y += sampleWidth ) {\n" +
				"\t\t\t\tint outY = y/sampleWidth;\n"+
				"\t\t\t\tint indexOut = output.startIndex + outY*output.stride;\n" +
				"\t\t\t\tint endBoxY = y + sampleWidth;\n" +
				"\t\t\t\tfor( int x = 0; x < maxX; x += sampleWidth ) {\n" +
				"\t\t\t\t\tint endBoxX = x + sampleWidth;\n" +
				"\n" +
				"\t\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\t\tfor( int yy = y; yy < endBoxY; yy++ ) {\n" +
				"\t\t\t\t\t\tint indexIn = input.startIndex + yy*input.stride + x;\n" +
				"\t\t\t\t\t\tfor( int xx = x; xx < endBoxX; xx++ ) {\n" +
				"\t\t\t\t\t\t\ttotal += input.data[indexIn++]"+bitwise+";\n" +
				"\t\t\t\t\t\t}\n" +
				"\t\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t\toutput.data[ indexOut++ ] = "+computeAve+";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// handle the right side\n" +
				"\t\tif( maxX != input.width ) {\n" +
				"\t\t\tint N = sampleWidth*(input.width-maxX);\n" +
				updateHalf +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, maxY, sampleWidth, y -> {\n" +
				"\t\t\tfor( int y = 0; y < maxY; y += sampleWidth ) {\n" +
				"\t\t\t\tint outY = y/sampleWidth;\n"+
				"\t\t\t\tint indexOut = output.startIndex + outY*output.stride + output.width-1;\n" +
				"\t\t\t\tint endBoxY = y + sampleWidth;\n" +
				"\n" +
				"\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\tfor( int yy = y; yy < endBoxY; yy++ ) {\n" +
				"\t\t\t\t\tint indexIn = input.startIndex + yy*input.stride + maxX;\n" +
				"\t\t\t\t\tfor( int xx = maxX; xx < input.width; xx++ ) {\n" +
				"\t\t\t\t\t\ttotal += input.data[indexIn++]"+bitwise+";\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\toutput.data[ indexOut ] = "+computeAve+";\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// handle the bottom\n" +
				"\t\tif( maxY != input.height ) {\n" +
				"\t\t\t"+sumType+" N = (input.height-maxY)*sampleWidth;\n" +
				updateHalf +
				"\t\t\tint indexOut0 = output.startIndex + (output.height-1)*output.stride;\n" +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, maxX, sampleWidth, x -> {\n" +
				"\t\t\tfor( int x = 0; x < maxX; x += sampleWidth ) {\n" +
				"\t\t\t\tint indexOut = indexOut0 + x/sampleWidth;\n" +
				"\t\t\t\tint endBoxX = x + sampleWidth;\n" +
				"\n" +
				"\t\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\t\tfor( int yy = maxY; yy < input.height; yy++ ) {\n" +
				"\t\t\t\t\tint indexIn = input.startIndex + yy*input.stride + x;\n" +
				"\t\t\t\t\tfor( int xx = x; xx < endBoxX; xx++ ) {\n" +
				"\t\t\t\t\t\ttotal += input.data[indexIn++]"+bitwise+";\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\toutput.data[ indexOut ] = "+computeAve+";\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// handle the bottom right\n" +
				"\t\tif( maxX != input.width && maxY != input.height ) {\n" +
				"\t\t\t"+sumType+" N = (input.height-maxY)*(input.width-maxX);\n" +
				updateHalf +
				"\t\t\tint indexOut = output.startIndex + (output.height-1)*output.stride + output.width-1;\n" +
				"\n" +
				"\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\tfor( int yy = maxY; yy < input.height; yy++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + yy*input.stride + maxX;\n" +
				"\t\t\t\tfor( int xx = maxX; xx < input.width; xx++ ) {\n" +
				"\t\t\t\t\ttotal += input.data[indexIn++]"+bitwise+";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\toutput.data[ indexOut ] = "+computeAve+";\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplAverageDownSampleN app = new GenerateImplAverageDownSampleN();
		app.generateCode();
	}
}
