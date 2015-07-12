/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.color.ColorHsv;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.color.GHistogramFeatureOps;
import boofcv.alg.feature.color.Histogram_F64;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * @author Peter Abeles
 */
public class ExampleColorHistogramFeature {

	/**
	 * Constructs a 3D histogram using RGB.  While RGB is a popular color space the resulting histogram will
	 * depend on lighting conditions and might not produce the most accurate results.
	 */
	public static List<double[]> coupledRGB( List<File> images ) {
		List<double[]> points = new ArrayList<double[]>();

		// The number of bins is an important parameter.  Try adjusting it
		Histogram_F64 histogram = new Histogram_F64(10,10,10);
		histogram.setRange(0, 0, 255);
		histogram.setRange(1, 0, 255);
		histogram.setRange(2, 0, 255);

		MultiSpectral<ImageFloat32> rgb = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, rgb, true);

			Histogram_F64 imageHist = histogram.copy();
			GHistogramFeatureOps.histogram(rgb,imageHist);

			UtilFeature.normalizeL2(imageHist);

			points.add(imageHist.value);
		}

		return points;
	}

	/**
	 * HSV stores color information in Hue and Saturation while intensity is in Value.  This computes a 2D histogram
	 * from hue and saturation only, which makes it lighting independent.
	 */
	public static List<double[]> coupledHueSat( List<File> images  ) {
		List<double[]> points = new ArrayList<double[]>();

		// The number of bins is an important parameter.  Try adjusting it
		Histogram_F64 histogram = new Histogram_F64(12,12);
		histogram.setRange(0, 0, 2.0*Math.PI); // range of hue is from 0 to 2PI
		histogram.setRange(1, 0, 1.0);         // range of saturation is from 0 to 1

		MultiSpectral<ImageFloat32> rgb = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);
		MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			hsv.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, rgb, true);
			ColorHsv.rgbToHsv_F32(rgb, hsv);

			MultiSpectral<ImageFloat32> hs = hsv.partialSpectrum(0,1);

			Histogram_F64 imageHist = histogram.copy();
			GHistogramFeatureOps.histogram(hs,imageHist);

			UtilFeature.normalizeL2(imageHist); // normalize so that image size doesn't matter

			points.add(imageHist.value);
		}

		return points;
	}

	/**
	 * Computes two independent histograms from hue and saturation.  Less affects by sparsity, but can produce worse
	 * worse results since the basic assumption that hue and saturation are decoupled is most of the time false.
	 */
	public static List<double[]> IndependentHueSat( List<File> images  ) {
		List<double[]> points = new ArrayList<double[]>();

		// The number of bins is an important parameter.  Try adjusting it
		TupleDesc_F64 histogramHue = new TupleDesc_F64(30);
		TupleDesc_F64 histogramValue = new TupleDesc_F64(30);

		List<TupleDesc_F64> histogramList = new ArrayList<TupleDesc_F64>();
		histogramList.add(histogramHue); histogramList.add(histogramValue);

		MultiSpectral<ImageFloat32> rgb = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);
		MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				throw new RuntimeException("Can't load image!");

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

	public static void main(String[] args) {

		String regex = "../data/applet/recognition/vacation/^\\w*.jpg";

		List<File> images = Arrays.asList(BoofMiscOps.findMatches(regex));
		Collections.sort(images);

		List<double[]> points = coupledHueSat(images);
//		List<double[]> points = IndependentHueSat(images);
//		List<double[]> points = coupledRGB(images);

//		int target = 0;
//		int target = 28;
//		int target = 38;
//		int target = 46;
		int target = 74;

		double[] targetPoint = points.get(target);

		// use a generic NN search algorithm.  Due to the data base side and the
		NearestNeighbor<File> nn = FactoryNearestNeighbor.exhaustive();
		FastQueue<NnData<File>> results = new FastQueue(NnData.class,true);

		nn.init(targetPoint.length);
		nn.setPoints(points, images);
		nn.findNearest(targetPoint, -1, 10, results);

		ListDisplayPanel gui = new ListDisplayPanel();

		// Add the target which the other images are being matched against
		gui.addImage(UtilImageIO.loadImage(images.get(target).getPath()), "Target");

		// The results will be the 10 best matches, but their order can be arbitrary
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

		// first match is always the target image, so skip it
		for (int i = 1; i < results.size; i++) {
			File file = results.get(i).data;
			BufferedImage image = UtilImageIO.loadImage(file.getPath());
			gui.addImage(image,"Match "+i);
			System.out.println("Errors "+results.get(i).distance);
		}

		gui.automaticPreferredSize();
		ShowImages.showWindow(gui,"Matches",true);
	}
}
