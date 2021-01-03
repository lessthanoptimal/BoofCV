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
import java.util.ArrayList;
import java.util.List;

import static boofcv.generate.AutoTypeImage.*;

/**
 * Generates functions for ImplPixelMath.
 *
 * @author Peter Abeles
 */
public class GenerateImplPixelMath extends CodeGeneratorBase {

	private AutoTypeImage input;
	private AutoTypeImage output;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printOperator1(AutoTypeImage.getGenericTypes());
		printOperator2(AutoTypeImage.getGenericTypes());
		printAbs();
		printNegative();

		List<TwoTemplate> listTwo = new ArrayList<>();
		listTwo.add(new Multiple());
		listTwo.add(new Divide());
		listTwo.add(new Plus());
		listTwo.add(new Minus(true));
		listTwo.add(new Minus(false));

		for (TwoTemplate t : listTwo) {
			print_img_scalar(t, false);
			print_img_scalar(t, true);
			print_img_scalar_Int_to_Float(t);
		}

		printAll();
		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"import boofcv.alg.misc.PixelMathLambdas.*;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Implementation of algorithms in PixelMath\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class " + className + " {\n\n");
	}

	public void printAll() {
		AutoTypeImage[] types = AutoTypeImage.getSpecificTypes();

		for (AutoTypeImage t : types) {
			input = t;

			printBoundImage();
			printDiffAbs();
		}

		AutoTypeImage[] outputsAdd = new AutoTypeImage[]{U16, S16, S32, S32, S32, S64, F32, F64};
		AutoTypeImage[] outputsSub = new AutoTypeImage[]{I16, S16, S32, S32, S32, S64, F32, F64};

		for (int i = 0; i < types.length; i++) {
			printAddTwoImages(types[i], outputsAdd[i]);
			printSubtractTwoImages(types[i], outputsSub[i]);

			if (!types[i].isInteger()) {
				printMultTwoImages(types[i], types[i]);
				printDivTwoImages(types[i], types[i]);
//				printLog(types[i],types[i]);
//				printLogSign(types[i], types[i]);
//				printSqrt(types[i], types[i]);
			}
		}

		printVal("log", ( in, out ) -> "\t\t\t\toutput[indexDst] = (" + in.getDataType() + ")Math.log(val + input[indexSrc]);",
				AutoTypeImage.getFloatingTypes(), AutoTypeImage.getFloatingTypes());
		printVal("logSign", ( in, out ) -> {
					String type = in.getDataType();
					return "\t\t\t\t" + type + " value = input[indexSrc];\n" +
							"\t\t\t\tif( value < 0 ) {\n" +
							"\t\t\t\t\toutput[indexDst] = (" + type + ")-Math.log(val - value);\n" +
							"\t\t\t\t} else {\n" +
							"\t\t\t\t\toutput[indexDst] = (" + type + ")Math.log(val + value);\n" +
							"\t\t\t\t}\n";
				},
				AutoTypeImage.getFloatingTypes(), AutoTypeImage.getFloatingTypes());

		print("sqrt", "Math.sqrt(input[indexSrc])", AutoTypeImage.getFloatingTypes());

		increaseBits(new AutoTypeImage[]{U8, U16, F32, F64}, new AutoTypeImage[]{U16, S32, F32, F64});
	}

	private void increaseBits( AutoTypeImage[] typesSrc, AutoTypeImage[] typesDst ) {

		print("pow2", ( in, out ) -> {
			String bitwise = in.getBitWise();
			return "\t\t\t\t" + in.getSumType() + " v = input[indexSrc]" + bitwise + ";\n" +
					"\t\t\t\toutput[indexDst] = (" + out.getDataType() + ")(v*v);\n";
		}, typesSrc, typesDst);

		for (int i = 0; i < typesSrc.length; i++) {
			printStdev(typesSrc[i], typesDst[i]);
		}
	}

	private void printOperator1( AutoTypeImage[] types ) {
		for (AutoTypeImage input : types) {
			String operatorType = input.getAbbreviatedType();

			String arrayType = input.getDataType();
			out.println(
					"\tpublic static void operator1( " + arrayType + "[] input, int inputStart, int inputStride,\n" +
							"\t\t\t\t\t\t\t   " + arrayType + "[] output, int outputStart, int outputStride,\n" +
							"\t\t\t\t\t\t\t   int rows, int cols,\n" +
							"\t\t\t\t\t\t\t   Function1_" + operatorType + " function ) {\n" +
							"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
							"\t\tfor (int y = 0; y < rows; y++) {\n" +
							"\t\t\tint indexSrc = inputStart + y*inputStride;\n" +
							"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
							"\t\t\tint end = indexSrc + cols;\n" +
							"\n" +
							"\t\t\tfor (; indexSrc < end; indexSrc++, indexDst++) {\n" +
							"\t\t\t\toutput[indexDst] = function.process(input[indexSrc]);\n" +
							"\t\t\t}\n" +
							"\t\t}\n" +
							"\t\t//CONCURRENT_ABOVE });\n" +
							"\t}\n");
		}
	}

	private void printOperator2( AutoTypeImage[] types ) {
		for (AutoTypeImage input : types) {
			String operatorType = input.getAbbreviatedType();
			String arrayType = input.getDataType();

			out.println(
					"\tpublic static void operator2( " + arrayType + "[] inputA, int inputStartA, int inputStrideA,\n" +
							"\t\t\t\t\t\t\t   " + arrayType + "[] inputB, int inputStartB, int inputStrideB,\n" +
							"\t\t\t\t\t\t\t   " + arrayType + "[] output, int outputStart, int outputStride,\n" +
							"\t\t\t\t\t\t\t   int rows, int cols,\n" +
							"\t\t\t\t\t\t\t   Function2_" + operatorType + " function ) {\n" +
							"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
							"\t\tfor (int y = 0; y < rows; y++) {\n" +
							"\t\t\tint indexA = inputStartA + y*inputStrideA;\n" +
							"\t\t\tint indexB = inputStartB + y*inputStrideB;\n" +
							"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
							"\t\t\tint end = indexA + cols;\n" +
							"\n" +
							"\t\t\tfor (; indexA < end; indexA++, indexB++, indexDst++) {\n" +
							"\t\t\t\toutput[indexDst] = function.process(inputA[indexA],inputB[indexB]);\n" +
							"\t\t\t}\n" +
							"\t\t}\n" +
							"\t\t//CONCURRENT_ABOVE });\n" +
							"\t}\n");
		}
	}

	private void print( String funcName, String operation, AutoTypeImage[] types ) {
		for (AutoTypeImage input : types) {
			String arrayType = input.getDataType();
			out.println(
					"\tpublic static void " + funcName + "( " + arrayType + "[] input, int inputStart, int inputStride,\n" +
							"\t\t\t\t\t\t\t   " + arrayType + "[] output, int outputStart, int outputStride,\n" +
							"\t\t\t\t\t\t\t   int rows, int cols ) {\n" +
							"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
							"\t\tfor (int y = 0; y < rows; y++) {\n" +
							"\t\t\tint indexSrc = inputStart + y*inputStride;\n" +
							"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
							"\t\t\tint end = indexSrc + cols;\n" +
							"\n" +
							"\t\t\tfor (; indexSrc < end; indexSrc++, indexDst++) {\n" +
							"\t\t\t\toutput[indexDst] = (" + input.getDataType() + ")" + operation + ";\n" +
							"\t\t\t}\n" +
							"\t\t}\n" +
							"\t\t//CONCURRENT_ABOVE });\n" +
							"\t}\n");
		}
	}

	private void print( String funcName, PrintString2 operation,
						AutoTypeImage[] typesSrc, AutoTypeImage[] typesDst ) {
		for (int idxType = 0; idxType < typesSrc.length; idxType++) {
			AutoTypeImage input = typesSrc[idxType];
			AutoTypeImage output = typesDst[idxType];

			String arrayTypeIn = input.getDataType();
			String arrayTypeOut = output.getDataType();

			out.println(
					"\tpublic static void " + funcName + "( " + arrayTypeIn + "[] input, int inputStart, int inputStride,\n" +
							"\t\t\t\t\t\t\t   " + arrayTypeOut + "[] output, int outputStart, int outputStride,\n" +
							"\t\t\t\t\t\t\t   int rows, int cols ) {\n" +
							"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
							"\t\tfor (int y = 0; y < rows; y++ ) {\n" +
							"\t\t\tint indexSrc = inputStart + y*inputStride;\n" +
							"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
							"\t\t\tint end = indexSrc + cols;\n" +
							"\n" +
							"\t\t\tfor (; indexSrc < end; indexSrc++, indexDst++) {\n" +
							operation.print(input, output) + "\n" +
							"\t\t\t}\n" +
							"\t\t}\n" +
							"\t\t//CONCURRENT_ABOVE });\n" +
							"\t}\n");
		}
	}

	private void printVal( String funcName, PrintString2 operation,
						   AutoTypeImage[] typesSrc, AutoTypeImage[] typesDst ) {
		for (int idxType = 0; idxType < typesSrc.length; idxType++) {
			AutoTypeImage input = typesSrc[idxType];
			AutoTypeImage output = typesDst[idxType];

			String arrayType = input.getDataType();
			String sumType = input.getSumType();
			out.println(
					"\tpublic static void " + funcName + "( " + arrayType + "[] input, int inputStart, int inputStride,\n" +
							"\t\t\t\t\t\t\t   " + sumType + " val,\n" +
							"\t\t\t\t\t\t\t   " + arrayType + "[] output, int outputStart, int outputStride,\n" +
							"\t\t\t\t\t\t\t   int rows, int cols ) {\n" +
							"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
							"\t\tfor (int y = 0; y < rows; y++) {\n" +
							"\t\t\tint indexSrc = inputStart + y*inputStride;\n" +
							"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
							"\t\t\tint end = indexSrc + cols;\n" +
							"\n" +
							"\t\t\tfor (; indexSrc < end; indexSrc++, indexDst++) {\n" +
							operation.print(input, output) +
							"\t\t\t}\n" +
							"\t\t}\n" +
							"\t\t//CONCURRENT_ABOVE });\n" +
							"\t}\n");
		}
	}

	public void printAbs() {
		print("abs", "Math.abs(input[indexSrc])", AutoTypeImage.getSigned());
	}

	public void printNegative() {
		print("negative", "-input[indexSrc]", AutoTypeImage.getSigned());
	}

	private void print_img_scalar( TwoTemplate template, boolean bounded ) {
		String funcName = template.getName();
		String varName = template.getVariableName();

		for (AutoTypeImage t : template.getTypes()) {
			input = t;
			output = t;
			print_img_scalar(template, bounded, funcName, varName);
		}
	}

	private void print_img_scalar_Int_to_Float( TwoTemplate template ) {
		String funcName = template.getName();
		String varName = template.getVariableName();

		for (AutoTypeImage t : template.getTypes()) {
			// float to float is already covered
			if (!t.isInteger())
				continue;
			input = t;
			output = F32;

			print_img_scalar(template, false, funcName, varName);
		}
	}

	private void print_img_scalar( TwoTemplate template, boolean bounded, String funcName, String varName ) {
		String variableType;
		if (template.isScaleOp())
			variableType = output.isInteger() ? "double" : output.getSumType();
		else
			variableType = output.getSumType();

		String funcArrayName = input.isSigned() ? funcName : funcName + "U";
		funcArrayName += template.isImageFirst() ? "_A" : "_B";

		if (bounded) {
			print_array_scalar_bounded(funcArrayName, variableType, varName, template.getOperation());
		} else {
			print_array_scalar(funcArrayName, variableType, varName, template.getOperation());
		}
	}

	public void print_array_scalar( String funcName, String varType, String varName, String operation ) {
		String arrayTypeSrc = input.getDataType();
		String arrayTypeDst = output.getDataType();

		String typeCast = varType.equals(output.getDataType()) ? "" : "(" + output.getDataType() + ")";

		out.println("\tpublic static void " + funcName + "( " + arrayTypeSrc + "[] input, int inputStart, int inputStride, \n" +
				"\t\t\t\t\t\t\t   " + varType + " " + varName + ",\n" +
				"\t\t\t\t\t\t\t   " + arrayTypeDst + "[] output, int outputStart, int outputStride,\n" +
				"\t\t\t\t\t\t\t   int rows, int cols ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
				"\t\tfor (int y = 0; y < rows; y++) {\n" +
				"\t\t\tint indexSrc = inputStart + y*inputStride;\n" +
				"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
				"\t\t\tint end = indexSrc + cols;\n" +
				"\n" +
				"\t\t\tfor (; indexSrc < end; indexSrc++, indexDst++) {\n" +
				"\t\t\t\toutput[indexDst] = " + typeCast + operation + ";\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n");
	}

	public void print_array_scalar_bounded( String funcName, String varType, String varName, String operation ) {
		String arrayType = input.getDataType();

		String sumType = input.getSumType();
		String typeCast = varType.equals(sumType) ? "" : "(" + sumType + ")";

		out.println("\tpublic static void " + funcName + "( " + arrayType + "[] input, int inputStart, int inputStride,\n" +
				"\t\t\t\t\t\t\t   " + varType + " " + varName + ", " + sumType + " lower, " + sumType + " upper,\n" +
				"\t\t\t\t\t\t\t   " + arrayType + "[] output, int outputStart, int outputStride,\n" +
				"\t\t\t\t\t\t\t   int rows, int cols ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rows,y->{\n" +
				"\t\tfor (int y = 0; y < rows; y++) {\n" +
				"\t\t\tint indexSrc = inputStart + y*inputStride;\n" +
				"\t\t\tint indexDst = outputStart + y*outputStride;\n" +
				"\t\t\tint end = indexSrc + cols;\n" +
				"\n" +
				"\t\t\tfor (; indexSrc < end; indexSrc++, indexDst++) {\n" +
				"\t\t\t\t" + sumType + " val = " + typeCast + operation + ";\n" +
				"\t\t\t\tif( val < lower ) val = lower;\n" +
				"\t\t\t\tif( val > upper ) val = upper;\n" +
				"\t\t\t\toutput[indexDst] = " + input.getTypeCastFromSum() + "val;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n");
	}

	public void printBoundImage() {

		String bitWise = input.getBitWise();
		String sumType = input.getSumType();

		out.print("\tpublic static void boundImage( " + input.getSingleBandName() + " img, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\tfinal int h = img.getHeight();\n" +
				"\t\tfinal int w = img.getWidth();\n" +
				"\n" +
				"\t\t" + input.getDataType() + "[] data = img.data;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint index = img.getStartIndex() + y * img.getStride();\n" +
				"\t\t\tint indexEnd = index+w;\n" +
				"\t\t\t// for(int x = 0; x < w; x++ ) {\n" +
				"\t\t\tfor (; index < indexEnd; index++) {\n" +
				"\t\t\t\t" + sumType + " value = data[index]" + bitWise + ";\n" +
				"\t\t\t\tif( value < min )\n" +
				"\t\t\t\t\tdata[index] = " + input.getTypeCastFromSum() + "min;\n" +
				"\t\t\t\telse if( value > max )\n" +
				"\t\t\t\t\tdata[index] = " + input.getTypeCastFromSum() + "max;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public void printDiffAbs() {
		String bitWise = input.getBitWise();
		String typeCast = input.isInteger() ? "(" + input.getDataType() + ")" : "";

		out.print("\tpublic static void diffAbs( " + input.getSingleBandName() + " imgA, " + input.getSingleBandName() + " imgB, " + input.getSingleBandName() + " output ) {\n" +
				"\n" +
				"\t\tfinal int h = imgA.getHeight();\n" +
				"\t\tfinal int w = imgA.getWidth();\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexA = imgA.getStartIndex() + y * imgA.getStride();\n" +
				"\t\t\tint indexB = imgB.getStartIndex() + y * imgB.getStride();\n" +
				"\t\t\tint indexDiff = output.getStartIndex() + y * output.getStride();\n" +
				"\t\t\t\n" +
				"\t\t\tint indexEnd = indexA+w;\n" +
				"\t\t\t// for(int x = 0; x < w; x++ ) {\n" +
				"\t\t\tfor (; indexA < indexEnd; indexA++, indexB++, indexDiff++ ) {\n" +
				"\t\t\t\toutput.data[indexDiff] = " + typeCast + "Math.abs((imgA.data[indexA] " + bitWise + ") - (imgB.data[indexB] " + bitWise + "));\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public void printAddTwoImages( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
		printTwoImageOperation("add", typeIn, typeOut, "+");
	}

	public void printSubtractTwoImages( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
		printTwoImageOperation("subtract", typeIn, typeOut, "-");
	}

	public void printMultTwoImages( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
		printTwoImageOperation("multiply", typeIn, typeOut, "*");
	}

	public void printDivTwoImages( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
		printTwoImageOperation("divide", typeIn, typeOut, "/");
	}

	public void printTwoImageOperation( String name, AutoTypeImage typeIn, AutoTypeImage typeOut, String op ) {

		String bitWise = typeIn.getBitWise();
		String typeCast = typeOut.isInteger() ? "(" + typeOut.getDataType() + ")" : "";

		out.print("\tpublic static void " + name + "( " + typeIn.getSingleBandName() + " imgA, " + typeIn.getSingleBandName() + " imgB, " + typeOut.getSingleBandName() + " output ) {\n" +
				"\n" +
				"\t\tfinal int h = imgA.getHeight();\n" +
				"\t\tfinal int w = imgA.getWidth();\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexA = imgA.getStartIndex() + y * imgA.getStride();\n" +
				"\t\t\tint indexB = imgB.getStartIndex() + y * imgB.getStride();\n" +
				"\t\t\tint indexOut = output.getStartIndex() + y * output.getStride();\n" +
				"\t\t\t\n" +
				"\t\t\tint indexEnd = indexA+w;\n" +
				"\t\t\t// for(int x = 0; x < w; x++ ) {\n" +
				"\t\t\tfor (; indexA < indexEnd; indexA++, indexB++, indexOut++ ) {\n" +
				"\t\t\t\toutput.data[indexOut] = " + typeCast + "((imgA.data[indexA] " + bitWise + ") " + op + " (imgB.data[indexB] " + bitWise + "));\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

//	public void printLog( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
//		String sumType = typeIn.getSumType();
//		String bitWise = typeIn.getBitWise();
//		String typeCast = typeOut != AutoTypeImage.F64 ? "("+typeOut.getDataType()+")" : "";
//
//		out.print(
//				"\tpublic static void log( "+typeIn.getSingleBandName()+" input, final "+sumType+" val, "+typeOut.getSingleBandName()+" output ) {\n" +
//				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.height,y->{\n" +
//				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
//				"\t\t\tint indexSrc = input.startIndex + y* input.stride;\n" +
//				"\t\t\tint indexDst = output.startIndex + y* output.stride;\n" +
//				"\t\t\tint end = indexSrc + input.width;\n" +
//				"\n" +
//				"\t\t\tfor( ; indexSrc < end; indexSrc++, indexDst++) {\n" +
//				"\t\t\t\toutput.data[indexDst] = "+typeCast+"Math.log(val + input.data[indexSrc]"+bitWise+");\n" +
//				"\t\t\t}\n" +
//				"\t\t}\n" +
//				"\t\t//CONCURRENT_ABOVE });\n" +
//				"\t}\n\n");
//	}
//
//	public void printLogSign( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
//		String sumType = typeIn.getSumType();
//		String bitWise = typeIn.getBitWise();
//		String typeCast = typeOut != AutoTypeImage.F64 ? "("+typeOut.getDataType()+")" : "";
//
//		out.print(
//				"\tpublic static void logSign( "+typeIn.getSingleBandName()+" input, final "+sumType+" val, "+typeOut.getSingleBandName()+" output ) {\n" +
//						"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.height,y->{\n" +
//						"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
//						"\t\t\tint indexSrc = input.startIndex + y* input.stride;\n" +
//						"\t\t\tint indexDst = output.startIndex + y* output.stride;\n" +
//						"\t\t\tint end = indexSrc + input.width;\n" +
//						"\n" +
//						"\t\t\tfor( ; indexSrc < end; indexSrc++, indexDst++) {\n" +
//						"\t\t\t\t"+sumType+" value = input.data[indexSrc]"+bitWise+";\n" +
//						"\t\t\t\tif( value < 0 ) {\n" +
//						"\t\t\t\t\toutput.data[indexDst] = "+typeCast+"-Math.log(val - value);\n" +
//						"\t\t\t\t} else {\n" +
//						"\t\t\t\t\toutput.data[indexDst] = "+typeCast+"Math.log(val + value);\n" +
//						"\t\t\t\t}\n" +
//						"\t\t\t}\n" +
//						"\t\t}\n" +
//						"\t\t//CONCURRENT_ABOVE });\n" +
//						"\t}\n\n");
//	}

//	public void printPow2( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
//		String bitWise = typeIn.getBitWise();
//		String sumType = typeIn.getSumType();
//		String typeCast = typeOut.getDataType().equals(sumType) ? "" : "("+typeOut.getDataType()+")";
//
//		out.print(
//				"\tpublic static void pow2( "+typeIn.getSingleBandName()+" input, "+typeOut.getSingleBandName()+" output ) {\n" +
//				"\n" +
//				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.height,y->{\n" +
//				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
//				"\t\t\tint indexSrc = input.startIndex + y* input.stride;\n" +
//				"\t\t\tint indexDst = output.startIndex + y* output.stride;\n" +
//				"\t\t\tint end = indexSrc + input.width;\n" +
//				"\n" +
//				"\t\t\tfor( ; indexSrc < end; indexSrc++, indexDst++) {\n" +
//				"\t\t\t\t"+sumType+" v = input.data[indexSrc]"+bitWise+";\n" +
//				"\t\t\t\toutput.data[indexDst] = "+typeCast+"(v*v);\n" +
//				"\t\t\t}\n" +
//				"\t\t}\n" +
//				"\t\t//CONCURRENT_ABOVE });\n" +
//				"\t}\n\n");
//	}

//	public void printSqrt( AutoTypeImage typeIn, AutoTypeImage typeOut ) {
//		String bitWise = typeIn.getBitWise();
//		String typeCast = typeOut != AutoTypeImage.F64 ? "("+typeOut.getDataType()+")" : "";
//
//		out.print(
//				"\tpublic static void sqrt( "+typeIn.getSingleBandName()+" input, "+typeOut.getSingleBandName()+" output ) {\n" +
//				"\n" +
//				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.height,y->{\n" +
//				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
//				"\t\t\tint indexSrc = input.startIndex + y* input.stride;\n" +
//				"\t\t\tint indexDst = output.startIndex + y* output.stride;\n" +
//				"\t\t\tint end = indexSrc + input.width;\n" +
//				"\n" +
//				"\t\t\tfor( ; indexSrc < end; indexSrc++, indexDst++) {\n" +
//				"\t\t\t\toutput.data[indexDst] = "+typeCast+"Math.sqrt(input.data[indexSrc]"+bitWise+");\n" +
//				"\t\t\t}\n" +
//				"\t\t}\n" +
//				"\t\t//CONCURRENT_ABOVE });\n" +
//				"\t}\n\n");
//	}

	public void printStdev( AutoTypeImage typeMean, AutoTypeImage typePow2 ) {
		String bitWiseMean = typeMean.getBitWise();
		String bitWisePow = typePow2.getBitWise();
		String typeCast = typeMean.getDataType();
		String sumType = typeMean.getSumType();

		out.print("\tpublic static void stdev( " + typeMean.getSingleBandName() + " mean, " + typePow2.getSingleBandName() + " pow2, " + typeMean.getSingleBandName() + " stdev ) {\n" +
				"\n" +
				"\t\tfinal int h = mean.getHeight();\n" +
				"\t\tfinal int w = mean.getWidth();\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor (int y = 0; y < h; y++) {\n" +
				"\t\t\tint indexMean  = mean.startIndex  + y * mean.stride;\n" +
				"\t\t\tint indexPow   = pow2.startIndex  + y * pow2.stride;\n" +
				"\t\t\tint indexStdev = stdev.startIndex + y * stdev.stride;\n" +
				"\n" +
				"\t\t\tint indexEnd = indexMean+w;\n" +
				"\t\t\t// for(int x = 0; x < w; x++ ) {\n" +
				"\t\t\tfor (; indexMean < indexEnd; indexMean++, indexPow++, indexStdev++ ) {\n" +
				"\t\t\t\t" + sumType + " mu = mean.data[indexMean]" + bitWiseMean + ";\n" +
				"\t\t\t\t" + sumType + " p2 = pow2.data[indexPow]" + bitWisePow + ";\n" +
				"\n" +
				"\t\t\t\tstdev.data[indexStdev] = (" + typeCast + ")Math.sqrt(Math.max(0,p2-mu*mu));\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	class Multiple implements TwoTemplate {

		@Override
		public String getVariableName() { return "value";}

		@Override
		public boolean isScaleOp() { return true; }

		@Override
		public boolean isImageFirst() { return true; }

		@Override
		public AutoTypeImage[] getTypes() { return AutoTypeImage.getSpecificTypes(); }

		@Override
		public String getName() {return "multiply";}

		@Override
		public String getOperation() {
			String round = output.isInteger() ? "Math.round" : "";

			return round + "((input[indexSrc] " + input.getBitWise() + ") * value)";
		}
	}

	class Divide implements TwoTemplate {

		@Override public String getVariableName() { return "denominator";}

		@Override public boolean isScaleOp() { return true; }

		@Override public boolean isImageFirst() { return true; }

		@Override public AutoTypeImage[] getTypes() { return AutoTypeImage.getSpecificTypes(); }

		@Override
		public String getName() {return "divide";}

		@Override
		public String getOperation() {
			String round = output.isInteger() ? "Math.round" : "";

			return round + "((input[indexSrc] " + input.getBitWise() + ") / denominator)";
		}
	}

	class Plus implements TwoTemplate {

		@Override public String getVariableName() { return "value";}

		@Override public boolean isScaleOp() { return false; }

		@Override public boolean isImageFirst() { return true; }

		@Override public AutoTypeImage[] getTypes() { return AutoTypeImage.getSpecificTypes(); }

		@Override
		public String getName() {return "plus";}

		@Override
		public String getOperation() {
			return "((input[indexSrc] " + input.getBitWise() + ") + value)";
		}
	}

	class Minus implements TwoTemplate {

		boolean imageFirst;

		public Minus( boolean imageFirst ) {
			this.imageFirst = imageFirst;
		}

		@Override public String getVariableName() { return "value";}

		@Override public boolean isScaleOp() { return false; }

		@Override public boolean isImageFirst() { return imageFirst; }

		@Override public AutoTypeImage[] getTypes() { return AutoTypeImage.getSpecificTypes(); }

		@Override
		public String getName() {return "minus";}

		@Override
		public String getOperation() {
			if (imageFirst)
				return "((input[indexSrc] " + input.getBitWise() + ") - value)";
			else
				return "(value - (input[indexSrc] " + input.getBitWise() + "))";
		}
	}

	interface Template {
		String getName();

		String getOperation();
	}

	interface TwoTemplate extends Template {
		String getVariableName();

		boolean isScaleOp();

		boolean isImageFirst();

		AutoTypeImage[] getTypes();
	}

	interface PrintString {
		String print( AutoTypeImage type );
	}

	interface PrintString2 {
		String print( AutoTypeImage typeSrc, AutoTypeImage typeDst );
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var gen = new GenerateImplPixelMath();
		gen.setModuleName("boofcv-ip");
		gen.generate();
	}
}
