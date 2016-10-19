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

package boofcv.alg.sfm.overhead;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;

/**
 * <p>
 * Converts a camera image into an overhead orthogonal view with known metric properties given a known transform from the
 * plane to camera.  This will only produce a valid orthogonal view when the
 * surface being viewed is entirely planar, non-planar objects are heavily distorted.  See {@link OverheadView}
 * for more details.
 * </p>

 * <p>
 * Usage Notes:
 * <ul>
 *     <li>The plane is defined using a {@link Se3_F64} which describes the transform from the
 *     plane to the camera.</li>
 *     <li>In the plane's reference frame the plane's normal vector is (0,1,0) or (0,-1,0) and
 *     contains the point (0,0,0).</li>
 * </ul>
 * </p>
 *
 * <p>
 * Implementation Notes:<br>
 * The transform is precomputed and stored in an array which is w*h*2*8 bytes, where (w,h) is the size of the overhead
 * image.
 * </p>

 * @author Peter Abeles
 */
public abstract class CreateSyntheticOverheadView<T extends ImageBase>
{
	// size of overhead image;
	protected int overheadWidth;
	protected int overheadHeight;

	// pixel coordinate for each pixel in the overhead image
	// if an element is null that means there is no corresponding image pixel
	protected Point2D_F32 mapPixels[];

	private FastQueue<Point2D_F32> points = new FastQueue<>(Point2D_F32.class, true);

	/**
	 * Specifies camera configurations.
	 * @param intrinsic Intrinsic camera parameters
	 * @param planeToCamera Transform from the plane to the camera.  This is the extrinsic parameters.
	 * @param centerX X-coordinate of camera center in the overhead image in world units.
	 * @param centerY Y-coordinate of camera center in the overhead image in world units.
	 * @param cellSize Size of each cell in the overhead image in world units.
	 * @param overheadWidth Number of columns in overhead image
	 * @param overheadHeight Number of rows in overhead image
	 */
	public void configure( CameraPinholeRadial intrinsic ,
						   Se3_F64 planeToCamera ,
						   double centerX, double centerY, double cellSize ,
						   int overheadWidth , int overheadHeight )
	{
		this.overheadWidth = overheadWidth;
		this.overheadHeight = overheadHeight;

		Point2Transform2_F64 normToPixel = LensDistortionOps.transformPoint(intrinsic).distort_F64(false, true);

		// Declare storage for precomputed pixel locations
		int overheadPixels = overheadHeight*overheadWidth;
		if( mapPixels == null || mapPixels.length < overheadPixels) {
			mapPixels = new Point2D_F32[overheadPixels];
		}
		points.reset();

		// -------- storage for intermediate results
		Point2D_F64 pixel = new Point2D_F64();
		// coordinate on the plane
		Point3D_F64 pt_plane = new Point3D_F64();
		// coordinate in camera reference frame
		Point3D_F64 pt_cam = new Point3D_F64();

		int indexOut = 0;
		for( int i = 0; i < overheadHeight; i++ ) {
			pt_plane.x = -(i*cellSize - centerY);
			for( int j = 0; j < overheadWidth; j++ , indexOut++ ) {
				pt_plane.z = j*cellSize - centerX;

				// plane to camera reference frame
				SePointOps_F64.transform(planeToCamera, pt_plane, pt_cam);

				// can't see behind the camera
				if( pt_cam.z > 0 ) {
					// compute normalized then convert to pixels
					normToPixel.compute(pt_cam.x/pt_cam.z,pt_cam.y/pt_cam.z,pixel);

					float x = (float)pixel.x;
					float y = (float)pixel.y;

					// make sure it's in the image
					if(BoofMiscOps.checkInside(intrinsic.width,intrinsic.height,x,y) ){
						Point2D_F32 p = points.grow();
						p.set(x,y);
						mapPixels[ indexOut ]= p;
					} else {
						mapPixels[ indexOut ]= null;
					}
				}
			}
		}
	}

	/**
	 * Returns corresponding pixel to pixel coordinate in overhead image
	 * @param x overhead pixel x-coordinate
	 * @param y overhead pixel y-coordinate
	 * @return Pixel in camera image
	 */
	public Point2D_F32 getOverheadToPixel( int x , int y ) {
		return mapPixels[y*overheadWidth + x];
	}

	/**
	 * Computes overhead view of input image.
	 *
	 * @param input (Input) Camera image.
	 * @param output (Output) Image containing overhead view.
	 */
	public abstract void process(T input,  T output);
}
