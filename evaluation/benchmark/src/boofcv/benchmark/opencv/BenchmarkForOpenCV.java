/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.benchmark.opencv;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.edge.DetectEdgeContour;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.edge.FactoryDetectEdgeContour;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F64;

import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
// todo canny edge
// todo hough line
// todo
public class BenchmarkForOpenCV<T extends ImageSingleBand, D extends ImageSingleBand> {

	final Class<T> imageType;
	final Class<D> derivType;
	final T input;
	final T inputLine;

	final static String imageName = "data/standard/barbara.png";
	final static String imageLineName = "data/simple_objects.jpg";
	final int radius = 2;
	final long TEST_TIME = 1000;
	final Random rand = new Random(234234);

	public BenchmarkForOpenCV(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
		this.input = UtilImageIO.loadImage(imageName,imageType);
		this.inputLine = UtilImageIO.loadImage(imageLineName,imageType);
	}

	public class Gaussian extends PerformerBase
	{
		T output = (T)input._createNew(input.width,input.height);
		T storage = (T)input._createNew(input.width,input.height);

		@Override
		public void process() {
			GBlurImageOps.gaussian(input,output,-1,radius,storage);
		}
	}

	public class Sobel extends PerformerBase
	{
		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);

		@Override
		public void process() {
			GImageDerivativeOps.sobel(input, derivX,derivY, BorderType.EXTENDED);
		}
	}

	public class Harris extends PerformerBase
	{
		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		GeneralFeatureDetector<T,D> detector;

		public Harris() {
			GImageDerivativeOps.sobel(input, derivX,derivY, BorderType.EXTENDED);
			detector = FactoryDetectPoint.createHarris(radius, false, 1, -1, derivType);
		}

		@Override
		public void process() {
			detector.process(input,derivX,derivY,null,null,null);
//			System.out.println("num found "+detector.getFeatures().size);
		}
	}

	public class HoughLine extends PerformerBase
	{
		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		DetectLineHoughPolar<T,D> detector;

		public HoughLine() {
			GImageDerivativeOps.sobel(input, derivX,derivY, BorderType.EXTENDED);
			detector = FactoryDetectLineAlgs.houghPolar(2, 40, 2, Math.PI / 180, 150, -1, imageType, derivType);
		}

		@Override
		public void process() {
			List<LineParametric2D_F32> lines =  detector.detect(inputLine);
//			System.out.println("total found lines: "+lines.size());
		}
	}

	// Canny has known algorithm issues with its runtime performance.
	public class Canny extends PerformerBase
	{
		DetectEdgeContour<T> detector;

		public Canny() {
			detector = FactoryDetectEdgeContour.canny(5,50,false,imageType,derivType);
		}

		@Override
		public void process() {
			detector.process(input);
		}
	}

	public class SURF extends PerformerBase
	{
		InterestPointDetector<T> detector;
		DescribeRegionPoint<T> describer;

		public SURF() {
			detector = FactoryInterestPoint.fastHessian(20, 2, -1, 1, 9, 4, 4);
			describer = FactoryDescribeRegionPoint.surf(true,imageType);
		}

		@Override
		public void process() {
			detector.detect(input);
			describer.setImage(input);

			int N = detector.getNumberOfFeatures();
			for( int i = 0; i < N; i++ ) {
				Point2D_F64 pt = detector.getLocation(i);
				double scale = detector.getScale(i);
				describer.process(pt.x,pt.y,0,scale,null);
			}

//			System.out.println("Found features: "+N);
		}
	}

	public void performTest() {
		System.out.println("=========  Profile Description width = "+input.width+" height = "+input.height);
		System.out.println();

//		ProfileOperation.printOpsPerSec(new Gaussian(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Sobel(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Harris(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Canny(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new HoughLine(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new SURF(), TEST_TIME);

		System.out.println();
	}

	public static void main( String args[] ) {
		BenchmarkForOpenCV<ImageFloat32,ImageFloat32> test =
				new BenchmarkForOpenCV<ImageFloat32,ImageFloat32>( ImageFloat32.class,ImageFloat32.class);

		test.performTest();
	}
}
