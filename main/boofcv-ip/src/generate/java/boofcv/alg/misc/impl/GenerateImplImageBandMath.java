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

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GenerateImplImageBandMath extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		create(AutoTypeImage.U8);
		create(AutoTypeImage.S16);
		create(AutoTypeImage.U16);
		create(AutoTypeImage.S32);
		create(AutoTypeImage.S64);
		create(AutoTypeImage.F32);
		create(AutoTypeImage.F64);

		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"\n" +
				"import org.ddogleg.sorting.QuickSelect;\n" +
				"import javax.annotation.Generated;\n" +
				"import boofcv.alg.misc.ImageMiscOps;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Implementation of algorithms in ImageBandMath\n" +
				generateDocString("Nico Stuurman","Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class " + className + " {\n\n");
	}

	protected void create( AutoTypeImage inputImg ) {
		printMinimum(inputImg);
		printMaximum(inputImg);
		printMedian(inputImg);
		printAverage(inputImg);
		printStdDev(inputImg);
	}

	void printMinimum( AutoTypeImage inputImg ) {
		String band = inputImg.getSingleBandName();
		String sumType = inputImg.getSumType();

		String typecast = inputImg.getTypeCastFromSum();
		String bitwise = inputImg.getBitWise();

		String max = inputImg.getMaxForSumType();

		out.print(
				"\tpublic static void minimum(Planar<"+band+"> input , "+band+" output, int startBand, int lastBand ) {\n" +
				"\t\tfinal int h = input.getHeight();\n" +
				"\t\tfinal int w = input.getWidth();\n" +
				"\n" +
				"\t\tfinal "+band+"[] bands = input.bands;\n" +
				"\t\t\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexInput = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint indexOutput = output.getStartIndex() + y * output.getStride();\n" +
				"\n" +
				"\t\t\tint indexEnd = indexInput+w;\n" +
				"\t\t\t// for(int x = 0; x < w; x++ ) {\n" +
				"\t\t\tfor (; indexInput < indexEnd; indexInput++, indexOutput++ ) {\n" +
				"\t\t\t\t"+sumType+" minimum = "+max+";\n" +
				"\t\t\t\tfor( int i = startBand; i <= lastBand; i++ ) {\n" +
				"\t\t\t\t\t"+sumType+" value = bands[i].data[ indexInput ] "+bitwise+";\n" +
				"\t\t\t\t\tif ( value < minimum) {\n" +
				"\t\t\t\t\t\tminimum = value;\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.data[indexOutput] = "+typecast+" minimum;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	void printMaximum( AutoTypeImage inputImg ) {
		String band = inputImg.getSingleBandName();
		String sumType = inputImg.getSumType();

		String typecast = inputImg.getTypeCastFromSum();
		String bitwise = inputImg.getBitWise();

		String minvalue = "-"+inputImg.getMaxForSumType();

		out.print(
				"\tpublic static void maximum(Planar<"+band+"> input , "+band+" output, int startBand, int lastBand ) {\n" +
				"\t\tfinal int h = input.getHeight();\n" +
				"\t\tfinal int w = input.getWidth();\n" +
				"\n" +
				"\t\tfinal "+band+"[] bands = input.bands;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor(int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexInput = input.startIndex + y * input.stride;\n" +
				"\t\t\tint indexOutput = output.startIndex + y * output.stride;\n" +
				"\n" +
				"\t\t\tint indexEnd = indexInput + w;\n" +
				"\t\t\t// for(int x = 0; x < w; x++ ) {\n" +
				"\t\t\tfor(; indexInput < indexEnd; indexInput++, indexOutput++) {\n" +
				"\t\t\t\t"+sumType+" maximum = "+minvalue+";\n" +
				"\t\t\t\tfor (int i = startBand; i <= lastBand; i++) {\n" +
				"\t\t\t\t\t"+sumType+" value = bands[i].data[ indexInput ] "+bitwise+";\n" +
				"\t\t\t\t\tif( value > maximum) {\n" +
				"\t\t\t\t\t\tmaximum = value;\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.data[indexOutput] = "+typecast+" maximum;\n" +
				"\t\t\t} \n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");

	}

	void printAverage( AutoTypeImage inputImg ) {
		String band = inputImg.getSingleBandName();
		String sumType = inputImg.getSumType();

		String typecast = inputImg.getTypeCastFromSum();
		String bitwise = inputImg.getBitWise();

		out.print(
				"\tpublic static void average(Planar<"+band+"> input , "+band+" output, int startBand, int lastBand ) {\n" +
				"\t\tfinal int h = input.getHeight();\n" +
				"\t\tfinal int w = input.getWidth();\n" +
				"\n" +
				"\t\tfinal "+band+"[] bands = input.bands;\n" +
				"\t\t"+sumType+" divisor = lastBand - startBand+1;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexInput = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint indexOutput = output.getStartIndex() + y * output.getStride();\n" +
				"\n" +
				"\t\t\tint indexEnd = indexInput + w;\n" +
				"\t\t\tfor (; indexInput < indexEnd; indexInput++, indexOutput++) {\n" +
				"\t\t\t"+sumType+" sum = bands[startBand].data[indexInput] "+bitwise+";\n" +
				"\t\t\t\tfor (int i = startBand+1; i <= lastBand; i++) {\n" +
				"\t\t\t\t\tsum += bands[i].data[indexInput] "+bitwise+";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.data[indexOutput] = "+typecast+" (sum/divisor);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");

	}

	void printStdDev( AutoTypeImage inputImg ) {
		String band = inputImg.getSingleBandName();
		String sumType = inputImg.getSumType();

		String bitwise = inputImg.getBitWise();
		String dataType = inputImg.getDataType();

		out.print(
				"\tpublic static void stdDev( Planar<"+band+"> input , "+band+" output , "+band+" avg, int startBand, int lastBand ) {\n" +
				"\n" +
				"\t\tfinal int h = input.getHeight();\n" +
				"\t\tfinal int w = input.getWidth();\n" +
				"\n" +
				"\t\tfinal "+band+"[] bands = input.bands;\n" +
				"\t\t"+sumType+" divisor = lastBand - startBand;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexInput = input.getStartIndex() + y*input.getStride();\n" +
				"\t\t\tint indexOutput = output.getStartIndex() + y*output.getStride();\n" +
				"\n" +
				"\t\t\tint indexEnd = indexInput + w;\n" +
				"\n" +
				"\t\t\tfor (; indexInput < indexEnd; indexInput++, indexOutput++) {\n" +
				"\t\t\t\t"+sumType+" sum = 0;\n" +
				"\t\t\t\tfor (int i = startBand; i <= lastBand; i++) {\n" +
				"\t\t\t\t\t"+sumType+" diff = (bands[i].data[indexInput] "+bitwise+") - (avg.data[indexInput] "+bitwise+");\n" +
				"\t\t\t\t\tsum += diff * diff;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.data[indexOutput] = ("+dataType+")Math.sqrt(sum/divisor);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	void printMedian( AutoTypeImage inputImg ) {
		String band = inputImg.getSingleBandName();
		String sumType = inputImg.getSumType();

		String typecast = inputImg.getTypeCastFromSum();
		String bitwise = inputImg.getBitWise();

		out.print(
				"\tpublic static void median( Planar<"+band+"> input , "+band+" output, int startBand, int lastBand ) {\n" +
				"\t\tfinal int h = input.getHeight();\n" +
				"\t\tfinal int w = input.getWidth();\n" +
				"\n" +
				"\t\tfinal "+band+"[] bands = input.bands;\n" +
				"\t\tfinal int numBands = lastBand - startBand + 1;\n" +
				"\n" +
				"\t\t// handle edge case\n" +
				"\t\tif (numBands == 1) {\n" +
				"\t\t\tImageMiscOps.copy(0, 0, 0, 0, input.getWidth(), input.getHeight(), input.getBand(startBand), output);\n" +
				"\t\t\treturn;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfinal int middle = numBands/2;\n" +
				"\t\tboolean isEven = numBands % 2 == 0;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tfinal "+sumType+"[] valueArray = new "+sumType+"[numBands];\n" +
				"\t\t\tint indexInput = input.getStartIndex() + y*input.getStride();\n" +
				"\t\t\tint indexOutput = output.getStartIndex() + y*output.getStride();\n" +
				"\n" +
				"\t\t\tint indexEnd = indexInput+w;\n" +
				"\t\t\tfor (; indexInput < indexEnd; indexInput++, indexOutput++) {\n" +
				"\t\t\t\tfor( int i = startBand; i <= lastBand; i++ ) {\n" +
				"\t\t\t\t\tvalueArray[i-startBand] = bands[i].data[indexInput]"+bitwise+";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tif (isEven) {\n" +
				"\t\t\t\t\t// Would a single quick sort be faster?\n" +
				"\t\t\t\t\t"+sumType+" val0 = QuickSelect.select(valueArray, middle, numBands);\n" +
				"\t\t\t\t\t"+sumType+" val1 = QuickSelect.select(valueArray, middle + 1, numBands);\n" +
				"\t\t\t\t\toutput.data[indexOutput] = "+typecast+"((val0+val1)/2);\n" +
				"\t\t\t\t} else {\n" +
				"\t\t\t\t\toutput.data[indexOutput] = "+typecast+"QuickSelect.select(valueArray, middle, numBands);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		GenerateImplImageBandMath gen = new GenerateImplImageBandMath();
		gen.parseArguments(args);
		gen.generateCode();
	}
}
