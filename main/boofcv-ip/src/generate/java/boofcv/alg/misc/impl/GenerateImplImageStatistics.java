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

package boofcv.alg.misc.impl;

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
public class GenerateImplImageStatistics extends CodeGeneratorBase {

	private AutoTypeImage input;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		printAll();
		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		out.print("import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import java.util.Arrays;\n" +
				"\n" +
				"//CONCURRENT_INLINE import java.util.ArrayList;\n" +
				"//CONCURRENT_INLINE import java.util.List;\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Computes statistical properties of pixels inside an image.\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n\n");
	}

	public void printAll() {
		AutoTypeImage[] types = AutoTypeImage.getSpecificTypes();

		ImageType.Family[] families = new ImageType.Family[]{ImageType.Family.GRAY, ImageType.Family.INTERLEAVED};

		List<CodeGenerator> functions = new ArrayList<>();
		functions.add(new GenerateMin());
		functions.add(new GenerateMax());
		functions.add(new GenerateMaxAbs());
		functions.add(new GenerateMeanDiffSq());
		functions.add(new GenerateMeanDiffAbs());

		for (AutoTypeImage t : types) {
			input = t;

			for (CodeGenerator generator : functions) {
				generator.printLowLevel();
			}
			for (ImageType.Family f : families) {
				printSum(f);
				if (t.isSigned()) {
					printSumAbs(f);
				}
			}

			printVariance();
			printHistogram();
			printHistogramScaled();
		}
	}

	public void printHistogram() {
		String sumType = input.getSumType();

		out.print("\tpublic static void histogram( " + input.getSingleBandName() + " input , " + sumType + " minValue , int[] histogram ) {\n" +
				"\t\tArrays.fill(histogram,0);\n" +
				"\n" +
				"\t\t//CONCURRENT_INLINE final List<int[]> list = new ArrayList<>();\n" +
				"\t\t//CONCURRENT_INLINE BoofConcurrency.loopBlocks(0,input.height,(y0,y1)->{\n" +
				"\t\t//CONCURRENT_BELOW final int[] h = new int[histogram.length];\n" +
				"\t\tfinal int[] h = histogram;\n" +
				"\t\t//CONCURRENT_BELOW for( int y = y0; y < y1; y++ ) {\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint index = input.startIndex + y*input.stride;\n" +
				"\t\t\tint end = index + input.width;\n" +
				"\n" +
				"\t\t\twhile( index < end ) {\n");
		if (input.isInteger()) {
			if (input.getNumBits() == 64)
				out.print("\t\t\t\th[(int)(input.data[index++] - minValue)]++;\n");
			else
				out.print("\t\t\t\th[(input.data[index++]" + input.getBitWise() + ") - minValue ]++;\n");
		} else {
			out.print("\t\t\t\th[(int)(input.data[index++] - minValue)]++;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_INLINE synchronized(list){list.add(h);}});\n" +
				"\t\t//CONCURRENT_INLINE for (int i = 0; i < list.size(); i++) {\n" +
				"\t\t//CONCURRENT_INLINE \tint[] h = list.get(i);\n" +
				"\t\t//CONCURRENT_INLINE \tfor (int j = 0; j < histogram.length; j++) {\n" +
				"\t\t//CONCURRENT_INLINE \t\thistogram[j] += h[j];\n" +
				"\t\t//CONCURRENT_INLINE \t}\n" +
				"\t\t//CONCURRENT_INLINE }\n" +
				"\t}\n\n");
	}

	public void printHistogramScaled() {
		String sumType = input.getSumType();

		out.print("\tpublic static void histogramScaled( " + input.getSingleBandName() + " input , " + sumType + " minValue , " + sumType + " maxValue, int[] histogram ) {\n" +
				"\t\tArrays.fill(histogram,0);\n" +
				"\n" +
				"\t\tfinal " + sumType + " histLength = histogram.length;\n" +
				"\t\tfinal " + sumType + " rangeValue = maxValue-minValue+1;\n" +
				"\t\t\n" +
				"\t\t//CONCURRENT_INLINE final List<int[]> list = new ArrayList<>();\n" +
				"\t\t//CONCURRENT_INLINE BoofConcurrency.loopBlocks(0,input.height,(y0,y1)->{\n" +
				"\t\t//CONCURRENT_BELOW final int[] h = new int[histogram.length];\n" +
				"\t\tfinal int[] h = histogram;\n" +
				"\t\t//CONCURRENT_BELOW for( int y = y0; y < y1; y++ ) {\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint index = input.startIndex + y*input.stride;\n" +
				"\t\t\tint end = index + input.width;\n" +
				"\n" +
				"\t\t\twhile( index < end ) {\n");
		if (input.isInteger()) {
			if (input.getNumBits() == 64)
				out.print("\t\t\t\th[(int)(histLength*(input.data[index++] - minValue)/rangeValue)]++;\n");
			else
				out.print("\t\t\t\th[(int)(histLength*((input.data[index++]" + input.getBitWise() + ") - minValue)/rangeValue) ]++;\n");
		} else {
			out.print("\t\t\t\th[(int)(histLength*(input.data[index++] - minValue)/rangeValue)]++;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_INLINE synchronized(list){list.add(h);}});\n" +
				"\t\t//CONCURRENT_INLINE for (int i = 0; i < list.size(); i++) {\n" +
				"\t\t//CONCURRENT_INLINE \tint[] h = list.get(i);\n" +
				"\t\t//CONCURRENT_INLINE \tfor (int j = 0; j < histogram.length; j++) {\n" +
				"\t\t//CONCURRENT_INLINE \t\thistogram[j] += h[j];\n" +
				"\t\t//CONCURRENT_INLINE \t}\n" +
				"\t\t//CONCURRENT_INLINE }\n" +
				"\t}\n\n");
	}

	public void printSum( ImageType.Family family ) {

		String bitWise = input.getBitWise();
		String columns = family == ImageType.Family.INTERLEAVED ? "*img.numBands" : "";
		String sumType = input.getSumType();
		String numTo = input.getSumNumberToType();
		out.print("\tpublic static " + sumType + " sum( " + input.getImageName(family) + " img ) {\n" +
				"\n" +
				"\t\tfinal int rows = img.height;\n" +
				"\t\tfinal int columns = img.width" + columns + ";\n" +
				"\n" +
				"\t\t//CONCURRENT_REMOVE_BELOW\n" +
				"\t\t" + sumType + " total = 0;\n" +
				"\n" +
				"\t\t//CONCURRENT_INLINE return BoofConcurrency.sum(0,img.height," + sumType + ".class,y->{\n" +
				"\t\t\t//CONCURRENT_BELOW " + sumType + " total = 0;\n" +
				"\t\tfor (int y = 0; y < rows; y++) {\n" +
				"\t\t\tint index = img.startIndex + y * img.stride;\n" +
				"\t\t\t\n" +
				"\t\t\tint indexEnd = index+columns;\n" +
				"\t\t\tfor (; index < indexEnd; index++ ) {\n" +
				"\t\t\t\ttotal += img.data[index] " + bitWise + ";\n" +
				"\t\t\t}\n" +
				"\t\t} return total;\n" +
				"\t\t//CONCURRENT_ABOVE return total;})." + numTo + ";\n" +
				"\t}\n\n");
	}

	public void printSumAbs( ImageType.Family family ) {

		String bitWise = input.getBitWise();
		String columns = family == ImageType.Family.INTERLEAVED ? "*img.numBands" : "";
		String sumType = input.getSumType();
		String numTo = input.getSumNumberToType();
		out.print(
				"\tpublic static " + sumType + " sumAbs( " + input.getImageName(family) + " img ) {\n" +
						"\n" +
						"\t\tfinal int rows = img.height;\n" +
						"\t\tfinal int columns = img.width" + columns + ";\n" +
						"\n" +
						"\t\t//CONCURRENT_REMOVE_BELOW\n" +
						"\t\t" + sumType + " total = 0;\n" +
						"\n" +
						"\t\t//CONCURRENT_INLINE return BoofConcurrency.sum(0,img.height," + sumType + ".class,y->{\n" +
						"\t\t\t//CONCURRENT_BELOW " + sumType + " total = 0;\n" +
						"\t\tfor (int y = 0; y < rows; y++) {\n" +
						"\t\t\tint index = img.startIndex + y * img.stride;\n" +
						"\t\t\t\n" +
						"\t\t\tint indexEnd = index+columns;\n" +
						"\t\t\tfor (; index < indexEnd; index++ ) {\n" +
						"\t\t\t\ttotal += Math.abs(img.data[index] " + bitWise + ");\n" +
						"\t\t\t}\n" +
						"\t\t} return total;\n" +
						"\t\t//CONCURRENT_ABOVE return total;})." + numTo + ";\n" +
						"\t}\n\n");
	}

	public void printVariance() {

		String bitWise = input.getBitWise();
		String sumType = input.isInteger() ? "double" : input.getSumType();
		String numTo = input.getSumNumberToType();

		out.print("\tpublic static " + sumType + " variance( " + input.getSingleBandName() + " img , " + sumType + " mean ) {\n" +
				"\n" +
				"\t\t//CONCURRENT_REMOVE_BELOW\n" +
				"\t\t" + sumType + " total = 0;\n" +
				"\n" +
				"\t\t//CONCURRENT_INLINE return BoofConcurrency.sum(0,img.height," + sumType + ".class,y->{\n" +
				"\t\t\t//CONCURRENT_BELOW " + sumType + " total = 0;\n" +
				"\t\tfor (int y = 0; y < img.height; y++) {\n" +
				"\t\t\tint index = img.getStartIndex() + y * img.getStride();\n" +
				"\n" +
				"\t\t\tint indexEnd = index+img.width;\n" +
				"\t\t\t// for(int x = 0; x < img.width; x++ ) {\n" +
				"\t\t\tfor (; index < indexEnd; index++ ) {\n" +
				"\t\t\t\t" + sumType + " d = (img.data[index]" + bitWise + ") - mean; \n" +
				"\t\t\t\ttotal += d*d;\n" +
				"\t\t\t}\n" +
				"\t\t} return total/(img.width*img.height);\n" +
				"\t\t//CONCURRENT_ABOVE return total;})." + numTo + "/(img.width*img.height);\n" +
				"\t}\n\n");
	}

	private class GenerateMin extends InitValue {

		public GenerateMin() {
			super("min", "min", "v < output");
		}

		@Override
		public String getValueMassage() { return "array[index] " + input.getBitWise(); }
	}

	private class GenerateMax extends InitValue {

		public GenerateMax() {
			super("max", "max", "v > output");
		}

		@Override
		public String getValueMassage() { return "array[index] " + input.getBitWise(); }
	}

	private class GenerateMaxAbs extends InitValue {

		public GenerateMaxAbs() {
			super("maxAbs", "max", "v > output");
		}

		@Override
		public String getValueMassage() {
			if (input.isSigned())
				return "Math.abs(array[index])";
			else
				return "array[index] " + input.getBitWise();
		}
	}

	private class GenerateMeanDiffSq extends GenerateDifference {
		public GenerateMeanDiffSq() {
			super("meanDiffSq", "sum", "difference*difference");
		}
	}

	private class GenerateMeanDiffAbs extends GenerateDifference {
		public GenerateMeanDiffAbs() {
			super("meanDiffAbs", "sum", "Math.abs(difference)");
		}
	}

	private abstract class InitValue implements CodeGenerator {

		String name;
		String conOp;
		String conditional;

		protected InitValue( String name, String conOp, String conditional ) {
			this.name = name;
			this.conOp = conOp;
			this.conditional = conditional;
		}

		@Override
		public void printLowLevel() {
			String sumType = input.getSumType();
			String name = this.name + (input.isSigned() ? "" : "U");

			out.print("\tpublic static " + sumType + " " + name + "( " + input.getDataType() + "[] array , int startIndex , int rows , int columns , int stride ) {\n" +
					"\n" +
					"\t\t//CONCURRENT_BELOW final " + sumType + " _output = array[startIndex]" + input.getBitWise() + ";\n" +
					"\t\t" + sumType + " output = array[startIndex]" + input.getBitWise() + ";\n" +
					"\n" +
					"\t\t//CONCURRENT_INLINE return BoofConcurrency." + conOp + "(0,rows," + sumType + ".class,y->{\n" +
					"\t\t\t//CONCURRENT_BELOW " + sumType + " output = _output;\n" +
					"\t\tfor( int y = 0; y < rows; y++ ) {\n" +
					"\t\t\tint index = startIndex + y*stride;\n" +
					"\t\t\tint end = index + columns;\n" +
					"\n" +
					"\t\t\tfor( ; index < end; index++ ) {\n" +
					"\t\t\t\t" + sumType + " v = " + getValueMassage() + ";\n" +
					"\t\t\t\tif( " + conditional + " )\n" +
					"\t\t\t\t\toutput = v;\n" +
					"\t\t\t}\n" +
					"\t\t} return output;\n" +
					"\t\t//CONCURRENT_ABOVE return output;})." + input.getSumNumberToType() + ";\n" +
					"\t}\n\n");
		}

		public abstract String getValueMassage();
	}

	private class GenerateDifference implements CodeGenerator {
		String name;
		String conOp;
		String operation;

		public GenerateDifference( String name, String conOp, String operation ) {
			this.name = name;
			this.conOp = conOp;
			this.operation = operation;
		}

		@Override
		public void printLowLevel() {
			String dataType = input.getDataType();
			String sumType = input.getSumType();
			String name = this.name + (input.isSigned() ? "" : "U");
			String bitWise = input.getBitWise();

			out.print("\tpublic static double " + name + "(" + dataType + " []dataA, int startIndexA , int strideA,\n" +
					"\t\t\t\t\t\t\t\t\t" + dataType + " []dataB, int startIndexB , int strideB,\n" +
					"\t\t\t\t\t\t\t\t\tint rows , int columns ) {\n" +
					"\t\t//CONCURRENT_REMOVE_BELOW\n" +
					"\t\t" + sumType + " total = 0;\n" +
					"\n" +
					"\t\t//CONCURRENT_INLINE return BoofConcurrency." + conOp + "(0,rows," + sumType + ".class,y->{\n" +
					"\t\t\t//CONCURRENT_BELOW " + sumType + " total = 0;\n" +
					"\t\tfor (int y = 0; y < rows; y++) {\n" +
					"\t\t\tint indexA = startIndexA + y * strideA;\n" +
					"\t\t\tint indexB = startIndexB + y * strideB;\n" +
					"\t\t\t\n" +
					"\t\t\tint indexEnd = indexA+columns;\n" +
					"\t\t\t\n" +
					"\t\t\tfor (; indexA < indexEnd; indexA++,indexB++) {\n" +
					"\t\t\t\t" + sumType + " difference = (dataA[indexA]" + bitWise + ")-(dataB[indexB]" + bitWise + ");\n" +
					"\t\t\t\ttotal += " + operation + ";\n" +
					"\t\t\t}\n" +
					"\t\t} return total / (double)(rows*columns);\n" +
					"\t\t//CONCURRENT_ABOVE return total;})." + input.getSumNumberToType() + "/ (double)(rows*columns);\n" +
					"\t}\n\n");
		}
	}

	private interface CodeGenerator {
		void printLowLevel();
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplImageStatistics gen = new GenerateImplImageStatistics();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generate();
	}
}
