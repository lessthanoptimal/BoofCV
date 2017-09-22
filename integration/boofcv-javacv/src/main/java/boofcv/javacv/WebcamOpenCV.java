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

package boofcv.javacv;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.WebcamInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.ddogleg.struct.GrowQueue_I8;

/**
 * @author Peter Abeles
 */
public class WebcamOpenCV implements WebcamInterface {
	@Override
	public <T extends ImageBase<T>> SimpleImageSequence<T>
	open(String device, int width, int height, ImageType<T> imageType)
	{
		OpenCVFrameGrabber grabber = null;
		if( device != null ) {
			try {
				int which = Integer.parseInt(device);
				grabber = new OpenCVFrameGrabber(which);
			}catch (NumberFormatException ignore) {
				grabber = new OpenCVFrameGrabber(0);
			}
			if( grabber == null ) {
				throw new RuntimeException("Can't find webcam with ID or name at "+device);
			}
		} else {
			grabber = new OpenCVFrameGrabber(0);
		}
		grabber.setImageWidth(width);
		grabber.setImageHeight(height);
		return new SimpleSequence<>(grabber, imageType);
	}

	public static class SimpleSequence<T extends ImageBase<T>> implements SimpleImageSequence<T> {

		OpenCVFrameGrabber grabber;
		int width,height;

		T output;

		int frameNumber;

		GrowQueue_I8 work = new GrowQueue_I8();

		boolean bgr_to_rgb;

		public SimpleSequence(OpenCVFrameGrabber grabber, ImageType<T> imageType) {
			this.grabber = grabber;

			try {
				this.grabber.start();
			} catch (FrameGrabber.Exception e) {
				throw new RuntimeException(e);
			}

			bgr_to_rgb = grabber.getPixelFormat() == 1;

			width = grabber.getImageWidth();
			height = grabber.getImageHeight();
			output = imageType.createImage(width,height);
		}

		@Override
		public int getNextWidth() {
			return width;
		}

		@Override
		public int getNextHeight() {
			return height;
		}

		@Override
		public boolean hasNext() {
			try {
				Frame image = grabber.grab();
				frameNumber = grabber.getFrameNumber();
				ConvertOpenCvFrame.convert(image,output,bgr_to_rgb,work);
				return true;
			} catch (FrameGrabber.Exception e) {
				return false;
			}
		}

		@Override
		public T next() {
			return output;
		}

		@Override
		public <InternalImage> InternalImage getGuiImage() {
			return null;
		}

		@Override
		public void close() {
			try {
				grabber.stop();
			} catch (FrameGrabber.Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getFrameNumber() {
			return frameNumber;
		}

		@Override
		public void setLoop(boolean loop) {

		}

		@Override
		public ImageType<T> getImageType() {
			return output.getImageType();
		}

		@Override
		public void reset() {

		}
	}
}
