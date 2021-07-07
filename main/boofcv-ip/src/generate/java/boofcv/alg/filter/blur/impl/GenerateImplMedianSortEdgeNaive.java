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

package boofcv.alg.filter.blur.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplMedianSortEdgeNaive extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printFunction(AutoTypeImage.F32);
		printFunction(AutoTypeImage.I);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.image.*;\n" +
				"import org.ddogleg.sorting.QuickSelect;\n" +
				"import org.ddogleg.struct.*;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Median filter which process only the image edges and uses quick select find the median.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * radius: size of the filter's box.<br>\n" +
				" * storage:  Used to store local values. If null an array will be declared.\n" +
				" * </p>\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n\n");
	}

	private void printFunction( AutoTypeImage image ) {

		String sumType = image.getSumType();
		String workspace = "DogArray_" + image.getDogArrayType();

		out.print("\tpublic static void process( " + image.getSingleBandName() + " input, " + image.getSingleBandName() + " output, int radiusX, int radiusY,\n" +
				"\t\t\t\t\t\t\t\t@Nullable " + workspace + " workspace ) {\n" +
				"\t\tint w = 2*radiusX + 1;\n" +
				"\t\tint h = 2*radiusY + 1;\n" +
				"\n" +
				"\t\t" + sumType + "[] workArray = BoofMiscOps.checkDeclare(workspace, w*h, false);\n" +
				"\n" +
				"\t\tfor (int y = 0; y < radiusY; y++) {\n" +
				"\t\t\tint minI = y - radiusY;\n" +
				"\t\t\tint maxI = y + radiusY + 1;\n" +
				"\t\t\tif (minI < 0) minI = 0;\n" +
				"\t\t\tif (maxI > input.height) maxI = input.height;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint minJ = x - radiusX;\n" +
				"\t\t\t\tint maxJ = x + radiusX + 1;\n" +
				"\n" +
				"\t\t\t\t// bound it ot be inside the image\n" +
				"\t\t\t\tif (minJ < 0) minJ = 0;\n" +
				"\t\t\t\tif (maxJ > input.width) maxJ = input.width;\n" +
				"\n" +
				"\t\t\t\tint index = 0;\n" +
				"\n" +
				"\t\t\t\tfor (int i = minI; i < maxI; i++) {\n" +
				"\t\t\t\t\tfor (int j = minJ; j < maxJ; j++) {\n" +
				"\t\t\t\t\t\tworkArray[index++] = input.get(j, i);\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// use quick select to avoid sorting the whole list\n" +
				"\t\t\t\t" + sumType + " median = QuickSelect.select(workArray, index/2, index);\n" +
				"\t\t\t\toutput.set(x, y, median);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor (int y = input.height - radiusY; y < input.height; y++) {\n" +
				"\t\t\tint minI = y - radiusY;\n" +
				"\t\t\tint maxI = y + radiusY + 1;\n" +
				"\t\t\tif (minI < 0) minI = 0;\n" +
				"\t\t\tif (maxI > input.height) maxI = input.height;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint minJ = x - radiusX;\n" +
				"\t\t\t\tint maxJ = x + radiusX + 1;\n" +
				"\n" +
				"\t\t\t\t// bound it ot be inside the image\n" +
				"\t\t\t\tif (minJ < 0) minJ = 0;\n" +
				"\t\t\t\tif (maxJ > input.width) maxJ = input.width;\n" +
				"\n" +
				"\t\t\t\tint index = 0;\n" +
				"\n" +
				"\t\t\t\tfor (int i = minI; i < maxI; i++) {\n" +
				"\t\t\t\t\tfor (int j = minJ; j < maxJ; j++) {\n" +
				"\t\t\t\t\t\tworkArray[index++] = input.get(j, i);\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// use quick select to avoid sorting the whole list\n" +
				"\t\t\t\t" + sumType + " median = QuickSelect.select(workArray, index/2, index);\n" +
				"\t\t\t\toutput.set(x, y, median);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor (int y = radiusY; y < input.height - radiusY; y++) {\n" +
				"\t\t\tint minI = y - radiusY;\n" +
				"\t\t\tint maxI = y + radiusY + 1;\n" +
				"\t\t\tfor (int x = 0; x < radiusX; x++) {\n" +
				"\t\t\t\tint minJ = x - radiusX;\n" +
				"\t\t\t\tint maxJ = x + radiusX + 1;\n" +
				"\n" +
				"\t\t\t\t// bound it ot be inside the image\n" +
				"\t\t\t\tif (minJ < 0) minJ = 0;\n" +
				"\t\t\t\tif (maxJ > input.width) maxJ = input.width;\n" +
				"\n" +
				"\t\t\t\tint index = 0;\n" +
				"\n" +
				"\t\t\t\tfor (int i = minI; i < maxI; i++) {\n" +
				"\t\t\t\t\tfor (int j = minJ; j < maxJ; j++) {\n" +
				"\t\t\t\t\t\tworkArray[index++] = input.get(j, i);\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// use quick select to avoid sorting the whole list\n" +
				"\t\t\t\t" + sumType + " median = QuickSelect.select(workArray, index/2, index);\n" +
				"\t\t\t\toutput.set(x, y, median);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor (int y = radiusY; y < input.height - radiusY; y++) {\n" +
				"\t\t\tint minI = y - radiusY;\n" +
				"\t\t\tint maxI = y + radiusY + 1;\n" +
				"\t\t\tfor (int x = input.width - radiusX; x < input.width; x++) {\n" +
				"\t\t\t\tint minJ = x - radiusX;\n" +
				"\t\t\t\tint maxJ = x + radiusX + 1;\n" +
				"\n" +
				"\t\t\t\t// bound it ot be inside the image\n" +
				"\t\t\t\tif (minJ < 0) minJ = 0;\n" +
				"\t\t\t\tif (maxJ > input.width) maxJ = input.width;\n" +
				"\n" +
				"\t\t\t\tint index = 0;\n" +
				"\n" +
				"\t\t\t\tfor (int i = minI; i < maxI; i++) {\n" +
				"\t\t\t\t\tfor (int j = minJ; j < maxJ; j++) {\n" +
				"\t\t\t\t\t\tworkArray[index++] = input.get(j, i);\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// use quick select to avoid sorting the whole list\n" +
				"\t\t\t\t" + sumType + " median = QuickSelect.select(workArray, index/2, index);\n" +
				"\t\t\t\toutput.set(x, y, median);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var app = new GenerateImplMedianSortEdgeNaive();
		app.setModuleName("boofcv-ip");
		app.generate();
	}
}
