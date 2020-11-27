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

package boofcv.alg.transform.census.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplCensusTransformInner extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		for( AutoTypeImage type : new AutoTypeImage[]{AutoTypeImage.U8,AutoTypeImage.U16,AutoTypeImage.F32}) {
			print3x3(type);
			print5x5(type);
			sample_S64(type);
			sample_IU16(type);
		}

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(
				"import boofcv.struct.image.*;\n" +
				"import boofcv.struct.image.InterleavedU16;\n" +
				"import org.ddogleg.struct.DogArray_I32;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n" +
				"\n" +
				"/**\n" +
				" * Implementations of Census transform.\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" {\n");
	}

	private void print3x3(AutoTypeImage src ) {
		String bitwise = src.getBitWise();
		String dataType = src.getDataType();
		String sumType = src.getSumType();

		out.print("\tpublic static void dense3x3( final "+src.getSingleBandName()+" input , final GrayU8 output ) {\n" +
				"\t\tfinal int height = input.height - 1;\n" +
				"\t\tfinal "+dataType+"[] src = input.data;\n" +
				"\n" +
				"\t\t// pre-compute offsets to pixels. row-major starting from upper row\n" +
				"\t\tfinal int offset0 = -input.stride - 1;\n" +
				"\t\tfinal int offset1 = -input.stride;\n" +
				"\t\tfinal int offset2 = -input.stride + 1;\n" +
				"\t\tfinal int offset3 = -1;\n" +
				"\t\tfinal int offset5 = +1;\n" +
				"\t\tfinal int offset6 = input.stride - 1;\n" +
				"\t\tfinal int offset7 = input.stride;\n" +
				"\t\tfinal int offset8 = input.stride + 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{\n" +
				"\t\tfor (int y = 1; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride + 1;\n" +
				"\t\t\tint indexDst = output.startIndex + y*output.stride + 1;\n" +
				"\n" +
				"\t\t\tfinal int end = indexDst + input.width - 2;\n" +
				"//\t\t\tfor (int x = 1; x < width-1; x++) {\n" +
				"\t\t\twhile (indexDst < end) {\n" +
				"\t\t\t\t"+sumType+" center = src[indexSrc]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\tint census = 0;\n" +
				"\n" +
				"\t\t\t\tif ((src[indexSrc + offset0]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x01;\n" +
				"\t\t\t\tif ((src[indexSrc + offset1]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x02;\n" +
				"\t\t\t\tif ((src[indexSrc + offset2]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x04;\n" +
				"\t\t\t\tif ((src[indexSrc + offset3]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x08;\n" +
				"\t\t\t\tif ((src[indexSrc + offset5]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x10;\n" +
				"\t\t\t\tif ((src[indexSrc + offset6]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x20;\n" +
				"\t\t\t\tif ((src[indexSrc + offset7]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x40;\n" +
				"\t\t\t\tif ((src[indexSrc + offset8]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x80;\n" +
				"\n" +
				"\t\t\t\toutput.data[indexDst] = (byte)census;\n" +
				"\n" +
				"\t\t\t\tindexDst++;\n" +
				"\t\t\t\tindexSrc++;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	private void print5x5( AutoTypeImage src ) {
		String bitwise = src.getBitWise();
		String dataType = src.getDataType();
		String sumType = src.getSumType();

		out.print("\tpublic static void dense5x5( final "+src.getSingleBandName()+" input , final GrayS32 output ) {\n" +
				"\t\tfinal int height = input.height-2;\n" +
				"\t\tfinal "+dataType+"[] src = input.data;\n" +
				"\n" +
				"\t\t// pre-compute offsets to pixels. row-major starting from upper row\n" +
				"\t\tfinal int offset00 = -2*input.stride-2;\n" +
				"\t\tfinal int offset01 = -2*input.stride-1;\n" +
				"\t\tfinal int offset02 = -2*input.stride;\n" +
				"\t\tfinal int offset03 = -2*input.stride+1;\n" +
				"\t\tfinal int offset04 = -2*input.stride+2;\n" +
				"\t\tfinal int offset10 = -input.stride-2;\n" +
				"\t\tfinal int offset11 = -input.stride-1;\n" +
				"\t\tfinal int offset12 = -input.stride;\n" +
				"\t\tfinal int offset13 = -input.stride+1;\n" +
				"\t\tfinal int offset14 = -input.stride+2;\n" +
				"\t\tfinal int offset20 = -2;\n" +
				"\t\tfinal int offset21 = -1;\n" +
				"\t\tfinal int offset23 = +1;\n" +
				"\t\tfinal int offset24 = +2;\n" +
				"\t\tfinal int offset30 = input.stride-2;\n" +
				"\t\tfinal int offset31 = input.stride-1;\n" +
				"\t\tfinal int offset32 = input.stride;\n" +
				"\t\tfinal int offset33 = input.stride+1;\n" +
				"\t\tfinal int offset34 = input.stride+2;\n" +
				"\t\tfinal int offset40 = 2*input.stride-2;\n" +
				"\t\tfinal int offset41 = 2*input.stride-1;\n" +
				"\t\tfinal int offset42 = 2*input.stride;\n" +
				"\t\tfinal int offset43 = 2*input.stride+1;\n" +
				"\t\tfinal int offset44 = 2*input.stride+2;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(2,height,y->{\n" +
				"\t\tfor (int y = 2; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride + 2;\n" +
				"\t\t\tint indexDst = output.startIndex + y*output.stride + 2;\n" +
				"\n" +
				"\t\t\tfinal int end = indexDst + input.width - 4;\n" +
				"//\t\t\tfor (int x = 2; x < width - 2; x++) {\n" +
				"\t\t\twhile (indexDst < end) {\n" +
				"\t\t\t\t"+sumType+" center = src[indexSrc]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\tint census = 0;\n" +
				"\n" +
				"\t\t\t\tif ((src[indexSrc + offset00]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000001;\n" +
				"\t\t\t\tif ((src[indexSrc + offset01]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000002;\n" +
				"\t\t\t\tif ((src[indexSrc + offset02]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000004;\n" +
				"\t\t\t\tif ((src[indexSrc + offset03]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000008;\n" +
				"\t\t\t\tif ((src[indexSrc + offset04]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000010;\n" +
				"\t\t\t\tif ((src[indexSrc + offset10]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000020;\n" +
				"\t\t\t\tif ((src[indexSrc + offset11]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000040;\n" +
				"\t\t\t\tif ((src[indexSrc + offset12]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000080;\n" +
				"\t\t\t\tif ((src[indexSrc + offset13]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000100;\n" +
				"\t\t\t\tif ((src[indexSrc + offset14]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000200;\n" +
				"\t\t\t\tif ((src[indexSrc + offset20]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000400;\n" +
				"\t\t\t\tif ((src[indexSrc + offset21]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x000800;\n" +
				"\t\t\t\tif ((src[indexSrc + offset23]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x001000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset24]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x002000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset30]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x004000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset31]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x008000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset32]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x010000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset33]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x020000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset34]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x040000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset40]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x080000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset41]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x100000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset42]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x200000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset43]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x400000;\n" +
				"\t\t\t\tif ((src[indexSrc + offset44]"+bitwise+") > center)\n" +
				"\t\t\t\t\tcensus |= 0x800000;\n" +
				"\n" +
				"\t\t\t\toutput.data[indexDst] = census;\n" +
				"\n" +
				"\t\t\t\tindexDst++;\n" +
				"\t\t\t\tindexSrc++;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	void sample_S64( AutoTypeImage src ) {
		String bitwise = src.getBitWise();
		String dataType = src.getDataType();
		String sumType = src.getSumType();

		out.print("\tpublic static void sample_S64( final "+src.getSingleBandName()+" input , final int radius , final DogArray_I32 offsets,\n" +
				"\t\t\t\t\t\t\t\t  final GrayS64 output ) {\n" +
				"\t\tfinal int height = input.height-radius;\n" +
				"\t\tfinal "+dataType+"[] src = input.data;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(radius,height,y->{\n" +
				"\t\tfor (int y = radius; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y * input.stride + radius;\n" +
				"\t\t\tint indexDst = output.startIndex + y * output.stride + radius;\n" +
				"\n" +
				"\t\t\tfinal int end = indexDst + input.width - 2*radius;\n" +
				"//\t\t\tfor (int x = radius; x < width-radius; x++) {\n" +
				"\t\t\twhile (indexDst < end) {\n" +
				"\t\t\t\t"+sumType+" center = src[indexSrc]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\tlong census = 0;\n" +
				"\t\t\t\tint bit = 1;\n" +
				"\t\t\t\tfor (int i = 0; i < offsets.size; i++) {\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[i]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= bit;\n" +
				"\t\t\t\t\tbit <<= 1;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\toutput.data[indexDst++] = census;\n" +
				"\t\t\t\tindexSrc++;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	void sample_IU16( AutoTypeImage src ) {
		String bitwise = src.getBitWise();
		String dataType = src.getDataType();
		String sumType = src.getSumType();

		out.print("\tpublic static void sample_IU16( final "+src.getSingleBandName()+" input , final int radius , final DogArray_I32 offsets,\n" +
				"\t\t\t\t\t\t\t\t   final InterleavedU16 output ) {\n" +
				"\t\tfinal int height = input.height-radius;\n" +
				"\t\tfinal "+dataType+"[] src = input.data;\n" +
				"\n" +
				"\t\tint bitBlocks = offsets.size/16;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(radius,height,y->{\n" +
				"\t\tfor (int y = radius; y < height; y++) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride + radius;\n" +
				"\t\t\tint indexDst = output.startIndex + y*output.stride + radius*output.numBands;\n" +
				"\n" +
				"\t\t\tfinal int end = indexDst + (input.width - 2*radius)*output.numBands;\n" +
				"//\t\t\tfor (int x = radius; x < width-radius; x++) {\n" +
				"\t\t\twhile (indexDst < end) {\n" +
				"\t\t\t\t"+sumType+" center = src[indexSrc]"+bitwise+";\n" +
				"\n" +
				"\t\t\t\tint idx = 0;\n" +
				"\t\t\t\tfor (int block = 0; block < bitBlocks; block++) {\n" +
				"\t\t\t\t\tshort census = 0;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0001;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0002;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0004;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0008;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0010;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0020;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0040;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0080;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0100;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0200;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0400;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x0800;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x1000;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x2000;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x4000;\n" +
				"\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\tcensus |= 0x8000;\n" +
				"\n" +
				"\t\t\t\t\toutput.data[indexDst++] = census;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tif( idx != offsets.size ) {\n" +
				"\t\t\t\t\tshort census = 0;\n" +
				"\t\t\t\t\tint bit = 1;\n" +
				"\t\t\t\t\twhile (idx < offsets.size) {\n" +
				"\t\t\t\t\t\tif ((src[indexSrc + offsets.data[idx++]]"+bitwise+") > center)\n" +
				"\t\t\t\t\t\t\tcensus |= bit;\n" +
				"\t\t\t\t\t\tbit <<= 1;\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t\toutput.data[indexDst++] = census;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tindexSrc++;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplCensusTransformInner app = new GenerateImplCensusTransformInner();
		app.generateCode();
	}
}
