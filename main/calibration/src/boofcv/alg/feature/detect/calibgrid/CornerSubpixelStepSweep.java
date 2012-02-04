/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.calibgrid;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ejml.UtilEjml;

import javax.imageio.ImageIO;

import static boofcv.alg.feature.detect.calibgrid.FindQuadCorners.incrementCircle;

/**
 * @author Peter Abeles
 */
public class CornerSubpixelStepSweep<T extends ImageSingleBand> {
	
	int w,h,N;

	Point2D_I32 pt[];
	double edgeValues[];
	double edgeResponseIn[];
	double edgeResponseOut[];

	Point2D_F64 cornerPoint = new Point2D_F64();
	
	public void process( InterpolatePixel<T> image ) {
		innit(image.getImage());

		// Find the two boundary regions along the edge
		computeEdgeValues(image);
		computeEdgeResponse();

		Point2D_F64 p0 = findPointByIndex(findMaxInterpolate(edgeResponseIn));
		Point2D_F64 p1 = findPointByIndex(findMaxInterpolate(edgeResponseOut));

		// compute the response for each pixel
		int maxX = -1;
		int maxY = -1;
		double maxResponse = 0;
		for( int y = 2; y < h-2; y++ ) {
			for( int x = 2; x < w-2; x++ ) {
				double totalResponse = 0;
				totalResponse += computeLineSignal(x,y,p0,image);
				totalResponse += computeLineSignal(x,y,p1,image);

				if( totalResponse > maxResponse ) {
					maxX = x;
					maxY = y;
					maxResponse = totalResponse;
				}
			}
		}

		cornerPoint.set(maxX,maxY);
	}

	public Point2D_F64 getCornerPoint() {
		return cornerPoint;
	}

	
	private float computeLineSignal( float x , float y , Point2D_F64 pt , InterpolatePixel<T> image )
	{
		float dx = (float)(x - pt.x);
		float dy = (float)(y - pt.y);
		float r = (float)Math.sqrt(dx*dx + dy*dy);
		dx /= r;
		dy /= r;

		float signal = 0;
		signal -= image.get(x-dx,y-dy);
		signal -= image.get(x,y);
		signal += image.get(x+dx,y+dy);
		signal += image.get(x+2*dx,y+2*dy);

		return signal;
	}

	private void innit(T image) {
		w = image.getWidth();
		h = image.getHeight();
		N = (w+h-2)*2;
		edgeValues = new double[N];
		edgeResponseIn = new double[N];
		edgeResponseOut = new double[N];
		pt = new Point2D_I32[N];
	}

	private void computeEdgeValues( InterpolatePixel<T> image) 
	{
		int index = 0;
		for( int i = 0; i < w; i++ ) {
			pt[index++] = new Point2D_I32(i,0);
		}
		for( int i = 1; i < h-1; i++ ) {
			pt[index++] = new Point2D_I32(w-1,i);
		}
		for( int i = w-1; i >= 0; i-- ) {
			pt[index++] = new Point2D_I32(i,h-1);
		}
		for( int i = h-2; i >= 1; i-- ) {
			pt[index++] = new Point2D_I32(0,i);
		}
		
		for( int i = 0; i < N; i++ ) {
			edgeValues[i] = image.get(pt[i].x,pt[i].y);
		}
	}

	private void computeEdgeResponse() {
		for( int i = 0; i < N; i++ ) {
			double a = edgeValues[incrementCircle(i, -1, N)];
			double b = edgeValues[incrementCircle(i, 0, N)];
			double c = edgeValues[incrementCircle(i, 1, N)];

			edgeResponseIn[i] = a - b - c;
			edgeResponseOut[i] = -a - b + c;

		}
	}
	
	private double findMaxInterpolate( double edgeResponse[] ) {

		double responseBest = 0;
		int indexBest = -1;
		
		for( int i = 0; i < N; i++ ) {
			double r = edgeResponse[i];
			if( r > responseBest ) {
				responseBest = r;
				indexBest = i;
			}
		}

		// todo how ot handle negative response here?
		double a0 = edgeResponse[incrementCircle(indexBest,-1,N)];
		double a2 = edgeResponse[incrementCircle(indexBest,1,N)];
		double top = (-1*a0+a2);
		double x;
		if( Math.abs(top) < UtilEjml.EPS )
			x = indexBest;
		else
			x = top/(Math.abs(a0)+ Math.abs(a2))+indexBest;

		if( x >= N )
			System.out.println("CRAGP");
		
		if( x < 0 )
			return N+x;
		else if( x >= N )
			return x-N;
		return x;
	}

	private Point2D_F64 findPointByIndex( double index ) {
		int i0 = (int)index;
		
		if( i0 >= pt.length )
			System.out.println("Adasd");

		Point2D_I32 p0 = pt[i0];
		Point2D_I32 p1 = pt[incrementCircle(i0,1,N)];
		
		double w = index-i0;
		Point2D_F64 ret = new Point2D_F64();
		
		ret.x = p0.x*(1-w) + p1.x*w;
		ret.y = p0.y*(1-w) + p1.y*w;

		return ret;
	}
	
	private void findEdgeStep() {
		
	}
}
