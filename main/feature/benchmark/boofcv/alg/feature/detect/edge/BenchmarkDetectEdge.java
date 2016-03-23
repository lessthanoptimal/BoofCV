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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkDetectEdge {
	static final long TEST_TIME = 1000;

	int width = 640;
	int height = 480;

	Random rand = new Random(234);

	GrayF32 input = new GrayF32(width,height);

	public void createImage() {
		for( int i = 0; i < 1000; i++ ) {
			int width = 10+rand.nextInt(50);
			int height = 10+rand.nextInt(50);

			int x = rand.nextInt(input.width);
			int y = rand.nextInt(input.height);

			int x1 = x+width;
			int y1 = y+height;
			if( x1 > input.width ) x1 = input.width;
			if( y1 > input.height ) y1 = input.height;

			width = x1-x;
			height = y1-y;

			ImageMiscOps.fillRectangle(input,rand.nextInt(100),x,y,width,height);
		}
	}

	public class CannyMark extends PerformerBase {

		CannyEdge<GrayF32,GrayF32> alg = FactoryEdgeDetectors.canny(2,false, false, GrayF32.class, GrayF32.class);
		GrayU8 output = new GrayU8(width,height);

		@Override
		public void process() {
			alg.process(input,5,10,output);
		}
	}

	public class CannyTrace extends PerformerBase {

		CannyEdge<GrayF32,GrayF32> alg = FactoryEdgeDetectors.canny(2,true, false, GrayF32.class, GrayF32.class);
		GrayU8 output = new GrayU8(width,height);

		@Override
		public void process() {
			alg.process(input,5,10,output);
		}
	}

	public void performTests() {
		createImage();

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new CannyMark(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new CannyTrace(), TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkDetectEdge benchmark = new BenchmarkDetectEdge();
		benchmark.performTests();
	}

}
