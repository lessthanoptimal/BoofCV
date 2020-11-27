/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * @author Peter Abeles
 */
public class GenerateImplThresholdImageOps extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printAll(AutoTypeImage.F32);
		printAll(AutoTypeImage.F64);
		printAll(AutoTypeImage.U8);
		printAll(AutoTypeImage.S16);
		printAll(AutoTypeImage.U16);
		printAll(AutoTypeImage.S32);

		printLocal(AutoTypeImage.U8);
		printLocal(AutoTypeImage.U16);
		printLocal(AutoTypeImage.F32);

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(
				"import javax.annotation.Generated;\n" +
				"\n" +
				"import pabeles.concurrency.GrowArray;\n" +
				"import org.ddogleg.struct.DogArray_F32;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import org.jetbrains.annotations.Nullable;\n" +
				"import boofcv.struct.image.*;\n" +
				"import boofcv.alg.filter.blur.BlurImageOps;\n" +
				"import boofcv.struct.ConfigLength;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * <p>Operations for thresholding images and converting them into a binary image.</p>\n" +
				generateDocString("Peter Abeles") +
				"@SuppressWarnings(\"Duplicates\")\n" +
				"public class "+className+" {\n\n");

	}

	public void printAll( AutoTypeImage imageIn ) {
		printThreshold(imageIn);
	}

	public void printLocal(AutoTypeImage imageIn) {
		printLocalMean(imageIn);
		printLocalGaussian(imageIn);
	}

	public void printThreshold( AutoTypeImage imageIn ) {
		out.print(
				"\tpublic static GrayU8 threshold( "+imageIn.getSingleBandName()+" input , GrayU8 output ,\n" +
				"\t\t\t\t\t\t\t\t\t\t"+imageIn.getSumType()+" threshold , boolean down )\n" +
				"\t{\n" +
				"\t\tif( down ) {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\t\tfor( int i = input.width; i>0; i-- ) {\n" +
				"\t\t\t\t\toutput.data[indexOut++] = (byte)((input.data[indexIn++]"+imageIn.getBitWise()+") <= threshold ? 1 : 0);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t} else {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\n" +
				"\t\t\t\tfor( int i = input.width; i>0; i-- ) {\n" +
				"\t\t\t\t\toutput.data[indexOut++] = (byte)((input.data[indexIn++]"+imageIn.getBitWise()+") > threshold ? 1 : 0);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	public void printLocalMean(AutoTypeImage imageIn) {

		String imageName = imageIn.getSingleBandName();
		String bitwise = imageIn.getBitWise();

		String workType = ("DogArray_"+imageIn.getKernelType()).replace("S32","I32");

		out.print(
				"\tpublic static GrayU8 localMean( "+imageName+" input , GrayU8 output ,\n" +
				"\t\t\t\t\t\t\t\t\t\t\t ConfigLength width , float scale , boolean down ,\n" +
				"\t\t\t\t\t\t\t\t\t\t\t "+imageName+" storage1 , "+imageName+" storage2 ,\n" +
				"\t\t\t\t\t\t\t\t\t\t\t @Nullable GrowArray<"+workType+"> storage3 ) {\n" +
				"\n" +
				"\t\tint radius = width.computeI(Math.min(input.width,input.height))/2;\n" +
				"\n" +
				"\t\t"+imageName+" mean = storage1;\n" +
				"\n" +
				"\t\tBlurImageOps.mean(input,mean,radius,storage2,storage3);\n" +
				"\n" +
				"\t\tif( down ) {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\t\t\t\tint indexMean = mean.startIndex + y*mean.stride;\n" +
				"\n" +
				"\t\t\t\tint end = indexIn + input.width;\n" +
				"\n" +
				"\t\t\t\twhile(indexIn < end) {\n" +
				"\t\t\t\t\tfloat threshold = (mean.data[indexMean++]"+bitwise+") * scale;\n" +
				"\t\t\t\t\toutput.data[indexOut++] = (input.data[indexIn++]"+bitwise+") <= threshold ? (byte)1:0;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t} else {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\t\t\t\tint indexMean = mean.startIndex + y*mean.stride;\n" +
				"\n" +
				"\t\t\t\tint end = indexIn + input.width;\n" +
				"\n" +
				"\t\t\t\twhile(indexIn < end) {\n" +
				"\t\t\t\t\tfloat threshold = (mean.data[indexMean++]"+bitwise+");\n" +
				"\t\t\t\t\toutput.data[indexOut++] = (input.data[indexIn++]"+bitwise+")*scale > threshold ? (byte)1:0;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");
	}

	public void printLocalGaussian(AutoTypeImage imageIn) {

		String imageName = imageIn.getSingleBandName();
		String sumType = imageIn.getSumType();
		String bitwise = imageIn.getBitWise();

		out.print(
				"\tpublic static GrayU8 localGaussian( "+imageName+" input , GrayU8 output ,\n" +
				"\t\t\t\t\t\t\t\t\t\tConfigLength width , float scale , boolean down ,\n" +
				"\t\t\t\t\t\t\t\t\t\t"+imageName+" storage1 , "+imageName+" storage2 ) {\n" +
				"\n" +
				"\t\tint radius = width.computeI(Math.min(input.width,input.height))/2;\n" +
				"\n" +
				"\t\t"+imageName+" blur = storage1;\n" +
				"\n" +
				"\t\tBlurImageOps.gaussian(input,blur,-1,radius,storage2);\n" +
				"\n" +
				"\t\tif( down ) {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\t\t\t\tint indexMean = blur.startIndex + y*blur.stride;\n" +
				"\n" +
				"\t\t\t\tint end = indexIn + input.width;\n" +
				"\n" +
				"\t\t\t\tfor( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {\n" +
				"\t\t\t\t\tfloat threshold = (blur.data[indexMean]"+bitwise+") * scale;\n" +
				"\n" +
				"\t\t\t\t\tif( (input.data[indexIn]"+bitwise+") <= threshold )\n" +
				"\t\t\t\t\t\toutput.data[indexOut] = 1;\n" +
				"\t\t\t\t\telse\n" +
				"\t\t\t\t\t\toutput.data[indexOut] = 0;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t} else {\n" +
				"\t\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {\n" +
				"\t\t\tfor( int y = 0; y < input.height; y++ ) {\n" +
				"\t\t\t\tint indexIn = input.startIndex + y*input.stride;\n" +
				"\t\t\t\tint indexOut = output.startIndex + y*output.stride;\n" +
				"\t\t\t\tint indexMean = blur.startIndex + y*blur.stride;\n" +
				"\n" +
				"\t\t\t\tint end = indexIn + input.width;\n" +
				"\n" +
				"\t\t\t\tfor( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {\n" +
				"\t\t\t\t\t"+sumType+" threshold = (blur.data[indexMean]"+bitwise+");\n" +
				"\n" +
				"\t\t\t\t\tif( (input.data[indexIn]"+bitwise+") * scale > threshold )\n" +
				"\t\t\t\t\t\toutput.data[indexOut] = 1;\n" +
				"\t\t\t\t\telse\n" +
				"\t\t\t\t\t\toutput.data[indexOut] = 0;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t\t//CONCURRENT_ABOVE });\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn output;\n" +
				"\t}\n\n");

	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplThresholdImageOps app = new GenerateImplThresholdImageOps();
		app.setModuleName("boofcv-ip");
		app.parseArguments(args);
		app.generate();
	}
}
