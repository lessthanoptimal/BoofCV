/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.alg.filter.binary.impl.ImplBinaryInnerOps;
import boofcv.alg.filter.binary.impl.ImplBinaryNaiveOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkBinaryOps  {
	static int imgWidth = 640;
	static int imgHeight = 480;

	static GrayU8 input = new GrayU8(imgWidth, imgHeight);
	static GrayU8 output = new GrayU8(imgWidth, imgHeight);

	public BenchmarkBinaryOps() {
		Random rand = new Random(234);
		// test structures and unstructured images
		// naive is some times faster in unstructured because it can escape earlier
		ImageMiscOps.fillUniform(input, rand, 0, 1);
//		ImageMiscOps.fillRectangle(input,1,100,200,150,100);
	}

	public int timeNaiveErode4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.erode4(input, output);
		return 0;
	}

	public int timeNaiveErode8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.erode8(input, output);
		return 0;
	}

	public int timeNaiveDilate4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.dilate4(input, output);
		return 0;
	}

	public int timeNaiveDilate8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.dilate8(input, output);
		return 0;
	}

	public int timeNaiveEdge4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.edge4(input, output);
		return 0;
	}

	public int timeNaiveEdge8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.edge8(input, output);
		return 0;
	}

	public int timeNaiveRemovePointNoise(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryNaiveOps.removePointNoise(input, output);
		return 0;
	}

	public int timeInnerErode4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.erode4(input, output);
		return 0;
	}

	public int timeInnerErode8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.erode8(input, output);
		return 0;
	}

	public int timeInnerDilate4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.dilate4(input, output);
		return 0;
	}

	public int timeInnerDilate8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.dilate8(input, output);
		return 0;
	}

	public int timeInnerEdge4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.edge4(input, output);
		return 0;
	}

	public int timeInnerEdge8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.edge8(input, output);
		return 0;
	}

	public int timeInnerRemovePointNoise(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryInnerOps.removePointNoise(input, output);
		return 0;
	}

	public int timeErode4(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.erode4(input, 1, output);
		return 0;
	}

	public int timeErode8(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.erode8(input, 1, output);
		return 0;
	}

	public int timeDilate4(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.dilate4(input, 1, output);
		return 0;
	}

	public int timeDilate8(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.dilate8(input, 1, output);
		return 0;
	}

	public int timeEdge4(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.edge4(input, output);
		return 0;
	}

	public int timeEdge8(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.edge8(input, output);
		return 0;
	}

	public int timeRemovePointNoise(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.removePointNoise(input, output);
		return 0;
	}

	public static void main(String args[]) {
		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");

//		Runner.main(BenchmarkBinaryOps.class, args);
	}
}
