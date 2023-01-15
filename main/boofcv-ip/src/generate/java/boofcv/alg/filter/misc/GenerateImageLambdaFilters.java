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

package boofcv.alg.filter.misc;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

public class GenerateImageLambdaFilters extends CodeGeneratorBase {
	@Override protected void generateCode() throws FileNotFoundException {
		printPreamble();

		for (AutoTypeImage type : AutoTypeImage.getGenericTypes()) {
			printInner(type);
			out.println("\t//CONCURRENT_OMIT_BEGIN");
			printBorder(type);
			out.println("\t//CONCURRENT_OMIT_END");
		}
		out.println("\t//CONCURRENT_OMIT_BEGIN");
		printLambdaFunctions();
		out.println("\t//CONCURRENT_OMIT_END");

		out.print("}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"//CONCURRENT_INLINE import boofcv.alg.filter.misc.ImageLambdaFilters.*;\n" +
				"\n" +
				"/**\n" +
				" * Image filters which have been abstracted using lambdas. In most situations the 'src' image is assumed to be\n" +
				" * passed in directory to the lambda, along with any other input parameters. What's given to the lambda\n" +
				" * are parameters which define the local region. For inner functions, it can be assumed that all pixel values passed\n" +
				" * in have a region contained entirely inside the region.\n" +
				" *\n" +
				" * <ol>\n" +
				" *     <li>rectangle-center: filter that's applied to a local rectangular region centered on a pixel</li>\n" +
				" * </ol>\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n\n");
	}

	private void printLambdaFunctions() {
		out.print("\t// indexPixel = index of pixel in the src image. Pixel index is passed in to avoid extra math\n" +
				"\n" +
				"\t// @formatter:off\n" +
				"\tpublic @FunctionalInterface interface RectCenter_S32 { int apply( int indexPixel, Object workspace ); }\n" +
				"\tpublic @FunctionalInterface interface RectCenter_S64 { long apply( int indexPixel, Object workspace ); }\n" +
				"\tpublic @FunctionalInterface interface RectCenter_F32 { float apply( int indexPixel, Object workspace ); }\n" +
				"\tpublic @FunctionalInterface interface RectCenter_F64 { double apply( int indexPixel, Object workspace ); }\n" +
				"\t// @formatter:on\n" +
				"\n" +
				"\t// (cx, cy) = center pixel. (x0,y0) = lower extent. (x1, y1) = upper extent\n" +
				"\n" +
				"\t// @formatter:off\n" +
				"\tpublic @FunctionalInterface interface Rect_S32 { int apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ); }\n" +
				"\tpublic @FunctionalInterface interface Rect_S64 { long apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ); }\n" +
				"\tpublic @FunctionalInterface interface Rect_F32 { float apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ); }\n" +
				"\tpublic @FunctionalInterface interface Rect_F64 { double apply(int cx, int cy,  int x0, int y0, int x1, int y1, Object workspace ); }\n" +
				"\t// @formatter:on\n");
	}

	private void printInner( AutoTypeImage image ) {
		String imageName = image.getSingleBandName();
		String dataType = image.getDataType();
		String filterType = image.getKernelType();

		out.print("\tpublic static void filterRectCenterInner( " + imageName + " src, int radiusX, int radiusY, " + imageName + " dst,\n" +
				"\t\t\t\t\t\t\t\t\t\t\t  @Nullable Object workspace, RectCenter_" + filterType + " filter ) {\n" +
				"\t\tfinal int y0 = radiusY;\n" +
				"\t\tfinal int y1 = src.height - radiusY;\n" +
				"\n" +
				"\t\t// Go through all inner pixels\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint indexSrc = src.startIndex + y*src.stride + radiusX;\n" +
				"\t\t\tint indexDst = dst.startIndex + y*dst.stride + radiusX;\n" +
				"\n" +
				"\t\t\t// index of last pixel along x-axis it should process\n" +
				"\t\t\tint end = src.startIndex + y*src.stride + src.width - radiusX;\n" +
				"\t\t\twhile (indexSrc < end) {\n" +
				"\t\t\t\t// Apply the transform. It's assumed that the src image has been passed into the lambda\n" +
				"\t\t\t\tdst.data[indexDst++] = (" + dataType + ")filter.apply(indexSrc++, workspace);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printBorder( AutoTypeImage image ) {
		String imageName = image.getSingleBandName();
		String typeCast = "(" + image.getDataType() + ")";
		String filterType = image.getKernelType();

		out.print("\tpublic static void filterRectCenterEdge( " + imageName + " src, int radiusX, int radiusY, " + imageName + " dst,\n" +
				"\t\t\t\t\t\t\t\t\t\t\t @Nullable Object workspace, Rect_" + filterType + " filter ) {\n" +
				"\t\t// top edge\n" +
				"\t\tfor (int y = 0; y < radiusY; y++) {\n" +
				"\t\t\tint y0 = 0;\n" +
				"\t\t\tint y1 = Math.min(src.height, y + radiusY + 1);\n" +
				"\n" +
				"\t\t\tint indexDstRow = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < src.width; x++) {\n" +
				"\t\t\t\tint x0 = Math.max(0, x - radiusX);\n" +
				"\t\t\t\tint x1 = Math.min(src.width, x + radiusX + 1);\n" +
				"\t\t\t\tdst.data[indexDstRow + x] = " + typeCast + "filter.apply(x, y, x0, y0, x1, y1, workspace);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// bottom edge\n" +
				"\t\tfor (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {\n" +
				"\t\t\tint y0 = Math.max(0, y - radiusY);\n" +
				"\t\t\tint y1 = src.height;\n" +
				"\n" +
				"\t\t\tint indexDstRow = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < src.width; x++) {\n" +
				"\t\t\t\tint x0 = Math.max(0, x - radiusX);\n" +
				"\t\t\t\tint x1 = Math.min(src.width, x + radiusX + 1);\n" +
				"\t\t\t\tdst.data[indexDstRow + x] = " + typeCast + "filter.apply(x, y, x0, y0, x1, y1, workspace);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {\n" +
				"\t\t\tint y0 = y - radiusY;\n" +
				"\t\t\tint y1 = y + radiusY + 1;\n" +
				"\n" +
				"\t\t\tint indexDstRow = dst.startIndex + y*dst.stride;\n" +
				"\n" +
				"\t\t\t// left side\n" +
				"\t\t\tfor (int x = 0; x < radiusX; x++) {\n" +
				"\t\t\t\tint x1 = Math.min(src.width, x + radiusX + 1);\n" +
				"\t\t\t\tdst.data[indexDstRow + x] = " + typeCast + "filter.apply(x, y, 0, y0, x1, y1, workspace);\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// right side\n" +
				"\t\t\tfor (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {\n" +
				"\t\t\t\tint x0 = Math.max(0, x - radiusX);\n" +
				"\t\t\t\tint x1 = Math.min(src.width, x + radiusX + 1);\n" +
				"\t\t\t\tdst.data[indexDstRow + x] = " + typeCast + "filter.apply(x, y, x0, y0, x1, y1, workspace);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var app = new GenerateImageLambdaFilters();
		app.setModuleName("boofcv-ip");
		app.generate();
	}
}
