/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect;

import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.CircularIndex;
import boofcv.struct.ConnectRule;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectChessboardCorners {

	// intensity image is forced to have this many integer levels for OTSU
	int grayLevels = 300;
	// feature radius and width
	int radius, width;

	GradientCornerIntensity<GrayF32> cornerIntensity;

	GrayF32 intensity = new GrayF32(1,1);
	GrayU8 binary = new GrayU8(1,1);
	InputToBinary<GrayF32> inputToBinary;

	BinaryContourFinderLinearExternal contourFinder = new BinaryContourFinderLinearExternal();

	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);
	FastQueue<Point2D_I32> contour = new FastQueue<>(Point2D_I32.class,true);

//	ImageGradient<GrayF32,GrayF32> gradient = FactoryDerivative.three(GrayF32.class,GrayF32.class);
	ImageGradient<GrayF32,GrayF32> gradient = FactoryDerivative.prewitt(GrayF32.class,GrayF32.class);

	GrayF32 derivX = new GrayF32(1,1), derivY= new GrayF32(1,1);

	InterpolatePixelS<GrayF32> interpX = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);
	InterpolatePixelS<GrayF32> interpY = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);

	ImageBorder<GrayF32> borderImg = FactoryImageBorder.single(GrayF32.class,BorderType.ZERO);
	ImageLineIntegral integral = new ImageLineIntegral();

	public DetectChessboardCorners( int radius ) {
		this.radius = radius;
		this.width = radius*2+1;
		cornerIntensity =  FactoryIntensityPointAlg.shiTomasi(radius, false, GrayF32.class);
//		cornerIntensity =  FactoryIntensityPointAlg.harris(radius, 0.04f,false, derivType);

		inputToBinary = FactoryThresholdBinary.globalOtsu(0, grayLevels,false,GrayF32.class);
//		inputToBinary = FactoryThresholdBinary.blockOtsu(ConfigLength.fixed(50),0.5,false,true,false,0.5,GrayF32.class);

		contourFinder.setMaxContour((width+1)*4+4);
		contourFinder.setMinContour(width*4-4);
		contourFinder.setConnectRule(ConnectRule.EIGHT);

		// just give it something. this will be changed later
		borderImg.setImage(derivX);
		integral.setImage(FactoryGImageGray.wrap(borderImg));
	}

	public void process( GrayF32 input ) {
		borderImg.setImage(input);

		System.out.println("  * gradient");
		gradient.process(input,derivX,derivY);

		interpX.setImage(derivX);
		interpY.setImage(derivY);

		System.out.println("  * corner intensity");
		cornerIntensity.process(derivX,derivY,intensity);

		// adjust intensity value so that its between 0 and levels for OTSU thresholding
		float featmax = ImageStatistics.max(intensity);
//		System.out.println("MAx = "+featmax);
		PixelMath.multiply(intensity, grayLevels /featmax,intensity);

//		int N = intensity.width*input.height;
//		for (int i = 0; i < N; i++) {
//			if( intensity.data[i] <= 2f ) {
//				intensity.data[i] = 0f;
//			}
//		}

		System.out.println("  * binarization");
		inputToBinary.process(intensity,binary);

		System.out.println("  * contour");
		contourFinder.process(binary);

		int dropped = 0;
		corners.reset();
		List<ContourPacked> packed = contourFinder.getContours();
		System.out.println("  * features.size = "+packed.size());
		for (int i = 0; i < packed.size(); i++) {
			contourFinder.loadContour(i,contour);

			Corner c = corners.grow();

			UtilPoint2D_I32.mean(contour.toList(),c);

			// remove bias
			c.x += 0.5;
			c.y += 0.5;

			computefeaturesLines(c.x,c.y,c);

//			System.out.println("radius = "+radius+" angle = "+c.angle);
//			System.out.println("intensity "+c.intensity);
			if( c.intensity < 50 ) { // TODO make configurable
				dropped++;
				corners.removeTail();
			}

			// TODO remaining features estimate corner to subpixel
		}

		System.out.println("Dropped "+dropped+" / "+packed.size());
	}

	private void computefeaturesLines( double cx , double cy , Corner corner ) {

		int N = 16;
		double lines[] = new double[N];

		double r = radius+2;

		for (int i = 0; i < N; i++) {
			double angle = Math.PI*i/N-Math.PI/2.0;
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			double x0 = cx-r*c;
			double y0 = cy-r*s;
			double x1 = cx+r*c;
			double y1 = cy+r*s;

			lines[i] = integral.compute(x0,y0,x1,y1);
		}

		double smoothed[] = new double[N];
		for (int i = 0; i < N; i++) {
			int start = CircularIndex.addOffset(i,-2,N);

			double sum = 0;
			for (int j = 0; j < 5; j++) {
				int index = CircularIndex.addOffset(start,j,N);
				sum += lines[index];
			}
			smoothed[i] = sum / 5;
		}

		// TODO Gaussian smoothing?
		int indexMin = 0;
		double valueMin = Double.MAX_VALUE;
		for (int i = 0; i < N; i++) {
			if( smoothed[i] < valueMin ) {
				valueMin = smoothed[i];
				indexMin = i;
			}
		}

		// TODO fit quadratic for extra precision
		corner.angle = Math.PI*indexMin/N-Math.PI/2.0;

		// compute intensity.  offset by 90
		double intensity = 0;
		for (int i = 0; i < N/2; i++) {
			int idx0 = CircularIndex.addOffset(indexMin,i-N/4,N);
			int idx1 = CircularIndex.addOffset(idx0,N/2,N);
			intensity += smoothed[idx1] - smoothed[idx0];
		}
		corner.intensity = intensity/(r*2+1+N/2);
	}

	public GrayF32 getIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public FastQueue<Corner> getCorners() {
		return corners;
	}

	public static class Corner extends Point2D_F64 {
		double angle;
		double intensity;

		public void set( Corner c ) {
			super.set(c);
			this.angle = c.angle;
			this.intensity = c.intensity;
		}

		public void set( Corner c , double scale ) {
			super.set(c.x*scale,c.y*scale);
			this.angle = c.angle;
			this.intensity = c.intensity;
		}
	}
}
