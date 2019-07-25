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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;

/**
 * @author Peter Abeles
 */
public class XCornerAbeles2019Intensity {

	int radius;
	FastQueue<Point2D_I32> circle = new FastQueue<>(Point2D_I32.class,true);

	public XCornerAbeles2019Intensity( int radius ) {
		this.radius = radius;
		DiscretizedCircle.coordinates(radius, circle);
	}

	public void process(GrayF32 input, GrayF32 intensity) {
		int radiusA = 3;
		int radiusD = 2;

		intensity.reshape(input.width,input.height);
		ImageMiscOps.fillBorder(intensity,0, radiusA);

		BoofConcurrency.loopFor(radiusA,input.height-radiusA,y->{
//		for (int y = radiusA; y < input.height - radiusA; y++) {
			int outputIdx = intensity.startIndex + y*intensity.stride + radiusA;
			for (int x = radiusA; x < input.width - radiusA; x++) {
				float a = input.unsafe_get(x,y - radiusA);
				float b = input.unsafe_get(x - radiusA,y);
				float c = input.unsafe_get(x,y + radiusA);
				float d = input.unsafe_get(x + radiusA,y);

				float e = input.unsafe_get(x - radiusD,y - radiusD);
				float f = input.unsafe_get(x - radiusD,y + radiusD);
				float g = input.unsafe_get(x + radiusD,y + radiusD);
				float h = input.unsafe_get(x + radiusD,y - radiusD);

				intensity.data[outputIdx++] = Math.max(score7(a,b,c,d),score7(e,f,g,h));
			}
		});
	}

	public void process2(GrayF32 input, GrayF32 intensity) {
		intensity.reshape(input.width,input.height);
		ImageMiscOps.fillBorder(intensity,0, radius);

		final int N = circle.size;
		final int circleQuad = circle.size/4;
		int idx1 = circleQuad;
		int idx2 = 2*circleQuad;
		int idx3 = 3*circleQuad;

//		for (int y = radius; y < input.height - radius; y++) {
		BoofConcurrency.loopFor(radius,input.height-radius,y->{
			int outputIdx = intensity.startIndex + y*intensity.stride + radius;
			for (int x = radius; x < input.width - radius; x++) {
				float max_value = -Float.MAX_VALUE;
				for (int offset = 0; offset < circleQuad; offset++) {
					Point2D_I32 ca = circle.get(offset);
					Point2D_I32 cb = circle.get((offset+idx1));
					Point2D_I32 cc = circle.get((offset+idx2)%N);
					Point2D_I32 cd = circle.get((offset+idx3)%N);

					float a = input.unsafe_get(x+ca.x,y+ca.y);
					float b = input.unsafe_get(x+cb.x,y+cb.y);
					float c = input.unsafe_get(x+cc.x,y+cc.y);
					float d = input.unsafe_get(x+cd.x,y+cd.y);
//
//					if( x == 419 && y == 346 ) {
//						System.out.println("white");
//						score2(a, b, c, d);
//					} else if( x == 431 && y == 340 ) {
//						System.out.println("black");
//						score2(a, b, c, d);
//					}

					float value = score(a,b,c,d);
					if( value > max_value )
						max_value = value;
				}
				intensity.data[outputIdx++] = max_value;
			}
		});
//		for (int y = radiusA; y < input.height - radiusA; y++) {
//			int outputIdx = intensity.startIndex + y*intensity.stride + radiusA;
//			for (int x = radiusA; x < input.width - radiusA; x++) {
//				intensity.data[outputIdx++] = score(x,y);
//			}
//		}
	}

	private float score( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;
		float div = mean + UtilEjml.F_EPS;

		a = (a-mean)/div;
		b = (b-mean)/div;
		c = (c-mean)/div;
		d = (d-mean)/div;

		return a*c + b*d;
	}

	private float score2( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		float abs_a = Math.abs(a);
		float abs_b = Math.abs(b);
		float abs_c = Math.abs(c);
		float abs_d = Math.abs(d);

		return a*c/(abs_a+abs_c+UtilEjml.F_EPS) + b*d/(abs_b+abs_d+UtilEjml.F_EPS);
	}

	private float score3( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		return a*c/(a*a + c*c+UtilEjml.F_EPS) + b*d/(b*b + d*d+UtilEjml.F_EPS);
	}

	private float score4( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		return a*c/(float)Math.sqrt(a*a + c*c+UtilEjml.F_EPS) + b*d/(float)Math.sqrt(b*b + d*d+UtilEjml.F_EPS);
	}

	private float score5( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		float divisor = (float)Math.sqrt(a*a + b*b + c*c + d*d+UtilEjml.F_EPS);

		return a*c/divisor + b*d/divisor;
	}

	private float score6( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		return a*c/(a*a + c*c+UtilEjml.F_EPS) + b*d/(b*b + d*d+UtilEjml.F_EPS);
	}

	private float score7( float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		return a*c/255f + b*d/255f;
	}

	private float score8( float middle , float a , float b , float c , float d ) {
		float mean = (a+b+c+d)/4f;

		a = (a-mean);
		b = (b-mean);
		c = (c-mean);
		d = (d-mean);

		// the middle should be approximately the average
		float error = mean-middle;
		error = 1.0f/(1.0f+error*error);

		return error*(a*c/255f + b*d/255f);
	}
}
