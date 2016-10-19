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

package boofcv.alg.background;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F32;

/**
 * <p>
 * Base class for classifying pixels and background based on the apparent motion of pixels when the camera is moving.
 * The camera motion is provided externally.
 * </p>
 *
 * There are three coordinate systems.
 * <ol>
 * <li><b>World</b> is the background model's coordinate system.</li>
 * <li><b>Home</b> is the image which all the camera motion is relative to</li>
 * <li><b>Current</b> is the location of the current image.</li>
 * </ol>
 *
 * <p>
 * The background model is composed of a single fixed sized image.  The background image size is specified in
 * the {@link #initialize(int, int, InvertibleTransform)} function.  After that the background model is updated
 * by calling {@link #updateBackground(InvertibleTransform, ImageBase)}.  To flag pixels as background/motion
 * call {@link #segment(InvertibleTransform, ImageBase, GrayU8)}.
 * </p>
 *
 * <p>
 * If a pixel in the current frame has no corresponding pixel in the background or the background
 * pixel hasn't been observed yet then it will be assigned a special value, which is user configurable.
 * The default value is 0, which is a background pixel. See {@link #setUnknownValue(int)}}
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class BackgroundModelMoving<T extends ImageBase,MotionModel extends InvertibleTransform<MotionModel>>
	extends BackgroundModel<T>
{

	// Convert the motion model into a usable format
	protected Point2Transform2Model_F32<MotionModel> transform;

	// transforms
	protected MotionModel homeToWorld;
	protected MotionModel worldToHome;

	protected MotionModel currentToWorld;
	protected MotionModel worldToCurrent;

	// width and height of background image.  must be set by child
	protected int backgroundWidth;
	protected int backgroundHeight;

	// storage for corners which are used to find bounding box
	protected Point2D_F32 corners[] = new Point2D_F32[4];
	// storage for transformed coordinate
	protected Point2D_F32 work = new Point2D_F32();


	/**
	 * Constructor which provides the motion model and image type
	 * @param transform Point transform which can be used to apply the motion model
	 * @param imageType Type of input image
	 */
	public BackgroundModelMoving(Point2Transform2Model_F32<MotionModel> transform, ImageType<T> imageType) {
		super(imageType);
		this.transform = transform;

		this.homeToWorld = transform.newInstanceModel();
		this.worldToHome = transform.newInstanceModel();
		this.currentToWorld = transform.newInstanceModel();
		this.worldToCurrent = transform.newInstanceModel();

		for (int i = 0; i < corners.length; i++) {
			corners[i] = new Point2D_F32();
		}
	}

	/**
	 * Initializes background model.  Specifies the size of the background image and transform from the "home" image
	 * to the background "world"
	 *
	 * @param backgroundWidth Width of background
	 * @param backgroundHeight Height of background
	 * @param homeToWorld Transform from home to world.
	 */
	public abstract void initialize( int backgroundWidth , int backgroundHeight , MotionModel homeToWorld );

	/**
	 * Updates the background with new image information.
	 *
	 * @param homeToCurrent Transform from home image to the current image
	 * @param frame The current image in the sequence
	 */
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
		x1++;y1++;

		if( x0 < 0 ) x0 = 0;
		if( x1 > backgroundWidth ) x1 = backgroundWidth;
		if( y0 < 0 ) y0 = 0;
		if( y1 > backgroundHeight ) y1 = backgroundHeight;

		updateBackground(x0,y0,x1,y1,frame);
	}

	/**
	 * Call to update the background with the frame inside the bounding box.  Implementing class needs to
	 * make sure the rectangle is inside the background.
	 */
	protected abstract void updateBackground( int x0 , int y0 , int x1 , int y1 , T frame );

	/**
	 * Invoke to use the background image to segment the current frame into background and foreground pixels
	 *
	 * @param homeToCurrent  Transform from home image to the current image
	 * @param frame current image
	 * @param segmented Segmented image. 0 = background, 1 = foreground/moving
	 */
	public void segment( MotionModel homeToCurrent , T frame , GrayU8 segmented ) {
		InputSanityCheck.checkSameShape(frame,segmented);

		worldToHome.concat(homeToCurrent, worldToCurrent);
		worldToCurrent.invert(currentToWorld);

		_segment(currentToWorld,frame,segmented);
	}

	protected abstract void _segment( MotionModel currentToWorld , T frame , GrayU8 segmented );
}
