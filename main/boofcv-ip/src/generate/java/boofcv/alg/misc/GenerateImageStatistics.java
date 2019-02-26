/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;
import boofcv.struct.image.ImageType;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;


/**
 * Generates functions inside of ImageStatistics.
 *
 * @author Peter Abeles
 */
public class GenerateImageStatistics extends CodeGeneratorBase {

	private AutoTypeImage input;

	public void generate() throws FileNotFoundException {
		printPreamble();
		printAll();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		out.print("import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import boofcv.alg.InputSanityCheck;\n" +
				"import java.util.Arrays;\n" +
				"import boofcv.alg.misc.impl.ImplImageStatistics;\n" +
				"import boofcv.alg.misc.impl.ImplImageStatistics_MT;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Computes statistical properties of pixels inside an image.\n" +
				" *\n" +
				generateDocString() +
				" *\n"+
				" * @author Peter Abeles\n" +
				" */\n" +
				generatedAnnotation() +
				"public class "+className+" {\n\n");
	}

	public void printAll() {
		AutoTypeImage types[] = AutoTypeImage.getSpecificTypes();

		ImageType.Family families[] = new ImageType.Family[]{ImageType.Family.GRAY,ImageType.Family.INTERLEAVED};

		List<CodeGenerator> functions = new ArrayList<>();
		functions.add( new GenerateMin());
		functions.add( new GenerateMax());
		functions.add( new GenerateMaxAbs());
		functions.add( new GenerateMeanDiffSq() );
		functions.add( new GenerateMeanDiffAbs() );

		for( AutoTypeImage t : types ) {
			input = t;

			for( CodeGenerator generator : functions ) {
				for( ImageType.Family f : families ) {
					generator.printHighLevel(f);
				}
			}
			for( ImageType.Family f : families ) {
				printSum(f);
				printMean(f);
			}

			printVariance();
			printHistogram();
		}
	}

