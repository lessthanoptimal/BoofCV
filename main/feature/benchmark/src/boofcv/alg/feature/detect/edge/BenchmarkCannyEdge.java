/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.edge.DetectEdgeContour;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.feature.detect.edge.FactoryDetectEdgeContour;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkCannyEdge {
	static final long TEST_TIME = 1000;

	int width = 640;
	int height = 480;

	Random rand = new Random(234);

	ImageFloat32 input = new ImageFloat32(width,height);

	public void createImage() {
		for( int i = 0; i < 50; i++ ) {
			int width = 50+rand.nextInt(50);
			int height = 50+rand.nextInt(50);

			int x = rand.nextInt(width);
			int y = rand.nextInt(height);

			int x1 = x+width;
			int y1 = y+height;
			if( x1 > width ) x1 = width;
			if( y1 > height ) y1 = height;

			width = x1-x;
			height = y1-y;

			ImageMiscOps.fillRectangle(input,rand.nextInt(100),x,y,width,height);
		}
	}

	public class CannyEdge extends PerformerBase {

		DetectEdgeContour<ImageFloat32> alg = FactoryDetectEdgeContour.canny(5,20,false,ImageFloat32.class,ImageFloat32.class);

		@Override
		public void process() {
			alg.process(input);
		}
	}

	public class ContourSimple extends PerformerBase {

		DetectEdgeContour<ImageFloat32> alg = FactoryDetectEdgeContour.binarySimple(15,true);

		@Override
		public void process() {
			alg.process(input);
		}
	}

	public void performTests() {
		createImage();

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new CannyEdge(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new ContourSimple(), TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkCannyEdge benchmark = new BenchmarkCannyEdge();
		benchmark.performTests();
	}

}
