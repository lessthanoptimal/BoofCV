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

/// Generates ImplConvolveMean_MT. Each block of rows is processed by ImplConvolveMean
public class GenerateImplConvolveMean_MT extends CodeGeneratorBase {

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

	public void printPreamble() {
		autoSelectName();
		out.print("""
				import boofcv.concurrency.BoofConcurrency;
				import boofcv.struct.border.ImageBorder_F32;
				import boofcv.struct.border.ImageBorder_F64;
				import boofcv.struct.border.ImageBorder_S32;
				import boofcv.struct.image.*;
				import org.ddogleg.struct.DogArray;
				import org.ddogleg.struct.DogArray_F32;
				import org.ddogleg.struct.DogArray_F64;
				import org.ddogleg.struct.DogArray_I32;
				import org.jetbrains.annotations.Nullable;
				import pabeles.concurrency.GrowArray;

				import javax.annotation.Generated;

				/**
				 * Concurrent implementation of {@link ImplConvolveMean}, with identical results. Each block of rows is processed
				 * by ImplConvolveMean, border included. Floating point images split rows at the rows where ImplConvolveMean
				 * restarts its running sums (see ImplConvolveBox.VERTICAL_RESTART), so the results don't depend on the number
				 * of threads. Integer images can split rows anywhere. vertical()'s workspaces hold one set of row buffers for each block.
				""" + generateDocString("Peter Abeles") + "public class " + className + " {\n\n");
	}

	public void addFunctions( AutoTypeImage imageIn, AutoTypeImage imageOut ) {
		var gen = new GenerateImplConvolveMean();
		gen.imageIn = imageIn;
		gen.imageOut = imageOut;
		printFunctions(gen, "", "border, ", "@Nullable BORDER_TYPE border");
		printFunctions(gen, "Inner", "", "");
	}

	/// horizontal() and vertical(), or the versions without the border
	private void printFunctions( GenerateImplConvolveMean gen, String suffix, String borderArg, String borderParam ) {
		String extra = borderParam.isEmpty() ? "" : ",\n\t\t\t\t\t\t\t\t\t\t  " + borderParam;
		out.print(gen.apply("""
					public static void horizontalSUFFIX( NAME_IN input, NAME_OUT output, int offset, int lengthEXTRA ) {
						BoofConcurrency.loopBlocks(0, input.height, 2, ( y0, y1 ) ->
								ImplConvolveMean.horizontalSUFFIX(input, output, offset, length, BORDER_ARGy0, y1));
					}

					public static void verticalSUFFIX( NAME_IN input, NAME_OUT output, int offset, int lengthEXTRA,
													  GrowArray<DogArray<ROW_ARRAY>> workspaces ) {
				""".replace("EXTRA", extra).replace("SUFFIX", suffix).replace("BORDER_ARG", borderArg)));
		if (gen.imageIn.isInteger()) {
			// integer sums don't depend on where a block restarts them
			out.print(gen.apply("""
							BoofConcurrency.loopBlocks(0, input.height, length, workspaces, ( rows, y0, y1 ) ->
									ImplConvolveMean.verticalSUFFIX(input, output, offset, length, BORDER_ARGy0, y1, rows));
						}

					""".replace("SUFFIX", suffix).replace("BORDER_ARG", borderArg)));
		} else {
			// blocks start where ImplConvolveMean restarts the running sums
			out.print(gen.apply("""
							final int blocks = ImplConvolveBox.restartBlocks(input.height, length);
							BoofConcurrency.loopBlocks(0, blocks, workspaces, ( rows, b0, b1 ) ->
									ImplConvolveMean.verticalSUFFIX(input, output, offset, length, BORDER_ARG
											ImplConvolveBox.restartRow(b0, blocks, input.height, offset),
											ImplConvolveBox.restartRow(b1, blocks, input.height, offset), rows));
						}

					""".replace("SUFFIX", suffix).replace("BORDER_ARG", borderArg)));
		}
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var generator = new GenerateImplConvolveMean_MT();
		generator.setModuleName("boofcv-ip");
		generator.generateCode();
	}
}
