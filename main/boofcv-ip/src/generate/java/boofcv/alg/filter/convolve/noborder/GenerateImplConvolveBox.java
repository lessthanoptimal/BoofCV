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
public class GenerateImplConvolveBox extends CodeGeneratorBase {

	AutoTypeImage imageIn;
	AutoTypeImage imageOut;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U8, AutoTypeImage.S32);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.S32, AutoTypeImage.S32);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.println("}");
	}

	public void addFunctions( AutoTypeImage imageIn , AutoTypeImage imageOut ) throws FileNotFoundException
	{
		this.imageIn = imageIn;
		this.imageOut = imageOut;
		printHorizontal();
		printVertical();
	}

	public void printPreamble() {
		autoSelectName();
		out.print(
				"import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import boofcv.concurrency.*;\n" +
				"import org.ddogleg.struct.DogArray_F32;\n" +
				"import org.ddogleg.struct.DogArray_F64;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import org.jetbrains.annotations.Nullable;\n");

		out.print("\n//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n");

		out.print("\n" +
				"/**\n" +
				" * <p>\n" +
				" * Convolves a box filter across an image. A box filter is equivalent to convolving a kernel with all 1's.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings({\"ForLoopReplaceableByForEach\",\"Duplicates\"})\n" +
				"public class " + className + " {\n\n");
	}

	public void printHorizontal() {

		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		out.print("\tpublic static void horizontal( " + imageIn.getSingleBandName() + " input , " + imageOut.getSingleBandName() + " output , int radius ) {\n" +
				"\t\tfinal int kernelWidth = radius*2 + 1;\n");

		String body = "";
		body += "\t\t\tint indexIn = input.startIndex + input.stride*y;\n" +
				"\t\t\tint indexOut = output.startIndex + output.stride*y + radius;\n" +
				"\n" +
				"\t\t\t" + sumType + " total = 0;\n" +
				"\n" +
				"\t\t\tint indexEnd = indexIn + kernelWidth;\n" +
				"\t\t\t\n" +
				"\t\t\tfor( ; indexIn < indexEnd; indexIn++ ) {\n" +
				"\t\t\t\ttotal += input.data[indexIn] " + bitWise + ";\n" +
				"\t\t\t}\n" +
				"\t\t\toutput.data[indexOut++] = " + typeCast + "total;\n" +
				"\n" +
				"\t\t\tindexEnd = indexIn + input.width - kernelWidth;\n" +
				"\t\t\tfor( ; indexIn < indexEnd; indexIn++ ) {\n" +
				"\t\t\t\ttotal -= input.data[ indexIn - kernelWidth ] " + bitWise + ";\n" +
				"\t\t\t\ttotal += input.data[ indexIn ] " + bitWise + ";\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut++] = " + typeCast + "total;\n" +
				"\t\t\t}\n";

		printParallel("y","0","input.height",body);
		out.print("\t}\n\n");
	}

	public void printVertical() {

		String typeCast = imageOut.getTypeCastFromSum();
		String sumType = imageIn.getSumType();
		String bitWise = imageIn.getBitWise();

		String workType = ("DogArray_"+imageIn.getKernelType()).replace("S32","I32");

		out.print("\tpublic static void vertical(" + imageIn.getSingleBandName() + " input, "
				+ imageOut.getSingleBandName() + " output, int radius, @Nullable GrowArray<"+workType+"> workspaces) {\n" +
				"\t\tworkspaces = BoofMiscOps.checkDeclare(workspaces, "+workType+"::new);\n" +
				"\t\tfinal "+workType+" work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal int kernelWidth = radius*2 + 1;\n" +
				"\n" +
				"\t\tfinal int backStep = kernelWidth*input.stride;\n");

		String body = "";

		body += "\t\t"+sumType+" totals[] = BoofMiscOps.checkDeclare(work,input.width,false);\n" +
				"\t\tfor( int x = 0; x < input.width; x++ ) {\n" +
				"\t\t\tint indexIn = input.startIndex + (y0-radius)*input.stride + x;\n" +
				"\t\t\tint indexOut = output.startIndex + output.stride*y0 + x;\n" +
				"\n" +
				"\t\t\t"+sumType+" total = 0;\n" +
				"\t\t\tint indexEnd = indexIn + input.stride*kernelWidth;\n" +
				"\t\t\tfor( ; indexIn < indexEnd; indexIn += input.stride) {\n" +
				"\t\t\t\ttotal += input.data[indexIn] "+bitWise+";\n" +
				"\t\t\t}\n" +
				"\t\t\ttotals[x] = total;\n" +
				"\t\t\toutput.data[indexOut] = "+typeCast+"total;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// change the order it is processed in to reduce cache misses\n" +
				"\t\tfor( int y = y0+1; y < y1; y++ ) {\n" +
				"\t\t\tint indexIn = input.startIndex + (y+radius)*input.stride;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\tfor( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {\n" +
				"\t\t\t\t"+sumType+" total = totals[ x ]  - (input.data[ indexIn - backStep ]"+bitWise+");\n" +
				"\t\t\t\ttotals[ x ] = total += input.data[ indexIn ]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut] = "+typeCast+"total;\n" +
				"\t\t\t}\n" +
				"\t\t}\n";

		printParallelBlock("y0","y1","radius","output.height-radius","kernelWidth",body);
		out.print("\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		GenerateImplConvolveBox generator = new GenerateImplConvolveBox();
		generator.setModuleName("boofcv-ip");
		generator.generateCode();
	}
}
