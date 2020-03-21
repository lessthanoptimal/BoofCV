/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * Generates functions inside of ImageMiscOps.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GenerateImageMiscOps extends CodeGeneratorBase {

	private AutoTypeImage imageType;
	private String imageName;
	private String imageNameI;
	private String dataType;
	private String bitWise;

	@Override
	public void generate() throws FileNotFoundException {
		printPreamble();
		printAllGeneric();
		printAllSpecific();
		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import boofcv.alg.misc.impl.ImplImageMiscOps;\n" +
				"import boofcv.struct.border.ImageBorder_F32;\n" +
				"import boofcv.struct.border.ImageBorder_F64;\n" +
				"import boofcv.struct.border.ImageBorder_S32;\n" +
				"import boofcv.struct.border.ImageBorder_S64;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"import java.util.Random;\n" +
				"import java.util.Arrays;\n" +
				"\n" +
				"\n" +
				"/**\n" +
				" * Basic image operations which have no place better to go.\n" +
				" *\n" +
				generateDocString() +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				generatedAnnotation() +
				"public class " + className + " {\n\n");
	}

	private void printAllGeneric() {
		AutoTypeImage[] types = AutoTypeImage.getGenericTypes();

		for( AutoTypeImage t : types ) {
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
			printRotateCW_one();
			printRotateCW_two();
			printRotateCW_two_interleaved();
			printRotateCCW_one();
			printRotateCCW_two();
			printRotateCCW_two_interleaved();
			printGrowBorder();
		}
	}

	private void printAllSpecific() {
		AutoTypeImage[] types = AutoTypeImage.getSpecificTypes();

		for( AutoTypeImage t : types ) {
			imageType = t;
			imageName = t.getSingleBandName();
			dataType = t.getDataType();
			bitWise = t.getBitWise();
			printAddUniformSB();
			printAddUniformIL();
			printAddGaussianSB();
			printAddGaussianIL();
		}
	}

	private void printCopyBorder() {
		boolean useGenerics = imageType.isInteger() && imageType.getNumBits() < 32;
		String typecast = imageType.getTypeCastFromSum();
		String borderName = "ImageBorder_"+imageType.getKernelType();
		borderName += useGenerics ? "<T>" : "";

		String imageNameSrc = useGenerics ? "T" : imageName;
		String generic = useGenerics ? "< T extends "+imageName+"<T>> " : "";

		out.print("\t/**\n" +
				"\t * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>\n" +
				"\t * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]\n" +
				"\t *\n" +
				"\t * @param srcX x-coordinate of corner in input image\n" +
				"\t * @param srcY y-coordinate of corner in input image\n" +
				"\t * @param dstX x-coordinate of corner in output image\n" +
				"\t * @param dstY y-coordinate of corner in output image\n" +
				"\t * @param width Width of region to be copied\n" +
				"\t * @param height Height of region to be copied\n" +
				"\t * @param input Input image\n" +
				"\t * @param border Border for input image   \n" +
				"\t * @param output output image\n" +
				"\t */\n" +
				"\tpublic static "+generic+"void copy( int srcX , int srcY , int dstX , int dstY , int width , int height ,\n" +
				"\t\t\t\t\t\t\t "+imageNameSrc+" input , "+borderName+" border, "+imageName+" output )\n" +
				"\t{\n" +
				"\t\tif( output.width < dstX+width || output.height < dstY+height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained in the output image. w=\"+output.width+\" < \"+(dstX+width)+\" or y=\"+output.height+\" < \"+(dstY+height));\n" +
				"\t\t\n" +
				"\t\t// Check to see if it's entirely contained inside the input image\n" +
				"\t\tif( srcX >= 0 && srcX+width <= input.width && srcY >= 0 && srcY+height <= input.height ) {\n" +
				"\t\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\t\tint indexSrc = input.startIndex + (srcY + y) * input.stride + srcX;\n" +
				"\t\t\t\tint indexDst = output.startIndex + (dstY + y) * output.stride + dstX;\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < width; x++) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\t// If any part is outside use the border. This isn't terribly efficient. A better approach is to\n" +
				"\t\t\t// handle all the possible outside regions independently. That code is significantly more complex so I'm\n" +
				"\t\t\t// punting it for a future person to write since this is good enough as it.\n" +
				"\t\t\tborder.setImage(input);\n" +
				"\t\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\t\tint indexDst = output.startIndex + (dstY + y) * output.stride + dstX;\n" +
				"\t\t\t\tfor (int x = 0; x < width; x++) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = "+typecast+"border.get(srcX+x,srcY+y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printCopy() {
		out.print("\t/**\n" +
				"\t * Copies a rectangular region from one image into another.<br>\n" +
				"\t * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]\n" +
				"\t *\n" +
				"\t * @param srcX x-coordinate of corner in input image\n" +
				"\t * @param srcY y-coordinate of corner in input image\n" +
				"\t * @param dstX x-coordinate of corner in output image\n" +
				"\t * @param dstY y-coordinate of corner in output image\n" +
				"\t * @param width Width of region to be copied\n" +
				"\t * @param height Height of region to be copied\n" +
				"\t * @param input Input image\n" +
				"\t * @param output output image\n" +
				"\t */\n" +
				"\tpublic static void copy( int srcX , int srcY , int dstX , int dstY , int width , int height ,\n" +
				"\t\t\t\t\t\t\t "+imageName+" input , "+imageName+" output ) {\n" +
				"\n" +
				"\t\tif( input.width < srcX+width || input.height < srcY+height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained in the input image\");\n" +
				"\t\tif( output.width < dstX+width || output.height < dstY+height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained in the output image\");\n" +
				"\n" +
				"\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + (srcY + y) * input.stride + srcX;\n" +
				"\t\t\tint indexDst = output.startIndex + (dstY + y) * output.stride + dstX;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x < width; x++) {\n" +
				"\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printCopy_Interleaved() {

		out.print("\t/**\n" +
				"\t * Copies a rectangular region from one image into another.<br>\n" +
				"\t * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]\n" +
				"\t *\n" +
				"\t * @param srcX x-coordinate of corner in input image\n" +
				"\t * @param srcY y-coordinate of corner in input image\n" +
				"\t * @param dstX x-coordinate of corner in output image\n" +
				"\t * @param dstY y-coordinate of corner in output image\n" +
				"\t * @param width Width of region to be copied\n" +
				"\t * @param height Height of region to be copied\n" +
				"\t * @param input Input image\n" +
				"\t * @param output output image\n" +
				"\t */\n" +
				"\tpublic static void copy( int srcX , int srcY , int dstX , int dstY , int width , int height ,\n" +
				"\t\t\t\t\t\t\t "+imageNameI+" input , "+imageNameI+" output ) {\n" +
				"\n" +
				"\t\tif( input.width < srcX+width || input.height < srcY+height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained input image\");\n" +
				"\t\tif( output.width < dstX+width || output.height < dstY+height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Copy region must be contained output image\");\n" +
				"\t\tif( output.numBands != input.numBands )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Number of bands must match. \"+input.numBands+\" != \"+output.numBands);\n" +
				"\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + (srcY + y) * input.stride + srcX*numBands;\n" +
				"\t\t\tint indexDst = output.startIndex + (dstY + y) * output.stride + dstX*numBands;\n" +
				"\n" +
				"\t\t\tSystem.arraycopy(input.data,indexSrc,output.data,indexDst,width*numBands);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFill()
	{
		String typeCast = imageType.getTypeCastFromSum();
		out.print("\t/**\n" +
				"\t * Fills the whole image with the specified value\n" +
				"\t *\n" +
				"\t * @param input An image.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t */\n" +
				"\tpublic static void fill("+imageName+" input, "+imageType.getSumType()+" value) {\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tArrays.fill(input.data,index,index+input.width, "+typeCast+"value);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillInterleaved()
	{
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();
		out.print("\t/**\n" +
				"\t * Fills the whole image with the specified value\n" +
				"\t *\n" +
				"\t * @param input An image.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t */\n" +
				"\tpublic static void fill("+imageName+" input, "+imageType.getSumType()+" value) {\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint end = index + input.width*input.numBands;\n" +
				"\t\t\tArrays.fill(input.data,index,end, "+typeCast+"value);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillInterleaved_bands()
	{
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\t/**\n" +
				"\t * Fills each band in the image with the specified values\n" +
				"\t *\n" +
				"\t * @param input An image.\n" +
				"\t * @param values Array which contains the values each band is to be filled with.\n" +
				"\t */\n" +
				"\tpublic static void fill("+imageName+" input, "+imageType.getSumType()+"[] values) {\n" +
				"\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tfor( int band = 0; band < numBands; band++ ) {\n" +
				"\t\t\t\tint index = input.getStartIndex() + y * input.getStride() + band;\n" +
				"\t\t\t\tint end = index + input.width*numBands - band;\n" +
				"\t\t\t\t"+imageType.getSumType()+" value = values[band];\n" +
				"\t\t\t\tfor (; index < end; index += numBands ) {\n" +
				"\t\t\t\t\tinput.data[index] = "+typeCast+"value;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillBand_Interleaved()
	{
		String imageName = imageType.getInterleavedName();
		String typeCast = imageType.getTypeCastFromSum();
		out.print(
				"\t/**\n" +
				"\t * Fills one band in the image with the specified value\n" +
				"\t *\n" +
				"\t * @param input An image.\n" +
				"\t * @param band Which band is to be filled with the specified value   \n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t */\n" +
				"\tpublic static void fillBand("+imageName+" input, int band , "+imageType.getSumType()+" value) {\n" +
				"\n" +
				"\t\tfinal int numBands = input.numBands;\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride() + band;\n" +
				"\t\t\tint end = index + input.width*numBands - band;\n" +
				"\t\t\tfor (; index < end; index += numBands ) {\n" +
				"\t\t\t\tinput.data[index] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printInsertBandInterleaved()
	{
		String singleName = imageType.getSingleBandName();
		String interleavedName = imageType.getInterleavedName();
		out.print(
				"\t/**\n" +
				"\t * Inserts a single band into a multi-band image overwriting the original band\n" +
				"\t *\n" +
				"\t * @param input Single band image\n" +
				"\t * @param band Which band the image is to be inserted into\n" +
				"\t * @param output The multi-band image which the input image is to be inserted into\n" +
				"\t */\n" +
				"\tpublic static void insertBand( "+singleName+" input, int band , "+interleavedName+" output) {\n" +
				"\n" +
				"\t\tfinal int numBands = output.numBands;\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint indexIn = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint indexOut = output.getStartIndex() + y * output.getStride() + band;\n" +
				"\t\t\tint end = indexOut + output.width*numBands - band;\n" +
				"\t\t\tfor (; indexOut < end; indexOut += numBands , indexIn++ ) {\n" +
				"\t\t\t\toutput.data[indexOut] = input.data[indexIn];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printExtractBandInterleaved()
	{
		String singleName = imageType.getSingleBandName();
		String interleavedName = imageType.getInterleavedName();
		out.print(
				"\t/**\n" +
						"\t * Extracts a single band from a multi-band image\n" +
						"\t *\n" +
						"\t * @param input Multi-band image\n" +
						"\t * @param band which bad is to be extracted   \n" +
						"\t * @param output The single band image\n" +
						"\t */\n" +
						"\tpublic static void extractBand( "+interleavedName+" input, int band , "+singleName+" output) {\n" +
						"\n" +
						"\t\tfinal int numBands = input.numBands;\n" +
						"\t\tfor (int y = 0; y < input.height; y++) {\n" +
						"\t\t\tint indexIn = input.getStartIndex() + y * input.getStride() + band;\n" +
						"\t\t\tint indexOut = output.getStartIndex() + y * output.getStride();\n" +
						"\t\t\tint end = indexOut + output.width;\n" +
						"\t\t\tfor (; indexOut < end; indexIn += numBands , indexOut++ ) {\n" +
						"\t\t\t\toutput.data[indexOut] = input.data[indexIn];\n" +
						"\t\t\t}\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printFillBorder()
	{
		String typeCast = imageType.getTypeCastFromSum();

		out.print("\t/**\n" +
				"\t * Fills the outside border with the specified value\n" +
				"\t *\n" +
				"\t * @param input An image.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t * @param radius Border width.   \n" +
				"\t */\n" +
				"\tpublic static void fillBorder("+imageName+" input, "+imageType.getSumType()+" value, int radius ) {\n" +
				"\n" +
				"\t\t// top and bottom\n" +
				"\t\tfor (int y = 0; y < radius; y++) {\n" +
				"\t\t\tint indexTop = input.startIndex + y * input.stride;\n" +
				"\t\t\tint indexBottom = input.startIndex + (input.height-y-1) * input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tinput.data[indexTop++] = "+typeCast+"value;\n" +
				"\t\t\t\tinput.data[indexBottom++] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// left and right\n" +
				"\t\tint h = input.height-radius;\n" +
				"\t\tint indexStart = input.startIndex + radius*input.stride;\n" +
				"\t\tfor (int x = 0; x < radius; x++) {\n" +
				"\t\t\tint indexLeft = indexStart + x;\n" +
				"\t\t\tint indexRight = indexStart + input.width-1-x;\n" +
				"\t\t\tfor (int y = radius; y < h; y++) {\n" +
				"\t\t\t\tinput.data[indexLeft] = "+typeCast+"value;\n" +
				"\t\t\t\tinput.data[indexRight] = "+typeCast+"value;\n" +
				"\t\t\t\t\n" +
				"\t\t\t\tindexLeft += input.stride;\n" +
				"\t\t\t\tindexRight += input.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}
	private void printFillBorder2()
	{
		String typeCast = imageType.getTypeCastFromSum();

		out.print("\t/**\n" +
				"\t * Fills the border with independent border widths for each side\n" +
				"\t *\n" +
				"\t * @param input An image.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t * @param borderX0 Width of border on left\n" +
				"\t * @param borderY0 Width of border on top   \n" +
				"\t * @param borderX1 Width of border on right\n" +
				"\t * @param borderY1 Width of border on bottom\n" +
				"\t */\n" +
				"\tpublic static void fillBorder("+imageName+" input, "+imageType.getSumType()+" value, int borderX0 , int borderY0 , int borderX1 , int borderY1 ) {\n" +
				"\n" +
				"\t\t// top and bottom\n" +
				"\t\tfor (int y = 0; y < borderY0; y++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + y * input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tinput.data[srcIdx++] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tfor (int y = input.height-borderY1; y < input.height; y++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + y * input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tinput.data[srcIdx++] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// left and right\n" +
				"\t\tint h = input.height-borderY1;\n" +
				"\t\tfor (int x = 0; x < borderX0; x++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + borderY0*input.stride + x;\n" +
				"\t\t\tfor (int y = borderY0; y < h; y++) {\n" +
				"\t\t\t\tinput.data[srcIdx] = "+typeCast+"value;\n" +
				"\t\t\t\tsrcIdx += input.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\tfor (int x = input.width-borderX1; x < input.width; x++) {\n" +
				"\t\t\tint srcIdx = input.startIndex + borderY0*input.stride + x;\n" +
				"\t\t\tfor (int y = borderY0; y < h; y++) {\n" +
				"\t\t\t\tinput.data[srcIdx] = "+typeCast+"value;\n" +
				"\t\t\t\tsrcIdx += input.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}


	private void printFillRectangle()
	{
		out.print("\t/**\n" +
				"\t * Draws a filled rectangle that is aligned along the image axis inside the image.\n" +
				"\t *\n" +
				"\t * @param img Image the rectangle is drawn in.  Modified\n" +
				"\t * @param value Value of the rectangle\n" +
				"\t * @param x0 Top left x-coordinate\n" +
				"\t * @param y0 Top left y-coordinate\n" +
				"\t * @param width Rectangle width\n" +
				"\t * @param height Rectangle height\n" +
				"\t */\n" +
				"\tpublic static void fillRectangle("+imageName+" img, "+imageType.getSumType()+" value, int x0, int y0, int width, int height) {\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\t\tint y1 = y0 + height;\n" +
				"\n" +
				"\t\tif( x0 < 0 ) x0 = 0; if( x1 > img.width ) x1 = img.width;\n" +
				"\t\tif( y0 < 0 ) y0 = 0; if( y1 > img.height ) y1 = img.height;\n" +
				"\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tfor (int x = x0; x < x1; x++) {\n" +
				"\t\t\t\timg.set(x, y, value);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillRectangleInterleaved()
	{
		String imageName = imageType.getInterleavedName();
		String dataType = imageType.getDataType();

		out.print("\t/**\n" +
				"\t * Draws a filled rectangle that is aligned along the image axis inside the image.  All bands\n" +
				"\t * are filled with the same value.\n" +
				"\t *\n" +
				"\t * @param img Image the rectangle is drawn in.  Modified\n" +
				"\t * @param value Value of the rectangle\n" +
				"\t * @param x0 Top left x-coordinate\n" +
				"\t * @param y0 Top left y-coordinate\n" +
				"\t * @param width Rectangle width\n" +
				"\t * @param height Rectangle height\n" +
				"\t */\n" +
				"\tpublic static void fillRectangle("+imageName+" img, "+dataType+" value, int x0, int y0, int width, int height) {\n" +
				"\t\tint x1 = x0 + width;\n" +
				"\t\tint y1 = y0 + height;\n" +
				"\n" +
				"\t\tif( x0 < 0 ) x0 = 0; if( x1 > img.width ) x1 = img.width;\n" +
				"\t\tif( y0 < 0 ) y0 = 0; if( y1 > img.height ) y1 = img.height;\n" +
				"\n" +
				"\t\tint length = (x1-x0)*img.numBands;\n" +
				"\t\tfor (int y = y0; y < y1; y++) {\n" +
				"\t\t\tint index = img.startIndex + y*img.stride + x0*img.numBands;\n" +
				"\t\t\tint indexEnd = index + length;\n" +
				"\t\t\twhile( index < indexEnd ) {\n" +
				"\t\t\t\timg.data[index++] = value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}");
	}

	private void printFillUniform() {

		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
		String maxInclusive = imageType.isInteger() ? "exclusive" : "inclusive";

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.\n" +
				"\t *\n" +
				"\t * @param img Image which is to be filled.  Modified,\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param min Minimum value of the distribution, inclusive\n" +
				"\t * @param max Maximum value of the distribution, "+maxInclusive+"\n" +
				"\t */\n" +
				"\tpublic static void fillUniform("+imageName+" img, Random rand , "+sumType+" min , "+sumType+" max) {\n" +
				"\t\t"+sumType+" range = max-min;\n" +
				"\n" +
				"\t\t"+dataType+"[] data = img.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < img.height; y++) {\n" +
				"\t\t\tint index = img.getStartIndex() + y * img.getStride();\n" +
				"\t\t\tfor (int x = 0; x < img.width; x++) {\n");
		if( imageType.isInteger() ) {
			if( imageType.getNumBits() < 32 ) {
				out.print("\t\t\t\tdata[index++] = "+typeCast+"(rand.nextInt(range)+min);\n");
			} else if( imageType.getNumBits() < 64) {
				out.print("\t\t\t\tdata[index++] = rand.nextInt((int)range)+min;\n");
			} else {
				// 0.9999 is to make sure max is exclusive and not inclusive
				out.print("\t\t\t\tdata[index++] = (long)(rand.nextDouble()*0.9999*range)+min;\n");
			}
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\tdata[index++] = rand.next"+randType+"()*range+min;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillUniformInterleaved() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
		String maxInclusive = imageType.isInteger() ? "exclusive" : "inclusive";

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.\n" +
				"\t *\n" +
				"\t * @param img Image which is to be filled.  Modified,\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param min Minimum value of the distribution, inclusive\n" +
				"\t * @param max Maximum value of the distribution, "+maxInclusive+"\n" +
				"\t */\n" +
				"\tpublic static void fillUniform("+imageName+" img, Random rand , "+sumType+" min , "+sumType+" max) {\n" +
				"\t\t"+sumType+" range = max-min;\n" +
				"\n" +
				"\t\t"+dataType+"[] data = img.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < img.height; y++) {\n" +
				"\t\t\tint index = img.getStartIndex() + y * img.getStride();\n" +
				"\t\t\tint end = index + img.width*img.numBands;\n" +
				"\t\t\tfor (; index <  end; index++) {\n");
		if( imageType.isInteger() ) {
			if( imageType.getNumBits() < 32 ) {
				out.print("\t\t\t\tdata[index] = "+typeCast+"(rand.nextInt(range)+min);\n");
			} else if( imageType.getNumBits() < 64) {
				out.print("\t\t\t\tdata[index] = rand.nextInt((int)range)+min;\n");
			} else {
				out.print("\t\t\t\tdata[index] = (long)(rand.nextDouble()*0.9999*range)+min;\n");
			}
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\tdata[index] = rand.next"+randType+"()*range+min;\n");
		}
		out.print("\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillGaussian() {

		String sumType = imageType.getSumType();
		String castToSum = sumType.compareTo("double") == 0 ? "" : "("+sumType+")";
		String typeCast = imageType.getTypeCastFromSum();

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from a Gaussian distribution.  A user\n" +
				"\t * specified lower and upper bound is provided to ensure that the values are within a legal\n" +
				"\t * range.  A drawn value outside the allowed range will be set to the closest bound.\n" +
				"\t * \n" +
				"\t * @param input Input image.  Modified.\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param mean Distribution's mean.\n" +
				"\t * @param sigma Distribution's standard deviation.\n" +
				"\t * @param lowerBound Lower bound of value clip\n" +
				"\t * @param upperBound Upper bound of value clip\n" +
				"\t */\n" +
				"\tpublic static void fillGaussian("+imageName+" input, Random rand , double mean , double sigma , "
				+sumType+" lowerBound , "+sumType+" upperBound ) {\n" +
				"\t\t"+dataType+"[] data = input.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\t"+sumType+" value = "+castToSum+"(rand.nextGaussian()*sigma+mean);\n" +
				"\t\t\t\tif( value < lowerBound ) value = lowerBound;\n" +
				"\t\t\t\tif( value > upperBound ) value = upperBound;\n" +
				"\t\t\t\tdata[index++] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillGaussianInterleaved() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String castToSum = sumType.compareTo("double") == 0 ? "" : "("+sumType+")";
		String typeCast = imageType.getTypeCastFromSum();

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from a Gaussian distribution.  A user\n" +
				"\t * specified lower and upper bound is provided to ensure that the values are within a legal\n" +
				"\t * range.  A drawn value outside the allowed range will be set to the closest bound.\n" +
				"\t * \n" +
				"\t * @param input Input image.  Modified.\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param mean Distribution's mean.\n" +
				"\t * @param sigma Distribution's standard deviation.\n" +
				"\t * @param lowerBound Lower bound of value clip\n" +
				"\t * @param upperBound Upper bound of value clip\n" +
				"\t */\n" +
				"\tpublic static void fillGaussian("+imageName+" input, Random rand , double mean , double sigma , "
				+sumType+" lowerBound , "+sumType+" upperBound ) {\n" +
				"\t\t"+dataType+"[] data = input.data;\n" +
				"\t\tint length = input.width*input.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint indexEnd = index+length;\n" +
				"\n" +
				"\t\t\twhile( index < indexEnd ) {\n" +
				"\t\t\t\t"+sumType+" value = "+castToSum+"(rand.nextGaussian()*sigma+mean);\n" +
				"\t\t\t\tif( value < lowerBound ) value = lowerBound;\n" +
				"\t\t\t\tif( value > upperBound ) value = upperBound;\n" +
				"\t\t\t\tdata[index++] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddUniformSB() {

		String sumType = imageType.getSumType();
		int min = imageType.getMin().intValue();
		int max = imageType.getMax().intValue();
		String typeCast = imageType.getTypeCastFromSum();

		out.print("\t/**\n" +
				"\t * Adds uniform i.i.d noise to each pixel in the image.  Noise range is min &le; X &lt; max.\n" +
				"\t */\n" +
				"\tpublic static void addUniform("+imageName+" input, Random rand , "+sumType+" min , "+sumType+" max) {\n" +
				"\t\t"+sumType+" range = max-min;\n" +
				"\n" +
				"\t\t"+dataType+"[] data = input.data;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n");
		if( imageType.isInteger() && imageType.getNumBits() != 64) {
			out.print("\t\t\t\t"+sumType+" value = (data[index] "+bitWise+") + rand.nextInt(range)+min;\n");
			if( imageType.getNumBits() < 32 ) {
				out.print("\t\t\t\tif( value < "+min+" ) value = "+min+";\n" +
						"\t\t\t\tif( value > "+max+" ) value = "+max+";\n" +
						"\n");
			}
		} else if( imageType.isInteger() ) {
			out.print("\t\t\t\t"+sumType+" value = data[index] + rand.nextInt((int)range)+min;\n");
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\t"+sumType+" value = data[index] + rand.next"+randType+"()*range+min;\n");
		}
		out.print("\t\t\t\tdata[index++] = "+typeCast+" value;\n" +
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

		out.print("\t/**\n" +
				"\t * Adds uniform i.i.d noise to each pixel in the image.  Noise range is min &le; X &lt; max.\n" +
				"\t */\n" +
				"\tpublic static void addUniform("+imageName+" input, Random rand , "+sumType+" min , "+sumType+" max) {\n" +
				"\t\t"+sumType+" range = max-min;\n" +
				"\n" +
				"\t\t"+dataType+"[] data = input.data;\n" +
				"\t\tint length = input.width*input.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\n" +
				"\t\t\t\tint indexEnd = index+length;\n" +
				"\t\t\t\twhile( index < indexEnd ) {\n");
		if( imageType.isInteger() && imageType.getNumBits() != 64) {
			out.print("\t\t\t\t"+sumType+" value = (data[index] "+bitWise+") + rand.nextInt(range)+min;\n");
			if( imageType.getNumBits() < 32 ) {
				out.print("\t\t\t\tif( value < "+min+" ) value = "+min+";\n" +
						"\t\t\t\tif( value > "+max+" ) value = "+max+";\n" +
						"\n");
			}
		} else if( imageType.isInteger() ) {
			out.print("\t\t\t\t"+sumType+" value = data[index] + rand.nextInt((int)range)+min;\n");
		} else {
			String randType = imageType.getRandType();
			out.print("\t\t\t\t"+sumType+" value = data[index] + rand.next"+randType+"()*range+min;\n");
		}
		out.print("\t\t\t\tdata[index++] = "+typeCast+" value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddGaussianSB() {
		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
		String sumCast = sumType.equals("double") ? "" : "("+sumType+")";

		out.print("\t/**\n" +
				"\t * Adds Gaussian/normal i.i.d noise to each pixel in the image.  If a value exceeds the specified\n"+
				"\t * it will be set to the closest bound.\n" +
				"\t * @param input Input image.  Modified.\n" +
				"\t * @param rand Random number generator.\n" +
				"\t * @param sigma Distributions standard deviation.\n" +
				"\t * @param lowerBound Allowed lower bound\n" +
				"\t * @param upperBound Allowed upper bound\n" +
				"\t */\n" +
				"\tpublic static void addGaussian("+imageName+" input, Random rand , double sigma , "
				+sumType+" lowerBound , "+sumType+" upperBound ) {\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\t"+sumType+" value = (input.data[index] "+bitWise+") + "+sumCast+"(rand.nextGaussian()*sigma);\n" +
				"\t\t\t\tif( value < lowerBound ) value = lowerBound;\n" +
				"\t\t\t\tif( value > upperBound ) value = upperBound;\n" +
				"\t\t\t\tinput.data[index++] = "+typeCast+" value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printAddGaussianIL() {
		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String typeCast = imageType.getTypeCastFromSum();
		String sumCast = sumType.equals("double") ? "" : "("+sumType+")";

		out.print("\t/**\n" +
				"\t * Adds Gaussian/normal i.i.d noise to each pixel in the image.  If a value exceeds the specified\n"+
				"\t * it will be set to the closest bound.\n" +
				"\t * @param input Input image.  Modified.\n" +
				"\t * @param rand Random number generator.\n" +
				"\t * @param sigma Distributions standard deviation.\n" +
				"\t * @param lowerBound Allowed lower bound\n" +
				"\t * @param upperBound Allowed upper bound\n" +
				"\t */\n" +
				"\tpublic static void addGaussian("+imageName+" input, Random rand , double sigma , "
				+sumType+" lowerBound , "+sumType+" upperBound ) {\n" +
				"\n" +
				"\t\tint length = input.width*input.numBands;\n" +
				"\n" +
				"\t\tfor (int y = 0; y < input.height; y++) {\n" +
				"\t\t\tint index = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint indexEnd = index+length;\n" +
				"\t\t\twhile( index < indexEnd ) {\n" +
				"\t\t\t\t"+sumType+" value = (input.data[index]"+bitWise+") + "+sumCast+"(rand.nextGaussian()*sigma);\n" +
				"\t\t\t\tif( value < lowerBound ) value = lowerBound;\n" +
				"\t\t\t\tif( value > upperBound ) value = upperBound;\n" +
				"\t\t\t\tinput.data[index++] = "+typeCast+"value;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFlipVertical() {
		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Flips the image from top to bottom\n" +
				"\t */\n" +
				"\tpublic static void flipVertical( "+imageName+" input ) {\n" +
				"\t\tint h2 = input.height/2;\n" +
				"\n" +
				"\t\tfor( int y = 0; y < h2; y++ ) {\n" +
				"\t\t\tint index1 = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint index2 = input.getStartIndex() + (input.height - y - 1) * input.getStride();\n" +
				"\n" +
				"\t\t\tint end = index1 + input.width;\n" +
				"\n" +
				"\t\t\twhile( index1 < end ) {\n" +
				"\t\t\t\t"+sumType+" tmp = input.data[index1];\n" +
				"\t\t\t\tinput.data[index1++] = input.data[index2];\n" +
				"\t\t\t\tinput.data[index2++] = ("+dataType+")tmp;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFlipHorizontal() {
		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Flips the image from left to right\n" +
				"\t */\n" +
				"\tpublic static void flipHorizontal( "+imageName+" input ) {\n" +
				"\t\tint w2 = input.width/2;\n" +
				"\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint index1 = input.getStartIndex() + y * input.getStride();\n" +
				"\t\t\tint index2 = index1 + input.width-1;\n" +
				"\n" +
				"\t\t\tint end = index1 + w2;\n" +
				"\n" +
				"\t\t\twhile( index1 < end ) {\n" +
				"\t\t\t\t"+sumType+" tmp = input.data[index1];\n" +
				"\t\t\t\tinput.data[index1++] = input.data[index2];\n" +
				"\t\t\t\tinput.data[index2--] = ("+dataType+")tmp;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCW_one() {
		String sumType = imageType.getSumType();
		out.print("\t/**\n" +
				"\t * In-place 90 degree image rotation in the clockwise direction.  Only works on\n" +
				"\t * square images.\n" +
				"\t */\n" +
				"\tpublic static void rotateCW( "+imageName+" image ) {\n" +
				"\t\tif( image.width != image.height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Image must be square\");\n" +
				"\n" +
				"\t\tint w = image.height/2 + image.height%2;\n" +
				"\t\tint h = image.height/2;\n" +
				"\n" +
				"\t\tfor( int y0 = 0; y0 < h; y0++ ) {\n" +
				"\t\t\tint y1 = image.height-y0-1;\n" +
				"\n" +
				"\t\t\tfor( int x0 = 0; x0 < w; x0++ ) {\n" +
				"\t\t\t\tint x1 = image.width-x0-1;\n" +
				"\n" +
				"\t\t\t\tint index0 = image.startIndex + y0*image.stride + x0;\n" +
				"\t\t\t\tint index1 = image.startIndex + x0*image.stride + y1;\n" +
				"\t\t\t\tint index2 = image.startIndex + y1*image.stride + x1;\n" +
				"\t\t\t\tint index3 = image.startIndex + x1*image.stride + y0;\n" +
				"\t\t\t\t\n" +
				"\t\t\t\t"+sumType+" tmp3 = image.data[index3];\n" +
				"\n" +
				"\t\t\t\timage.data[index3] = image.data[index2];\n" +
				"\t\t\t\timage.data[index2] = image.data[index1];\n" +
				"\t\t\t\timage.data[index1] = image.data[index0];\n" +
				"\t\t\t\timage.data[index0] = ("+dataType+")tmp3;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCW_two() {
		out.print("\t/**\n" +
				"\t * Rotates the image 90 degrees in the clockwise direction.\n" +
				"\t */\n" +
				"\tpublic static void rotateCW( "+imageName+" input , "+imageName+" output ) {\n" +
				"\t\tif( input.width != output.height || input.height != output.width )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Incompatible shapes\");\n" +
				"\n" +
				"\t\tint h = input.height-1;\n" +
				"\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\toutput.unsafe_set(h-y,x,input.data[indexIn++]);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCW_two_interleaved() {
		out.print("\t/**\n" +
				"\t * Rotates the image 90 degrees in the clockwise direction.\n" +
				"\t */\n" +
				"\tpublic static void rotateCW( "+imageNameI+" input , "+imageNameI+" output ) {\n" +
				"\t\tif( input.width != output.height || input.height != output.width || input.numBands != output.numBands )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Incompatible shapes\");\n" +
				"\n" +
				"\t\tint h = input.height-1;\n" +
				"\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint indexDst = output.getIndex(h-y,x);\n" +
				"\n" +
				"\t\t\t\tint end = indexSrc + input.numBands;\n" +
				"\t\t\t\twhile( indexSrc != end ) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_one() {
		String sumType = imageType.getSumType();
		out.print("\t/**\n" +
				"\t * In-place 90 degree image rotation in the counter-clockwise direction.  Only works on\n" +
				"\t * square images.\n" +
				"\t */\n" +
				"\tpublic static void rotateCCW( "+imageName+" image ) {\n" +
				"\t\tif( image.width != image.height )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Image must be square\");\n" +
				"\n" +
				"\t\tint w = image.height/2 + image.height%2;\n" +
				"\t\tint h = image.height/2;\n" +
				"\n" +
				"\t\tfor( int y0 = 0; y0 < h; y0++ ) {\n" +
				"\t\t\tint y1 = image.height-y0-1;\n" +
				"\n" +
				"\t\t\tfor( int x0 = 0; x0 < w; x0++ ) {\n" +
				"\t\t\t\tint x1 = image.width-x0-1;\n" +
				"\n" +
				"\t\t\t\tint index0 = image.startIndex + y0*image.stride + x0;\n" +
				"\t\t\t\tint index1 = image.startIndex + x0*image.stride + y1;\n" +
				"\t\t\t\tint index2 = image.startIndex + y1*image.stride + x1;\n" +
				"\t\t\t\tint index3 = image.startIndex + x1*image.stride + y0;\n" +
				"\t\t\t\t\n" +
				"\t\t\t\t"+sumType+" tmp0 = image.data[index0];\n" +
				"\n" +
				"\t\t\t\timage.data[index0] = image.data[index1];\n" +
				"\t\t\t\timage.data[index1] = image.data[index2];\n" +
				"\t\t\t\timage.data[index2] = image.data[index3];\n" +
				"\t\t\t\timage.data[index3] = ("+dataType+")tmp0;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_two() {
		out.print("\t/**\n" +
				"\t * Rotates the image 90 degrees in the counter-clockwise direction.\n" +
				"\t */\n" +
				"\tpublic static void rotateCCW( "+imageName+" input , "+imageName+" output ) {\n" +
				"\t\tif( input.width != output.height || input.height != output.width )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Incompatible shapes\");\n" +
				"\n" +
				"\t\tint w = input.width-1;\n" +
				"\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\toutput.unsafe_set(y,w-x,input.data[indexIn++]);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_two_interleaved() {
		out.print("\t/**\n" +
				"\t * Rotates the image 90 degrees in the counter-clockwise direction.\n" +
				"\t */\n" +
				"\tpublic static void rotateCCW( "+imageNameI+" input , "+imageNameI+" output ) {\n" +
				"\t\tif( input.width != output.height || input.height != output.width || input.numBands != output.numBands )\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Incompatible shapes\");\n" +
				"\n" +
				"\t\tint w = input.width-1;\n" +
				"\n" +
				"\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride;\n" +
				"\t\t\tfor (int x = 0; x < input.width; x++) {\n" +
				"\t\t\t\tint indexDst = output.getIndex(y,w-x);\n" +
				"\n" +
				"\t\t\t\tint end = indexSrc + input.numBands;\n" +
				"\t\t\t\twhile( indexSrc != end ) {\n" +
				"\t\t\t\t\toutput.data[indexDst++] = input.data[indexSrc++];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printGrowBorder() {
		String borderName = "ImageBorder_"+imageType.getKernelType();
		String generic = "";
		String srcType = imageName;
		if( imageType.isInteger() && imageType.getNumBits() < 32 ) {
			generic = "<T extends GrayI"+imageType.getNumBits()+"<T>>\n\t";
			srcType = "T";
			borderName += "<T>";
		}
		out.print("\t/**\n" +
				"\t * Creates a new image which is a copy of the src image but extended with border pixels.\n" +
				"\t * \n" +
				"\t * @param src (Input) source image\n" +
				"\t * @param border (Input) image border generator\n" +
				"\t * @param borderX0 (Input) Border x-axis lower extent\n" +
				"\t * @param borderY0 (Input) Border y-axis lower extent\n" +
				"\t * @param borderX1 (Input) Border x-axis upper extent\n" +
				"\t * @param borderY1 (Input) Border y-axis upper extent\n" +
				"\t * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY\n" +
				"\t */\n" +
				"\tpublic static "+generic+"void growBorder("+srcType+" src , "+borderName+" border, int borderX0, int borderY0, int borderX1, int borderY1, "+srcType+" dst ) {\n" +
				"\t\tImplImageMiscOps.growBorder(src,border,borderX0,borderY0,borderX1,borderY1,dst);\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImageMiscOps gen = new GenerateImageMiscOps();
		gen.generate();
	}
}
