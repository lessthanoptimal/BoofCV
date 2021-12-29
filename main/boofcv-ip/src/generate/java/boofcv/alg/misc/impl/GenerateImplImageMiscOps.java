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

package boofcv.alg.misc.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * Generates functions inside of ImplImageMiscOps.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GenerateImplImageMiscOps extends CodeGeneratorBase {

	private AutoTypeImage imageType;
	private String imageName;
	private String imageNameI;
	private String dataType;
	private String bitWise;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		printAllGeneric();
		printAllSpecific();
		out.println("}");
	}

	private void printPreamble() {
		out.print(
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.misc.BoofLambdas;\n" +
				"import boofcv.struct.image.*;\n" +
				"import boofcv.alg.misc.ImageMiscOps;\n" +
				"import boofcv.struct.border.ImageBorder_F32;\n" +
				"import boofcv.struct.border.ImageBorder_F64;\n" +
				"import boofcv.struct.border.ImageBorder_S32;\n" +
				"import boofcv.struct.border.ImageBorder_S64;\n" +
				"\n" +
				"import java.util.Random;\n" +
				"import java.util.Arrays;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * Implementations of functions for {@link ImageMiscOps}\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n\n");
	}

	private void printAllGeneric() {
		AutoTypeImage[] types = AutoTypeImage.getGenericTypes();

		for (AutoTypeImage t : types) {
			imageType = t;
			imageName = t.getSingleBandName();
			imageNameI = t.getInterleavedName();
			dataType = t.getDataType();
			printCopyBorder();
			printCopy();
			printCopy_Interleaved();
			printFill();
			printFillInterleaved();
			printFillInterleaved_bands();
			printFillBand_Interleaved();
			printInsertBandInterleaved();
			printExtractBandInterleaved();
			printFillBorder();
			printFillBorder2();
			printFillRectangle();
			printFillRectangleInterleaved();
			printFillUniform();
			printFillUniformInterleaved();
			printFillGaussian();
			printFillGaussianInterleaved();
			printFlipVertical();
			printFlipHorizontal();
			printTranspose_two();
			printTranspose_two_interleaved();
			printRotateCW_one();
			printRotateCW_two();
			printRotateCW_two_interleaved();
			printRotateCCW_one();
			printRotateCCW_two();
			printRotateCCW_two_interleaved();
			growBorder();
			printFindValues();
		}
	}

	private void printAllSpecific() {
		AutoTypeImage[] types = AutoTypeImage.getSpecificTypes();

		for (AutoTypeImage t : types) {
			imageType = t;
			imageName = t.getSingleBandName();
			dataType = t.getDataType();
			bitWise = t.getBitWise();
			if (!bitWise.isEmpty())
				bitWise = " " + bitWise;
			printAddUniformSB();
			printAddUniformIL();
			printAddGaussianSB();
			printAddGaussianIL();
		}
	}

	private void printCopyBorder() {
		boolean useGenerics = imageType.isInteger() && imageType.getNumBits() < 32;
		String typecast = imageType.getTypeCastFromSum();
		String borderName = "ImageBorder_" + imageType.getKernelType();
		borderName += useGenerics ? "<T>" : "";

		String imageNameSrc = useGenerics ? "T" : imageName;
		String generic = useGenerics ? "< T extends " + imageName + "<T>> " : "";

		out.print("\tpublic static " + generic + "void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,\n" +
				"\t\t\t\t\t\t\t " + imageNameSrc + " input, " + borderName + " border, " + imageName + " output ) {\n" +
				"\t\tif (output.width < dstX + width || output.height < dstY + height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained in the output image. w=\"+output.width+\" < \"+(dstX+width)+\" or y=\"+output.height+\" < \"+(dstY+height));\n" +
				"\n" +
				"\t\t// Check to see if it's entirely contained inside the input image\n" +
				"\t\tif (srcX >= 0 && srcX + width <= input.width && srcY >= 0 && srcY + height <= input.height) {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{\n" +
				"\t\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\t\tint indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;\n" +
				"\t\t\t\tint indexDst = output.startIndex + (dstY + y)*output.stride + dstX;\n" +
				"\n" +
				"\t\t\t\tSystem.arraycopy(input.data, indexSrc, output.data, indexDst, width);\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t} else {\n" +
				"\t\t\t// If any part is outside use the border. This isn't terribly efficient. A better approach is to\n" +
				"\t\t\t// handle all the possible outside regions independently. That code is significantly more complex so I'm\n" +
				"\t\t\t// punting it for a future person to write since this is good enough as it.\n" +
				"\t\t\tborder.setImage(input);\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{\n" +
				"\t\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\t\tint indexDst = output.startIndex + (dstY + y)*output.stride + dstX;\n" +
				"\t\t\t\tfor (int x = 0; x < width; x++) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = " + typecast + "border.get(srcX + x, srcY + y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printCopy() {
		out.print(
				"\tpublic static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,\n" +
				"\t\t\t\t\t\t\t " + imageName + " input, " + imageName + " output ) {\n" +
				"\t\tif (input.width < srcX + width || input.height < srcY + height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained in the input image\");\n" +
				"\t\tif (output.width < dstX + width || output.height < dstY + height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained in the output image\");\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{\n" +
				"\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + (srcY + y)*input.stride + srcX;\n" +
				"\t\t\tint indexDst = output.startIndex + (dstY + y)*output.stride + dstX;\n" +
				"\n" +
				"\t\t\tSystem.arraycopy(input.data, indexSrc, output.data, indexDst, width);\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printCopy_Interleaved() {

		out.print(
				"\tpublic static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,\n" +
				"\t\t\t\t\t\t\t " + imageNameI + " input, " + imageNameI + " output ) {\n" +
				"\t\tif (input.width < srcX + width || input.height < srcY + height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained input image\");\n" +
				"\t\tif (output.width < dstX + width || output.height < dstY + height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained output image\");\n" +
				"\t\tif (output.numBands != input.numBands)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Number of bands must match. \" + input.numBands + \" != \" + output.numBands);\n" +
				"\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y->{\n" +
				"\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + (srcY + y)*input.stride + srcX*numBands;\n" +
				"\t\t\tint indexDst = output.startIndex + (dstY + y)*output.stride + dstX*numBands;\n" +
				"\n" +
				"\t\t\tSystem.arraycopy(input.data, indexSrc, output.data, indexDst, width*numBands);\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFill() {
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\tpublic static void fill( " + imageName + " input, " + imageType.getSumType() + " value ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y*input.getStride();\n" +
				"\t\t\tArrays.fill(input.data, index, index + input.width, " + typeCast + "value);\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFillInterleaved() {
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\tpublic static void fill( " + imageName + " input, " + imageType.getSumType() + " value ) {\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y*input.getStride();\n" +
				"\t\t\tint end = index + input.width*input.numBands;\n" +
				"\t\t\tArrays.fill(input.data, index, end, " + typeCast + "value);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillInterleaved_bands() {
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\tpublic static void fill( " + imageName + " input, " + imageType.getSumType() + "[] values ) {\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tfor (int band = 0; band < numBands; band++) {\n" +
				"\t\t\t\tint index = input.getStartIndex() + y*input.getStride() + band;\n" +
				"\t\t\t\tint end = index + input.width*numBands - band;\n" +
				"\t\t\t\t" + imageType.getSumType() + " value = values[band];\n" +
				"\t\t\t\tfor (; index < end; index += numBands) {\n" +
				"\t\t\t\t\tinput.data[index] = " + typeCast + "value;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFillBand_Interleaved() {
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\tpublic static void fillBand( " + imageName + " input, int band, " + imageType.getSumType() + " value ) {\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y*input.getStride() + band;\n" +
				"\t\t\tint end = index + input.width*numBands - band;\n" +
				"\t\t\tfor (; index < end; index += numBands) {\n" +
				"\t\t\t\tinput.data[index] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printInsertBandInterleaved() {
		String singleName = imageType.getSingleBandName();
		String interleavedName = imageType.getInterleavedName();
		out.print(
				"\tpublic static void insertBand( " + singleName + " input, int band, " + interleavedName + " output ) {\n" +
				"\t\tfinal int numBands = output.numBands;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.getStartIndex() + y*input.getStride();\n" +
				"\t\t\tint indexOut = output.getStartIndex() + y*output.getStride() + band;\n" +
				"\t\t\tint end = indexOut + output.width*numBands - band;\n" +
				"\t\t\tfor (; indexOut < end; indexOut += numBands, indexIn++) {\n" +
				"\t\t\t\toutput.data[indexOut] = input.data[indexIn];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printExtractBandInterleaved() {
		String singleName = imageType.getSingleBandName();
		String interleavedName = imageType.getInterleavedName();
		out.print(
				"\tpublic static void extractBand( " + interleavedName + " input, int band, " + singleName + " output ) {\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.getStartIndex() + y*input.getStride() + band;\n" +
				"\t\t\tint indexOut = output.getStartIndex() + y*output.getStride();\n" +
				"\t\t\tint end = indexOut + output.width;\n" +
				"\t\t\tfor (; indexOut < end; indexIn += numBands, indexOut++) {\n" +
				"\t\t\t\toutput.data[indexOut] = input.data[indexIn];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFillBorder() {
		String typeCast = imageType.getTypeCastFromSum();

		out.print(
				"\tpublic static void fillBorder( " + imageName + " input, " + imageType.getSumType() + " value, int radius ) {\n" +
				"\t\t// top and bottom\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,y->{\n" +
				"\t\tfor (int y = 0; y < radius; y++) {\n" +
				"\t\t\tint indexTop = input.startIndex + y*input.stride;\n" +
				"\t\t\tint indexBottom = input.startIndex + (input.height - y - 1)*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tinput.data[indexTop++] = " + typeCast + "value;\n" +
				"\t\t\t\tinput.data[indexBottom++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\n" +
				"\t\t// left and right\n" +
				"\t\tint h = input.height - radius;\n" +
				"\t\tint indexStart = input.startIndex + radius*input.stride;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,radius,x->{\n" +
				"\t\tfor (int x = 0; x < radius; x++) {\n" +
				"\t\t\tint indexLeft = indexStart + x;\n" +
				"\t\t\tint indexRight = indexStart + input.width - 1 - x;\n" +
				"\t\t\tfor (int y = radius; y < h; y++) {\n" +
				"\t\t\t\tinput.data[indexLeft] = " + typeCast + "value;\n" +
				"\t\t\t\tinput.data[indexRight] = " + typeCast + "value;\n" +
				"\n" +
				"\t\t\t\tindexLeft += input.stride;\n" +
				"\t\t\t\tindexRight += input.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFillBorder2() {
		String typeCast = imageType.getTypeCastFromSum();

		out.print(
				"\tpublic static void fillBorder( " + imageName + " input, " + imageType.getSumType() + " value, int borderX0, int borderY0, int borderX1, int borderY1 ) {\n" +
				"\t\t// top and bottom\n" +
				"\t\tfor (int y = 0; y < borderY0; y++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tinput.data[srcIdx++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tfor (int y = input.height - borderY1; y < input.height; y++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tinput.data[srcIdx++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// left and right\n" +
				"\t\tint h = input.height - borderY1;\n" +
				"\t\tfor (int x = 0; x < borderX0; x++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + borderY0*input.stride + x;\n" +
				"\t\t\tfor (int y = borderY0; y < h; y++) {\n" +
				"\t\t\t\tinput.data[srcIdx] = " + typeCast + "value;\n" +
				"\t\t\t\tsrcIdx += input.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tfor (int x = input.width - borderX1; x < input.width; x++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + borderY0*input.stride + x;\n" +
				"\t\t\tfor (int y = borderY0; y < h; y++) {\n" +
				"\t\t\t\tinput.data[srcIdx] = " + typeCast + "value;\n" +
				"\t\t\t\tsrcIdx += input.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillRectangle() {
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\tpublic static void fillRectangle( " + imageName + " image, " + imageType.getSumType() + " value, int x0, int y0, int width, int height ) {\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\t\tint y1 = y0 + height;\n" +
				"\n" +
				"\t\tif (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;\n" +
				"\t\tif (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;\n" +
				"\t\tfinal int _x0 = x0;\n" +
				"\t\tfinal int _x1 = x1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y->{\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint index = image.startIndex + y*image.stride + _x0;\n" +
				"\t\t\tArrays.fill(image.data, index, index + _x1 - _x0, " + typeCast + "value);\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFillRectangleInterleaved() {
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();

		out.print(
				"\tpublic static void fillRectangle(" + imageName + " image, " + imageType.getSumType() + " value, int x0, int y0, int width, int height ) {\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\t\tint y1 = y0 + height;\n" +
				"\n" +
				"\t\tif (x0 < 0) x0 = 0; if (x1 > image.width) x1 = image.width;\n" +
				"\t\tif (y0 < 0) y0 = 0; if (y1 > image.height) y1 = image.height;\n" +
				"\t\tfinal int _x0 = x0;\n" +
				"\n" +
				"\t\tint length = (x1 - x0)*image.numBands;\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1, y->{\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint index = image.startIndex + y*image.stride + _x0*image.numBands;\n" +
				"\t\t\tArrays.fill(image.data, index, index + length, " + typeCast + "value);\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFillUniform() {

		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
//		String maxInclusive = imageType.isInteger() ? "exclusive" : "inclusive";

		out.print(
				"\tpublic static void fillUniform( " + imageName + " image, Random rand, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\t" + sumType + " range = max - min;\n" +
				"\n" +
				"\t\t" + dataType + "[] data = image.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tfor (int x = 0; x < image.width; x++) {\n");
		if (imageType.isInteger()) {
			if (imageType.getNumBits() < 32) {
				out.print("\t\t\t\tdata[index++] = " + typeCast + "(rand.nextInt(range) + min);\n");
			} else if (imageType.getNumBits() < 64) {
				out.print("\t\t\t\tdata[index++] = rand.nextInt((int)range) + min;\n");
			} else {
				// 0.9999 is to make sure max is exclusive and not inclusive
				out.print("\t\t\t\tdata[index++] = (long)(rand.nextDouble()*0.9999*range) + min;\n");
			}
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\tdata[index++] = rand.next" + randType + "()*range + min;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillUniformInterleaved() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
//		String maxInclusive = imageType.isInteger() ? "exclusive" : "inclusive";

		out.print(
				"\tpublic static void fillUniform( " + imageName + " image, Random rand, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\t" + sumType + " range = max - min;\n" +
				"\n" +
				"\t\t" + dataType + "[] data = image.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tint end = index + image.width*image.numBands;\n" +
				"\t\t\tfor (; index < end; index++) {\n");
		if (imageType.isInteger()) {
			if (imageType.getNumBits() < 32) {
				out.print("\t\t\t\tdata[index] = " + typeCast + "(rand.nextInt(range) + min);\n");
			} else if (imageType.getNumBits() < 64) {
				out.print("\t\t\t\tdata[index] = rand.nextInt((int)range) + min;\n");
			} else {
				out.print("\t\t\t\tdata[index] = (long)(rand.nextDouble()*0.9999*range) + min;\n");
			}
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\tdata[index] = rand.next" + randType + "()*range + min;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillGaussian() {

		String sumType = imageType.getSumType();
		String castToSum = sumType.compareTo("double") == 0 ? "" : "(" + sumType + ")";
		String typeCast = imageType.getTypeCastFromSum();

		out.print( "\tpublic static void fillGaussian( " + imageName + " image, Random rand, double mean, double sigma, "
					+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
					"\t\t" + dataType + "[] data = image.data;\n" +
					"\n" +
					"\t\tfor (int y = 0; y < image.height; y++) {\n" +
					"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
					"\t\t\tfor (int x = 0; x < image.width; x++) {\n" +
					"\t\t\t\t" + sumType + " value = " + castToSum + "(rand.nextGaussian()*sigma + mean);\n" +
					"\t\t\t\tif (value < lowerBound) value = lowerBound;\n" +
					"\t\t\t\tif (value > upperBound) value = upperBound;\n" +
					"\t\t\t\tdata[index++] = " + typeCast + "value;\n" +
					"\t\t\t}\n" +
					"\t\t}\n" +
					"\t}\n\n");
	}

	private void printFillGaussianInterleaved() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String castToSum = sumType.compareTo("double") == 0 ? "" : "(" + sumType + ")";
		String typeCast = imageType.getTypeCastFromSum();

		out.print(
				"\tpublic static void fillGaussian( " + imageName + " image, Random rand, double mean, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\t\t" + dataType + "[] data = image.data;\n" +
				"\t\tint length = image.width*image.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tint indexEnd = index + length;\n" +
				"\n" +
				"\t\t\twhile (index < indexEnd) {\n" +
				"\t\t\t\t" + sumType + " value = " + castToSum + "(rand.nextGaussian()*sigma + mean);\n" +
				"\t\t\t\tif (value < lowerBound) value = lowerBound;\n" +
				"\t\t\t\tif (value > upperBound) value = upperBound;\n" +
				"\t\t\t\tdata[index++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddUniformSB() {

		String sumType = imageType.getSumType();
		int min = imageType.getMin().intValue();
		int max = imageType.getMax().intValue();
		String typeCast = imageType.getTypeCastFromSum();

		out.print(
				"\tpublic static void addUniform( " + imageName + " image, Random rand, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\t" + sumType + " range = max - min;\n" +
				"\n" +
				"\t\t" + dataType + "[] data = image.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tfor (int x = 0; x < image.width; x++) {\n");
		if (imageType.isInteger() && imageType.getNumBits() != 64) {
			out.print("\t\t\t\t" + sumType + " value = (data[index]" + bitWise + ") + rand.nextInt(range) + min;\n");
			if (imageType.getNumBits() < 32) {
				out.print("\t\t\t\tif (value < " + min + ") value = " + min + ";\n" +
						"\t\t\t\tif (value > " + max + ") value = " + max + ";\n" +
						"\n");
			}
		} else if (imageType.isInteger()) {
			out.print("\t\t\t\t" + sumType + " value = data[index] + rand.nextInt((int)range) + min;\n");
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\t" + sumType + " value = data[index] + rand.next" + randType + "()*range + min;\n");
		}
		out.print("\t\t\t\tdata[index++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddUniformIL() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		int min = imageType.getMin().intValue();
		int max = imageType.getMax().intValue();
		String typeCast = imageType.getTypeCastFromSum();

		out.print(
				"\tpublic static void addUniform( " + imageName + " image, Random rand, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\t" + sumType + " range = max - min;\n" +
				"\n" +
				"\t\t" + dataType + "[] data = image.data;\n" +
				"\t\tint length = image.width*image.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\n" +
				"\t\t\tint indexEnd = index + length;\n" +
				"\t\t\twhile (index < indexEnd) {\n");
		if (imageType.isInteger() && imageType.getNumBits() != 64) {
			out.print("\t\t\t\t" + sumType + " value = (data[index]" + bitWise + ") + rand.nextInt(range) + min;\n");
			if (imageType.getNumBits() < 32) {
				out.print("\t\t\t\tif (value < " + min + ") value = " + min + ";\n" +
						"\t\t\t\tif (value > " + max + ") value = " + max + ";\n" +
						"\n");
			}
		} else if (imageType.isInteger()) {
			out.print("\t\t\t\t" + sumType + " value = data[index] + rand.nextInt((int)range) + min;\n");
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\t" + sumType + " value = data[index] + rand.next" + randType + "()*range + min;\n");
		}
		out.print("\t\t\t\tdata[index++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddGaussianSB() {
		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
		String sumCast = sumType.equals("double") ? "" : "(" + sumType + ")";

		out.print(
				"\tpublic static void addGaussian( " + imageName + " image, Random rand, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tfor (int x = 0; x < image.width; x++) {\n" +
				"\t\t\t\t" + sumType + " value = (image.data[index]" + bitWise + ") + " + sumCast + "(rand.nextGaussian()*sigma);\n" +
				"\t\t\t\tif (value < lowerBound) value = lowerBound;\n" +
				"\t\t\t\tif (value > upperBound) value = upperBound;\n" +
				"\t\t\t\timage.data[index++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddGaussianIL() {
		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
		String sumCast = sumType.equals("double") ? "" : "(" + sumType + ")";

		out.print(
				"\tpublic static void addGaussian( " + imageName + " image, Random rand, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\t\tint length = image.width*image.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tint indexEnd = index + length;\n" +
				"\t\t\twhile (index < indexEnd) {\n" +
				"\t\t\t\t" + sumType + " value = (image.data[index]" + bitWise + ") + " + sumCast + "(rand.nextGaussian()*sigma);\n" +
				"\t\t\t\tif (value < lowerBound) value = lowerBound;\n" +
				"\t\t\t\tif (value > upperBound) value = upperBound;\n" +
				"\t\t\t\timage.data[index++] = " + typeCast + "value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFlipVertical() {
		String sumType = imageType.getSumType();

		out.print(
				"\tpublic static void flipVertical( " + imageName + " image ) {\n" +
				"\t\tint h2 = image.height/2;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h2, y->{\n" +
				"\t\tfor (int y = 0; y < h2; y++) {\n" +
				"\t\t\tint index1 = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tint index2 = image.getStartIndex() + (image.height - y - 1)*image.getStride();\n" +
				"\n" +
				"\t\t\tint end = index1 + image.width;\n" +
				"\n" +
				"\t\t\twhile (index1 < end) {\n" +
				"\t\t\t\t" + sumType + " tmp = image.data[index1];\n" +
				"\t\t\t\timage.data[index1++] = image.data[index2];\n" +
				"\t\t\t\timage.data[index2++] = (" + dataType + ")tmp;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printFlipHorizontal() {
		String sumType = imageType.getSumType();

		out.print(
				"\tpublic static void flipHorizontal( " + imageName + " image ) {\n" +
				"\t\tint w2 = image.width/2;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y->{\n" +
				"\t\tfor (int y = 0; y < image.height; y++) {\n" +
				"\t\t\tint index1 = image.getStartIndex() + y*image.getStride();\n" +
				"\t\t\tint index2 = index1 + image.width - 1;\n" +
				"\n" +
				"\t\t\tint end = index1 + w2;\n" +
				"\n" +
				"\t\t\twhile (index1 < end) {\n" +
				"\t\t\t\t" + sumType + " tmp = image.data[index1];\n" +
				"\t\t\t\timage.data[index1++] = image.data[index2];\n" +
				"\t\t\t\timage.data[index2--] = (" + dataType + ")tmp;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printRotateCW_one() {
		String sumType = imageType.getSumType();
		out.print(
				"\tpublic static void rotateCW( " + imageName + " image ) {\n" +
				"\t\tif (image.width != image.height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Image must be square\");\n" +
				"\n" +
				"\t\tint w = image.height/2 + image.height%2;\n" +
				"\t\tint h = image.height/2;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{\n" +
				"\t\tfor (int y0 = 0; y0 < h; y0++) {\n" +
				"\t\t\tint y1 = image.height - y0 - 1;\n" +
				"\n" +
				"\t\t\tfor (int x0 = 0; x0 < w; x0++) {\n" +
				"\t\t\t\tint x1 = image.width - x0 - 1;\n" +
				"\n" +
				"\t\t\t\tint index0 = image.startIndex + y0*image.stride + x0;\n" +
				"\t\t\t\tint index1 = image.startIndex + x0*image.stride + y1;\n" +
				"\t\t\t\tint index2 = image.startIndex + y1*image.stride + x1;\n" +
				"\t\t\t\tint index3 = image.startIndex + x1*image.stride + y0;\n" +
				"\n" +
				"\t\t\t\t" + sumType + " tmp3 = image.data[index3];\n" +
				"\n" +
				"\t\t\t\timage.data[index3] = image.data[index2];\n" +
				"\t\t\t\timage.data[index2] = image.data[index1];\n" +
				"\t\t\t\timage.data[index1] = image.data[index0];\n" +
				"\t\t\t\timage.data[index0] = (" + dataType + ")tmp3;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printTranspose_two() {
		out.print(
				"\tpublic static void transpose( " + imageName + " input, " + imageName + " output ) {\n" +
				"\t\toutput.reshape(input.height, input.width);\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\toutput.unsafe_set(y, x, input.data[indexIn++]);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printTranspose_two_interleaved() {
		out.print(
				"\tpublic static void transpose( " + imageNameI + " input, " + imageNameI + " output ) {\n" +
				"\t\toutput.reshape(input.height, input.width, input.numBands);\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint indexDst = output.getIndex(y, x);\n" +
				"\n" +
				"\t\t\t\tint end = indexSrc + input.numBands;\n" +
				"\t\t\t\twhile (indexSrc != end) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printRotateCW_two() {
		out.print(
				"\tpublic static void rotateCW( " + imageName + " input, " + imageName + " output ) {\n" +
				"\t\toutput.reshape(input.height, input.width);\n" +
				"\n" +
				"\t\tint h = input.height - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\toutput.unsafe_set(h - y, x, input.data[indexIn++]);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printRotateCW_two_interleaved() {
		out.print(
				"\tpublic static void rotateCW( " + imageNameI + " input, " + imageNameI + " output ) {\n" +
				"\t\toutput.reshape(input.height, input.width, input.numBands);\n" +
				"\n" +
				"\t\tint h = input.height - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint indexDst = output.getIndex(h - y, x);\n" +
				"\n" +
				"\t\t\t\tint end = indexSrc + input.numBands;\n" +
				"\t\t\t\twhile (indexSrc != end) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_one() {
		String sumType = imageType.getSumType();
		out.print(
				"\tpublic static void rotateCCW( " + imageName + " image ) {\n" +
				"\t\tif (image.width != image.height)\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Image must be square\");\n" +
				"\n" +
				"\t\tint w = image.height/2 + image.height%2;\n" +
				"\t\tint h = image.height/2;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y0->{\n" +
				"\t\tfor (int y0 = 0; y0 < h; y0++) {\n" +
				"\t\t\tint y1 = image.height - y0 - 1;\n" +
				"\n" +
				"\t\t\tfor (int x0 = 0; x0 < w; x0++) {\n" +
				"\t\t\t\tint x1 = image.width - x0 - 1;\n" +
				"\n" +
				"\t\t\t\tint index0 = image.startIndex + y0*image.stride + x0;\n" +
				"\t\t\t\tint index1 = image.startIndex + x0*image.stride + y1;\n" +
				"\t\t\t\tint index2 = image.startIndex + y1*image.stride + x1;\n" +
				"\t\t\t\tint index3 = image.startIndex + x1*image.stride + y0;\n" +
				"\n" +
				"\t\t\t\t" + sumType + " tmp0 = image.data[index0];\n" +
				"\n" +
				"\t\t\t\timage.data[index0] = image.data[index1];\n" +
				"\t\t\t\timage.data[index1] = image.data[index2];\n" +
				"\t\t\t\timage.data[index2] = image.data[index3];\n" +
				"\t\t\t\timage.data[index3] = (" + dataType + ")tmp0;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_two() {
		out.print(
				"\tpublic static void rotateCCW( " + imageName + " input, " + imageName + " output ) {\n" +
				"\t\toutput.reshape(input.height, input.width);\n" +
				"\n" +
				"\t\tint w = input.width - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\toutput.unsafe_set(y, w - x, input.data[indexIn++]);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_two_interleaved() {
		out.print(
				"\tpublic static void rotateCCW( " + imageNameI + " input, " + imageNameI + " output ) {\n" +
				"\t\toutput.reshape(input.height, input.width, input.numBands);\n" +
				"\n" +
				"\t\tint w = input.width - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y->{\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint indexDst = output.getIndex(y, w - x);\n" +
				"\n" +
				"\t\t\t\tint end = indexSrc + input.numBands;\n" +
				"\t\t\t\twhile (indexSrc != end) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void growBorder() {
		String borderName = "ImageBorder_" + imageType.getKernelType();
		String generic = "";
		String typecast;
		String srcType = imageName;
		if (imageType.isInteger() && imageType.getNumBits() < 32) {
			typecast = "(" + dataType + ")";
			generic = "<T extends GrayI" + imageType.getNumBits() + "<T>>\n\t";
			srcType = "T";
			borderName += "<T>";
		} else {
			typecast = "";
		}
		out.print("\tpublic static " + generic + "void growBorder( " + srcType + " src, " + borderName + " border, int borderX0, int borderY0, int borderX1, int borderY1, " + srcType + " dst ) {\n" +
				"\t\tdst.reshape(src.width + borderX0 + borderX1, src.height + borderY0 + borderY1);\n" +
				"\t\tborder.setImage(src);\n" +
				"\n" +
				"\t\t// Copy src into the inner portion of dst\n" +
				"\t\tImageMiscOps.copy(0, 0, borderX0, borderY0, src.width, src.height, src, dst);\n" +
				"\n" +
				"\t\t// Top border\n" +
				"\t\tfor (int y = 0; y < borderY0; y++) {\n" +
				"\t\t\tint idxDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = " + typecast + "border.get(x - borderX0, y - borderY0);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t// Bottom border\n" +
				"\t\tfor (int y = 0; y < borderY1; y++) {\n" +
				"\t\t\tint idxDst = dst.startIndex + (dst.height - borderY1 + y)*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = " + typecast + "border.get(x - borderX0, src.height + y);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t// Left and right border\n" +
				"\t\tfor (int y = borderY0; y < dst.height - borderY1; y++) {\n" +
				"\t\t\tint idxDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < borderX0; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = " + typecast + "border.get(x - borderX0, y - borderY0);\n" +
				"\t\t\t}\n" +
				"\t\t\tidxDst = dst.startIndex + y*dst.stride + src.width + borderX0;\n" +
				"\t\t\tfor (int x = 0; x < borderX1; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = " + typecast + "border.get(src.width + x, y - borderY0);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFindValues() {
		String suffix = imageType.getGenericAbbreviated();

		out.print(
				"\tpublic static void findAndProcess( " + imageType.getSingleBandName() + " input, BoofLambdas.Match_" + suffix +
				" finder, BoofLambdas.ProcessIIB process ) {\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++, index++) {\n" +
				"\t\t\t\tif (finder.process(input.data[index])) {\n" +
				"\t\t\t\t\tif (!process.process(x, y))\n" +
				"\t\t\t\t\t\treturn;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplImageMiscOps gen = new GenerateImplImageMiscOps();
		gen.setModuleName("boofcv-ip");
		gen.generate();
	}
}
