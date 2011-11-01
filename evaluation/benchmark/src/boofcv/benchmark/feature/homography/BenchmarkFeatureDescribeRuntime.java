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


import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Evaluates how fast a descriptor is at runtime.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureDescribeRuntime<T extends ImageBase> {

	Class<T> imageType;
	DescribeRegionPoint<T> alg;

	public BenchmarkFeatureDescribeRuntime(Class<T> imageType, DescribeRegionPoint<T> alg) {
		this.imageType = imageType;
		this.alg = alg;
	}

	public void benchmark( String directory , int imageNumber , String detector )
			throws IOException
	{
		String detectName = String.format("%s/DETECTED_img%d_%s.txt",directory,imageNumber,detector);
		String imageName = String.format("%s/img%d.png",directory,imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));

		T input = ConvertBufferedImage.convertFrom(image,null,imageType);

		List<DetectionInfo> detections = LoadBenchmarkFiles.loadDetection(detectName);

		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			TupleDesc_F64 desc = new TupleDesc_F64(alg.getDescriptionLength());

			long before = System.currentTimeMillis();

			alg.setImage(input);

			for( DetectionInfo d : detections ) {
				alg.process(d.location.x, d.location.y, d.yaw, d.scale, desc);
			}

			long after = System.currentTimeMillis();
			long elapsed = after-before;

			System.out.println("time = "+elapsed);

			if( elapsed < best )
				best = elapsed;
		}

		System.out.println();
		System.out.println("Best = "+best);
	}

	public static void main( String args[] ) throws IOException {

//		DescribeRegionPoint<ImageFloat32> alg = FactoryDescribeRegionPoint.surf(true,ImageFloat32.class);
		DescribeRegionPoint<ImageFloat32> alg = FactoryDescribeRegionPoint.surfm(true, ImageFloat32.class);


		BenchmarkFeatureDescribeRuntime<ImageFloat32> benchmark =
				new BenchmarkFeatureDescribeRuntime<ImageFloat32>(ImageFloat32.class,alg);

		benchmark.benchmark("../evaluation/data/mikolajczk/boat", 1, "FH");
	}
}
