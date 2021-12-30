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
//	private String dataType;
//	private String bitWise;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		printAllGeneric();
		printAllSpecific();
		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.misc.BoofLambdas;\n" +
				"import boofcv.struct.image.*;\n" +
				"import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.alg.misc.impl.ImplImageMiscOps;\n" +
				"import boofcv.alg.misc.impl.ImplImageMiscOps_MT;\n" +
				"import boofcv.struct.border.ImageBorder_F32;\n" +
				"import boofcv.struct.border.ImageBorder_F64;\n" +
				"import boofcv.struct.border.ImageBorder_S32;\n" +
				"import boofcv.struct.border.ImageBorder_S64;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"import java.util.Random;\n" +
				"\n" +
				"/**\n" +
				" * Basic image operations which have no place better to go.\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n" +
				"\t/**\n" +
				"\t * If the image has fewer than this elements do not run the concurrent version of the function since it could\n" +
				"\t * run slower\n" +
				"\t */\n" +
				"\tpublic static int MIN_ELEMENTS_CONCURRENT = 400*400;\n" +
				"\t\n" +
				"\tpublic static boolean runConcurrent( ImageBase image ) {\n" +
				"\t\treturn runConcurrent(image.width*image.height);\n" +
				"\t}\n" +
				"\tpublic static boolean runConcurrent( int numElements ) {\n" +
				"\t\treturn BoofConcurrency.isUseConcurrent() && (numElements >= MIN_ELEMENTS_CONCURRENT);\n" +
				"\t}\n\n");
	}

	private void printAllGeneric() {
		AutoTypeImage[] types = AutoTypeImage.getGenericTypes();

		for (AutoTypeImage t : types) {
			imageType = t;
			imageName = t.getSingleBandName();
			imageNameI = t.getInterleavedName();
//			dataType = t.getDataType();
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
			printRotateCW_one();
			printTranspose_two_interleaved();
			printRotateCW_two();
			printRotateCW_two_interleaved();
			printRotateCCW_one();
			printRotateCCW_two();
			printRotateCCW_two_interleaved();
			printGrowBorder();
			printFindValues();
		}
	}

	private void printAllSpecific() {
		AutoTypeImage[] types = AutoTypeImage.getSpecificTypes();

		for (AutoTypeImage t : types) {
			imageType = t;
			imageName = t.getSingleBandName();
//			dataType = t.getDataType();
//			bitWise = t.getBitWise();
			printAddUniformSB();
			printAddUniformIL();
			printAddGaussianSB();
			printAddGaussianIL();
		}
	}

	private void printCopyBorder() {
		boolean useGenerics = imageType.isInteger() && imageType.getNumBits() < 32;
		String borderName = "ImageBorder_" + imageType.getKernelType();
		borderName += useGenerics ? "<T>" : "";

		String imageNameSrc = useGenerics ? "T" : imageName;
		String generic = useGenerics ? "< T extends " + imageName + "<T>> " : "";

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
				"\t * @param border Border for input image\n" +
				"\t * @param output output image\n" +
				"\t */\n" +
				"\tpublic static " + generic + "void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,\n" +
				"\t\t\t\t\t\t\t " + imageNameSrc + " input, " + borderName + " border, " + imageName + " output ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent((dstY-srcY)*(dstX-srcX))) {\n" +
				"//\t\t\tImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);\n" +
				"//\t\t}\n" +
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
				"\tpublic static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,\n" +
				"\t\t\t\t\t\t\t " + imageName + " input, " + imageName + " output ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent((dstY-srcY)*(dstX-srcX))) {\n" +
				"//\t\t\tImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);\n" +
				"//\t\t}\n" +
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
				"\tpublic static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,\n" +
				"\t\t\t\t\t\t\t " + imageNameI + " input, " + imageNameI + " output ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent((dstY-srcY)*(dstX-srcX))) {\n" +
				"//\t\t\tImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);\n" +
				"//\t\t}\n" +
				"\t}\n\n");
	}

	private void printFill() {
		out.print("\t/**\n" +
				"\t * Fills the whole image with the specified value\n" +
				"\t *\n" +
				"\t * @param image An image. Modified.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t */\n" +
				"\tpublic static void fill( " + imageName + " image, " + imageType.getSumType() + " value ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent(image)) {\n" +
				"//\t\t\tImplImageMiscOps_MT.fill(image, value);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.fill(image, value);\n" +
				"//\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillInterleaved() {
		String imageName = imageType.getInterleavedName();
		out.print("\t/**\n" +
				"\t * Fills the whole image with the specified value\n" +
				"\t *\n" +
				"\t * @param image An image. Modified.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t */\n" +
				"\tpublic static void fill( " + imageName + " image, " + imageType.getSumType() + " value ) {\n" +
				"\t\tif (runConcurrent(image)) {\n" +
				"\t\t\tImplImageMiscOps_MT.fill(image, value);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.fill(image, value);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillInterleaved_bands() {
		String imageName = imageType.getInterleavedName();
		out.print(
				"\t/**\n" +
						"\t * Fills each band in the image with the specified values\n" +
						"\t *\n" +
						"\t * @param image An image. Modified.\n" +
						"\t * @param values Array which contains the values each band is to be filled with.\n" +
						"\t */\n" +
						"\tpublic static void fill( " + imageName + " image, " + imageType.getSumType() + "[] values ) {\n" +
						"\t\tif (runConcurrent(image)) {\n" +
						"\t\t\tImplImageMiscOps_MT.fill(image, values);\n" +
						"\t\t} else {\n" +
						"\t\t\tImplImageMiscOps.fill(image, values);\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printFillBand_Interleaved() {
		String imageName = imageType.getInterleavedName();
		out.print(
				"\t/**\n" +
						"\t * Fills one band in the image with the specified value\n" +
						"\t *\n" +
						"\t * @param image An image. Modified.\n" +
						"\t * @param band Which band is to be filled with the specified value\n" +
						"\t * @param value The value that the image is being filled with.\n" +
						"\t */\n" +
						"\tpublic static void fillBand( " + imageName + " image, int band, " + imageType.getSumType() + " value ) {\n" +
						"\t\tif (runConcurrent(image)) {\n" +
						"\t\t\tImplImageMiscOps_MT.fillBand(image, band, value);\n" +
						"\t\t} else {\n" +
						"\t\t\tImplImageMiscOps.fillBand(image, band, value);\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printInsertBandInterleaved() {
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
						"\tpublic static void insertBand( " + singleName + " input, int band, " + interleavedName + " output) {\n" +
						"\t\tif (runConcurrent(input)) {\n" +
						"\t\t\tImplImageMiscOps_MT.insertBand(input, band, output);\n" +
						"\t\t} else {\n" +
						"\t\t\tImplImageMiscOps.insertBand(input, band, output);\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printExtractBandInterleaved() {
		String singleName = imageType.getSingleBandName();
		String interleavedName = imageType.getInterleavedName();
		out.print(
				"\t/**\n" +
						"\t * Extracts a single band from a multi-band image\n" +
						"\t *\n" +
						"\t * @param input Multi-band image.\n" +
						"\t * @param band which bad is to be extracted\n" +
						"\t * @param output The single band image. Modified.\n" +
						"\t */\n" +
						"\tpublic static void extractBand( " + interleavedName + " input, int band, " + singleName + " output) {\n" +
						"\t\tif (runConcurrent(input)) {\n" +
						"\t\t\tImplImageMiscOps_MT.extractBand(input, band, output);\n" +
						"\t\t} else {\n" +
						"\t\t\tImplImageMiscOps.extractBand(input, band, output);\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printFillBorder() {
		out.print("\t/**\n" +
				"\t * Fills the outside border with the specified value\n" +
				"\t *\n" +
				"\t * @param image An image. Modified.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t * @param radius Border width.\n" +
				"\t */\n" +
				"\tpublic static void fillBorder( " + imageName + " image, " + imageType.getSumType() + " value, int radius ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent(image)) {\n" +
				"//\t\t\tImplImageMiscOps_MT.fillBorder(image, value, radius);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.fillBorder(image, value, radius);\n" +
				"//\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillBorder2() {
		out.print("\t/**\n" +
				"\t * Fills the border with independent border widths for each side\n" +
				"\t *\n" +
				"\t * @param image An image.\n" +
				"\t * @param value The value that the image is being filled with.\n" +
				"\t * @param borderX0 Width of border on left\n" +
				"\t * @param borderY0 Width of border on top\n" +
				"\t * @param borderX1 Width of border on right\n" +
				"\t * @param borderY1 Width of border on bottom\n" +
				"\t */\n" +
				"\tpublic static void fillBorder( " + imageName + " image, " + imageType.getSumType() + " value, int borderX0, int borderY0, int borderX1, int borderY1 ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent(image)) {\n" +
				"//\t\t\tImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);\n" +
				"//\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillRectangle() {
		out.print("\t/**\n" +
				"\t * Draws a filled rectangle that is aligned along the image axis inside the image.\n" +
				"\t *\n" +
				"\t * @param image The image the rectangle is drawn in. Modified\n" +
				"\t * @param value Value of the rectangle\n" +
				"\t * @param x0 Top left x-coordinate\n" +
				"\t * @param y0 Top left y-coordinate\n" +
				"\t * @param width Rectangle width\n" +
				"\t * @param height Rectangle height\n" +
				"\t */\n" +
				"\tpublic static void fillRectangle( " + imageName + " image, " + imageType.getSumType() + " value, int x0, int y0, int width, int height ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent(image)) {\n" +
				"//\t\t\tImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);\n" +
				"//\t\t}\n" +
				"\t}\n\n");
	}

	private void printFillRectangleInterleaved() {
		String imageName = imageType.getInterleavedName();

		out.print("\t/**\n" +
				"\t * Draws a filled rectangle that is aligned along the image axis inside the image. All bands\n" +
				"\t * are filled with the same value.\n" +
				"\t *\n" +
				"\t * @param image The image the rectangle is drawn in. Modified\n" +
				"\t * @param value Value of the rectangle\n" +
				"\t * @param x0 Top left x-coordinate\n" +
				"\t * @param y0 Top left y-coordinate\n" +
				"\t * @param width Rectangle width\n" +
				"\t * @param height Rectangle height\n" +
				"\t */\n" +
				"\tpublic static void fillRectangle( " + imageName + " image, " + imageType.getSumType() + " value, int x0, int y0, int width, int height ) {\n" +
				"//\t\tconcurrent isn't faster in benchmark results\n" +
				"//\t\tif (runConcurrent(image)) {\n" +
				"//\t\t\tImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);\n" +
				"//\t\t} else {\n" +
				"\t\tImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);\n" +
				"//\t\t}\n" +
				"\t}");
	}

	private void printFillUniform() {

		String sumType = imageType.getSumType();
		String maxInclusive = imageType.isInteger() ? "exclusive" : "inclusive";

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.\n" +
				"\t *\n" +
				"\t * @param img Image which is to be filled. Modified.\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param min Minimum value of the distribution, inclusive\n" +
				"\t * @param max Maximum value of the distribution, " + maxInclusive + "\n" +
				"\t */\n" +
				"\tpublic static void fillUniform( " + imageName + " img, Random rand, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\tImplImageMiscOps.fillUniform(img, rand, min, max);\n" +
				"\t}\n\n");
	}

	private void printFillUniformInterleaved() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();
		String maxInclusive = imageType.isInteger() ? "exclusive" : "inclusive";

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.\n" +
				"\t *\n" +
				"\t * @param img Image which is to be filled. Modified.\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param min Minimum value of the distribution, inclusive\n" +
				"\t * @param max Maximum value of the distribution, " + maxInclusive + "\n" +
				"\t */\n" +
				"\tpublic static void fillUniform( " + imageName + " img, Random rand, " + sumType + " min, " + sumType + " max ) {\n" +
				"\t\tImplImageMiscOps.fillUniform(img, rand, min, max);\n" +
				"\t}\n\n");
	}

	private void printFillGaussian() {

		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from a Gaussian distribution. A user\n" +
				"\t * specified lower and upper bound is provided to ensure that the values are within a legal\n" +
				"\t * range. A drawn value outside the allowed range will be set to the closest bound.\n" +
				"\t * \n" +
				"\t * @param input Input image. Modified.\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param mean Distribution's mean.\n" +
				"\t * @param sigma Distribution's standard deviation.\n" +
				"\t * @param lowerBound Lower bound of value clip\n" +
				"\t * @param upperBound Upper bound of value clip\n" +
				"\t */\n" +
				"\tpublic static void fillGaussian( " + imageName + " input, Random rand, double mean, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\t\tImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);\n" +
				"\t}\n\n");
	}

	private void printFillGaussianInterleaved() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Sets each value in the image to a value drawn from a Gaussian distribution. A user\n" +
				"\t * specified lower and upper bound is provided to ensure that the values are within a legal\n" +
				"\t * range. A drawn value outside the allowed range will be set to the closest bound.\n" +
				"\t * \n" +
				"\t * @param input Input image. Modified.\n" +
				"\t * @param rand Random number generator\n" +
				"\t * @param mean Distribution's mean.\n" +
				"\t * @param sigma Distribution's standard deviation.\n" +
				"\t * @param lowerBound Lower bound of value clip\n" +
				"\t * @param upperBound Upper bound of value clip\n" +
				"\t */\n" +
				"\tpublic static void fillGaussian( " + imageName + " input, Random rand, double mean, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\t\tImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);\n" +
				"\t}\n\n");
	}

	private void printAddUniformSB() {

		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.\n" +
				"\t */\n" +
				"\tpublic static void addUniform(" + imageName + " input, Random rand, " + sumType + " min, " + sumType + " max) {\n" +
				"\t\tImplImageMiscOps.addUniform(input, rand, min, max);\n" +
				"\t}\n\n");
	}

	private void printAddUniformIL() {

		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.\n" +
				"\t */\n" +
				"\tpublic static void addUniform(" + imageName + " input, Random rand, " + sumType + " min, " + sumType + " max) {\n" +
				"\t\tImplImageMiscOps.addUniform(input, rand, min, max);\n" +
				"\t}\n\n");
	}

	private void printAddGaussianSB() {
		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified\n" +
				"\t * it will be set to the closest bound.\n" +
				"\t * @param image Input image. Modified.\n" +
				"\t * @param rand Random number generator.\n" +
				"\t * @param sigma Distributions standard deviation.\n" +
				"\t * @param lowerBound Allowed lower bound\n" +
				"\t * @param upperBound Allowed upper bound\n" +
				"\t */\n" +
				"\tpublic static void addGaussian(" + imageName + " image, Random rand, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\t\tImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);\n" +
				"\t}\n\n");
	}

	private void printAddGaussianIL() {
		String imageName = imageType.getInterleavedName();
		String sumType = imageType.getSumType();

		out.print("\t/**\n" +
				"\t * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified\n" +
				"\t * it will be set to the closest bound.\n" +
				"\t * @param image Input image. Modified.\n" +
				"\t * @param rand Random number generator.\n" +
				"\t * @param sigma Distributions standard deviation.\n" +
				"\t * @param lowerBound Allowed lower bound\n" +
				"\t * @param upperBound Allowed upper bound\n" +
				"\t */\n" +
				"\tpublic static void addGaussian(" + imageName + " image, Random rand, double sigma, "
				+ sumType + " lowerBound, " + sumType + " upperBound ) {\n" +
				"\t\tImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);\n" +
				"\t}\n\n");
	}

	private void printFlipVertical() {
		out.print("\t/** Flips the image from top to bottom */\n" +
				"\tpublic static void flipVertical( " + imageName + " image ) {\n" +
				"\t\tif (runConcurrent(image)) {\n" +
				"\t\t\tImplImageMiscOps_MT.flipVertical(image);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.flipVertical(image);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFlipHorizontal() {
		out.print("\t/** Flips the image from left to right */\n" +
				"\tpublic static void flipHorizontal( " + imageName + " image ) {\n" +
				"\t\tif (runConcurrent(image)) {\n" +
				"\t\t\tImplImageMiscOps_MT.flipHorizontal(image);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.flipHorizontal(image);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printTranspose_two() {
		String genericType = genericType(imageName);
		out.print("\t/** Transposes the image */\n" +
				"\tpublic static <T extends " + genericType + "> T transpose( T input, @Nullable T output ) {\n" +
				"\t\toutput = (T)InputSanityCheck.checkDeclareNoReshape(input, output);\n" +
				"\t\t//if (runConcurrent(input)) {\n" +
				"\t\t//\tImplImageMiscOps_MT.transpose(input, output);\n" +
				"\t\t//} else {\n" +
				"\t\tImplImageMiscOps.transpose(input, output);\n" +
				"\t\t//}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printTranspose_two_interleaved() {
		String genericType = genericType(imageNameI);
		out.print("\t/** Transposes the image */\n" +
				"\tpublic static <T extends " + genericType + "> T transpose( T input, @Nullable T output ) {\n" +
				"\t\toutput = (T)InputSanityCheck.checkDeclareNoReshape(input, output);\n" +
				"\t\tif (runConcurrent(input)) {\n" +
				"\t\t\tImplImageMiscOps_MT.transpose(input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.transpose(input, output);\n" +
				"\t\t}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printRotateCW_one() {
		out.print("\t/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */\n" +
				"\tpublic static void rotateCW( " + imageName + " image ) {\n" +
				"\t\tif (runConcurrent(image)) {\n" +
				"\t\t\tImplImageMiscOps_MT.rotateCW(image);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.rotateCW(image);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCW_two() {
		String genericType = genericType(imageName);
		out.print("\t/** Rotates the image 90 degrees in the clockwise direction. */\n" +
				"\tpublic static <T extends " + genericType + "> T rotateCW( T input, @Nullable T output ) {\n" +
				"\t\toutput = (T)InputSanityCheck.checkDeclareNoReshape(input, output);\n" +
				"\t\tif (runConcurrent(input)) {\n" +
				"\t\t\tImplImageMiscOps_MT.rotateCW(input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.rotateCW(input, output);\n" +
				"\t\t}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printRotateCW_two_interleaved() {
		String genericType = genericType(imageNameI);
		out.print("\t/** Rotates the image 90 degrees in the clockwise direction. */\n" +
				"\tpublic static <T extends " + genericType + "> T rotateCW( T input, @Nullable T output ) {\n" +
				"\t\toutput = (T)InputSanityCheck.checkDeclareNoReshape(input, output);\n" +
				"\t\tif (runConcurrent(input)) {\n" +
				"\t\t\tImplImageMiscOps_MT.rotateCW(input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.rotateCW(input, output);\n" +
				"\t\t}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_one() {
		out.print("\t/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */\n" +
				"\tpublic static void rotateCCW( " + imageName + " image ) {\n" +
				"\t\tif (runConcurrent(image)) {\n" +
				"\t\t\tImplImageMiscOps_MT.rotateCCW(image);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.rotateCCW(image);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_two() {
		String genericType = genericType(imageName);
		out.print("\t/** Rotates the image 90 degrees in the counter-clockwise direction. */\n" +
				"\tpublic static <T extends " + genericType + "> T rotateCCW( T input, @Nullable T output ) {\n" +
				"\t\toutput = (T)InputSanityCheck.checkDeclareNoReshape(input, output);\n" +
				"\t\tif (runConcurrent(input)) {\n" +
				"\t\t\tImplImageMiscOps_MT.rotateCCW(input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.rotateCCW(input, output);\n" +
				"\t\t}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printRotateCCW_two_interleaved() {
		String genericType = genericType(imageNameI);
		out.print("\t/** Rotates the image 90 degrees in the counter-clockwise direction. */\n" +
				"\tpublic static <T extends " + genericType + "> T rotateCCW( T input, @Nullable T output ) {\n" +
				"\t\toutput = (T)InputSanityCheck.checkDeclareNoReshape(input, output);\n" +
				"\t\tif (runConcurrent(input)) {\n" +
				"\t\t\tImplImageMiscOps_MT.rotateCCW(input, output);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.rotateCCW(input, output);\n" +
				"\t\t}\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	private void printGrowBorder() {
		String borderName = "ImageBorder_" + imageType.getKernelType();
		String generic = "";
		String srcType = imageName;
		if (imageType.isInteger() && imageType.getNumBits() < 32) {
			generic = "<T extends GrayI" + imageType.getNumBits() + "<T>>\n\t";
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
				"\tpublic static " + generic + "void growBorder( " + srcType + " src, " + borderName + " border, int borderX0, int borderY0, int borderX1, int borderY1, " + srcType + " dst ) {\n" +
				"\t\tif (runConcurrent(src)) {\n" +
				"\t\t\tImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);\n" +
				"\t\t} else {\n" +
				"\t\t\tImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFindValues() {
		String suffix = imageType.getGenericAbbreviated();

		out.print("\t/**\n" +
				"\t * Using the provided functions, finds all pixel values which match then calls the process function\n" +
				"\t *\n" +
				"\t * @param input (Input) Image\n" +
				"\t * @param finder (Input) Checks to see if the pixel value matches the criteria\n" +
				"\t * @param process (Input) When a match is found this function is called and given the coordinates. true = continue\n" +
				"\t */\n" +
				"\tpublic static void findAndProcess( " + imageType.getSingleBandName() + " input, BoofLambdas.Match_" + suffix +
				" finder, BoofLambdas.ProcessIIB process ) {\n" +
				"\t\tImplImageMiscOps.findAndProcess(input, finder, process);\n" +
				"\t}\n\n");
	}

	private String genericType( String imageName ) {
		String genericType = imageName;
		if (imageType.isInteger() && (imageName.endsWith("I8") || imageName.endsWith("I16"))) {
			genericType = imageName + "<T>";
		}
		return genericType;
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImageMiscOps gen = new GenerateImageMiscOps();
		gen.setModuleName("boofcv-ip");
		gen.generate();
	}
}
