/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

public class GenerateImplConvertPlanarToGray extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		for( AutoTypeImage in : AutoTypeImage.getSpecificTypes()) {
			printAverage(in);
		}

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(
				"import javax.annotation.Generated;\n" +
				"import boofcv.struct.image.*;\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Low level implementations of different methods for converting {@link Planar} into\n" +
				" * {@link boofcv.struct.image.ImageGray}.\n" +
				" * \n" +
				" * <ul>\n" +
				" * <li>Average computes the average value of each pixel across the bands.\n" +
				" * </ul>\n" +
				" * \n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class "+className+" {\n\n");
	}

	private void printAverage( AutoTypeImage in ) {

		String imageType = in.getSingleBandName();
		String sumType = in.getSumType();
		String typecast = in.getTypeCastFromSum();
		String bitwise = in.getBitWise();

		out.print("\tpublic static void average( Planar<"+imageType+"> from , "+imageType+" to ) {\n" +
				"\t\tint numBands = from.getNumBands();\n" +
				"\n" +
				"\t\tif( numBands == 1 ) {\n" +
				"\t\t\tto.setTo(from.getBand(0));\n" +
				"\t\t} else if( numBands == 3 ) {\n" +
				"\t\t\t"+imageType+" band0 = from.getBand(0);\n" +
				"\t\t\t"+imageType+" band1 = from.getBand(1);\n" +
				"\t\t\t"+imageType+" band2 = from.getBand(2);\n" +
				"\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, from.height, y -> {\n" +
				"\t\t\tfor (int y = 0; y < from.height; y++) {\n" +
				"\t\t\t\tint indexFrom = from.getIndex(0, y);\n" +
				"\t\t\t\tint indexTo = to.getIndex(0, y);\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < from.width; x++ , indexFrom++ ) {\n" +
				"\t\t\t\t\t"+sumType+" sum = band0.data[indexFrom]"+bitwise+";\n" +
				"\t\t\t\t\tsum += band1.data[indexFrom]"+bitwise+";\n" +
				"\t\t\t\t\tsum += band2.data[indexFrom]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\t\tto.data[indexTo++] = "+typecast+"(sum/3);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t} else {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, from.height, y -> {\n" +
				"\t\t\tfor (int y = 0; y < from.height; y++) {\n" +
				"\t\t\t\tint indexFrom = from.getIndex(0, y);\n" +
				"\t\t\t\tint indexTo = to.getIndex(0, y);\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < from.width; x++ , indexFrom++ ) {\n" +
				"\t\t\t\t\t"+sumType+" sum = 0;\n" +
				"\t\t\t\t\tfor( int b = 0; b < numBands; b++ ) {\n" +
				"\t\t\t\t\t\tsum +=  from.bands[b].data[indexFrom]"+bitwise+";\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t\tto.data[indexTo++] = "+typecast+"(sum/numBands);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplConvertPlanarToGray app = new GenerateImplConvertPlanarToGray();

		app.generateCode();
	}
}
