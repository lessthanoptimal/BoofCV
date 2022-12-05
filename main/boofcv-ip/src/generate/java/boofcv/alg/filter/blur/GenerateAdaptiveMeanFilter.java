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

package boofcv.alg.filter.blur;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

public class GenerateAdaptiveMeanFilter extends CodeGeneratorBase {
	@Override protected void generateCode() throws FileNotFoundException {
		printPreamble();

		printProcess(AutoTypeImage.U8);
		printProcess(AutoTypeImage.U16);
		printProcess(AutoTypeImage.F32);
		printProcess(AutoTypeImage.F64);

		printComputeFilter("int");
		printComputeFilter("float");
		printComputeFilter("double");

		out.print("}\n");
	}

	private void printPreamble() {
		out.println("import boofcv.alg.filter.misc.ImageLambdaFilters;\n" +
				"import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.image.*;\n" +
				"import lombok.Getter;\n" +
				"import lombok.Setter;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * <p>Given an estimate of image noise sigma, adaptive applies a mean filter dependent on local image statistics in\n" +
				" * order to preserve edges, see [1]. This implementation uses multiple images to store intermediate results\n" +
				" * to fully utilize efficient implementations of mean filters.</p>\n" +
				" *\n" +
				" * <pre>f(x,y) = g(x,y) - (noise)/(local noise)*[ g(x,y) - mean(x,y) ]</pre>\n" +
				" *\n" +
				" * Where noise is the estimated variance of pixel \"noise\", \"local noise\" is the local variance inside the region, mean\n" +
				" * is the mean of the local region at (x,y). If the ratio is more than one it is set to one.\n" +
				" *\n" +
				" * <ol>\n" +
				" *      <li> Rafael C. Gonzalez and Richard E. Woods, \"Digital Image Processing\" 4nd Ed. 2018.</li>\n" +
				" * </ol>\n" +
				generateDocString("Peter Abeles") +
				"public class AdaptiveMeanFilter {\n" +
				"\t// Note: This could be made to run WAY faster by using a histogram,\n" +
				"\t//       then modifying it while sliding it across the image\n" +
				"\t// Note: Add concurrent implementation\n" +
				"\n" +
				"\t/** Defines the symmetric rectangular region. width = 2*radius + 1 */\n" +
				"\t@Getter @Setter int radiusX, radiusY;\n" +
				"\n" +
				"\t/** Defines the expected additive Gaussian pixel noise that the input image is corrupted by */\n" +
				"\t@Getter double noiseVariance;\n" +
				"\n" +
				"\tpublic AdaptiveMeanFilter( int radiusX, int radiusY ) {\n" +
				"\t\tthis.radiusX = radiusX;\n" +
				"\t\tthis.radiusY = radiusY;\n" +
				"\t}\n" +
				"\n" +
				"\tpublic AdaptiveMeanFilter() {}\n");
	}

