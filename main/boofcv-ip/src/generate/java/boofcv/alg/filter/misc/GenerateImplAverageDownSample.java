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

public class GenerateImplAverageDownSample extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		horizontal(AutoTypeImage.U8, AutoTypeImage.F32);
		vertical(AutoTypeImage.F32, AutoTypeImage.I8);

		horizontal(AutoTypeImage.U16, AutoTypeImage.F32);
		vertical(AutoTypeImage.F32, AutoTypeImage.I16);

		horizontal(AutoTypeImage.F32, AutoTypeImage.F32);
		vertical(AutoTypeImage.F32, AutoTypeImage.F32);

		horizontal(AutoTypeImage.F64,AutoTypeImage.F64);
		vertical(AutoTypeImage.F64,AutoTypeImage.F64);

		out.print("}\n");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p> * Overlays a rectangular grid on top of the src image and computes the average value within each cell\n" +
				" * which is then written into the dst image. The dst image must be smaller than or equal to the src image.</p>\n" +
				" *\n" +
				" * <p>\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n");
	}

	private void horizontal( AutoTypeImage input , AutoTypeImage output ) {

		String inputSumType = input.getSumType();
		String bitwise = input.getBitWise();

		out.print("\t/**\n" +
				"\t * Down samples the image along the x-axis only. Image height's must be the same.\n" +
				"\t * @param src Input image. Not modified.\n" +
				"\t * @param dst Output image. Modified.\n" +
				"\t */\n" +
				"\tpublic static void horizontal( "+input.getSingleBandName()+" src , "+output.getSingleBandName()+" dst ) {\n" +
				"\n" +
				"\t\tif( src.width < dst.width )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src width must be >= dst width\");\n" +
				"\t\tif( src.height != dst.height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src height must equal dst height\");\n" +
				"\n" +
				"\t\tfloat scale = src.width/(float)dst.width;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {\n" +
				"\t\tfor (int y = 0; y < dst.height; y++) {\n" +
				"\t\t\tint indexDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < dst.width-1; x++, indexDst++ ) {\n" +
				"\t\t\t\tfloat srcX0 = x*scale;\n" +
				"\t\t\t\tfloat srcX1 = (x+1)*scale;\n" +
				"\n" +
				"\t\t\t\tint isrcX0 = (int)srcX0;\n" +
				"\t\t\t\tint isrcX1 = (int)srcX1;\n" +
				"\n" +
				"\t\t\t\tint index = src.getIndex(isrcX0,y);\n" +
				"\n" +
				"\t\t\t\t// compute value of overlapped region\n" +
				"\t\t\t\tfloat startWeight = (1.0f-(srcX0-isrcX0));\n" +
				"\t\t\t\t"+inputSumType+" start = src.data[index++]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\t"+inputSumType+" middle = 0;\n" +
				"\t\t\t\tfor( int i = isrcX0+1; i < isrcX1; i++ ) {\n" +
				"\t\t\t\t\tmiddle += src.data[index++]"+bitwise+";\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tfloat endWeight = (srcX1%1);\n" +
				"\t\t\t\t"+inputSumType+" end = src.data[index]"+bitwise+";\n" +
				"\t\t\t\tdst.data[indexDst] = (start*startWeight + middle + end*endWeight)/scale;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// handle the last area as a special case\n" +
				"\t\t\tint x = dst.width-1;\n" +
				"\t\t\tfloat srcX0 = x*scale;\n" +
				"\n" +
				"\t\t\tint isrcX0 = (int)srcX0;\n" +
				"\t\t\tint isrcX1 = src.width-1;\n" +
				"\n" +
				"\t\t\tint index = src.getIndex(isrcX0,y);\n" +
				"\n" +
				"\t\t\t// compute value of overlapped region\n" +
				"\t\t\tfloat startWeight = (1.0f-(srcX0-isrcX0));\n" +
				"\t\t\t"+inputSumType+" start = src.data[index++]"+bitwise+";\n" +
				"\n" +
				"\t\t\t"+inputSumType+" middle = 0;\n" +
				"\t\t\tfor( int i = isrcX0+1; i < isrcX1; i++ ) {\n" +
				"\t\t\t\tmiddle += src.data[index++]"+bitwise+";\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t"+inputSumType+" end = isrcX1 != isrcX0 ? src.data[index]"+bitwise+" : 0;\n" +
				"\t\t\tdst.data[indexDst] = (start*startWeight + middle + end)/scale;\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void vertical( AutoTypeImage input , AutoTypeImage output ) {

		String inputSumType = input.getSumType();
		String outputTypecast = "("+output.getDataType()+")";
		String round = output.isInteger() ? "+ 0.5f" : "";

		out.print("\t/**\n" +
				"\t * Down samples the image along the y-axis only. Image width's must be the same.\n" +
				"\t * @param src Input image. Not modified.\n" +
				"\t * @param dst Output image. Modified.\n" +
				"\t */\n" +
				"\tpublic static void vertical( "+input.getSingleBandName()+" src , "+output.getSingleBandName()+" dst ) {\n" +
				"\n" +
				"\t\tif( src.height < dst.height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src height must be >= dst height\");\n" +
				"\t\tif( src.width != dst.width )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"src width must equal dst width\");\n" +
				"\n" +
				"\t\tfloat scale = src.height/(float)dst.height;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.width, x -> {\n" +
				"\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\tint indexDst = dst.startIndex + x;\n" +
				"\t\t\tfor (int y = 0; y < dst.height-1; y++) {\n" +
				"\n" +
				"\t\t\t\tfloat srcY0 = y*scale;\n" +
				"\t\t\t\tfloat srcY1 = (y+1)*scale;\n" +
				"\n" +
				"\t\t\t\tint isrcY0 = (int)srcY0;\n" +
				"\t\t\t\tint isrcY1 = (int)srcY1;\n" +
				"\n" +
				"\t\t\t\tint index = src.getIndex(x,isrcY0);\n" +
				"\n" +
				"\t\t\t\t// compute value of overlapped region\n" +
				"\t\t\t\tfloat startWeight = (1.0f-(srcY0-isrcY0));\n" +
				"\t\t\t\t"+inputSumType+" start = src.data[index];\n" +
				"\t\t\t\tindex += src.stride;\n" +
				"\n" +
				"\t\t\t\t"+inputSumType+" middle = 0;\n" +
				"\t\t\t\tfor( int i = isrcY0+1; i < isrcY1; i++ ) {\n" +
				"\t\t\t\t\tmiddle += src.data[index];\n" +
				"\t\t\t\t\tindex += src.stride;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tfloat endWeight = (srcY1%1);\n" +
				"\t\t\t\t"+inputSumType+" end = src.data[index];\n" +
				"\t\t\t\tdst.data[indexDst] = "+outputTypecast+"((start*startWeight + middle + end*endWeight)/scale "+round+");\n" +
				"\t\t\t\tindexDst += dst.stride;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// handle the last area as a special case\n" +
				"\t\t\tint y = dst.height-1;\n" +
				"\t\t\tfloat srcY0 = y*scale;\n" +
				"\n" +
				"\t\t\tint isrcY0 = (int)srcY0;\n" +
				"\t\t\tint isrcY1 = src.height-1;\n" +
				"\n" +
				"\t\t\tint index = src.getIndex(x,isrcY0);\n" +
				"\n" +
				"\t\t\t// compute value of overlapped region\n" +
				"\t\t\tfloat startWeight = (1.0f-(srcY0-isrcY0));\n" +
				"\t\t\t"+inputSumType+" start = src.data[index];\n" +
				"\t\t\tindex += src.stride;\n" +
				"\n" +
				"\t\t\t"+inputSumType+" middle = 0;\n" +
				"\t\t\tfor( int i = isrcY0+1; i < isrcY1; i++ ) {\n" +
				"\t\t\t\tmiddle += src.data[index];\n" +
				"\t\t\t\tindex += src.stride;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t"+inputSumType+" end = isrcY1 != isrcY0 ? src.data[index]: 0;\n" +
				"\t\t\tdst.data[indexDst] = "+outputTypecast+"((start*startWeight + middle + end)/scale "+round+");\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");

	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplAverageDownSample app = new GenerateImplAverageDownSample();
		app.generateCode();
	}
}
