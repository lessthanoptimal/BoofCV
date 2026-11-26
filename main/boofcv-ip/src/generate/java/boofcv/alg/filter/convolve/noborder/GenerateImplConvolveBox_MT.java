/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import static boofcv.alg.filter.convolve.noborder.GenerateImplConvolveBox.bufferArray;
import static boofcv.alg.filter.convolve.noborder.GenerateImplConvolveBox.bufferType;

/// Generates ImplConvolveBox_MT. Each block of rows is processed by ImplConvolveBox
public class GenerateImplConvolveBox_MT extends CodeGeneratorBase {

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

	public void printPreamble() {
		autoSelectName();
		out.print("import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.struct.image.*;\n" +
				"import org.ddogleg.struct.DogArray;\n" +
				"import org.ddogleg.struct.DogArray_F32;\n" +
				"import org.ddogleg.struct.DogArray_F64;\n" +
				"import org.ddogleg.struct.DogArray_I16;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import pabeles.concurrency.GrowArray;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * Concurrent implementation of {@link ImplConvolveBox}, with identical results. Each block of rows is processed\n" +
				" * by ImplConvolveBox. Floating point images split rows at the rows where ImplConvolveBox restarts its running\n" +
				" * sums (see VERTICAL_RESTART), so the results don't depend on the number of threads. That is about 1.5x faster\n" +
				" * than splitting columns, which needs no restarts. Integer images can split rows anywhere. vertical()'s\n" +
				" * workspaces hold one set of row buffers for each block.\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n\n");
	}

	public void addFunctions( AutoTypeImage imageIn, AutoTypeImage imageOut ) {
		String nameIn = imageIn.getSingleBandName(), nameOut = imageOut.getSingleBandName();
		String rowsType = "DogArray<" + bufferArray(bufferType(imageOut, imageIn)) + ">";

		out.print("\tpublic static void horizontal( " + nameIn + " input, " + nameOut + " output, int radius ) {\n" +
				"\t\tBoofConcurrency.loopBlocks(0, input.height, 2, ( y0, y1 ) ->\n" +
				"\t\t\t\tImplConvolveBox.horizontal(input, output, radius, y0, y1));\n" +
				"\t}\n\n");

		out.print("\tpublic static void vertical( " + nameIn + " input, " + nameOut + " output, int radius,\n" +
				"\t\t\t\t\t\t\t\t GrowArray<" + rowsType + "> workspaces ) {\n");
		if (imageIn.isInteger()) {
			// integer sums don't depend on where a block restarts them
			out.print("\t\tBoofConcurrency.loopBlocks(0, input.height, 2*radius + 1, workspaces, ( rows, y0, y1 ) ->\n" +
					"\t\t\t\tImplConvolveBox.vertical(input, output, radius, y0, y1, rows));\n");
		} else {
			// blocks start where ImplConvolveBox restarts the running sums
			out.print("\t\tfinal int blocks = ImplConvolveBox.restartBlocks(input.height, radius);\n" +
					"\t\tBoofConcurrency.loopBlocks(0, blocks, workspaces, ( rows, b0, b1 ) ->\n" +
					"\t\t\t\tImplConvolveBox.vertical(input, output, radius,\n" +
					"\t\t\t\t\t\tImplConvolveBox.restartRow(b0, blocks, input.height, radius),\n" +
					"\t\t\t\t\t\tImplConvolveBox.restartRow(b1, blocks, input.height, radius), rows));\n");
		}
		out.print("\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var generator = new GenerateImplConvolveBox_MT();
		generator.setModuleName("boofcv-ip");
		generator.generateCode();
	}
}
