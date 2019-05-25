///*
// * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package boofcv.alg.feature.detect.line;
//
//
//import boofcv.abst.feature.detect.extract.NonMaxSuppression;
//import boofcv.struct.image.GrayU8;
//import boofcv.struct.image.ImageGray;
//import georegression.metric.UtilAngle;
//import georegression.struct.line.LineParametric2D_F32;
//import georegression.struct.point.Point2D_I16;
//
///**
// * <p>
// * Hough transform based line detector.  Lines are parameterized into polar coordinates. A line equation
// * is found using its (x,y) coordinate and the gradient at that point.
// * </p>
// *
// * <p>
// * [1] Section 9.3 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd Ed. 2005
// * </p>
// *
// * @author Peter Abeles
// */
//public class HoughTransformLinePolarGradient extends HoughTransformGradient {
//
//	// maximum allowed range
//	float r_max;
//
//	/**
//	 * Specifies parameters of transform.
//	 *
//	 * @param extractor Extracts local maxima from transform space.  A set of candidates is provided, but can be ignored.
//	 */
//	public HoughTransformLinePolarGradient(NonMaxSuppression extractor, int numBinsRange , int numBinsAngle)
//	{
//		super(extractor,-1);
//		transform.reshape(numBinsRange,numBinsAngle);
//	}
//
//	public int getNumBinsRange() {
//		return transform.getWidth();
//	}
//
//	public int getNumBinsAngle() {
//		return transform.getHeight();
//	}
//
//	@Override
//	protected void transformToLine(float x, float y, LineParametric2D_F32 l) {
//		float w2 = transform.width/2;
//		float r = (r_max*(x-w2)/w2);
//		float c = (float)Math.cos(y);
//		float s = (float)Math.sin(y);
//
//		float x0 = r*c+originX;
//		float y0 = r*s+originY;
//		l.p.set(x0,y0);
//		l.slope.set(-s,c);
//	}
//
//	/**
//	 * Takes the detected point along the line and its gradient and converts it into transform space.
//	 * @param x point in image.
//	 * @param y point in image.
//	 * @param derivX gradient of point.
//	 * @param derivY gradient of point.
//	 */
//	@Override
//	protected void parameterize( int x , int y , float derivX , float derivY )
//	{
//		// put the point in a new coordinate system centered at the image's origin
//		x -= originX;
//		y -= originY;
//
//		int w2 = transform.width/2;
//
//		int numBinsAngle = transform.getHeight();
//
//		double theta = UtilAngle.atanSafe(-derivY,derivX)+Math.PI/2.0;
//		double cos = Math.cos(theta);
//		double sin = Math.sin(theta);
//
//		int tranY = (int)(numBinsAngle*theta/Math.PI);
//
//		// distance of closest point on line from a line defined by the point (x,y) and
//		// the tangent theta=PI*i/height
//		double p = x*cos + y*sin;
//
//		int col = (int)Math.floor(p * w2 / r_max) + w2;
//		int index = transform.startIndex + tranY*transform.stride + col;
//		if( tranY < 0 || tranY >= transform.height || col < 0 || col >= transform.width)
//			System.out.println("Fooo");
//		transform.data[index]++;
//	}
//}
