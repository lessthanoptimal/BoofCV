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

package boofcv.android.gui;

import android.graphics.Canvas;
import android.hardware.Camera;
import android.view.View;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.encoding.ConvertNV21;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;

/**
 * <p>
 * Processing class for displaying more complex visualizations data.  Children of this class must properly lock
 * down the GUI when processing data that can be read/written to when updating GUI.  This is done by synchronizing
 * around the class variable {@link #lockGui}.
 * </p>
 *
 * <p>
 * The canvas is resized and centered for display purposes.  The scale and translation factors applied to the
 * canvas prior to it being passed in to the child of this class can be access through the class parameters or
 * getter functions. The size of the output image can be accessed in a similar manor, see outputWidth and
 * outputHeight.  The just mentioned output size is used to compute the scale and translation and can be
 * overridden inside {@link #init} or {@link #declareImages(int, int)}.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class VideoRenderProcessing<T extends ImageBase> extends Thread implements VideoProcessing {

	// Type of BoofCV iamge
	ImageType<T> imageType;

	// BoofCV images which the video stream is converted into.  At any given time one is continuously being
	// written to while the other is being processed by the image processing routine
	T image;
	T image2;

	volatile boolean requestStop = false;
	volatile boolean running = false;

	// size of the area being down for output.  defaults to image size
	// used to compute the transform (scale and translation) to center the image in the displau
	protected int outputWidth;
	protected int outputHeight;

	protected View view;
	// thread which is processing the video frames
	Thread thread;

	/**
	 * Lock used ever reading or writing to display related data.  User should use this to ensure
	 * that the processing() function and render() function's don't stead on each other's feet.
	 */
	protected final Object lockGui = new Object();
	/**
	 * Lock used when converting the video stream.  Should not need to be used by the user.
	 */
	protected final Object lockConvert = new Object();

	// scale and translation applied to the canvas
	protected double scale;
	protected double tranX,tranY;

	// if true the input image is flipped horizontally
	boolean flipHorizontal;

	// number of degrees the camera preview needs to be rotated
	int previewRotation;

	protected VideoRenderProcessing(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	@Override
	public void init(View view, Camera camera , Camera.CameraInfo info , int previewRotation ) {
		synchronized (lockGui) {
			this.view = view;

			// Front facing cameras need to be flipped to appear correctly
			flipHorizontal = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
			this.previewRotation = previewRotation;
			Camera.Size size = camera.getParameters().getPreviewSize();
			outputWidth = size.width;
			outputHeight = size.height;
			declareImages(size.width,size.height);
		}

		// start the thread for processing
		running = true;
		start();
	}

	@Override
	public void onDraw(Canvas canvas) {
		synchronized (lockGui) {
			// the process class could have been swapped
			if( image == null )
				return;

			int w = view.getWidth();
			int h = view.getHeight();

			// fill the window and center it
			double scaleX = w/(double)outputWidth;
			double scaleY = h/(double)outputHeight;

			scale = Math.min(scaleX,scaleY);
			tranX = (w-scale*outputWidth)/2;
			tranY = (h-scale*outputHeight)/2;

			canvas.translate((float)tranX,(float)tranY);
			canvas.scale((float)scale,(float)scale);

			render(canvas, scale);
		}
	}

	/**
	 * Converts a coordinate from pixel to the output image coordinates
	 */
	protected void imageToOutput( double x , double y , Point2D_F64 pt ) {
		pt.x = x/scale - tranX/scale;
		pt.y = y/scale - tranY/scale;
	}

	/**
	 * Converts a coordinate from output image coordinates to input image
	 */
	protected void outputToImage( double x , double y , Point2D_F64 pt ) {
		pt.x = x*scale + tranX;
		pt.y = y*scale + tranY;
	}


	@Override
	public void convertPreview(byte[] bytes, Camera camera) {
		if( thread == null )
			return;

		synchronized ( lockConvert ) {
			if( imageType.getFamily() == ImageType.Family.GRAY )
				ConvertNV21.nv21ToGray(bytes, image.width, image.height, (ImageGray) image,(Class) image.getClass());
			else if( imageType.getFamily() == ImageType.Family.PLANAR ) {
				if (imageType.getDataType() == ImageDataType.U8)
					ConvertNV21.nv21ToMsRgb_U8(bytes, image.width, image.height, (Planar) image);
				else if (imageType.getDataType() == ImageDataType.F32)
					ConvertNV21.nv21ToMsRgb_F32(bytes, image.width, image.height, (Planar) image);
				else
					throw new RuntimeException("Oh Crap");
			} else if( imageType.getFamily() == ImageType.Family.INTERLEAVED ) {
				if( imageType.getDataType() == ImageDataType.U8)
					ConvertNV21.nv21ToInterleaved(bytes, image.width, image.height, (InterleavedU8) image);
				else if( imageType.getDataType() == ImageDataType.F32)
					ConvertNV21.nv21ToInterleaved(bytes, image.width, image.height, (InterleavedF32) image);
				else
					throw new RuntimeException("Oh Crap");
			} else {
				throw new RuntimeException("Unexpected image type: "+imageType);
			}

			if( previewRotation == 180 ) {
				if( flipHorizontal ) {
					GImageMiscOps.flipVertical(image);
				} else {
					GImageMiscOps.flipVertical(image);
					GImageMiscOps.flipHorizontal(image);
				}
			} else if( flipHorizontal )
				GImageMiscOps.flipHorizontal(image);
		}
		// wake up the thread and tell it to do some processing
		thread.interrupt();
	}

	@Override
	public void stopProcessing() {
		if( thread == null )
			return;

		requestStop = true;
		while( running ) {
			// wake the thread up if needed
			thread.interrupt();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}
	}

	@Override
	public void run() {
		thread = Thread.currentThread();
		while( !requestStop ) {
			synchronized ( thread ) {
				try {
					wait();
					if( requestStop )
						break;
				} catch (InterruptedException e) {}
			}

			// swap gray buffers so that convertPreview is modifying the copy which is not in use
			synchronized ( lockConvert ) {
				T tmp = image;
				image = image2;
				image2 = tmp;
			}

			process(image2);

			view.postInvalidate();
		}
		running = false;
	}

	/**
	 * Scaling applied to the drawing canvas
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Translation x applied to the drawing canvas
	 */
	public double getTranX() {
		return tranX;
	}

	/**
	 * Translation y applied to the drawing canvas
	 */
	public double getTranY() {
		return tranY;
	}

	/**
	 * <p>Image processing is done here and is invoked in its own thread, removing any hard time constraints.
	 * When modifying data structures that are read inside of {@link #render} be sure to use synchronize with
	 * {@link #lockGui} to avoid crashes or weird visual artifacts.  Use of lockGui should be minimized to
	 * ensure a fast and responsive GUI</p>
	 *
	 */
	protected abstract void process( T gray );

	/**
	 * Results computed by {@link #process} are visualized here.  This function is called inside a
	 * synchronize(lockGui) block.  The provided canvas has been adjusted to be centered in the view and to account
	 * for the resolution difference of the preview image and the display.
	 *
	 * @param canvas Canvas which is to be displayed.
	 * @param imageToOutput Scale factor from input image to output display.  Can also be accessed via {@link #getScale}
	 */
	protected abstract void render(  Canvas canvas , double imageToOutput );

	protected void declareImages( int width , int height ) {
		image = imageType.createImage(width, height);
		image2 = imageType.createImage(width, height);
	}
}
