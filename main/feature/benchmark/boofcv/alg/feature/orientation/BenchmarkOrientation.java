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

package boofcv.alg.feature.orientation;

import boofcv.abst.feature.orientation.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I32;

import java.util.Random;

import static boofcv.factory.feature.orientation.FactoryOrientationAlgs.*;


/**
 * @author Peter Abeles
 */
public class BenchmarkOrientation<I extends ImageSingleBand, D extends ImageSingleBand> {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);
	static int NUM_POINTS = 1000;
	static int RADIUS = 6;

	final static int width = 640;
	final static int height = 480;

	I image;
	D derivX;
	D derivY;
	ImageSingleBand ii;

	Point2D_I32 pts[];
	double scales[];

	Class<I> imageType;
	Class<D> derivType;

	public BenchmarkOrientation( Class<I> imageType , Class<D> derivType ) {

		this.imageType = imageType;
		this.derivType = derivType;

		Class integralType = ImageFloat32.class == imageType ? ImageFloat32.class : ImageSInt32.class;

		image = GeneralizedImageOps.createSingleBand(imageType, width, height);
		ii = GeneralizedImageOps.createSingleBand(integralType, width, height);
		derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(image, rand, 0, 100);
		GIntegralImageOps.transform(image,ii);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		gradient.process(image,derivX,derivY);

		pts = new Point2D_I32[NUM_POINTS];
		scales = new double[NUM_POINTS];
		int border = 6;
		for( int i = 0; i < NUM_POINTS; i++ ) {
			int x = rand.nextInt(width-border*2)+border;
			int y = rand.nextInt(height-border*2)+border;
			pts[i] = new Point2D_I32(x,y);
			scales[i] = rand.nextDouble()*10+0.8;
		}

	}

	public class Gradient implements Performer {

		OrientationGradient<D> alg;
		String name;

		public Gradient(String name, OrientationGradient<D> alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.setImage(derivX,derivY);
			for( int i = 0; i < pts.length; i++ ) {
				Point2D_I32 p = pts[i];
				alg.setScale(scales[i]);
				alg.compute(p.x,p.y);
			}
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public class Image implements Performer {

		OrientationImage alg;
		String name;

		public Image(String name, OrientationImage alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.setImage(image);
			for( int i = 0; i < pts.length; i++ ) {
				Point2D_I32 p = pts[i];
				alg.setScale(scales[i]);
				alg.compute(p.x,p.y);
			}
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public class Integral implements Performer {

		OrientationIntegral alg;
		String name;

		public Integral(String name, OrientationIntegral alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.setImage(ii);
			for( int i = 0; i < pts.length; i++ ) {
				Point2D_I32 p = pts[i];
				alg.setScale(scales[i]);
				alg.compute(p.x,p.y);
			}
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public void perform() {
		System.out.println("=========  Profile Image Size " + width + " x " + height + " ========== "+imageType.getSimpleName());
		System.out.println();

		OrientationHistogramSift sift = FactoryOrientationAlgs.sift(null);
		SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f,5,4,false);
		OrientationSiftToImage siftWrapped = new OrientationSiftToImage(sift,ss);


		ConfigAverageIntegral confAverageIIW = new ConfigAverageIntegral();
		confAverageIIW.weightSigma = -1;
		ConfigSlidingIntegral confSlidingIIW = new ConfigSlidingIntegral();
		confSlidingIIW.weightSigma = -1;


		ProfileOperation.printOpsPerSec(new Image("SIFT", siftWrapped), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Image("No Gradient", nogradient(RADIUS,imageType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Gradient("Average", average(RADIUS,false,derivType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Gradient("Average W", average(RADIUS, true, derivType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Gradient("Histogram", histogram(15, RADIUS, false, derivType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Gradient("Histogram W", histogram(15, RADIUS, true, derivType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Gradient("Sliding", sliding(15, Math.PI / 3.0, RADIUS, false, derivType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Gradient("Sliding W", sliding(15, Math.PI / 3.0, RADIUS, true, derivType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Integral("Image II", image_ii(RADIUS, 1, 4, 0, imageType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Integral("Image II W", image_ii(RADIUS, 1, 4, -1, imageType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Integral("Average II", average_ii(null, imageType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Integral("Average II W", average_ii(confAverageIIW, imageType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Integral("Sliding II", sliding_ii(null, imageType)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Integral("Sliding II W", sliding_ii(confSlidingIIW, imageType)), TEST_TIME);
	}

	public static void main( String argsp[ ] ) {
		BenchmarkOrientation<ImageFloat32,ImageFloat32> alg = new BenchmarkOrientation<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
//		BenchmarkOrientation<ImageUInt8,ImageSInt16> alg = new BenchmarkOrientation<ImageUInt8,ImageSInt16>(ImageUInt8.class, ImageSInt16.class);

		alg.perform();
	}
}
