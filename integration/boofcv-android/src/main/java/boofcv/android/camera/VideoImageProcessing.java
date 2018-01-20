/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.android.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import boofcv.android.ConvertBitmap;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Visualizes a process where the output is simply a rendered Bitmap image.  Provides an input image
 * in a BoofCV image and a Bitmap to render the output to.
 *
 * @author Peter Abeles
 */
public abstract class VideoImageProcessing<T extends ImageBase<T>> extends VideoRenderProcessing<T> {

	// output image which is modified by processing thread
	private Bitmap output;
	// output image which is displayed by the GUI
	private Bitmap outputGUI;
	// storage used during image convert
	private byte[] storage;

	/**
	 * Constructor
	 *
	 * @param imageType Type of image the video stream is to be converted to
	 */
	protected VideoImageProcessing(ImageType<T> imageType) {
		super(imageType);
	}

	@Override
	protected void declareImages( int width , int height ) {
		super.declareImages(width,height);
		output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
		outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
		storage = ConvertBitmap.declareStorage(output,storage);
	}

	@Override
	protected void process(T gray) {
		process(gray,output,storage);
		synchronized ( lockGui ) {
			Bitmap tmp = output;
			output = outputGUI;
			outputGUI = tmp;
		}
	}

	@Override
	protected void render(Canvas canvas, double imageToOutput) {
		synchronized ( lockGui ) {
			canvas.drawBitmap(outputGUI,0,0,null);
		}
	}

	/**
	 * Process video stream and computes output Bitmap for display.
	 *
	 * @param image (Input) Video stream translated into a BoofCV type image.
	 * @param output (Output) Storage for Bitmap image which is to be displayed.
	 * @param storage Array which can be used when converting the boofcv image into a Bitmap.
	 */
	protected abstract void process( T image , Bitmap output , byte[] storage );
}
