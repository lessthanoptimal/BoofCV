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

package boofcv.benchmark.feature.homography;


import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Evaluates how fast interest points are detected at runtime.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureDetectRuntime<T extends ImageBase> {

	Class<T> imageType;
	InterestPointDetector<T> alg;

	public BenchmarkFeatureDetectRuntime(Class<T> imageType, InterestPointDetector<T> alg) {
		this.imageType = imageType;
		this.alg = alg;
	}

	public void benchmark( String directory , int imageNumber )
			throws IOException
	{
		String imageName = String.format("%s/img%d.png", directory, imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));

		T input = ConvertBufferedImage.convertFrom(image, null, imageType);

		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			long before = System.currentTimeMillis();

			alg.detect(input);

			long after = System.currentTimeMillis();
			long elapsed = after-before;

			System.out.println("time = "+elapsed+" num detected "+alg.getNumberOfFeatures());

			if( elapsed < best )
				best = elapsed;
		}

		System.out.println();
		System.out.println("Best = "+best);
	}

	public static void main( String args[] ) throws IOException {

		InterestPointDetector<ImageFloat32> alg = FactoryInterestPoint.fastHessian(100, 2, -1, 1, 9, 4, 4);

		BenchmarkFeatureDetectRuntime<ImageFloat32> benchmark =
				new BenchmarkFeatureDetectRuntime<ImageFloat32>(ImageFloat32.class,alg);

		benchmark.benchmark("../evaluation/data/mikolajczk/boat", 1);
	}
}
