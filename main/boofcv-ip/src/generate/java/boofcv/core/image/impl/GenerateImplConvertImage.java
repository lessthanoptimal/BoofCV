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

package boofcv.core.image.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GenerateImplConvertImage extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		for( AutoTypeImage in : AutoTypeImage.getSpecificTypes()) {
			for( AutoTypeImage out : AutoTypeImage.getGenericTypes() ) {
				if( in == out )
					continue;

				printConvertSingle(in, out);
				printConvertInterleaved(in, out);
			}
			printInterleaveToPlanar(in);
			printPlanarToInterleaved(in);
		}

		// Add a few common special cases so that a temporary image doesn't need to be created
		printInterleaveToPlanar(AutoTypeImage.U8,AutoTypeImage.F32);
		printInterleaveToPlanar(AutoTypeImage.F32,AutoTypeImage.U8);
		printPlanarToInterleaved(AutoTypeImage.U8,AutoTypeImage.F32);
		printPlanarToInterleaved(AutoTypeImage.F32,AutoTypeImage.U8);

		out.print("}\n");
	}

	private void printPreamble() {
		out.print(
				"import javax.annotation.Generated;\n" +
				"import boofcv.struct.image.*;\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Functions for converting between different primitive image types. Numerical values do not change or are closely approximated\n" +
				" * in these functions. \n" +
				" * </p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class "+className+" {\n\n");
	}

	private void printConvertSingle(AutoTypeImage imageIn, AutoTypeImage imageOut) {

		String typeCast = "( "+imageOut.getDataType()+" )";
		String bitWise = imageIn.getBitWise();

		boolean sameTypes = imageIn.getDataType().compareTo(imageOut.getDataType()) == 0;

		if( imageIn.isInteger() && imageOut.isInteger() &&
				((imageOut.getNumBits() == 32 && imageIn.getNumBits() != 64) ||
				(imageOut.getNumBits() == 64)) )
			typeCast = "";
		else if( sameTypes && imageIn.isSigned() )
			typeCast = "";

		out.print("\tpublic static void convert( "+imageIn.getSingleBandName()+" input, "+imageOut.getSingleBandName()+" output ) {\n" +
				"\n" +
				"\t\tif (input.isSubimage() || output.isSubimage()) {\n" +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\t\tint indexSrc = input.getIndex(0, y);\n" +
				"\t\t\t\tint indexDst = output.getIndex(0, y);\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = "+typeCast+"( input.data[indexSrc++] "+bitWise+");\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\t} else {\n" +
				"\t\t\tfinal int N = input.width * input.height;\n" +
				"\n");

		if( sameTypes ) {
			out.print("\t\t\tSystem.arraycopy(input.data, 0, output.data, 0, N);\n");
		} else {
			out.print(
					"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0,N,(i0,i1)->{\n" +
					"\t\t\tint i0 = 0, i1 = N;\n" +
					"\t\t\tfor (int i = i0; i < i1; i++) {\n" +
					"\t\t\t\toutput.data[i] = "+typeCast+"( input.data[i] "+bitWise+");\n" +
					"\t\t\t}\n" +
					"\t\t\t//CONCURRENT_INLINE });\n"
					);
		}
		out.print("\t\t}\n" +
				"\t}\n\n");
	}

	private void printConvertInterleaved( AutoTypeImage imageIn , AutoTypeImage imageOut ) {

		String typeCast = "( "+imageOut.getDataType()+" )";
		String bitWise = imageIn.getBitWise();

		boolean sameTypes = imageIn.getDataType().compareTo(imageOut.getDataType()) == 0;

		if( imageIn.isInteger() && imageOut.isInteger() &&
				((imageOut.getNumBits() == 32 && imageIn.getNumBits() != 64) ||
						(imageOut.getNumBits() == 64)) )
			typeCast = "";
		else if( sameTypes && imageIn.isSigned() )
			typeCast = "";

		out.print("\tpublic static void convert( "+imageIn.getInterleavedName()+" input, "+imageOut.getInterleavedName()+" output ) {\n" +
				"\n" +
				"\t\tif (input.isSubimage() || output.isSubimage()) {\n" +
				"\t\t\tfinal int N = input.width * input.getNumBands();\n" +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\t\tint indexSrc = input.getIndex(0, y);\n" +
				"\t\t\t\tint indexDst = output.getIndex(0, y);\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < N; x++) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = "+typeCast+"( input.data[indexSrc++] "+bitWise+");\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\t} else {\n" +
				"\t\t\tfinal int N = input.width * input.height * input.getNumBands();\n" +
				"\n");

		if( sameTypes ) {
			out.print("\t\t\tSystem.arraycopy(input.data, 0, output.data, 0, N);\n");
		} else {
			out.print(
					"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0,N,(i0,i1)->{\n" +
					"\t\t\tint i0 = 0, i1 = N;\n" +
					"\t\t\tfor (int i = i0; i < i1; i++) {\n" +
					"\t\t\t\toutput.data[i] = "+typeCast+"( input.data[i] "+bitWise+");\n" +
					"\t\t\t}\n" +
					"\t\t\t//CONCURRENT_INLINE });\n");
		}
		out.print("\t\t}\n" +
				"\t}\n\n");
	}

	private void printInterleaveToPlanar(AutoTypeImage imageIn) {
		String inputName = imageIn.getInterleavedName();
		String bandName = imageIn.getSingleBandName();

		out.print(
				"\tpublic static void convert( "+inputName+" input , Planar<"+bandName+"> output ) {\n" +
				"\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\t\tfor (int i = 0; i < numBands; i++) {\n" +
				"\t\t\t"+bandName+" band = output.bands[i];\n" +
				"\t\t\tfinal int offset = i;\n" +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\t\tint indexSrc = y*input.stride + input.startIndex + offset;\n" +
				"\t\t\t\tint indexDst = y*output.stride + output.startIndex;\n" +
				"\t\t\t\tint end = indexDst + input.width;\n" +
				"\t\t\t\twhile( indexDst != end ) {\n" +
				"\t\t\t\t\tband.data[indexDst++] = input.data[indexSrc];\n" +
				"\t\t\t\t\tindexSrc += numBands;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printInterleaveToPlanar(AutoTypeImage imageIn, AutoTypeImage imageOut) {
		String inputName = imageIn.getInterleavedName();
		String bandName = imageOut.getSingleBandName();
		String bitWise = imageIn.getBitWise();
		String type=imageIn.getAbbreviatedType()+""+imageOut.getAbbreviatedType();
		String typecast = "";
		if( imageOut.isInteger() ) {
			typecast = "("+imageOut.getDataType()+")";
		}
		out.print(
				"\tpublic static void convert"+type+"( "+inputName+" input , Planar<"+bandName+"> output ) {\n" +
						"\n" +
						"\t\tfinal int numBands = input.numBands;\n" +
						"\t\tfor (int i = 0; i < numBands; i++) {\n" +
						"\t\t\t"+bandName+" band = output.bands[i];\n" +
						"\t\t\tfinal int offset = i;\n" +
						"\n" +
						"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
						"\t\t\tfor (int y = 0; y < input.height; y++) {\n" +
						"\t\t\t\tint indexSrc = y*input.stride + input.startIndex + offset;\n" +
						"\t\t\t\tint indexDst = y*output.stride + output.startIndex;\n" +
						"\t\t\t\tint end = indexDst + input.width;\n" +
						"\t\t\t\twhile( indexDst != end ) {\n" +
						"\t\t\t\t\tband.data[indexDst++] = "+typecast+"(input.data[indexSrc]"+bitWise+");\n" +
						"\t\t\t\t\tindexSrc += numBands;\n" +
						"\t\t\t\t}\n" +
						"\t\t\t}\n" +
						"\t\t\t//CONCURRENT_ABOVE });\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printPlanarToInterleaved(AutoTypeImage imageIn) {
		String outputName = imageIn.getInterleavedName();
		String bandName = imageIn.getSingleBandName();

		out.print(
				"\tpublic static void convert( Planar<"+bandName+"> input , "+outputName+" output ) {\n" +
				"\n" +
				"\t\tfinal int numBands = input.getNumBands();\n" +
				"\t\tfor (int i = 0; i < numBands; i++) {\n" +
				"\t\t\t"+bandName+" band = input.bands[i];\n" +
				"\t\t\tfinal int offset = i;\n" +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\t\tint indexSrc = y * input.stride + input.startIndex;\n" +
				"\t\t\t\tint indexDst = y * output.stride + output.startIndex + offset;\n" +
				"\t\t\t\tint end = indexSrc + input.width;\n" +
				"\t\t\t\t\n" +
				"\t\t\t\twhile( indexSrc != end ) { \n" +
				"\t\t\t\t\toutput.data[indexDst] = band.data[indexSrc++];\n" +
				"\t\t\t\t\tindexDst += numBands;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printPlanarToInterleaved(AutoTypeImage imageIn, AutoTypeImage imageOut) {
		String outputName = imageOut.getInterleavedName();
		String bandName = imageIn.getSingleBandName();
		String bitwise = imageIn.getBitWise();

		String type=imageIn.getAbbreviatedType()+""+imageOut.getAbbreviatedType();
		String typecast = "";
		if( imageOut.isInteger() ) {
			typecast = "("+imageOut.getDataType()+")";
		}

		out.print(
				"\tpublic static void convert"+type+"( Planar<"+bandName+"> input , "+outputName+" output ) {\n" +
						"\n" +
						"\t\tfinal int numBands = input.getNumBands();\n" +
						"\t\tfor (int i = 0; i < numBands; i++) {\n" +
						"\t\t\t"+bandName+" band = input.bands[i];\n" +
						"\t\t\tfinal int offset = i;\n" +
						"\n" +
						"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
						"\t\t\tfor (int y = 0; y < input.height; y++) {\n" +
						"\t\t\t\tint indexSrc = y * input.stride + input.startIndex;\n" +
						"\t\t\t\tint indexDst = y * output.stride + output.startIndex + offset;\n" +
						"\t\t\t\tint end = indexSrc + input.width;\n" +
						"\n" +
						"\t\t\t\twhile( indexSrc != end ) { \n" +
						"\t\t\t\t\toutput.data[indexDst] = "+typecast+"(band.data[indexSrc++]"+bitwise+");\n" +
						"\t\t\t\t\tindexDst += numBands;\n" +
						"\t\t\t\t}\n" +
						"\t\t\t}\n" +
						"\t\t\t//CONCURRENT_ABOVE });\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplConvertImage app = new GenerateImplConvertImage();
		app.parseArguments(args);
		app.generateCode();
	}
}
