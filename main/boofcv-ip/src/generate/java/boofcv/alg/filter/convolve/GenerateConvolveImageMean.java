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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.noborder.GenerateImplConvolveMean;
import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/// Generates ConvolveImageMean, which picks between ImplConvolveMean and ImplConvolveMean_MT
public class GenerateConvolveImageMean extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I8);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.print("""
					private static boolean isConcurrent( ImageGray<?> input, int minPixels ) {
						return BoofConcurrency.USE_CONCURRENT && input.width*input.height >= minPixels;
					}
				}
				""");
	}

	public void addFunctions( AutoTypeImage imageIn, AutoTypeImage imageOut ) {
		var impl = new GenerateImplConvolveMean();
		impl.imageIn = imageIn;
		impl.imageOut = imageOut;

		out.print(impl.apply("""
					/// Mean along x. Near the image border only the pixels inside the image are averaged
					public static void horizontal( NAME_IN input, NAME_OUT output, int offset, int length ) {
						output.reshape(input);

						if (BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length))
							return;

						horizontal(input, output, offset, length, null, false);
					}

					/// Mean along y. Near the image border only the pixels inside the image are averaged
					public static void vertical( NAME_IN input, NAME_OUT output, int offset, int length ) {
						output.reshape(input);

						if (BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length))
							return;

						vertical(input, output, offset, length, null, false);
					}

					/**
					 * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.
					 *
					 * @param input The input image. Not modified.
					 * @param output Where the resulting image is written to. Modified.
					 * @param offset Start offset from pixel coordinate
					 * @param length How long the mean filter is
					 * @param border Pixels outside the image. If null, the image border isn't processed.
					 */
					public static void horizontal( NAME_IN input, NAME_OUT output, int offset, int length,
												   @Nullable BORDER_TYPE border ) {
						output.reshape(input);
						horizontal(input, output, offset, length, border, border == null);
					}

					/**
					 * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.
					 *
					 * @param input The input image. Not modified.
					 * @param output Where the resulting image is written to. Modified.
					 * @param offset Start offset from pixel coordinate
					 * @param length How long the mean filter is
					 * @param border Pixels outside the image. If null, the image border isn't processed.
					 */
					public static void vertical( NAME_IN input, NAME_OUT output, int offset, int length,
												 @Nullable BORDER_TYPE border ) {
						output.reshape(input);
						vertical(input, output, offset, length, border, border == null);
					}

					/// Picks single or multi threaded. If skipBorder only the inner image is processed
					private static void horizontal( NAME_IN input, NAME_OUT output, int offset, int length,
													@Nullable BORDER_TYPE border, boolean skipBorder ) {
						if (border != null)
							border.setImage(input);

						boolean concurrent = isConcurrent(input, CONCURRENT_MIN_PIXELS_HORIZONTAL);
						if (skipBorder) {
							if (concurrent)
								ImplConvolveMean_MT.horizontalInner(input, output, offset, length);
							else
								ImplConvolveMean.horizontalInner(input, output, offset, length);
						} else if (concurrent) {
							ImplConvolveMean_MT.horizontal(input, output, offset, length, border);
						} else {
							ImplConvolveMean.horizontal(input, output, offset, length, border);
						}
					}

					/// Picks single or multi threaded. If skipBorder only the inner image is processed
					private static void vertical( NAME_IN input, NAME_OUT output, int offset, int length,
												  @Nullable BORDER_TYPE border, boolean skipBorder ) {
						if (border != null)
							border.setImage(input);

						GrowArray<DogArray<ROW_ARRAY>> workspaces = BoofConcurrency.takeRowBlocks_ROW_SUFFIX();
						try {
							boolean concurrent = isConcurrent(input, CONCURRENT_MIN_PIXELS_VERTICAL);
							if (skipBorder) {
								if (concurrent) {
									ImplConvolveMean_MT.verticalInner(input, output, offset, length, workspaces);
								} else {
									workspaces.reset();
									ImplConvolveMean.verticalInner(input, output, offset, length, workspaces.grow());
								}
							} else if (concurrent) {
								ImplConvolveMean_MT.vertical(input, output, offset, length, border, workspaces);
							} else {
								workspaces.reset();
								ImplConvolveMean.vertical(input, output, offset, length, border, workspaces.grow());
							}
						} finally {
							BoofConcurrency.releaseRowBlocks_ROW_SUFFIX();
						}
					}

				""".replace("ROW_SUFFIX", impl.rowArray().replace("DogArray_", ""))));
	}

	private void printPreamble() {
		out.print("""
				import boofcv.alg.filter.convolve.noborder.ImplConvolveMean;
				import boofcv.alg.filter.convolve.noborder.ImplConvolveMean_MT;
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
				 * <p>Convolves a mean filter across the image. The mean value of all the pixels are computed inside the kernel.</p>
				""" + generateDocString("Peter Abeles") + "public class " + className + " {\n" + """
					/// Images with fewer pixels than this are processed single threaded by vertical(), since dispatching to
					/// threads costs more
					public static int CONCURRENT_MIN_PIXELS_VERTICAL = 500*500;

					/// Same as CONCURRENT_MIN_PIXELS_VERTICAL for horizontal()
					public static int CONCURRENT_MIN_PIXELS_HORIZONTAL = 400*400;

				""");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateConvolveImageMean gen = new GenerateConvolveImageMean();
		gen.setModuleName("boofcv-ip");
		gen.generate();
	}
}
