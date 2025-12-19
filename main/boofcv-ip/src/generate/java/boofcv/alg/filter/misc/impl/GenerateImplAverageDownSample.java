/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

public class GenerateImplAverageDownSample extends CodeGeneratorBase {
	@Override public void generateCode() throws FileNotFoundException {
		printPreamble();

		naiveHorizontal(AutoTypeImage.U8);
		naiveHorizontal(AutoTypeImage.U16);
		naiveHorizontal(AutoTypeImage.F32);
		naiveHorizontal(AutoTypeImage.F64);
		naivePixelVertical(AutoTypeImage.U8);
		naivePixelVertical(AutoTypeImage.U16);
		naivePixelVertical(AutoTypeImage.F32);
		naivePixelVertical(AutoTypeImage.F64);

		horizontal(AutoTypeImage.U8, AutoTypeImage.F32);
		verticalOutInt(AutoTypeImage.I8);

		horizontal(AutoTypeImage.U16, AutoTypeImage.F32);
		verticalOutInt(AutoTypeImage.I16);

		horizontal(AutoTypeImage.F32, AutoTypeImage.F32);
		verticalFloat(AutoTypeImage.F32);

		horizontal(AutoTypeImage.F64, AutoTypeImage.F64);
		verticalFloat(AutoTypeImage.F64);

		out.print("}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"import org.ddogleg.struct.DogArray_F32;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"import pabeles.concurrency.GrowArray;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p> * Overlays a rectangular grid on top of the src image and computes the average value within each cell\n" +
				" * which is then written into the dst image. The dst image must be smaller than or equal to the src image.</p>\n" +
				" *\n" +
				" * <p>\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n" +
				"\n" +
				"\t/// Computes the border of the upper extent for the output image for regions that are entirely contained inside\n" +
				"\t/// the input image\n" +
				"\tprotected static int upperBorder(int inputLength, int outputLength, float offset, float regionWidth) {\n" +
				"\t\treturn outputLength*regionWidth - offset > inputLength ? outputLength - 1 : outputLength;\n" +
				"\t}\n\n");
	}

	private void naiveHorizontal( AutoTypeImage input ) {
		String outputType = input.getNumBits() < 64 ? "float" : "double";

		out.print("\tpublic static " + outputType + " naivePixelHorizontal( " + input.getSingleBandName() + " input, float offset, float regionWidth, int outX, int outY ) {\n" +
				"\t\t// Convert to input pixel coordinates for the range of values it will sample\n" +
				"\t\tfloat x0 = outX*regionWidth + offset;\n" +
				"\t\tfloat x1 = x0 + regionWidth;\n" +
				"\n" +
				"\t\t// Discrete pixels it will sample\n" +
				"\t\tint idx0 = (int)Math.floor(x0);\n" +
				"\t\tint idx1 = (int)Math.min(input.width, Math.ceil(x1));\n" +
				"\n" +
				"\t\t// Can't sample outside the image\n" +
				"\t\tif (idx0 < 0) {\n" +
				"\t\t\tidx0 = 0;\n" +
				"\t\t\tx0 = 0.0f;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// Sample values and compute overlap\n" +
				"\t\tfloat area = 0.0f;\n" +
				"\t\t" + outputType + " sum = 0;\n" +
				"\t\tfor (int x = idx0; x < idx1; x++) {\n" +
				"\t\t\tfloat intersection = Math.min(x + 1.0f, x1) - x0;\n" +
				"\t\t\tarea += intersection;\n" +
				"\t\t\tsum += intersection*input.get(x, outY);\n" +
				"\t\t\tx0 = x + 1.0f;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn sum/area;\n" +
				"\t}\n\n");
	}

	private void naivePixelVertical( AutoTypeImage input ) {
		String outputType = input.getNumBits() < 64 ? "float" : "double";

		out.print("\tpublic static " + outputType + " naivePixelVertical( " + input.getSingleBandName() + " input, float offset, float regionWidth, int outX, int outY ) {\n" +
				"\t\t// Convert to input pixel coordinates for the range of values it will sample\n" +
				"\t\tfloat y0 = outY*regionWidth + offset;\n" +
				"\t\tfloat y1 = y0 + regionWidth;\n" +
				"\n" +
				"\t\t// Discrete pixels it will sample\n" +
				"\t\tint idx0 = (int)Math.floor(y0);\n" +
				"\t\tint idx1 = (int)Math.min(input.height, Math.ceil(y1));\n" +
				"\n" +
				"\t\t// Can't sample outside the image\n" +
				"\t\tif (idx0 < 0) {\n" +
				"\t\t\tidx0 = 0;\n" +
				"\t\t\ty0 = 0.0f;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// Sample values and compute overlap\n" +
				"\t\tfloat area = 0.0f;\n" +
				"\t\t" + outputType + " sum = 0.0f;\n" +
				"\t\tfor (int y = idx0; y < idx1; y++) {\n" +
				"\t\t\tfloat intersection = Math.min(y + 1.0f, y1) - y0;\n" +
				"\t\t\tarea += intersection;\n" +
				"\t\t\tsum += intersection*input.get(outX, y);\n" +
				"\t\t\ty0 = y + 1.0f;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn sum/area;\n" +
				"\t}\n\n");
	}

