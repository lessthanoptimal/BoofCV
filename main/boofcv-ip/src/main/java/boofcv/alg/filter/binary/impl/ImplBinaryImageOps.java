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

package boofcv.alg.filter.binary.impl;

import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation for all operations which are not seperated by inner and outer algorithms
 */
public class ImplBinaryImageOps {
	public static void logicAnd( GrayU8 inputA, GrayU8 inputB, GrayU8 output ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, inputA.height, y -> {
		for (int y = 0; y < inputA.height; y++) {
			int indexA = inputA.startIndex + y*inputA.stride;
			int indexB = inputB.startIndex + y*inputB.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = indexA + inputA.width;
			for (; indexA < end; indexA++, indexB++, indexOut++) {
				int valA = inputA.data[indexA];
				output.data[indexOut] = valA == 1 && valA == inputB.data[indexB] ? (byte)1 : (byte)0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void logicOr( GrayU8 inputA, GrayU8 inputB, GrayU8 output ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, inputA.height, y -> {
		for (int y = 0; y < inputA.height; y++) {
			int indexA = inputA.startIndex + y*inputA.stride;
			int indexB = inputB.startIndex + y*inputB.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = indexA + inputA.width;
			for (; indexA < end; indexA++, indexB++, indexOut++) {
				output.data[indexOut] = inputA.data[indexA] == 1 || 1 == inputB.data[indexB] ? (byte)1 : (byte)0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void logicXor( GrayU8 inputA, GrayU8 inputB, GrayU8 output ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, inputA.height, y -> {
		for (int y = 0; y < inputA.height; y++) {
			int indexA = inputA.startIndex + y*inputA.stride;
			int indexB = inputB.startIndex + y*inputB.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = indexA + inputA.width;
			for (; indexA < end; indexA++, indexB++, indexOut++) {
				output.data[indexOut] = inputA.data[indexA] != inputB.data[indexB] ? (byte)1 : (byte)0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void invert( GrayU8 input, GrayU8 output ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = index + input.width;
			for (; index < end; index++, indexOut++) {
				output.data[indexOut] = input.data[index] == 0 ? (byte)1 : (byte)0;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void relabel( GrayS32 input, int labels[] ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int index = input.startIndex + y*input.stride;
			int end = index + input.width;

			for (; index < end; index++) {
				int val = input.data[index];
				input.data[index] = labels[val];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void labelToBinary( GrayS32 labelImage, GrayU8 binaryImage ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, labelImage.height, y -> {
		for (int y = 0; y < labelImage.height; y++) {

			int indexIn = labelImage.startIndex + y*labelImage.stride;
			int indexOut = binaryImage.startIndex + y*binaryImage.stride;

			int end = indexIn + labelImage.width;

			for (; indexIn < end; indexIn++, indexOut++) {
				if (0 == labelImage.data[indexIn]) {
					binaryImage.data[indexOut] = 0;
				} else {
					binaryImage.data[indexOut] = 1;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void labelToBinary( GrayS32 labelImage, GrayU8 binaryImage,
									  boolean selectedBlobs[] ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, labelImage.height, y -> {
		for (int y = 0; y < labelImage.height; y++) {

			int indexIn = labelImage.startIndex + y*labelImage.stride;
			int indexOut = binaryImage.startIndex + y*binaryImage.stride;

			int end = indexIn + labelImage.width;

			for (; indexIn < end; indexIn++, indexOut++) {
				int val = labelImage.data[indexIn];
				if (selectedBlobs[val]) {
					binaryImage.data[indexOut] = 1;
				} else {
					binaryImage.data[indexOut] = 0;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}
}
