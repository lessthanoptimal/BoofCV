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

/**
 * Interface for processing and visualizing camera previews on Android devices.  {@link VideoDisplayActivity}
 * class handles all the low level Android boiler plate for capturing and displaying video data.  When new a new frame
 * arrives {@link #convertPreview} is called and very little time can be sent in this function or else the
 * application will crash. When the view is ready to be updated {@link #onDraw} is called, which should be
 * executed quickly to avoid slowing down the GUI.  To accomplish for of these speed requirements it is
 * recommended that a new thread be launched and used to process incoming video data and compute the output.
 *
 * @see VideoImageProcessing
 * @see VideoRenderProcessing
 *
 * @author Peter Abeles
 */
public interface VideoProcessing {

	public void init( View view , Camera camera , Camera.CameraInfo info , int rotation );

	/**
	 * Invoked by Android GUI thread.  Should be run as fast as possible to avoid making the GUI feel sluggish.
	 *
	 * @param canvas Use this canvas to draw results onto.  Already be adjusted for the display and camera
	 *               preview size.   .
	 */
	public void onDraw(Canvas canvas);


	/**
	 * Called inside the camera preview thread after new data has arrived.  Must be as fast as possible to avoid
	 * crashing the application.  Just convert the NV21 data into a more useful format
	 *
	 * @param bytes Data from preview image in NV21 format.
	 * @param camera Reference to camera
	 */
	public void convertPreview( byte[] bytes, Camera camera );

	/**
	 * Blocks until all processing has stopped
	 */
	public void stopProcessing();

}