	private void horizontal( AutoTypeImage input, AutoTypeImage output ) {
		String outputSumType = output.getSumType();
		String bitwise = input.getBitWise();

		out.print("\t/// Down samples the image along the x-axis only. Image height's must be the same.\n" +
				"\t///\n" +
				"\t/// @param src Input image. Not modified.\n" +
				"\t/// @param centered The kernel will be centered to avoid shifting pixel values.\n" +
				"\t/// @param dst Output image. Modified.\n" +
				"\tpublic static void horizontal( " + input.getSingleBandName() + " src , boolean centered, " + output.getSingleBandName() + " dst ) {\n" +
				"\n" +
				"\t\tif (src.width < dst.width)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src width must be >= dst width\");\n" +
				"\t\tif (src.height != dst.height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src height must equal dst height\");\n" +
				"\n" +
				"\t\tfloat scale = src.width/(float)dst.width;\n" +
				"\t\tfloat offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;\n" +
				"\t\tint dstBorder0 = centered ? 1 : 0;\n" +
				"\t\tint dstBorder1 = upperBorder(src.width, dst.width, offset, scale);\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {\n" +
				"\t\tfor (int y = 0; y < dst.height; y++) {\n" +
				"\t\t\tint indexDst = dst.startIndex + y*dst.stride;\n" +
				"\n" +
				"\t\t\tif (centered) {\n" +
				"\t\t\t\tdst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, 0, y);\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tfor (int x = dstBorder0; x < dstBorder1; x++) {\n" +
				"\t\t\t\tfloat srcX0 = x*scale + offset;\n" +
				"\t\t\t\tfloat srcX1 = (x + 1)*scale + offset;\n" +
				"\n" +
				"\t\t\t\tint isrcX0 = (int)srcX0;\n" +
				"\t\t\t\tint isrcX1 = (int)srcX1;\n" +
				"\n" +
				"\t\t\t\tint indexSrc = src.getIndex(isrcX0, y);\n" +
				"\n" +
				"\t\t\t\t// compute value of overlapped region\n" +
				"\t\t\t\t" + outputSumType + " sum = (isrcX0 + 1 - srcX0)*(src.data[indexSrc++]" + bitwise + ");\n" +
				"\n" +
				"\t\t\t\tfor (int i = isrcX0 + 1; i < isrcX1; i++) {\n" +
				"\t\t\t\t\tsum += src.data[indexSrc++]" + bitwise + ";\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tif (isrcX1 < srcX1) {\n" +
				"\t\t\t\t\tsum += (srcX1 - isrcX1)*(src.data[indexSrc]" + bitwise + ");\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tdst.data[indexDst++] = sum/scale;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tif (dstBorder1 != dst.width) {\n" +
				"\t\t\t\tdst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, dstBorder1, y);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void verticalOutInt( AutoTypeImage output ) {
		String outputTypecast = "(" + output.getDataType() + ")";
		String round = output.isInteger() ? "+ 0.5f" : "";

		out.print("\t/// Same as vertical but workspace is null by default\n" +
				"\tpublic static void vertical( GrayF32 src, boolean centered, " + output.getSingleBandName() + " dst) {\n" +
				"\t\tvertical(src, centered, dst, null);\n" +
				"\t}\n\n");

		out.print("\t/// Down samples the image along the y-axis only. Image width's must be the same.\n" +
				"\t///\n" +
				"\t/// @param src Input image. Not modified.\n" +
				"\t/// @param centered The kernel will be centered to avoid shifting pixel values.\n" +
				"\t/// @param dst Output image. Modified.\n" +
				"\t/// @param workspaces (Optional) Storage for workspace used to store intermediate results\n" +
				"\tpublic static void vertical( GrayF32 src, boolean centered, " + output.getSingleBandName() + " dst, @Nullable GrowArray<DogArray_F32> workspaces ) {\n" +
				"\t\tif (src.height < dst.height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src height must be >= dst height\");\n" +
				"\t\tif (src.width != dst.width)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src width must equal dst width\");\n" +
				"\n" +
				"\t\tfloat scale = src.height/(float)dst.height;\n" +
				"\t\tfloat offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;\n" +
				"\t\tint dstBorder0 = centered ? 1 : 0;\n" +
				"\t\tint dstBorder1 = upperBorder(src.width, dst.width, offset, scale);\n" +
				"\n" +
				"\t\t// If workspace was not provided declare it\n" +
				"\t\tif (workspaces == null) {\n" +
				"\t\t\tworkspaces = new GrowArray<>(DogArray_F32::new);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tworkspaces.reset();\n" +
				"\t\tDogArray_F32 workspace = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tint idx0 = 0, idx1 = dst.height; //CONCURRENT_REMOVE_LINE\n" +
				"\n" +
				"\t\t//CONCURRENT_INLINE BoofConcurrency.loopBlocks(0, dst.height, workspaces, (workspace, idx0, idx1) -> {\n" +
				"\t\tfloat[] workArray = workspace.resize(dst.width).data;\n" +
				"\t\tfor (int y = idx0; y < idx1; y++) {\n" +
				"\t\t\tif (y < dstBorder0) {\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tworkArray[x] = naivePixelVertical(src, offset, scale, x, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else if (y >= dstBorder1) {\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tworkArray[x] = naivePixelVertical(src, offset, scale, x, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\t// process incrementally along the columns to reduce CPU cache misses\n" +
				"\t\t\t\tfloat y0 = y*scale + offset;\n" +
				"\t\t\t\tfloat y1 = (y + 1)*scale + offset;\n" +
				"\n" +
				"\t\t\t\t// Convert to integer values\n" +
				"\t\t\t\tint isrcY0 = (int)y0;\n" +
				"\t\t\t\tint isrcY1 = (int)y1;\n" +
				"\n" +
				"\t\t\t\tint srcIndex = src.getIndex(0, isrcY0);\n" +
				"\t\t\t\tfloat weight0 = 1 - (y0 - isrcY0);\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tworkArray[x] = weight0*src.data[srcIndex++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tisrcY0++;\n" +
				"\n" +
				"\t\t\t\tfor (int innerY = isrcY0; innerY < isrcY1; innerY++) {\n" +
				"\t\t\t\t\tsrcIndex = src.getIndex(0, innerY);\n" +
				"\t\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\t\tworkArray[x] += src.data[srcIndex++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tif (isrcY1 < y1) {\n" +
				"\t\t\t\t\tfloat weight1 = y1%1;\n" +
				"\t\t\t\t\tsrcIndex = src.getIndex(0, isrcY1);\n" +
				"\t\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\t\tworkArray[x] += weight1*src.data[srcIndex++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tworkArray[x] /= scale;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tint dstIndex = dst.getIndex(0, y);\n" +
				"\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\tdst.data[dstIndex++] = " + outputTypecast + "(workArray[x] + " + round + ");\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_INLINE });\n" +
				"\t}\n\n");
	}

	private void verticalFloat( AutoTypeImage image ) {
		out.print("\t/// Down samples the image along the y-axis only. Image width's must be the same.\n" +
				"\t///\n" +
				"\t/// @param src Input image. Not modified.\n" +
				"\t/// @param centered The kernel will be centered to avoid shifting pixel values.\n" +
				"\t/// @param dst Output image. Modified.\n" +
				"\tpublic static void vertical( " + image.getSingleBandName() + " src, boolean centered, " + image.getSingleBandName() + " dst ) {\n" +
				"\t\tif (src.height < dst.height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src height must be >= dst height\");\n" +
				"\t\tif (src.width != dst.width)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src width must equal dst width\");\n" +
				"\n" +
				"\t\tfloat scale = src.height/(float)dst.height;\n" +
				"\t\tfloat offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;\n" +
				"\t\tint dstBorder0 = centered ? 1 : 0;\n" +
				"\t\tint dstBorder1 = upperBorder(src.width, dst.width, offset, scale);\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {\n" +
				"\t\tfor (int y = 0; y < dst.height; y++) {\n" +
				"\t\t\tint dstIndex = dst.getIndex(0, y);\n" +
				"\t\t\tif (y < dstBorder0) {\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tdst.data[dstIndex++] = naivePixelVertical(src, offset, scale, x, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else if (y >= dstBorder1) {\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tdst.data[dstIndex++] = naivePixelVertical(src, offset, scale, x, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\t// process incrementally along the columns to reduce CPU cache misses\n" +
				"\t\t\t\tfloat y0 = y*scale + offset;\n" +
				"\t\t\t\tfloat y1 = (y + 1)*scale + offset;\n" +
				"\n" +
				"\t\t\t\t// Convert to integer values\n" +
				"\t\t\t\tint isrcY0 = (int)y0;\n" +
				"\t\t\t\tint isrcY1 = (int)y1;\n" +
				"\n" +
				"\t\t\t\tint srcIndex = src.getIndex(0, isrcY0);\n" +
				"\t\t\t\tfloat weight0 = 1 - (y0 - isrcY0);\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tdst.data[dstIndex + x] = weight0*src.data[srcIndex++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tisrcY0++;\n" +
				"\n" +
				"\t\t\t\tfor (int innerY = isrcY0; innerY < isrcY1; innerY++) {\n" +
				"\t\t\t\t\tsrcIndex = src.getIndex(0, innerY);\n" +
				"\t\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\t\tdst.data[dstIndex + x] += src.data[srcIndex++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tif (isrcY1 < y1) {\n" +
				"\t\t\t\t\tfloat weight1 = y1%1;\n" +
				"\t\t\t\t\tsrcIndex = src.getIndex(0, isrcY1);\n" +
				"\t\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\t\tdst.data[dstIndex + x] += weight1*src.data[srcIndex++];\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\t\tdst.data[dstIndex + x] /= scale;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var gen = new GenerateImplAverageDownSample();
		gen.setModuleName("boofcv-ip");
		gen.generate();
	}
}
