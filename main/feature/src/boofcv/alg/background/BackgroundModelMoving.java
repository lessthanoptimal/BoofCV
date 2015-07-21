/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.distort.PointTransformModel_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;

/**
 * @author Peter Abeles
 */
public abstract class BackgroundModelMoving<T extends ImageBase,MotionModel extends InvertibleTransform<MotionModel>>
{

	protected ImageType<T> imageType;
	protected PointTransformModel_F32<MotionModel> transform;

	protected MotionModel homeToWorld;
	protected MotionModel worldToHome;

	protected MotionModel currentToWorld;
	protected MotionModel worldToCurrent;

	protected Point2D_F32 corners[] = new Point2D_F32[4];

	protected Point2D_F32 work = new Point2D_F32();

	public BackgroundModelMoving(PointTransformModel_F32<MotionModel> transform, ImageType<T> imageType) {
		this.transform = transform;
		this.imageType = imageType;

		this.homeToWorld = transform.newInstanceModel();
		this.currentToWorld = transform.newInstanceModel();
		this.worldToCurrent = transform.newInstanceModel();

		for (int i = 0; i < corners.length; i++) {
			corners[i] = new Point2D_F32();
		}
	}

	public abstract void initialize( int backgroundWidth , int backgroundHeight , MotionModel homeToWorld );

	public abstract void reset();

	public void updateBackground(MotionModel homeToCurrent, T frame) {
		worldToHome.concat(homeToCurrent, worldToCurrent);
		worldToCurrent.invert(currentToWorld);

		// find the distorted polygon of the current image in the "home" background reference frame
		transform.setModel(currentToWorld);
		transform.compute(0, 0, corners[0]);
		transform.compute(frame.width-1,0,corners[1]);
		transform.compute(frame.width-1,frame.height-1,corners[2]);
		transform.compute(0, frame.height-1, corners[3]);

		// find the bounding box
		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int x1 = -Integer.MAX_VALUE;
		int y1 = -Integer.MAX_VALUE;

		for (int i = 0; i < 4; i++) {
			Point2D_F32 p = corners[i];
			int x = (int)p.x;
			int y = (int)p.y;

			if( x0 > x ) x0 = x;
			if( y0 > y ) y0 = y;
			if( x1 < x ) x1 = x;
			if( y1 < y ) y1 = y;
		}

		updateBackground(x0,y0,x1+1,y1+1,frame);
	}

	protected abstract void updateBackground( int x0 , int y0 , int x1 , int y1 , T frame );

	public void segment( MotionModel homeToCurrent , T frame , ImageUInt8 segmented ) {
		InputSanityCheck.checkSameShape(frame,segmented);

		worldToHome.concat(homeToCurrent, worldToCurrent);
		worldToCurrent.invert(currentToWorld);

		_segment(currentToWorld,frame,segmented);
	}

	protected abstract void _segment( MotionModel currentToWorld , T frame , ImageUInt8 segmented );

	public ImageType<T> getImageType() {
		return imageType;
	}
}
