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
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ConnectRule;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

public class DetectChessboardCorners {

	int levels = 300;
	int radius = 1;
	int width = radius*2+1;

	GradientCornerIntensity<GrayF32> cornerIntensity;

	GrayF32 intensity = new GrayF32(1,1);
	GrayU8 binary = new GrayU8(1,1);
	InputToBinary<GrayF32> inputToBinary;

	BinaryContourFinderLinearExternal contourFinder = new BinaryContourFinderLinearExternal();

	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);
	FastQueue<Point2D_I32> contour = new FastQueue<>(Point2D_I32.class,true);

	GrayF32 derivX , derivY;

	InterpolatePixelS<GrayF32> interpX = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
	InterpolatePixelS<GrayF32> interpY = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);


	public DetectChessboardCorners() {
		cornerIntensity =  FactoryIntensityPointAlg.shiTomasi(radius, false, GrayF32.class);
//		cornerIntensity =  FactoryIntensityPointAlg.harris(radius, 0.04f,false, derivType);

//		inputToBinary = FactoryThresholdBinary.globalFixed(2_000,false,GrayF32.class);
		inputToBinary = FactoryThresholdBinary.globalOtsu(0,levels,false,GrayF32.class);
		// TODO Ostu with a minimum allowed threshold for when there's no  chessboard
//		inputToBinary = FactoryThresholdBinary.blockOtsu(ConfigLength.fixed(50),1.0,false,true,true,0.5,GrayF32.class);

		contourFinder.setMaxContour((width+1)*4+4);
		contourFinder.setMinContour(width*4-4);
		contourFinder.setConnectRule(ConnectRule.EIGHT);
	}

	public void process( GrayF32 input , GrayF32 derivX , GrayF32 derivY ) {
		// TODO scale input, compute deriv here
		this.derivX = derivX;
		this.derivY = derivY;

		interpX.setImage(derivX);
		interpY.setImage(derivY);

		cornerIntensity.process(derivX,derivY,intensity);

		// adjust intensity value so that its between 0 and levels for OTSU thresholding
		float featmax = ImageStatistics.max(intensity);
//		System.out.println("MAx = "+featmax);
		PixelMath.multiply(intensity,levels/featmax,intensity);

//		int N = intensity.width*input.height;
//		for (int i = 0; i < N; i++) {
//			if( intensity.data[i] <= 2f ) {
//				intensity.data[i] = 0f;
//			}
//		}

		inputToBinary.process(intensity,binary);
		contourFinder.process(binary);

		int dropped = 0;
		corners.reset();
		List<ContourPacked> packed = contourFinder.getContours();
		for (int i = 0; i < packed.size(); i++) {
			contourFinder.loadContour(i,contour);

			Corner c = corners.grow();

			UtilPoint2D_I32.mean(contour.toList(),c);
			// remove bias
			c.x += 0.5;
			c.y += 0.5;

			c.angle = computeAngle((float)(c.x),(float)(c.y));
			c.intensity = computeCheckerIntensity((float)(c.x),(float)(c.y),c.angle);

			System.out.println("intensity "+c.intensity);
			if( c.intensity < 10 ) {
				dropped++;
				corners.removeTail();
			}
		}

		System.out.println("Dropped "+dropped+" / "+packed.size());
	}

	private double computeAngle( float cx , float cy ) {

		float sdx = 0, sdy = 0;

		int radius = this.radius+1;
		for (int iy = -radius; iy <= radius; iy++) {
			for (int ix = -radius; ix <= radius; ix++) {
				float dx = interpX.get(cx+ix,cy+iy);
				float dy = interpY.get(cx+ix,cy+iy);

				sdx += dx*Math.signum(iy);
				sdy += dy*Math.signum(ix);
			}
		}

		return UtilAngle.atanSafe(sdy,sdx);
	}

	// TODO can intensity be computed at the same time as angle? can angle be used better?
	private double computeCheckerIntensity( float cx , float cy , double angle ) {

		double c = Math.cos(angle);
		double s = Math.sin(angle);


		float sdx0 = 0, sdy0 = 0;
		float sdx1 = 0, sdy1 = 0;

		int radius = this.radius+1;
		for (int iy = -radius; iy <= radius; iy++) {
			for (int ix = -radius; ix <= radius; ix++) {
				float dx = interpX.get(cx+ix,cy+iy);
				float dy = interpY.get(cx+ix,cy+iy);

				if( ix*s - iy*c > 0 ) {
					sdx0 += dx;
					sdy0 += dy;
				} else {
					sdx1 += dx;
					sdy1 += dy;
				}

			}
		}

		sdx0 /= width*width;
		sdy0 /= width*width;
		sdx1 /= width*width;
		sdy1 /= width*width;

		double dx = sdx0-sdx1;
		double dy = sdy0-sdy1;

		double d0 = Math.abs(sdx0*sdy0);
		double d1 = Math.abs(sdx1*sdy1);

		return Math.sqrt(Math.sqrt(dx*dx + dy*dy)*Math.sqrt(Math.min(d0,d1)));
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
	}
}
