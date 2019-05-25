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
//import boofcv.alg.InputSanityCheck;
//import boofcv.alg.feature.detect.peak.MeanShiftPeak;
//import boofcv.alg.misc.ImageMiscOps;
//import boofcv.alg.weights.WeightPixelGaussian_F32;
//import boofcv.struct.QueueCorner;
//import boofcv.struct.border.BorderType;
//import boofcv.struct.image.*;
//import georegression.struct.line.LineParametric2D_F32;
//import georegression.struct.point.Point2D_I16;
//import org.ddogleg.struct.FastQueue;
//import org.ddogleg.struct.GrowQueue_F32;
//
///**
// * <p>
// * Hough transform based line detector.  Lines are parameterized based upon the (x,y) coordinate
// * of the closest point to the origin.  This parametrization is more convenient since it corresponds
// * directly with the image space.  See [1] for more details.
// * </p>
// *
// * <p>
// * The line's direction is estimated using the gradient at each point flagged as belonging to a line.
// * To minimize error the image center is used as the coordinate system's center.  However lines which
// * lie too close to the center can't have their orientation accurately estimated.
// * </p>
// *
// * <p>
// * [1] Section 9.3 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd Ed. 2005
// * </p>
// *
// * @author Peter Abeles
// */
//public class HoughTransformLineFootOfNorm extends HoughTransformGradient {
//
//	/**
//	 * Specifies parameters of transform.
//	 *
//	 * @param extractor Extracts local maxima from transform space.  A set of candidates is provided, but can be ignored.
//	 * @param minDistanceFromOrigin Distance from the origin in which lines will not be estimated.  In transform space.  Try 5.
//	 */
//	public HoughTransformLineFootOfNorm(NonMaxSuppression extractor,
//										int minDistanceFromOrigin)
//	{
//		super(extractor,minDistanceFromOrigin);
//	}
//
//	@Override
//	public <D extends ImageGray<D>> void transform(D derivX, D derivY, GrayU8 binary) {
//		transform.reshape(derivX.width,derivY.height);
//		super.transform(derivX, derivY, binary);
//	}
//
//	@Override
//	protected void transformToLine(float x, float y, LineParametric2D_F32 l) {
//		l.p.x = x;
//		l.p.y = y;
//		l.slope.x = -(l.p.y-originY);
//		l.slope.y = l.p.x-originX;
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
//		// this minimizes error, which is a function of distance from origin
//		x -= originX;
//		y -= originY;
//
//		float v = (x*derivX + y*derivY)/(derivX*derivX + derivY*derivY);
//		float vx = v*derivX + originX;
//		float vy = v*derivY + originY;
//
//		// finds the foot a line normal equation and put the point into image coordinate
//		int x0 = (int)vx;
//		int y0 = (int)vy;
//
//		// weights for bilinear interpolate type weightings
//		float wx = vx-x0;
//		float wy = vy-y0;
//
//		// make a soft decision and spread counts across neighbors
//		addParameters(x0,y0, (1f-wx)*(1f-wy));
//		addParameters(x0+1,y0, (wx)*(1f-wy));
//		addParameters(x0,y0+1, (1f-wx)*(wy));
//		addParameters(x0+1,y0+1, (wx)*(wy));
//	}
//}
