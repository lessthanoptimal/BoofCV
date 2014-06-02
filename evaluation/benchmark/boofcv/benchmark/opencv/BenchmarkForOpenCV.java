/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.ConnectRule;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.line.LineParametric2D_F32;

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

	final static String imageName = "../data/evaluation/standard/barbara.png";
	final static String imageLineName = "../data/evaluation/simple_objects.jpg";
	final int radius = 2;
	final long TEST_TIME = 1000;
	final Random rand = new Random(234234);

	public BenchmarkForOpenCV(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
		this.input = UtilImageIO.loadImage(imageName, imageType);
		this.inputLine = UtilImageIO.loadImage(imageLineName, imageType);
	}

	public class Gaussian extends PerformerBase {
		T output = (T) input._createNew(input.width, input.height);
		T storage = (T) input._createNew(input.width, input.height);

		@Override
		public void process() {
			GBlurImageOps.gaussian(input, output, -1, radius, storage);
		}
	}

	public class Sobel extends PerformerBase {
		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);

		@Override
		public void process() {
			GImageDerivativeOps.sobel(input, derivX, derivY, BorderType.EXTENDED);
		}
	}

	public class Harris extends PerformerBase {
		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		GeneralFeatureDetector<T, D> detector;

		public Harris() {
			GImageDerivativeOps.sobel(input, derivX, derivY, BorderType.EXTENDED);
			detector = FactoryDetectPoint.createHarris(new ConfigGeneralDetector(-1,radius,1), false, derivType);
		}

		@Override
		public void process() {
			detector.process(input, derivX, derivY, null, null, null);
//			System.out.println("num found "+detector.getFeatures().size);
		}
	}

	public class HoughLine extends PerformerBase {
		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.width, input.height);
		DetectLineHoughPolar<T, D> detector;

		public HoughLine() {
			GImageDerivativeOps.sobel(input, derivX, derivY, BorderType.EXTENDED);
			detector = FactoryDetectLineAlgs.houghPolar(
					new ConfigHoughPolar(2, 40, 2, Math.PI / 180, 150, -1), imageType, derivType);
		}

		@Override
		public void process() {
			List<LineParametric2D_F32> lines = detector.detect(inputLine);
//			System.out.println("total found lines: "+lines.size());
		}
	}

	// Canny has known algorithm issues with its runtime performance.
	public class Canny extends PerformerBase {
		CannyEdge<T,D> detector;
		ImageUInt8 output = new ImageUInt8(input.width,input.height);

		public Canny() {
			detector = FactoryEdgeDetectors.canny(2,false,false, imageType, derivType);
		}

		@Override
		public void process() {
			detector.process(input,5, 50,output);
		}
	}

	public class Contour extends PerformerBase {

		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);

		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.EIGHT);

		@Override
		public synchronized void process() {
			GThresholdImageOps.threshold(input, binary, 75, true);
			alg.process(binary,labeled);
		}
	}

	public class SURF extends PerformerBase {
		DetectDescribePoint<T,SurfFeature> detector;

		public SURF() {
			// the fast implementation is closer to OpenCV's SURF implementation for stability
			detector = FactoryDetectDescribe.surfFast(new ConfigFastHessian(20, 2, -1, 1, 9, 4, 4),
					null, null, imageType);
		}

		@Override
		public void process() {
			detector.detect(input);
//			System.out.println("Found features: "+N);
		}
	}

	public void performTest() {
		System.out.println("=========  Profile Description width = " + input.width + " height = " + input.height);
		System.out.println();

//		ProfileOperation.printOpsPerSec(new Gaussian(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Sobel(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Harris(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Canny(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Contour(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new HoughLine(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new SURF(), TEST_TIME);

		System.out.println();
	}

	public static void main(String args[]) {
		BenchmarkForOpenCV<ImageFloat32, ImageFloat32> test =
				new BenchmarkForOpenCV<ImageFloat32, ImageFloat32>(ImageFloat32.class, ImageFloat32.class);

		test.performTest();
	}
}
