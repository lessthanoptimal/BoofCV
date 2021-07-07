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

package boofcv.alg.enhance.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplEnhanceHistogram extends CodeGeneratorBase {

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		applyTransform_U(AutoTypeImage.U8);
		applyTransform_U(AutoTypeImage.U16);
		applyTransform_S(AutoTypeImage.S8);
		applyTransform_S(AutoTypeImage.S16);
		applyTransform_S(AutoTypeImage.S32);

		printInner(AutoTypeImage.U8);
		printInner(AutoTypeImage.U16);

		out.print("\n" +
				"}\n");
	}

	private void printInner( AutoTypeImage image ) {
		equalizeLocalNaive(image);
		equalizeLocalInner(image);
		equalizeLocalRow(image);
		equalizeLocalCol(image);
		localHistogram(image);
	}

	private void printPreamble() {
		out.print(
				"import pabeles.concurrency.GrowArray;\n" +
				"import boofcv.misc.BoofMiscOps;\n" +
				"import boofcv.struct.image.*;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p>Functions for enhancing images using the image histogram.</p>\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n\n");
	}

	private void applyTransform_U( AutoTypeImage image ) {
		String typecast = image.getTypeCastFromSum();
		String bitwise = image.getBitWise();

		out.print("\tpublic static void applyTransform( "+image.getSingleBandName()+" input , int transform[] , "+image.getSingleBandName()+" output ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.height,i->{\n" +
				"\t\tfor( int i = 0; i < input.height; i++ ) {\n" +
				"\t\t\tint indexInput = input.startIndex + i*input.stride;\n" +
				"\t\t\tint indexOutput = output.startIndex + i*output.stride;\n" +
				"\n" +
				"\t\t\tfor( int j = 0; j < input.width; j++ ) {\n" +
				"\t\t\t\toutput.data[indexOutput++] = "+typecast+"transform[input.data[indexInput++] "+bitwise+"];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void applyTransform_S( AutoTypeImage image ) {
		String typecast = image.getTypeCastFromSum();

		out.print("\tpublic static void applyTransform( "+image.getSingleBandName()+" input , int transform[] , int minValue , "+image.getSingleBandName()+" output ) {\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,input.height,i->{\n" +
				"\t\tfor( int i = 0; i < input.height; i++ ) {\n" +
				"\t\t\tint indexInput = input.startIndex + i*input.stride;\n" +
				"\t\t\tint indexOutput = output.startIndex + i*output.stride;\n" +
				"\n" +
				"\t\t\tfor( int j = 0; j < input.width; j++ ) {\n" +
				"\t\t\t\toutput.data[indexOutput++] = "+typecast+"transform[input.data[indexInput++]- minValue];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void equalizeLocalNaive( AutoTypeImage image ) {
		String name = image.getSingleBandName();
		String typecast = image.getTypeCastFromSum();
		String bitwise = image.getBitWise();

		out.print("\t/**\n" +
				"\t * Inefficiently computes the local histogram, but can handle every possible case for image size and\n" +
				"\t * local region size\n" +
				"\t */\n" +
				"\tpublic static void equalizeLocalNaive( "+name+" input, int radius, int histogramLength, "+name+" output ,\n" +
				"\t\t\t\t\t\t\t\t\t\t   GrowArray<DogArray_I32> workspaces ) {\n" +
				"\t\tfinal DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tfinal int width = 2*radius + 1;\n" +
				"\t\tfinal int maxValue = histogramLength - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0,input.height,workspaces,(work,idx0,idx1)->{\n" +
				"\t\tint idx0 = 0, idx1 = input.height;\n" +
				"\t\tint[] histogram = BoofMiscOps.checkDeclare(work, histogramLength, false);\n" +
				"\t\tfor( int y = idx0; y < idx1; y++ ) {\n" +
				"\t\t\t// make sure it's inside the image bounds\n" +
				"\t\t\tint y0 = y-radius;\n" +
				"\t\t\tint y1 = y+radius+1;\n" +
				"\t\t\tif( y0 < 0 ) {\n" +
				"\t\t\t\ty0 = 0; y1 = width;\n" +
				"\t\t\t\tif( y1 > input.height )\n" +
				"\t\t\t\t\ty1 = input.height;\n" +
				"\t\t\t} else if( y1 > input.height ) {\n" +
				"\t\t\t\ty1 = input.height;\n" +
				"\t\t\t\ty0 = y1 - width;\n" +
				"\t\t\t\tif( y0 < 0 )\n" +
				"\t\t\t\t\ty0 = 0;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// pixel indexes\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\tfor( int x = 0; x < input.width; x++ ) {\n" +
				"\t\t\t\t// make sure it's inside the image bounds\n" +
				"\t\t\t\tint x0 = x-radius;\n" +
				"\t\t\t\tint x1 = x+radius+1;\n" +
				"\t\t\t\tif( x0 < 0 ) {\n" +
				"\t\t\t\t\tx0 = 0; x1 = width;\n" +
				"\t\t\t\t\tif( x1 > input.width )\n" +
				"\t\t\t\t\t\tx1 = input.width;\n" +
				"\t\t\t\t} else if( x1 > input.width ) {\n" +
				"\t\t\t\t\tx1 = input.width;\n" +
				"\t\t\t\t\tx0 = x1 - width;\n" +
				"\t\t\t\t\tif( x0 < 0 )\n" +
				"\t\t\t\t\t\tx0 = 0;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// compute the local histogram\n" +
				"\t\t\t\tlocalHistogram(input,x0,y0,x1,y1,histogram);\n" +
				"\n" +
				"\t\t\t\t// only need to compute up to the value of the input pixel\n" +
				"\t\t\t\tint inputValue =  input.data[indexIn++] "+bitwise+";\n" +
				"\t\t\t\tint sum = 0;\n" +
				"\t\t\t\tfor( int i = 0; i <= inputValue; i++ ) {\n" +
				"\t\t\t\t\tsum += histogram[i];\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tint area = (y1-y0)*(x1-x0);\n" +
				"\t\t\t\toutput.data[indexOut++] = "+typecast+"((sum*maxValue)/area);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE }});\n" +
				"\t}\n\n");
	}

	private void equalizeLocalInner( AutoTypeImage image ) {
		String name = image.getSingleBandName();
		String typecast = image.getTypeCastFromSum();
		String bitwise = image.getBitWise();

		out.print(
				"\t/**\n" +
				"\t * Performs local histogram equalization just on the inner portion of the image\n" +
				"\t */\n" +
				"\tpublic static void equalizeLocalInner( "+name+" input, int radius, int histogramLength, "+name+" output,\n" +
				"\t\t\t\t\t\t\t\t\t\t GrowArray<DogArray_I32> workspaces ) {\n" +
				"\n" +
				"\t\tfinal DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\tint width = 2*radius+1;\n" +
				"\t\tint area = width*width;\n" +
				"\t\tint maxValue = histogramLength - 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius,input.height-radius,workspaces,(work,y0,y1)->{\n" +
				"\t\tint y0 = radius, y1 = input.height-radius;\n" +
				"\t\tint[] histogram = BoofMiscOps.checkDeclare(work, histogramLength, false);\n" +
				"\t\tfor( int y = y0; y < y1; y++ ) {\n" +
				"\t\t\tlocalHistogram(input,0,y-radius,width,y+radius+1,histogram);\n" +
				"\n" +
				"\t\t\t// compute equalized pixel value using the local histogram\n" +
				"\t\t\tint inputValue = input.unsafe_get(radius, y);\n" +
				"\t\t\tint sum = 0;\n" +
				"\t\t\tfor( int i = 0; i <= inputValue; i++ ) {\n" +
				"\t\t\t\tsum += histogram[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\toutput.set(radius,y, (sum*maxValue)/area );\n" +
				"\n" +
				"\t\t\t// start of old and new columns in histogram region\n" +
				"\t\t\tint indexOld = input.startIndex + y*input.stride;\n" +
				"\t\t\tint indexNew = indexOld+width;\n" +
				"\n" +
				"\t\t\t// index of pixel being examined\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride+radius+1;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride+radius+1;\n" +
				"\n" +
				"\t\t\tfor( int x = radius+1; x < input.width-radius; x++ ) {\n" +
				"\n" +
				"\t\t\t\t// update local histogram by removing the left column\n" +
				"\t\t\t\tfor (int i = -radius; i <= radius; i++) {\n" +
				"\t\t\t\t\thistogram[input.data[indexOld + i*input.stride] "+bitwise+"]--;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// update local histogram by adding the right column\n" +
				"\t\t\t\tfor (int i = -radius; i <= radius; i++) {\n" +
				"\t\t\t\t\thistogram[input.data[indexNew + i*input.stride] "+bitwise+"]++;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\t// compute equalized pixel value using the local histogram\n" +
				"\t\t\t\tinputValue =  input.data[indexIn++] "+bitwise+";\n" +
				"\t\t\t\tsum = 0;\n" +
				"\t\t\t\tfor (int i = 0; i <= inputValue; i++) {\n" +
				"\t\t\t\t\tsum += histogram[i];\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\toutput.data[indexOut++] = "+typecast+"((sum*maxValue)/area);\n" +
				"\n" +
				"\t\t\t\tindexOld++;\n" +
				"\t\t\t\tindexNew++;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE }});\n" +
				"\t}\n\n");
	}

	private void equalizeLocalRow( AutoTypeImage image ) {
		String name = image.getSingleBandName();
		String typecast = image.getTypeCastFromSum();
		String bitwise = image.getBitWise();

		out.println("\t/**\n" +
				"\t * Local equalization along a row. Image must be at least the histogram's width (2*r+1) in width and height.\n" +
				"\t */\n" +
				"\tpublic static void equalizeLocalRow( "+name+" input, int radius, int histogramLength, int startY, "+name+" output,\n" +
				"\t\t\t\t\t\t\t\t\t\t GrowArray<DogArray_I32> workspaces ) {\n" +
				"\n" +
				"\t\tint width = 2*radius+1;\n" +
				"\t\tint area = width*width;\n" +
				"\t\tint maxValue = histogramLength - 1;\n" +
				"\n" +
				"\t\tworkspaces.reset();\n" +
				"\t\tint[] histogram = BoofMiscOps.checkDeclare(workspaces.grow(), histogramLength, false);\n" +
				"\t\tint[] transform = BoofMiscOps.checkDeclare(workspaces.grow(), histogramLength, false);\n" +
				"\n" +
				"\t\t// specify the top and bottom of the histogram window and make sure it is inside bounds\n" +
				"\t\tint hist0 = startY;\n" +
				"\t\tint hist1 = startY+width;\n" +
				"\t\tif (hist1 > input.height) {\n" +
				"\t\t\thist1 = input.height;\n" +
				"\t\t\thist0 = hist1 - width;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// the upper and lower bounds of the region being equalized\n" +
				"\t\tint region0 = startY;\n" +
				"\t\tint region1 = startY+radius;\n" +
				"\n" +
				"\t\t// local histogram and transformation\n" +
				"\t\tlocalHistogram(input,0,hist0,width,hist1,histogram);\n" +
				"\n" +
				"\t\tint sum = 0;\n" +
				"\t\tfor (int i = 0; i < histogram.length; i++) {\n" +
				"\t\t\ttransform[i] = sum += histogram[i];\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// equalize the first square region\n" +
				"\t\tfor (int y = region0; y < region1; y++) {\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\tfor (int x = 0; x <= radius; x++) {\n" +
				"\t\t\t\tint inputValue =  input.data[indexIn++] & 0xff;\n" +
				"\t\t\t\toutput.data[indexOut++] = "+typecast+"((transform[ inputValue ]*maxValue)/area);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// move right while equalizing the columns one at a time\n" +
				"\t\tfor (int x = radius+1; x < input.width-radius-1; x++) {\n" +
				"\n" +
				"\t\t\t// remove the left most column\n" +
				"\t\t\tint indexIn = input.startIndex + x-radius-1;\n" +
				"\t\t\tfor( int y = hist0; y < hist1; y++ ) {\n" +
				"\t\t\t\thistogram[input.data[indexIn + y*input.stride] "+bitwise+"]--;\n" +
				"\t\t\t}\n" +
				"\t\t\t// add the right most column\n" +
				"\t\t\tindexIn += width;\n" +
				"\t\t\tfor( int y = hist0; y < hist1; y++ ) {\n" +
				"\t\t\t\thistogram[input.data[indexIn + y*input.stride] "+bitwise+"]++;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// compute transformation table\n" +
				"\t\t\tsum = 0;\n" +
				"\t\t\tfor (int i = 0; i < histogram.length; i++) {\n" +
				"\t\t\t\ttransform[i] = sum += histogram[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// compute the output down the column\n" +
				"\t\t\tindexIn = input.startIndex + region0*input.stride + x;\n" +
				"\t\t\tint indexOut = output.startIndex + region0*output.stride + x;\n" +
				"\t\t\tfor (int y = 0; y < radius; y++) {\n" +
				"\t\t\t\tint inputValue =  input.data[indexIn] & 0xff;\n" +
				"\t\t\t\toutput.data[indexOut] = "+typecast+"((transform[ inputValue ]*maxValue)/area);\n" +
				"\n" +
				"\t\t\t\tindexIn += input.stride;\n" +
				"\t\t\t\tindexOut += output.stride;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// equalize the final square region\n" +
				"\t\tlocalHistogram(input,input.width-width,hist0,input.width,hist1,histogram);\n" +
				"\n" +
				"\t\tsum = 0;\n" +
				"\t\tfor (int i = 0; i < histogram.length; i++) {\n" +
				"\t\t\ttransform[i] = sum += histogram[i];\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor (int y = region0; y < region1; y++) {\n" +
				"\t\t\tint x = input.width-radius-1;\n" +
				"\n" +
				"\t\t\tint indexIn = input.startIndex + y*input.stride + x;\n" +
				"\t\t\tint indexOut = output.startIndex + y*output.stride + x;\n" +
				"\n" +
				"\t\t\tfor (; x < input.width; x++) {\n" +
				"\t\t\t\tint inputValue =  input.data[indexIn++] & 0xff;\n" +
				"\t\t\t\toutput.data[indexOut++] = "+typecast+"((transform[ inputValue ]*maxValue)/area);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t}\n\n");

	}

	private void equalizeLocalCol( AutoTypeImage image ) {
		String name = image.getSingleBandName();
		String typecast = image.getTypeCastFromSum();
		String bitwise = image.getBitWise();

		out.print("\t/**\n" +
				"\t * Local equalization along a column. Image must be at least the histogram's width (2*r+1) in width and height.\n" +
				"\t */\n" +
				"\tpublic static void equalizeLocalCol( "+name+" input, int radius, int histogramLength, int startX, "+name+" output,\n" +
				"\t\t\t\t\t\t\t\t\t\t GrowArray<DogArray_I32> workspaces ) {\n" +
				"\n" +
				"\t\tint width = 2*radius+1;\n" +
				"\t\tint area = width*width;\n" +
				"\t\tint maxValue = histogramLength - 1;\n" +
				"\n" +
				"\t\tworkspaces.reset();\n" +
				"\t\tint[] histogram = BoofMiscOps.checkDeclare(workspaces.grow(), maxValue, false);\n" +
				"\t\tint[] transform = BoofMiscOps.checkDeclare(workspaces.grow(), maxValue, false);\n" +
				"\n" +
				"\t\t// specify the top and bottom of the histogram window and make sure it is inside bounds\n" +
				"\t\tint hist0 = startX;\n" +
				"\t\tint hist1 = startX+width;\n" +
				"\t\tif( hist1 > input.width ) {\n" +
				"\t\t\thist1 = input.width;\n" +
				"\t\t\thist0 = hist1 - width;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// initialize the histogram. ignore top border\n" +
				"\t\tlocalHistogram(input,hist0,0,hist1,width,histogram);\n" +
				"\n" +
				"\t\t// compute transformation table\n" +
				"\t\tint sum = 0;\n" +
				"\t\tfor (int i = 0; i < histogram.length; i++) {\n" +
				"\t\t\ttransform[i] = sum += histogram[i];\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// compute the output across the row\n" +
				"\t\tint indexIn = input.startIndex + radius*input.stride + startX;\n" +
				"\t\tint indexOut = output.startIndex + radius*output.stride + startX;\n" +
				"\t\tfor (int x = 0; x < radius; x++) {\n" +
				"\t\t\tint inputValue =  input.data[indexIn++] & 0xff;\n" +
				"\t\t\toutput.data[indexOut++] = "+typecast+"((transform[ inputValue ]*maxValue)/area);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// move down while equalizing the rows one at a time\n" +
				"\t\tfor (int y = radius+1; y < input.height-radius; y++) {\n" +
				"\n" +
				"\t\t\t// remove the top most row\n" +
				"\t\t\tindexIn = input.startIndex + (y-radius-1)*input.stride;\n" +
				"\t\t\tfor (int x = hist0; x < hist1; x++) {\n" +
				"\t\t\t\thistogram[input.data[indexIn + x] "+bitwise+"]--;\n" +
				"\t\t\t}\n" +
				"\t\t\t// add the bottom most row\n" +
				"\t\t\tindexIn += width*input.stride;\n" +
				"\t\t\tfor (int x = hist0; x < hist1; x++) {\n" +
				"\t\t\t\thistogram[input.data[indexIn + x]"+bitwise+"]++;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// compute transformation table\n" +
				"\t\t\tsum = 0;\n" +
				"\t\t\tfor (int i = 0; i < histogram.length; i++) {\n" +
				"\t\t\t\ttransform[i] = sum += histogram[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// compute the output across the row\n" +
				"\t\t\tindexIn = input.startIndex + y*input.stride + startX;\n" +
				"\t\t\tindexOut = output.startIndex + y*output.stride + startX;\n" +
				"\t\t\tfor (int x = 0; x < radius; x++) {\n" +
				"\t\t\t\tint inputValue =  input.data[indexIn++] & 0xff;\n" +
				"\t\t\t\toutput.data[indexOut++] = "+typecast+"((transform[ inputValue ]*maxValue)/area);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");

	}

	private void localHistogram( AutoTypeImage image ) {
		String name = image.getSingleBandName();
		String bitwise = image.getBitWise();

		out.print("\t/**\n" +
				"\t * Computes the local histogram just for the specified inner region\n" +
				"\t */\n" +
				"\tpublic static void localHistogram( "+name+" input, int x0, int y0, int x1, int y1, int[] histogram ) {\n" +
				"\t\tfor (int i = 0; i < histogram.length; i++)\n" +
				"\t\t\thistogram[i] = 0;\n" +
				"\n" +
				"\t\tfor (int i = y0; i < y1; i++) {\n" +
				"\t\t\tint index = input.startIndex + i*input.stride + x0;\n" +
				"\t\t\tint end = index + x1-x0;\n" +
				"\t\t\tfor( ; index < end; index++ ) {\n" +
				"\t\t\t\thistogram[input.data[index] "+bitwise+"]++;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplEnhanceHistogram app = new GenerateImplEnhanceHistogram();
		app.setModuleName("boofcv-ip");
		app.parseArguments(args);
		app.generate();
	}
}
