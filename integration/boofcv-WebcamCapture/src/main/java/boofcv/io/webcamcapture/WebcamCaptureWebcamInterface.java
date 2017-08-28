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

package boofcv.io.webcamcapture;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.WebcamInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Wrapper around webcam capture which allows its images to be used inside the {@link SimpleImageSequence}.
 *
 * @author Peter Abeles
 */
public class WebcamCaptureWebcamInterface implements WebcamInterface {
	@Override
	public <T extends ImageBase<T>> SimpleImageSequence<T>
	open(String device, int width, int height, ImageType<T> imageType) {

		if( device != null ) {
			Webcam webcam;
			try {
				int which = Integer.parseInt(device);
				webcam = Webcam.getWebcams().get(which);
			}catch (NumberFormatException ignore) {
				webcam = UtilWebcamCapture.findDevice(device);
			}
			if( webcam == null ) {
				throw new RuntimeException("Can't find webcam with ID or name at "+device);
			}

			try {
				if (width >= 0 && height >= 0) {
					UtilWebcamCapture.adjustResolution(webcam, width, height);
				}
				webcam.open();

				return new SimpleSequence<>(webcam, imageType);
			} catch (RuntimeException ignore) {}
		}
		return new SimpleSequence<>(device, width, height, imageType);
	}

	public static class SimpleSequence<T extends ImageBase<T>> implements SimpleImageSequence<T> {

		Webcam webcam;
		int width,height;

		T output;
		BufferedImage bufferedImage;
		int frames = 0;


		public SimpleSequence(String device, int width, int height, ImageType<T> imageType) {
			this(UtilWebcamCapture.openDefault(width,height),imageType);
		}

		public SimpleSequence(Webcam webcam, ImageType<T> imageType) {
			this.webcam = webcam;

			Dimension d = webcam.getDevice().getResolution();
			width = d.width;
			height = d.height;

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
			bufferedImage = webcam.getImage();
			return bufferedImage != null;
		}

		@Override
		public T next() {
			if( bufferedImage == null )
				bufferedImage = webcam.getImage();
			ConvertBufferedImage.convertFrom(bufferedImage, output, true);
			return output;
		}

		@Override
		public void close() {
			webcam.close();
		}

		@Override
		public int getFrameNumber() {
			return frames;
		}

		@Override
		public void setLoop(boolean loop) {}

		@Override
		public ImageType<T> getImageType() {
			return output.getImageType();
		}

		@Override
		public void reset() {
			throw new RuntimeException("Not supported");
		}

		@Override
		public BufferedImage getGuiImage() {
			return bufferedImage;
		}
	}
}