	private void printProcess( AutoTypeImage image ) {
		String imageName = image.getSingleBandName();
		String sumType = image.getSumType();
		String bitwise = image.getBitWise();
		String noiseTypeCast = sumType.equals("float") ? "(float)" : "";

		out.print("\t/**\n" +
				"\t * Applies the filter to the src image and saves the results to the dst image. The shape of dst is modified\n" +
				"\t * to match src.\n" +
				"\t *\n" +
				"\t * @param src (Input) Image. Not modified.\n" +
				"\t * @param dst (Output) Image. Modified.\n" +
				"\t */\n" +
				"\tpublic void process( " + imageName + " src, " + imageName + " dst ) {\n" +
				"\t\tBoofMiscOps.checkTrue(radiusX >= 0, \"Radius must not be negative\");\n" +
				"\t\tBoofMiscOps.checkTrue(radiusY >= 0, \"Radius must not be negative\");\n" +
				"\t\tdst.reshape(src.width, src.height);\n" +
				"\n" +
				"\t\tint regionX = radiusX*2 + 1;\n" +
				"\t\tint regionY = radiusY*2 + 1;\n" +
				"\t\tvar localValues = new " + sumType + "[regionX*regionY];\n" +
				"\n" +
				"\t\t// Apply filter to inner region\n" +
				"\t\tImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, localValues, ( indexCenter, w ) -> {\n" +
				"\t\t\tvar values = (" + sumType + "[])w;\n" +
				"\n" +
				"\t\t\t// copy values of local region into an array\n" +
				"\t\t\tint valueIndex = 0;\n" +
				"\t\t\tint pixelRowIndex = indexCenter - radiusX - radiusY*src.stride;\n" +
				"\n" +
				"\t\t\tfor (int y = 0; y < regionY; y++) {\n" +
				"\t\t\t\tint pixelIndex = pixelRowIndex + y*src.stride;\n" +
				"\t\t\t\tfor (int x = 0; x < regionX; x++) {\n" +
				"\t\t\t\t\tlocalValues[valueIndex++] = src.data[pixelIndex++]" + bitwise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tfinal int N = localValues.length;\n" +
				"\t\t\treturn computeFilter(" + noiseTypeCast + "noiseVariance, localValues[N/2], values, N);\n" +
				"\t\t});\n" +
				"\n" +
				"\t\t// Apply filter to image border\n" +
				"\t\tImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, localValues, ( cx, cy, x0, y0, x1, y1, w ) -> {\n" +
				"\t\t\tvar values = (" + sumType + "[])w;\n" +
				"\n" +
				"\t\t\tfor (int y = y0, valueIndex = 0; y < y1; y++) {\n" +
				"\t\t\t\tint indexSrc = src.startIndex + y*src.stride + x0;\n" +
				"\t\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\t\tvalues[valueIndex++] = src.data[indexSrc++]" + bitwise + ";\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// Compute index of center pixel using local grid in row-major order\n" +
				"\t\t\tint indexCenter = (cy - y0)*(x1 - x0) + cx - x0;\n" +
				"\n" +
				"\t\t\t// number of elements in local region\n" +
				"\t\t\tfinal int N = (x1 - x0)*(y1 - y0);\n" +
				"\n" +
				"\t\t\treturn computeFilter(" + noiseTypeCast + "noiseVariance, values[indexCenter], values, N);\n" +
				"\t\t});\n" +
				"\t}\n\n");
	}

	private void printComputeFilter( String valueType ) {
		String sumType = valueType.equals("float") ? "float" : "double";
		String round = valueType.equals("int") ? " + 0.5" : "";
		String zero = sumType.equals("float") ? "0.0f" : "0.0";
		String one = sumType.equals("float") ? "1.0f" : "1.0";

		out.print("\t/**\n" +
				"\t * Apply the filter using pixel values copied into the array\n" +
				"\t *\n" +
				"\t * @param noiseVariance Assumed image noise variance\n" +
				"\t * @param centerValue Value of image at center pixel\n" +
				"\t * @param N Length of array\n" +
				"\t */\n" +
				"\tprivate static " + valueType + " computeFilter( " + sumType + " noiseVariance, " + valueType + " centerValue, " + valueType + "[] values, int N ) {\n" +
				"\t\t// Compute local mean and variance statistics\n" +
				"\t\t" + sumType + " localMean = " + zero + ";\n" +
				"\t\tfor (int i = 0; i < N; i++) {\n" +
				"\t\t\tlocalMean += values[i];\n" +
				"\t\t}\n" +
				"\t\tlocalMean /= N;\n" +
				"\n" +
				"\t\t" + sumType + " localVariance = " + zero + ";\n" +
				"\t\tfor (int i = 0; i < N; i++) {\n" +
				"\t\t\tdouble diff = values[i] - localMean;\n" +
				"\t\t\tlocalVariance += diff*diff;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tlocalVariance /= N;\n" +
				"\n" +
				"\t\tif (localVariance == " + zero + ")\n" +
				"\t\t\treturn centerValue;\n" +
				"\n" +
				"\t\t// Apply the formula. 0.5 is to round instead of floor. Works because it's always positive\n" +
				"\t\treturn (" + valueType + ")(centerValue - Math.min(" + one + ", noiseVariance/localVariance)*(centerValue - localMean)" + round + ");\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var app = new GenerateAdaptiveMeanFilter();
		app.setModuleName("boofcv-ip");
		app.generate();
	}
}
