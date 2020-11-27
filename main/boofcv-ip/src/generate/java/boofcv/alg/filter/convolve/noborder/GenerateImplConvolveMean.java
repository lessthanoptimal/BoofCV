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

package boofcv.alg.filter.convolve.noborder;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplConvolveMean extends CodeGeneratorBase {

	AutoTypeImage imageIn;
	AutoTypeImage imageOut;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I8);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.println("}");
	}

	public void addFunctions( AutoTypeImage imageIn , AutoTypeImage imageOut ) throws FileNotFoundException {
		this.imageIn = imageIn;
		this.imageOut = imageOut;
		printHorizontal();
		printVertical();
	}

	public void printPreamble() {
		out.print(
				"import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import boofcv.concurrency.*;\n" +
				"import org.ddogleg.struct.DogArray_F32;\n" +
				"import org.ddogleg.struct.DogArray_F64;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n");

		out.print(
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Convolves a mean filter across the image. The mean value of all the pixels are computed inside the kernel.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\",\"Duplicates\"})\n" +
				"public class " + className + " {\n\n");
	}

	public void printHorizontal() {

		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String declareHalf = imageIn.isInteger() ? "\t\tfinal " + sumType + " halfDivisor = divisor/2;\n" : "";
		String divide = imageIn.isInteger() ? "(total+halfDivisor)/divisor" : "total/divisor";

		out.print("\tpublic static void horizontal( " + imageIn.getSingleBandName() + " input ," + imageOut.getSingleBandName() + " output, int offset, int length ) {\n" +
				"\t\tfinal " + sumType + " divisor = length;\n" +
				declareHalf);
		String body = "";

		body += "\t\t\tint indexIn = input.startIndex + input.stride*y;\n" +
				"\t\t\tint indexOut = output.startIndex + output.stride*y + offset;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\n" +
				"\t\t\tint indexEnd = indexIn + length;\n" +
				"\t\t\t\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn++) {\n" +
				"\t\t\t\ttotal += input.data[indexIn] " + bitWise + ";\n" +
				"\t\t\t}\n" +
				"\t\t\toutput.data[indexOut++] = " + typeCast + "("+divide+");\n" +
				"\n" +
				"\t\t\tindexEnd = indexIn + input.width - length;\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn++) {\n" +
				"\t\t\t\ttotal -= input.data[indexIn - length] " + bitWise + ";\n" +
				"\t\t\t\ttotal += input.data[indexIn] " + bitWise + ";\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut++] = " + typeCast + "("+divide+");\n" +
				"\t\t\t}\n";
		printParallel("y","0","input.height",body);
		out.print("\t}\n\n");
	}

	public void printVertical() {

		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String declareHalf = imageIn.isInteger() ? "\t\tfinal " + sumType + " halfDivisor = divisor/2;\n" : "";
		String divide = imageIn.isInteger() ? "(total + halfDivisor)/divisor" : "total/divisor";

		String workType = ("DogArray_"+imageIn.getKernelType()).replace("S32","I32");

		out.print("\tpublic static void vertical( "+imageIn.getSingleBandName()+" input, "+
				imageOut.getSingleBandName()+" output, int offset, int length, @Nullable GrowArray<"+workType+"> workspaces ) {\n" +
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, "+workType+"::new);\n" +
				"\t\tfinal "+workType+" work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal int backStep = length*input.stride;\n" +
				"\t\tfinal int offsetEnd = length - offset - 1;\n" +
				"\n" +
				"\t\tfinal "+sumType+" divisor = length;\n" +
				declareHalf +
				"\n" +
				"\t\t// To reduce cache misses it is processed along rows instead of going down columns, which is\n" +
				"\t\t// more natural for a vertical convolution. For parallel processes this requires building\n" +
				"\t\t// a book keeping array for each thread.\n");

		String body = "";

		body += "\t\t"+sumType+"[] totals = BoofMiscOps.checkDeclare(work, input.width, false);\n" +
				"\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\tint indexIn = input.startIndex + (y0 - offset)*input.stride + x;\n" +
				"\t\t\tint indexOut = output.startIndex + output.stride*y0 + x;\n" +
				"\n" +
				"\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\tint indexEnd = indexIn + input.stride*length;\n" +
				"\t\t\tfor (; indexIn < indexEnd; indexIn += input.stride) {\n" +
				"\t\t\t\ttotal += input.data[indexIn] "+bitWise+";\n" +
				"\t\t\t}\n" +
				"\t\t\ttotals[x] = total;\n" +
				"\t\t\toutput.data[indexOut] = "+typeCast+"("+divide+");\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// change the order it is processed in to reduce cache misses\n" +
				"\t\tfor (int y = y0 + 1; y < y1; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + (y + offsetEnd)*input.stride;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++, indexIn++, indexOut++) {\n" +
				"\t\t\t\t"+sumType+" total = totals[x] - (input.data[indexIn - backStep]"+bitWise+");\n" +
				"\t\t\t\ttotals[x] = total += input.data[indexIn]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut] = "+typeCast+"("+divide+");\n" +
				"\t\t\t}\n" +
				"\t\t}\n";

		printParallelBlock("y0","y1","offset","output.height - offsetEnd","length",body);

		out.print("\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		var generator = new GenerateImplConvolveMean();
		generator.setModuleName("boofcv-ip");
		generator.generate();
	}
}
