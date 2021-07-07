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

package boofcv.alg.feature.detect.edge.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateImplGradientToEdgeFeatures extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printFunctions(AutoTypeImage.F32);
		printFunctions(AutoTypeImage.S16);
		printFunctions(AutoTypeImage.S32);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Implementations of the core algorithms of {@link boofcv.alg.feature.detect.edge.GradientToEdgeFeatures}.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * WARNING: Do not modify. Automatically generated by "+getClass().getSimpleName()+".\n" +
				" * </p>\n" +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class "+className+" {\n\n");
	}

	private void printFunctions(AutoTypeImage derivType )
	{
		printItensityE(derivType);
		printIntensityAbs(derivType);
		printDirection(derivType);
		printDirection2(derivType);
	}

	private void printItensityE(AutoTypeImage derivType) {

		String bitWise = derivType.getBitWise();
		String sumType = derivType.getSumType();

		out.print("\tstatic public void intensityE( "+derivType.getSingleBandName()+" derivX , "+derivType.getSingleBandName()+" derivY , GrayF32 intensity )\n" +
				"\t{\n" +
				"\t\tfinal int w = derivX.width;\n" +
				"\t\tfinal int h = derivY.height;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tint indexX = derivX.startIndex + y*derivX.stride;\n" +
				"\t\t\tint indexY = derivY.startIndex + y*derivY.stride;\n" +
				"\t\t\tint indexI = intensity.startIndex + y*intensity.stride;\n" +
				"\n" +
				"\t\t\tint end = indexX + w;\n" +
				"\t\t\tfor( ; indexX < end; indexX++ , indexY++ , indexI++ ) {\n" +
				"\t\t\t\t"+sumType+" dx = derivX.data[indexX]"+bitWise+";\n" +
				"\t\t\t\t"+sumType+" dy = derivY.data[indexY]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\tintensity.data[indexI] = (float)Math.sqrt(dx*dx + dy*dy);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printIntensityAbs(AutoTypeImage derivType) {

		String bitWise = derivType.getBitWise();

		out.print("\tstatic public void intensityAbs( "+derivType.getSingleBandName()+" derivX , "+derivType.getSingleBandName()+" derivY , GrayF32 intensity )\n" +
				"\t{\n" +
				"\t\tfinal int w = derivX.width;\n" +
				"\t\tfinal int h = derivY.height;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tint indexX = derivX.startIndex + y*derivX.stride;\n" +
				"\t\t\tint indexY = derivY.startIndex + y*derivY.stride;\n" +
				"\t\t\tint indexI = intensity.startIndex + y*intensity.stride;\n" +
				"\n" +
				"\t\t\tint end = indexX + w;\n" +
				"\t\t\tfor( ; indexX < end; indexX++ , indexY++ , indexI++ ) {\n" +
				"\n" +
				"\t\t\t\tintensity.data[indexI] = Math.abs(derivX.data[indexX]"+bitWise+") +  Math.abs(derivY.data[indexY]"+bitWise+");\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printDirection(AutoTypeImage derivType) {

		String bitWise = derivType.getBitWise();
		String sumType = derivType.getSumType();

		out.print("\tstatic public void direction( "+derivType.getSingleBandName()+" derivX , "+derivType.getSingleBandName()+" derivY , GrayF32 angle )\n" +
				"\t{\n" +
				"\t\tfinal int w = derivX.width;\n" +
				"\t\tfinal int h = derivY.height;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tint indexX = derivX.startIndex + y*derivX.stride;\n" +
				"\t\t\tint indexY = derivY.startIndex + y*derivY.stride;\n" +
				"\t\t\tint indexA = angle.startIndex + y*angle.stride;\n" +
				"\n" +
				"\t\t\tint end = indexX + w;\n" +
				"\t\t\tfor( ; indexX < end; indexX++ , indexY++ , indexA++ ) {\n" +
				"\t\t\t\t"+sumType+" dx = derivX.data[indexX]"+bitWise+";\n" +
				"\t\t\t\t"+sumType+" dy = derivY.data[indexY]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\t// compute the angle while avoiding divided by zero errors\n");
		if( derivType.isInteger() ) {
			out.print("\t\t\t\tangle.data[indexA] = dx == 0 ? (float)(Math.PI/2.0) : (float)Math.atan((double)dy/(double)dx);\n");
		} else {
			out.print("\t\t\t\tangle.data[indexA] = Math.abs(dx) < 1e-10f ? (float)(Math.PI/2.0) : (float)Math.atan(dy/dx);\n");
		}

		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printDirection2(AutoTypeImage derivType) {

		String bitWise = derivType.getBitWise();
		String sumType = derivType.getSumType();

		out.print("\tstatic public void direction2( "+derivType.getSingleBandName()+" derivX , "+derivType.getSingleBandName()+" derivY , GrayF32 angle )\n" +
				"\t{\n" +
				"\t\tfinal int w = derivX.width;\n" +
				"\t\tfinal int h = derivY.height;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,h,y->{\n" +
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tint indexX = derivX.startIndex + y*derivX.stride;\n" +
				"\t\t\tint indexY = derivY.startIndex + y*derivY.stride;\n" +
				"\t\t\tint indexA = angle.startIndex + y*angle.stride;\n" +
				"\n" +
				"\t\t\tint end = indexX + w;\n" +
				"\t\t\tfor( ; indexX < end; indexX++ , indexY++ , indexA++ ) {\n" +
				"\t\t\t\t"+sumType+" dx = derivX.data[indexX]"+bitWise+";\n" +
				"\t\t\t\t"+sumType+" dy = derivY.data[indexY]"+bitWise+";\n" +
				"\n" +
				"\t\t\t\t// compute the angle while avoiding divided by zero errors\n" +
				"\t\t\t\tangle.data[indexA] = (float)Math.atan2(dy,dx);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplGradientToEdgeFeatures app = new GenerateImplGradientToEdgeFeatures();
		app.parseArguments(args);
		app.generateCode();
	}
}
