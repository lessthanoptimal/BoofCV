/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.depth;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.radtan.RemoveRadialPtoN_F64;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Various functions and operations specific to visual depth sensors.
 *
 * @author Peter Abeles
 */
public class VisualDepthOps {
	/**
	 * Creates a point cloud from a depth image.
	 * @param param Intrinsic camera parameters for depth image
	 * @param depth depth image.  each value is in millimeters.
	 * @param cloud Output point cloud
	 */
	public static void depthTo3D(CameraPinholeRadial param , GrayU16 depth , FastQueue<Point3D_F64> cloud ) {
		cloud.reset();

		Point2Transform2_F64 p2n = LensDistortionOps.transformPoint(param).undistort_F64(true,false);

		Point2D_F64 n = new Point2D_F64();

		for( int y = 0; y < depth.height; y++ ) {
			int index = depth.startIndex + y*depth.stride;
			for( int x = 0; x < depth.width; x++ ) {
				int mm = depth.data[index++] & 0xFFFF;

				// skip pixels with no depth information
				if( mm == 0 )
					continue;

				// this could all be precomputed to speed it up
				p2n.compute(x,y,n);

				Point3D_F64 p = cloud.grow();
				p.z = mm;
				p.x = n.x*p.z;
				p.y = n.y*p.z;
			}
		}
	}

	/**
	 * Creates a point cloud from a depth image and saves the color information.  The depth and color images are
	 * assumed to be aligned.
	 * @param param Intrinsic camera parameters for depth image
	 * @param depth depth image.  each value is in millimeters.
	 * @param rgb Color image that's aligned to the depth.
	 * @param cloud Output point cloud
	 * @param cloudColor Output color for each point in the cloud
	 */
	public static void depthTo3D(CameraPinholeRadial param , Planar<GrayU8> rgb , GrayU16 depth ,
								 FastQueue<Point3D_F64> cloud , FastQueueArray_I32 cloudColor ) {
		cloud.reset();
		cloudColor.reset();

		RemoveRadialPtoN_F64 p2n = new RemoveRadialPtoN_F64();
		p2n.setK(param.fx,param.fy,param.skew,param.cx,param.cy).setDistortion(param.radial,param.t1,param.t2);

		Point2D_F64 n = new Point2D_F64();

		GrayU8 colorR = rgb.getBand(0);
		GrayU8 colorG = rgb.getBand(1);
		GrayU8 colorB = rgb.getBand(2);

		for( int y = 0; y < depth.height; y++ ) {
			int index = depth.startIndex + y*depth.stride;
			for( int x = 0; x < depth.width; x++ ) {
				int mm = depth.data[index++] & 0xFFFF;

				// skip pixels with no depth information
				if( mm == 0 )
					continue;

				// this could all be precomputed to speed it up
				p2n.compute(x,y,n);

				Point3D_F64 p = cloud.grow();
				p.z = mm;
				p.x = n.x*p.z;
				p.y = n.y*p.z;

				int color[] = cloudColor.grow();

				color[0] = colorR.unsafe_get(x,y);
				color[1] = colorG.unsafe_get(x,y);
				color[2] = colorB.unsafe_get(x,y);
			}
		}
	}
}
