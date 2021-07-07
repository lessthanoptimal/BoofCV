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

package boofcv.core.image;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateConvertImage extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		for( AutoTypeImage in : AutoTypeImage.getSpecificTypes()) {
			for( AutoTypeImage out : AutoTypeImage.getSpecificTypes() ) {
				if( in == out )
					continue;

				printConvertSingle(in, out);
				printConvertInterleaved(in, out);
			}
			printPlanarAverage(in);
			printPlanarToInterleaved(in);
			printInterleaveAverage(in);
			printInterleaveToPlanar(in);
			printIntegerRange(in);
		}

		printInterleaveToPlanar(AutoTypeImage.U8,AutoTypeImage.F32);
		printInterleaveToPlanar(AutoTypeImage.F32,AutoTypeImage.U8);
		printPlanarToInterleaved(AutoTypeImage.U8,AutoTypeImage.F32);
		printPlanarToInterleaved(AutoTypeImage.F32,AutoTypeImage.U8);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.core.image.impl.ImplConvertImage;\n" +
				"import boofcv.core.image.impl.ImplConvertImage_MT;\n" +
				"import boofcv.core.image.impl.ImplConvertPlanarToGray;\n" +
				"import boofcv.core.image.impl.ImplConvertPlanarToGray_MT;\n" +
				"import boofcv.core.image.impl.ConvertInterleavedToSingle;\n" +
				"import boofcv.core.image.impl.ConvertInterleavedToSingle_MT;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Functions for converting between different image types. Pixel values are converted by typecasting.\n" +
				" * When converting between signed and unsigned types, care should be taken to avoid numerical overflow.\n" +
				" * </p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class "+className+" {\n\n");
	}

	private void printConvertSingle(AutoTypeImage imageIn, AutoTypeImage imageOut) {

		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Converts an {@link boofcv.struct.image."+imageIn.getSingleBandName()+"} into a {@link boofcv.struct.image."+imageOut.getSingleBandName()+"}.\n" +
				"\t * </p>\n" +
				"\t *\n" +
				"\t * @param input Input image which is being converted. Not modified.\n" +
				"\t * @param output (Optional) The output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static "+imageOut.getSingleBandName()+" convert("+imageIn.getSingleBandName()+" input, "+imageOut.getSingleBandName()+" output) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new "+imageOut.getSingleBandName()+"(input.width, input.height);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// threaded code is not significantly faster here\n" +
				"\t\tImplConvertImage.convert(input, output);\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printConvertInterleaved(AutoTypeImage imageIn, AutoTypeImage imageOut) {

		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Converts an {@link boofcv.struct.image."+imageIn.getInterleavedName()+"} into a {@link boofcv.struct.image."+imageOut.getInterleavedName()+"}.\n" +
				"\t * </p>\n" +
				"\t *\n" +
				"\t * @param input Input image which is being converted. Not modified.\n" +
				"\t * @param output (Optional) The output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static "+imageOut.getInterleavedName()+" convert("+imageIn.getInterleavedName()+" input, "+imageOut.getInterleavedName()+" output) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new "+imageOut.getInterleavedName()+"(input.width, input.height, input.numBands);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height,input.numBands);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// threaded code is not significantly faster here\n" +
				"\t\tImplConvertImage.convert(input, output);\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printPlanarAverage(AutoTypeImage imageIn) {

		String imageName = imageIn.getSingleBandName();

		out.print("\t/**\n" +
				"\t * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel\n" +
				"\t * across all the bands.\n" +
				"\t * \n" +
				"\t * @param input Input Planar image that is being converted. Not modified.\n" +
				"\t * @param output (Optional) The single band output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static "+imageName+" average( Planar<"+imageName+"> input , "+imageName+" output ) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new "+imageName+"(input.width, input.height);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tImplConvertPlanarToGray_MT.average(input,output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplConvertPlanarToGray.average(input,output);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printInterleaveAverage(AutoTypeImage imageIn) {

		String inputName = imageIn.getInterleavedName();
		String outputName = imageIn.getSingleBandName();

		out.print("\t/**\n" +
				"\t * Converts a {@link "+inputName+"} into a {@link "+outputName+"} by computing the average value of each pixel\n" +
				"\t * across all the bands.\n" +
				"\t * \n" +
				"\t * @param input (Input) The ImageInterleaved that is being converted. Not modified.\n" +
				"\t * @param output (Optional) The single band output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static "+outputName+" average( "+inputName+" input , "+outputName+" output ) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new "+outputName+"(input.width, input.height);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tConvertInterleavedToSingle_MT.average(input,output);\n" +
				"\t\t} else {\n" +
				"\t\t\tConvertInterleavedToSingle.average(input,output);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printInterleaveToPlanar(AutoTypeImage imageIn) {
		String inputName = imageIn.getInterleavedName();
		String bandName = imageIn.getSingleBandName();

		out.print("\t/**\n" +
				"\t * Converts a {@link "+inputName+"} into the equivalent {@link Planar}\n" +
				"\t * \n" +
				"\t * @param input (Input) ImageInterleaved that is being converted. Not modified.\n" +
				"\t * @param output (Optional) The output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static Planar<"+bandName+"> convert( "+inputName+" input , Planar<"+bandName+"> output ) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new Planar<>("+bandName+".class,input.width, input.height,input.numBands);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height,input.numBands);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tImplConvertImage_MT.convert(input,output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplConvertImage.convert(input,output);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printInterleaveToPlanar(AutoTypeImage imageIn, AutoTypeImage imageOut) {
		String inputName = imageIn.getInterleavedName();
		String bandName = imageOut.getSingleBandName();

		String type=imageIn.getAbbreviatedType()+""+imageOut.getAbbreviatedType();

		out.print("\t/**\n" +
				"\t * Converts a {@link "+inputName+"} into the equivalent {@link Planar}\n" +
				"\t * \n" +
				"\t * @param input (Input) ImageInterleaved that is being converted. Not modified.\n" +
				"\t * @param output (Optional) The output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static Planar<"+bandName+"> convert"+type+"( "+inputName+" input , Planar<"+bandName+"> output ) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new Planar<>("+bandName+".class,input.width, input.height,input.numBands);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height,input.numBands);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tImplConvertImage_MT.convert"+type+"(input,output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplConvertImage.convert"+type+"(input,output);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printPlanarToInterleaved(AutoTypeImage imageIn) {
		String outputName = imageIn.getInterleavedName();
		String bandName = imageIn.getSingleBandName();

		out.print("\t/**\n" +
				"\t * Converts a {@link Planar} into the equivalent {@link "+outputName+"}\n" +
				"\t *\n" +
				"\t * @param input (Input) Planar image that is being converted. Not modified.\n" +
				"\t * @param output (Optional) The output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static "+outputName+" convert( Planar<"+bandName+"> input , "+outputName+" output ) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new "+outputName+"(input.width, input.height,input.getNumBands());\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height,input.getNumBands());\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tImplConvertImage_MT.convert(input,output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplConvertImage.convert(input,output);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printPlanarToInterleaved(AutoTypeImage imageIn, AutoTypeImage imageOut) {
		String outputName = imageOut.getInterleavedName();
		String bandName = imageIn.getSingleBandName();

		String type=imageIn.getAbbreviatedType()+""+imageOut.getAbbreviatedType();

		out.print("\t/**\n" +
				"\t * Converts a {@link Planar} into the equivalent {@link "+outputName+"}\n" +
				"\t *\n" +
				"\t * @param input (Input) Planar image that is being converted. Not modified.\n" +
				"\t * @param output (Optional) The output image. If null a new image is created. Modified.\n" +
				"\t * @return Converted image.\n" +
				"\t */\n" +
				"\tpublic static "+outputName+" convert"+type+"( Planar<"+bandName+"> input , "+outputName+" output ) {\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new "+outputName+"(input.width, input.height,input.getNumBands());\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height,input.getNumBands());\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tImplConvertImage_MT.convert"+type+"(input,output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplConvertImage.convert"+type+"(input,output);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printIntegerRange( AutoTypeImage imageIn ) {

		String sumType = imageIn.getSumType();
		String round = imageIn.isInteger() ? "" : imageIn.getNumBits()==32? "+ 0.5f" : "+ 0.5";
		String bitwise = imageIn.getBitWise();

		out.print("\t/**\n" +
				"\t * Converts pixel values in the input image into an integer values from 0 to numValues. \n" +
				"\t * @param input Input image\n" +
				"\t * @param min minimum input pixel value, inclusive\n" +
				"\t * @param max maximum input pixel value, inclusive\n" +
				"\t * @param numValues Number of possible pixel values in output image\n" +
				"\t * @param output (Optional) Storage for the output image. Can be null.\n" +
				"\t * @return The converted output image.\n" +
				"\t */\n" +
				"\tpublic static GrayU8 convert("+imageIn.getSingleBandName()+" input , "+sumType+" min , "+sumType+" max , int numValues , GrayU8 output )\n" +
				"\t{\n" +
				"\t\tif (output == null) {\n" +
				"\t\t\toutput = new GrayU8(input.width, input.height);\n" +
				"\t\t} else {\n" +
				"\t\t\toutput.reshape(input.width,input.height);\n" +
				"\t\t}\n" +
				"\t\tif( numValues < 0 || numValues > 256 )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"0 <= numValues <= 256\");\n" +
				"\n" +
				"\t\tnumValues -= 1;" +
				"\n" +
				"\t\t"+sumType+" range = max-min;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint value = (int)(numValues*((input.data[indexIn++]"+bitwise+")-min)/range "+round+");\n" +
				"\t\t\t\toutput.data[indexOut++] = (byte)value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\treturn output;\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateConvertImage app = new GenerateConvertImage();

		app.generateCode();
	}
}
