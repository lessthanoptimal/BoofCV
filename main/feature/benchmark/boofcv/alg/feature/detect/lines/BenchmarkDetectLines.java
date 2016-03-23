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

package boofcv.alg.feature.detect.lines;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.feature.detect.line.DetectLineSegment;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkDetectLines<T extends ImageGray, D extends ImageGray> {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	T input;
	Class<T> imageType;
	Class<D> derivType;

	float edgeThreshold = 30;
	int maxLines = 10;

	public BenchmarkDetectLines( Class<T> imageType , Class<D> derivType ) {
		this.imageType = imageType;
		this.derivType = derivType;
		input = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

	}

	public class HoughPolar extends PerformerBase {

		DetectLine<T> detector =
				FactoryDetectLineAlgs.houghPolar(new ConfigHoughPolar(3, 30, 4, Math.PI / 180, edgeThreshold, maxLines), imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public class HoughFoot extends PerformerBase {

		DetectLine<T> detector =
				FactoryDetectLineAlgs.houghFoot(new ConfigHoughFoot(3, 10, 5, edgeThreshold, maxLines), imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public class HoughFootSub extends PerformerBase {

		DetectLine<T> detector =
				FactoryDetectLineAlgs.houghFootSub(new ConfigHoughFootSubimage(3, 6, 5, edgeThreshold, maxLines, 2, 2), imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public class LineRansac extends PerformerBase {

		DetectLineSegment<T> detector =
				FactoryDetectLineAlgs.lineRansac(40, 30, 2.36, true, imageType, derivType);

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public void benchmark( BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFromSingle(image, input, imageType);

		ProfileOperation.printOpsPerSec(new HoughPolar(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HoughFoot(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HoughFootSub(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new LineRansac(), TEST_TIME);
		System.out.println("done");
	}

	public static void main(String args[]) throws IOException {
		BufferedImage image = ImageIO.read(new File(UtilIO.pathExample("lines_indoors.jpg")));
//		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("lines_indoors.jpg");

		System.out.println("=========  Profile Image Size " + image.getWidth() + " x " + image.getHeight()+ " ==========");
		System.out.println();

		BenchmarkDetectLines app = new BenchmarkDetectLines(GrayF32.class,GrayF32.class);
		app.benchmark(image);

	}
}
