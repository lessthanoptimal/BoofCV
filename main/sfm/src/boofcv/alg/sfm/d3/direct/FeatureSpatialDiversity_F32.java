///*
// * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
//package boofcv.alg.sfm.d3.direct;
//
//import georegression.metric.UtilAngle;
//import georegression.struct.point.Point2D_F32;
//import georegression.struct.point.Point2D_F32;
//import georegression.struct.point.Point3D_F32;
//import org.ddogleg.struct.FastQueue;
//import org.ddogleg.struct.GrowQueue_F32;
//import org.ejml.alg.fixed.FixedOps2_D32;
//import org.ejml.alg.fixed.FixedOps3_D32;
//import org.ejml.data.FixedMatrix2x2_32F;
//import org.ejml.data.FixedMatrix3x3_32F;
//
///**
// * Checks to see if the features being tracked form
// *
// * @author Peter Abeles
// */
//// TODO estimate spartial diversity in radians
//public class FeatureSpatialDiversity_F32 {
//
//	FixedMatrix2x2_32F var = new FixedMatrix2x2_32F();
//
//	FastQueue<Point2D_F32> norm = new FastQueue<>(Point2D_F32.class, true);
//
//
//	public void configure( float hfov , float vfov ) {
//
//	}
//
//	public void reset() {
//		norm.reset();
//		norm.reset();
//	}
//
//	/**
//	 * Adds the estimated 3D location of a feature.
//	 * @param x
//	 * @param y
//	 * @param z
//	 */
//	public void addPoint( float x , float y , float z ) {
//		norm.grow().set(x/z, y/z);
//	}
//
//	public void process() {
//
//		// TODO rethink this.  I was angular variance in horizontal + vertical direction
//		computeCovarince();
//
//		float eigenvalue = smallestEigenvalue();
//
//		float angle = (float)Math.atan2(eigenvalue, 1);
//		System.out.println("Angle in degrees = "+ UtilAngle.degree(angle));
//	}
//
//	private void computeCovarince() {
//		float meanX=0,meanY=0;
//
//		for (int i = 0; i < norm.size; i++) {
//			Point2D_F32 p = norm.get(i);
//			meanX += p.x;
//			meanY += p.y;
//		}
//
//		meanX /= norm.size;
//		meanY /= norm.size;
//
//		var.a11 = var.a12 = var.a22 = 0;
//
//		for (int i = 0; i < norm.size; i++) {
//			Point2D_F32 p = norm.get(i);
//
//			float dx = p.x-meanX;
//			float dy = p.y-meanY;
//
//			var.a11 += dx*dx;
//			var.a12 += dx*dy;
//			var.a22 += dy*dy;
//		}
//
//		FixedOps2_D32.divide(var, norm.size-1);
//
//		// compute the smallest eigenvalue
//		float left = (var.a11 + var.a22) * 0.5;
//		float b = (var.a11 - var.a22) * 0.5;
//		float right = Math.sqrt(b * b + var.a12 * var.a12);
//
//		// the smallest eigenvalue will be minus the right side
//		float eigenvalue = left - right;
//
//
//	}
//
//	private float smallestEigenvalue() {
//		// compute the smallest eigenvalue
//		float left = (var.a11 + var.a22) * 0.5;
//		float b = (var.a11 - var.a22) * 0.5;
//		float right = Math.sqrt(b * b + var.a12 * var.a12);
//
//		// the smallest eigenvalue will be minus the right side
//		return left - right;
//	}
//
//}
//
