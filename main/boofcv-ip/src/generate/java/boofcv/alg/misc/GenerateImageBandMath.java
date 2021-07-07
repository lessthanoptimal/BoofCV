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

package boofcv.alg.misc;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Nico Stuurman
 * @author Peter Abeles
 */

@SuppressWarnings("Duplicates")
public class GenerateImageBandMath extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		printCheckInput();

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
				"import boofcv.alg.misc.impl.ImplImageBandMath;\n" +
				"import boofcv.alg.misc.impl.ImplImageBandMath_MT;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"\n" +
				"/**\n" +
				" * Collection of functions that project Bands of Planar images onto\n" +
				" * a single image. Can be used to perform projections such as\n" +
				" * minimum, maximum, average, median, standard Deviation.\n" +
				" */\n" +
				generateDocString("Nico Stuurman","Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class " + className + " {\n\n");
	}

	private void printCheckInput() {

		out.println("\tpublic static <T extends ImageGray<T>> void checkInput(Planar<T> input, int startBand, int lastBand) {\n" +
				"\t\tif (startBand < 0 || lastBand < 0) {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"startBand or lastBand is less than zero\");\n" +
				"\t\t}\n" +
				"\t\tif (startBand > lastBand) {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"startBand should <= lastBand\");\n" +
				"\t\t}\n" +
				"\t\tif (lastBand >= input.getNumBands()) {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"lastBand should be less than number of Bands in input\");\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	protected void create(AutoTypeImage inputImg) {
		String[] actions = {"minimum", "maximum", "average", "median"};
		for (String action : actions) {
			printAction(inputImg, action);
		}
		printStdDev(inputImg);
	}


	void printAction(AutoTypeImage inputImg, String action) {
		String band = inputImg.getSingleBandName();

		out.print(
			"\t/**\n" +
			"\t * Computes the "+action+" for each pixel across all bands in the {@link Planar} image.\n" +
			"\t *\n" +
			"\t * @param input     Planar image\n" +
			"\t * @param output    Gray scale image containing "+action+" pixel values\n" +
			"\t */\n" +
			"\tpublic static void "+action+"(Planar<"+band+"> input, "+band+" output) {\n" +
			"\t\toutput.reshape(input.width,input.height);\n" +
			"\t\tif (BoofConcurrency.USE_CONCURRENT) {\n" +
			"\t\t\tImplImageBandMath_MT."+action+"(input, output, 0, input.getNumBands() - 1);\n" +
			"\t\t} else {\n" +
			"\t\t\tImplImageBandMath."+action+"(input, output, 0, input.getNumBands() - 1);\n" +
			"\t\t}\n" +
			"\t}\n\n" +

			"\t/**\n" +
			"\t * Computes the "+action+" for each pixel across selected bands in the {@link Planar} image.\n" +
			"\t *\n" +
			"\t * @param input     Planar image\n" +
			"\t * @param output    Gray scale image containing "+action+" pixel values\n" +
			"\t * @param startBand First band to be included in the projection\n" +
			"\t * @param lastBand  Last band to be included in the projection\n" +
			"\t */\n" +
			"\tpublic static void "+action+"(Planar<"+band+"> input, "+band+" output, int startBand, int lastBand) {\n" +
			"\t\tcheckInput(input, startBand, lastBand);\n" +
			"\t\toutput.reshape(input.width,input.height);\n" +
			"\t\tif (BoofConcurrency.USE_CONCURRENT) {\n" +
			"\t\t\tImplImageBandMath_MT."+action+"(input, output, startBand, lastBand);\n" +
			"\t\t} else {\n" +
			"\t\t\tImplImageBandMath."+action+"(input, output, startBand, lastBand);\n" +
			"\t\t}\n" +
			"\t}\n\n"
		);
	}

	public void printStdDev(AutoTypeImage inputImg) {
		String band = inputImg.getSingleBandName();

		out.print("\t/**\n" +
			"\t * Computes the standard deviation for each pixel across all bands in the {@link Planar}\n" +
			"\t * image.\n" +
			"\t * @param input     Planar image - not modified\n" +
			"\t * @param output    Gray scale image containing average pixel values - modified\n" +
			"\t * @param avg       Input Gray scale image containing average image. Can be null\n" +
			"\t*/\n" +
			"\tpublic static void stdDev(Planar<"+band+"> input, "+band+" output, @Nullable "+band+" avg) {\n"+
			"\t\tstdDev(input,output,avg,0,input.getNumBands() - 1);\n" +
			"\t}\n\n" +

			"\t/**\n" +
			"\t * Computes the standard deviation for each pixel across all bands in the {@link Planar}\n" +
			"\t * image.\n" +
			"\t * @param input     Planar image - not modified\n" +
			"\t * @param output    Gray scale image containing average pixel values - modified\n" +
			"\t * @param avg       Input Gray scale image containing average image. Can be null\n" +
			"\t * @param startBand First band to be included in the projection\n" +
			"\t * @param lastBand  Last band to be included in the projection\n" +
			"\t*/\n" +
			"\tpublic static void stdDev(Planar<"+band+"> input, "+band+" output, @Nullable "+band+" avg, int startBand, int lastBand) {\n"+
			"\t\tcheckInput(input, startBand, lastBand);\n" +
			"\t\toutput.reshape(input.width,input.height);\n" +
			"\t\tif( avg == null ) {\n" +
			"\t\t\tavg = new "+band+"(input.width,input.height);\n" +
			"\t\t\taverage(input,avg,startBand,lastBand);\n" +
			"\t\t}\n" +
			"\t\tif (BoofConcurrency.USE_CONCURRENT) {\n" +
			"\t\t\tImplImageBandMath_MT.stdDev(input,output,avg,startBand,lastBand);\n" +
			"\t\t} else {\n" +
			"\t\t\tImplImageBandMath.stdDev(input,output,avg,startBand,lastBand);\n" +
			"\t\t}\n" +
			"\t}\n\n"
		);
	}

	public static void main(String[] args) throws FileNotFoundException {
		GenerateImageBandMath gen = new GenerateImageBandMath();
		gen.parseArguments(args);
		gen.generateCode();
	}


}
