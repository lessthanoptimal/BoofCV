/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

public class GenerateGeometricMeanFilter extends CodeGeneratorBase {
	@Override protected void generateCode() throws FileNotFoundException {
		printPreamble();

		printFunction(AutoTypeImage.U8);
		printFunction(AutoTypeImage.U16);
		printFunction(AutoTypeImage.F32);
		printFunction(AutoTypeImage.F64);

		out.print("}\n");
	}

	private void printPreamble() {
		out.println("import boofcv.alg.filter.misc.ImageLambdaFilters;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.alg.filter.misc.ImageLambdaFilters_MT;\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Implementation of Geometric Mean filter as describes in [1] with modifications to avoid numerical issues.\n" +
				" *\n" +
				" * <p>\n" +
				" * Compute a scale factor so that the average pixel intensity will be one. As a result, after scaling,\n" +
				" * the product of pixel values will also be close to 1, avoiding over and under flow issues.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>NOTE: This could be made MUCH faster using a similar technique to how the mean filter is done by\n" +
				" * separating x and y axis computations. This has not been done yet since it's not yet a bottleneck\n" +
				" * in any application. Maybe you would want to add it?</p>\n" +
				" *\n" +
				" * <ol>\n" +
				" *      <li> Rafael C. Gonzalez and Richard E. Woods, \"Digital Image Processing\" 4nd Ed. 2018.</li>\n" +
				" * </ol>\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n");
	}

	private void printFunction( AutoTypeImage image ) {
		String imageName = image.getSingleBandName();
		String bitwise = image.getBitWise();
		String round = image.isInteger() ? " + 0.5" : "";
		String sumType = image.getSumType();
		String productType = imageName.equals("GrayF32") ? "float" : "double";
		String one = imageName.equals("GrayF32") ? "1.0f" : "1.0";

		if (bitwise.length() > 1)
			bitwise = " " + bitwise;

		out.println("\t/**\n" +
				"\t * Applies the geometric mean blur operator.\n" +
				"\t *\n" +
				"\t * @param src Input image\n" +
				"\t * @param radiusX Region's radius along x-axis\n" +
				"\t * @param radiusY Region's radius along y-axis\n" +
				"\t * @param mean Mean of input image. Used to scale pixel values so that they average 1.0\n" +
				"\t * @param dst Output image with results\n" +
				"\t */\n" +
				"\tpublic static void filter( " + imageName + " src, int radiusX, int radiusY, " + productType + " mean, " + imageName + " dst ) {\n" +
				"\t\tdst.reshape(src.width, src.height);\n" +
				"\n" +
				"\t\t// Width and height of kernel\n" +
				"\t\tint kx = radiusX*2 + 1;\n" +
				"\t\tint ky = radiusY*2 + 1;\n" +
				"\n" +
				"\t\t// What power the product is multiplied by\n" +
				"\t\t" + productType + " power = " + one + "/(kx*ky);\n" +
				"\n" +
				"\t\t// apply to the inner image\n" +
				"\t\t//CONCURRENT_BELOW ImageLambdaFilters_MT.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {\n" +
				"\t\tImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {\n" +
				"\t\t\tint indexRow = indexCenter - radiusX - src.stride*radiusY;\n" +
				"\n" +
				"\t\t\t" + productType + " product = " + one + ";\n" +
				"\n" +
				"\t\t\tfor (int y = 0; y < ky; y++) {\n" +
				"\t\t\t\tint indexPixel = indexRow + src.stride*y;\n" +
				"\t\t\t\tfor (int x = 0; x < kx; x++) {\n" +
				"\t\t\t\t\tproduct *= (src.data[indexPixel++]" + bitwise + ")/mean;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\treturn (" + sumType + ")(mean*Math.pow(product, power)" + round + ");\n" +
				"\t\t});\n" +
				"\n" +
				"\t\t// Apply to image edge with an adaptive region size\n" +
				"\t\t//CONCURRENT_BELOW ImageLambdaFilters_MT.filterRectCenterEdge(src, radiusX, radiusY, dst, null, ( cx, cy, x0, y0, x1, y1, w ) -> {\n" +
				"\t\tImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, ( cx, cy, x0, y0, x1, y1, w ) -> {\n" +
				"\t\t\t" + productType + " product = " + one + ";\n" +
				"\n" +
				"\t\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\t\tint indexPixel = src.startIndex + y*src.stride + x0;\n" +
				"\t\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\t\tproduct *= (src.data[indexPixel++]" + bitwise + ")/mean;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// + 0.5 so that it rounds to nearest integer\n" +
				"\t\t\treturn (" + sumType + ")(mean*Math.pow(product, " + one + "/((x1 - x0)*(y1 - y0)))" + round + ");\n" +
				"\t\t});\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var app = new GenerateGeometricMeanFilter();
		app.setModuleName("boofcv-ip");
		app.generate();
	}
}
