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

package boofcv.examples.recognition;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.color.GHistogramFeatureOps;
import boofcv.alg.feature.color.HistogramFeatureOps;
import boofcv.alg.feature.color.Histogram_F64;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * Demonstration of how to find similar images using color histograms.  Image color histograms here are treated as
 * features and extracted using a more flexible algorithm than when they are used for image processing.  It's
 * more flexible in that the bin size can be varied and n-dimensions are supported.
 *
 * In this example, histograms for about 150 images are generated.  A target image is selected and the 10 most
 * similar images, according to Euclidean distance of the histograms, are found. This illustrates several concepts;
 * 1) How to construct a histogram in 1D, 2D, 3D, ..etc,  2) Histograms are just feature descriptors.
 * 3) Advantages of different color spaces.
 *
 * Euclidean distance is used here since that's what the nearest-neighbor search uses.  It's possible to compare
 * two histograms using any of the distance metrics in DescriptorDistance too.
 *
 * @author Peter Abeles
 */
public class ExampleColorHistogramLookup {

	/**
	 * HSV stores color information in Hue and Saturation while intensity is in Value.  This computes a 2D histogram
	 * from hue and saturation only, which makes it lighting independent.
	 */
	public static List<double[]> coupledHueSat( List<File> images  ) {
		List<double[]> points = new ArrayList<>();

		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,1,1,3);
		Planar<GrayF32> hsv = new Planar<>(GrayF32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			hsv.reshape(buffered.getWidth(), buffered.getHeight());

			ConvertBufferedImage.convertFrom(buffered, rgb, true);
			ColorHsv.rgbToHsv_F32(rgb, hsv);

			Planar<GrayF32> hs = hsv.partialSpectrum(0,1);

			// The number of bins is an important parameter.  Try adjusting it
			Histogram_F64 histogram = new Histogram_F64(12,12);
			histogram.setRange(0, 0, 2.0*Math.PI); // range of hue is from 0 to 2PI
			histogram.setRange(1, 0, 1.0);         // range of saturation is from 0 to 1

			// Compute the histogram
			GHistogramFeatureOps.histogram(hs,histogram);

			UtilFeature.normalizeL2(histogram); // normalize so that image size doesn't matter

			points.add(histogram.value);
		}

		return points;
	}

	/**
	 * Computes two independent 1D histograms from hue and saturation.  Less affects by sparsity, but can produce
	 * worse results since the basic assumption that hue and saturation are decoupled is most of the time false.
	 */
	public static List<double[]> independentHueSat( List<File> images  ) {
		List<double[]> points = new ArrayList<>();

		// The number of bins is an important parameter.  Try adjusting it
		TupleDesc_F64 histogramHue = new TupleDesc_F64(30);
		TupleDesc_F64 histogramValue = new TupleDesc_F64(30);

		List<TupleDesc_F64> histogramList = new ArrayList<>();
		histogramList.add(histogramHue); histogramList.add(histogramValue);

		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,1,1,3);
		Planar<GrayF32> hsv = new Planar<>(GrayF32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			hsv.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, rgb, true);
			ColorHsv.rgbToHsv_F32(rgb, hsv);

			GHistogramFeatureOps.histogram(hsv.getBand(0), 0, 2*Math.PI,histogramHue);
			GHistogramFeatureOps.histogram(hsv.getBand(1), 0, 1, histogramValue);

			// need to combine them into a single descriptor for processing later on
			TupleDesc_F64 imageHist = UtilFeature.combine(histogramList,null);

			UtilFeature.normalizeL2(imageHist); // normalize so that image size doesn't matter

			points.add(imageHist.value);
		}

		return points;
	}

	/**
	 * Constructs a 3D histogram using RGB.  RGB is a popular color space, but the resulting histogram will
	 * depend on lighting conditions and might not produce the accurate results.
	 */
	public static List<double[]> coupledRGB( List<File> images ) {
		List<double[]> points = new ArrayList<>();

		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, rgb, true);

			// The number of bins is an important parameter.  Try adjusting it
			Histogram_F64 histogram = new Histogram_F64(10,10,10);
			histogram.setRange(0, 0, 255);
			histogram.setRange(1, 0, 255);
			histogram.setRange(2, 0, 255);

			GHistogramFeatureOps.histogram(rgb,histogram);

			UtilFeature.normalizeL2(histogram); // normalize so that image size doesn't matter

			points.add(histogram.value);
		}

		return points;
	}

	/**
	 * Computes a histogram from the gray scale intensity image alone.  Probably the least effective at looking up
	 * similar images.
	 */
	public static List<double[]> histogramGray( List<File> images ) {
		List<double[]> points = new ArrayList<>();

		GrayU8 gray = new GrayU8(1,1);
		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			gray.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, gray, true);

			TupleDesc_F64 imageHist = new TupleDesc_F64(150);
			HistogramFeatureOps.histogram(gray, 255, imageHist);

			UtilFeature.normalizeL2(imageHist); // normalize so that image size doesn't matter

			points.add(imageHist.value);
		}

		return points;
	}

	public static void main(String[] args) {

		String imagePath = UtilIO.pathExample("recognition/vacation");
		List<File> images = Arrays.asList(UtilIO.findMatches(new File(imagePath),"\\w*.jpg"));
		Collections.sort(images);

		// Different color spaces you can try
		List<double[]> points = coupledHueSat(images);
//		List<double[]> points = independentHueSat(images);
//		List<double[]> points = coupledRGB(images);
//		List<double[]> points = histogramGray(images);

		// A few suggested image you can try searching for
		int target = 0;
//		int target = 28;
//		int target = 38;
//		int target = 46;
//		int target = 65;
//		int target = 77;

		double[] targetPoint = points.get(target);

		// Use a generic NN search algorithm.  This uses Euclidean distance as a distance metric.
		NearestNeighbor<File> nn = FactoryNearestNeighbor.exhaustive();
		FastQueue<NnData<File>> results = new FastQueue(NnData.class,true);

		nn.init(targetPoint.length);
		nn.setPoints(points, images);
		nn.findNearest(targetPoint, -1, 10, results);

		ListDisplayPanel gui = new ListDisplayPanel();

		// Add the target which the other images are being matched against
		gui.addImage(UtilImageIO.loadImage(images.get(target).getPath()), "Target", ScaleOptions.ALL);

		// The results will be the 10 best matches, but their order can be arbitrary.  For display purposes
		// it's better to do it from best fit to worst fit
		Collections.sort(results.toList(), new Comparator<NnData>() {
			@Override
			public int compare(NnData o1, NnData o2) {
				if( o1.distance < o2.distance)
					return -1;
				else if( o1.distance > o2.distance )
					return 1;
				else
					return 0;
			}
		});

		// Add images to GUI -- first match is always the target image, so skip it
		for (int i = 1; i < results.size; i++) {
			File file = results.get(i).data;
			double error = results.get(i).distance;
			BufferedImage image = UtilImageIO.loadImage(file.getPath());
			gui.addImage(image, String.format("Error %6.3f", error), ScaleOptions.ALL);
		}

		ShowImages.showWindow(gui,"Similar Images",true);
	}
}
