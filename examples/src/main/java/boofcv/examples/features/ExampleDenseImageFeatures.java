/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.features;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.alg.feature.dense.DescribeDenseHogFastAlg;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.factory.feature.dense.FactoryDescribeImageDenseAlg;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;

import java.awt.image.BufferedImage;

/**
 * This example shows you how to compute dense image features. They are typically used for classification and
 * recognition type tasks. They are very fast to compute and were considered state of the art until around 2012.
 *
 * This example doesn't really do anything useful. All it does is compute and print out the features for the sake
 * of simplicity. A more flushed out example with a use case can be found in
 * {@link boofcv.examples.recognition.ExampleClassifySceneKnn}.
 *
 * For a visualization of HOG features see https://youtu.be/qMTtdiujAtQ?t=437.
 *
 * @author Peter Abeles
 */
public class ExampleDenseImageFeatures {

	// Here's an example of how to use the high level interface. There are a variety of algorithms to choose from
	// For much larger images you might need to shrink the image down or change the cell size to get good results.
	public static void HighLevel( GrayF32 input) {
		System.out.println("\n------------------- Dense High Level");
		DescribeImageDense<GrayF32,TupleDesc_F64> describer = FactoryDescribeImageDense.
				hog(new ConfigDenseHoG(),input.getImageType());
//				sift(new ConfigDenseSift(),GrayF32.class);
//				surfFast(new ConfigDenseSurfFast(),GrayF32.class);

		// process the image and compute the dense image features
		describer.process(input);

		// print out part of the first few features
		System.out.println("Total Features = "+describer.getLocations().size());
		for (int i = 0; i < 5; i++) {
			Point2D_I32 p = describer.getLocations().get(i);
			TupleDesc_F64 d = describer.getDescriptions().get(i);

			System.out.printf("%3d %3d = [ %f %f %f %f\n",p.x,p.y,d.value[0],d.value[1],d.value[2],d.value[3]);

			// You would process the feature descriptor here
		}
	}

	public static void LowLevelHOG( GrayF32 input ) {
		DescribeDenseHogFastAlg<GrayF32> describer = FactoryDescribeImageDenseAlg.
				hogFast(new ConfigDenseHoG(), ImageType.single(GrayF32.class));

		// The low level API gives you access to more information about the image. You can explicitly traverse it
		// by rows and columns, and access the histogram for a region. The histogram has an easy to understand
		// physical meaning.
		describer.setInput(input);
		describer.process();

		// Let's print a few parameters just because we can. They can be modified using the configuration class passed in
		System.out.println("\n------------------- HOG Low Level");
		System.out.println("HOG pixels per cell "+describer.getPixelsPerCell());
		System.out.println("HOG region width "+describer.getRegionWidthPixelX());
		System.out.println("HOG region height "+describer.getRegionWidthPixelY());
		System.out.println("HOG bins "+describer.getOrientationBins());

		// go through all the cells in the image. If you only wanted to process part of the image it could be done here
		// In the high level API the order of cells isn't specified and might be in some arbitrary order or in some
		// cases might even skip regions depending on the implementation. HOG will allways compute a cell

		for ( int i = 0; i < describer.getCellRows(); i++ ) {
			for (int j = 0; j < describer.getCellCols(); j++) {
				DescribeDenseHogFastAlg.Cell c = describer.getCell(i,j);

				// this is where you could do processing on an individual cell
			}
		}
	}

	public static void main(String[] args) {
		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample("segment/berkeley_man.jpg"));
		GrayF32 input = ConvertBufferedImage.convertFrom(buffered,(GrayF32)null);

		HighLevel(input);
		LowLevelHOG(input);

	}
}