	public void printHistogram() {
		String sumType = input.getSumType();

		out.print("\t/**\n" +
				"\t * Computes the histogram of intensity values for the image.\n" +
				"\t * \n" +
				"\t * @param input (input) Image.\n" +
				"\t * @param minValue (input) Minimum possible intensity value   \n" +
				"\t * @param histogram (output) Storage for histogram. Number of elements must be equal to max value.\n" +
				"\t */\n" +
				"\tpublic static void histogram( "+input.getSingleBandName()+" input , "+sumType+" minValue , int histogram[] ) {\n" +
				"\t\tArrays.fill(histogram,0);\n" +
				"\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint index = input.startIndex + y*input.stride;\n" +
				"\t\t\tint end = index + input.width;\n" +
				"\n" +
				"\t\t\twhile( index < end ) {\n");
		if( input.isInteger()) {
			if( input.getNumBits() == 64 )
				out.print("\t\t\t\thistogram[(int)(input.data[index++] - minValue)]++;\n");
			else
				out.print("\t\t\t\thistogram[(input.data[index++]"+input.getBitWise()+") - minValue ]++;\n");
		} else {
			out.print("\t\t\t\thistogram[(int)(input.data[index++] - minValue)]++;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public void printSum( ImageType.Family family ) {

		String sumType = input.getSumType();

		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Returns the sum of all the pixels in the image.\n" +
				"\t * </p>\n" +
				"\t * \n" +
				"\t * @param img Input image. Not modified.\n" +
				"\t */\n" +
				"\tpublic static "+sumType+" sum( "+input.getImageName(family)+" img ) {\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\treturn ImplImageStatistics_MT.sum(img);\n" +
				"\t\t} else {\n" +
				"\t\t\treturn ImplImageStatistics.sum(img);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public void printMean( ImageType.Family family  ) {
		String columns = family == ImageType.Family.INTERLEAVED ? "*img.numBands" : "";
		String sumType = input.isInteger() ? "double" : input.getSumType();

		out.print("\t/**\n" +
				"\t * Returns the mean pixel intensity value.\n" +
				"\t * \n" +
				"\t * @param img Input image.  Not modified.\n" +
				"\t * @return Mean pixel intensity value\n" +
				"\t */\n" +
				"\tpublic static "+sumType+" mean( "+input.getImageName(family)+" img ) {\n" +
				"\t\treturn sum(img)/("+sumType+")(img.width*img.height"+columns+");\n" +
				"\t}\n\n");
	}

	public void printVariance() {

		String sumType = input.isInteger() ? "double" : input.getSumType();

		out.print("\t/**\n" +
				"\t * Computes the variance of pixel intensity values inside the image.\n" +
				"\t *\n" +
				"\t * @param img Input image. Not modified.\n" +
				"\t * @param mean Mean pixel intensity value.   \n" +
				"\t * @return Pixel variance   \n" +
				"\t */\n" +
				"\tpublic static "+sumType+" variance( "+input.getSingleBandName()+" img , "+sumType+" mean ) {\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\treturn ImplImageStatistics_MT.variance(img,mean);\n" +
				"\t\t} else {\n" +
				"\t\t\treturn ImplImageStatistics.variance(img,mean);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private class GenerateMin extends InitValue {

		public GenerateMin() {
			super("min", "v < output",
					"\t/**\n" +
					"\t * Returns the minimum element value.\n" +
					"\t * \n" +
					"\t * @param input Input image. Not modified.\n" +
					"\t * @return Minimum pixel value.\n" +
					"\t */"
					);
		}

		@Override
		public String getValueMassage() { return "array[index] "+input.getBitWise(); }
	}

	private class GenerateMax extends InitValue {

		public GenerateMax() {
			super("max", "v > output",
							"\t/**\n" +
							"\t * Returns the maximum element value.\n" +
							"\t * \n" +
							"\t * @param input Input image. Not modified.\n" +
							"\t * @return Maximum pixel value.\n" +
							"\t */"
			);
		}

		@Override
		public String getValueMassage() { return "array[index] "+input.getBitWise(); }
	}

	private class GenerateMaxAbs extends InitValue {

		public GenerateMaxAbs() {
			super("maxAbs", "v > output",
							"\t/**\n" +
							"\t * Returns the maximum element value.\n" +
							"\t * \n" +
							"\t * @param input Input image. Not modified.\n" +
							"\t * @return Maximum pixel value.\n" +
							"\t */"
			);
		}

		@Override
		public String getValueMassage() {
			if( input.isSigned() )
				return "Math.abs(array[index])";
			else
				return "array[index] "+input.getBitWise();
		}
	}

	private class GenerateMeanDiffSq extends GenerateDifference {
		public GenerateMeanDiffSq() {
			super("meanDiffSq", "difference*difference",
							"\t/**\n" +
							"\t * <p>Computes the mean squared error (MSE) between the two images.</p>\n" +
							"\t *\n" +
							"\t * @param imgA first image. Not modified.\n" +
							"\t * @param imgB second image. Not modified.\n" +
							"\t * @return error between the two images.\n" +
							"\t */");
		}
	}

	private class GenerateMeanDiffAbs extends GenerateDifference {
		public GenerateMeanDiffAbs() {
			super("meanDiffAbs", "Math.abs(difference)",
					"\t/**\n" +
							"\t * <p>Computes the mean of absolute value error between the two images.</p>\n" +
							"\t *\n" +
							"\t * @param imgA first image. Not modified.\n" +
							"\t * @param imgB second image. Not modified.\n" +
							"\t * @return error between the two images.\n" +
							"\t */");
		}
	}

	private abstract class InitValue implements CodeGenerator {

		String name;
		String conditional;
		String javaDoc;

		public InitValue(String name, String conditional, String javaDoc) {
			this.name = name;
			this.conditional = conditional;
			this.javaDoc = javaDoc;
		}

		public void printHighLevel( ImageType.Family family ) {

			String sumType = input.getSumType();
			String columns = family == ImageType.Family.INTERLEAVED ? "input.width*input.numBands" : "input.width";
			String nameUn = this.name + (input.isSigned() ? "" : "U");

			out.println(javaDoc);
			out.print(
					"\tpublic static "+sumType+" "+name+"( "+input.getImageName(family)+" input ) {\n" +
					"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
					"\t\t\treturn ImplImageStatistics_MT."+nameUn+"(input.data, input.startIndex, input.height, "+columns+" , input.stride);\n" +
					"\t\t} else {\n" +
					"\t\t\treturn ImplImageStatistics."+nameUn+"(input.data, input.startIndex, input.height, "+columns+" , input.stride);\n" +
					"\t\t}\n" +
					"\t}\n\n");
		}
		public abstract String getValueMassage();
	}

	private class GenerateDifference implements CodeGenerator {
		String name;
		String operation;
		String javaDoc;

		public GenerateDifference(String name, String operation, String javaDoc) {
			this.name = name;
			this.operation = operation;
			this.javaDoc = javaDoc;
		}

		@Override
		public void printHighLevel(ImageType.Family family) {

			String columns = family == ImageType.Family.INTERLEAVED ? "imgA.width*imgA.numBands" : "imgA.width";
			String nameUn = this.name + (input.isSigned() ? "" : "U");
			String imageName = input.getImageName(family);

			out.println(javaDoc);
			out.print("\tpublic static double "+name+"("+imageName+" imgA, "+imageName+" imgB ) {\n" +
					"\t\tInputSanityCheck.checkSameShape(imgA,imgB);\n" +
					"\t\tif(BoofConcurrency.USE_CONCURRENT) {\n" +
					"\t\t\treturn ImplImageStatistics_MT."+nameUn+"(imgA.data,imgA.startIndex,imgA.stride, imgB.data,imgB.startIndex,imgB.stride,imgA.height, "+columns+");\n" +
					"\t\t} else {\n" +
					"\t\t\treturn ImplImageStatistics."+nameUn+"(imgA.data,imgA.startIndex,imgA.stride, imgB.data,imgB.startIndex,imgB.stride,imgA.height, "+columns+");\n" +
					"\t\t}\n" +
					"\t}\n\n");
		}
	}

	private interface CodeGenerator {
		void printHighLevel( ImageType.Family family );
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImageStatistics gen = new GenerateImageStatistics();
		gen.generate();
	}
}
