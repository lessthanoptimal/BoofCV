/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.lines;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.feature.detect.line.DetectLineSegment;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkDetectLines<T extends ImageBase, D extends ImageBase> {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	T input;
	Class<T> imageType;
	Class<D> derivType;

	float edgeThreshold = 30;

	public BenchmarkDetectLines( Class<T> imageType , Class<D> derivType ) {
		this.imageType = imageType;
		this.derivType = derivType;
		input = GeneralizedImageOps.createImage(imageType,1,1);

	}

	public class HoughPolar extends PerformerBase {

		DetectLine<T> detector =
				FactoryDetectLine.houghPolar(5, 175, 300, 360, edgeThreshold, imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public class HoughFoot extends PerformerBase {

		DetectLine<T> detector =
				FactoryDetectLine.houghFoot(6, 10, 5, edgeThreshold, imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public class HoughFootSub extends PerformerBase {

		DetectLine<T> detector =
				FactoryDetectLine.houghFootSub(6, 8, 5, edgeThreshold, 2, 2, imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public class LineRansac extends PerformerBase {

		DetectLineSegment<T> detector =
				FactoryDetectLine.lineRansac(40, 30, 2.36, true, imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public void benchmark( BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,input,imageType);

		ProfileOperation.printOpsPerSec(new HoughPolar(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HoughFoot(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HoughFootSub(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new LineRansac(), TEST_TIME);
		System.out.println("done");
	}

	public static void main(String args[]) {
		BufferedImage image = UtilImageIO.loadImage("../evaluation/data/lines_indoors.jpg");
//		BufferedImage image = UtilImageIO.loadImage("../applet/data/lines_indoors.jpg");

		System.out.println("=========  Profile Image Size " + image.getWidth() + " x " + image.getHeight()+ " ==========");
		System.out.println();

		BenchmarkDetectLines app = new BenchmarkDetectLines(ImageFloat32.class,ImageFloat32.class);
		app.benchmark(image);

	}
}
