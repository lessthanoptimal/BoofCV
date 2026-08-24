/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.io.ffmpeg;

import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import org.bytedeco.copiedstuff.FFmpegFrameGrabber;
import org.bytedeco.copiedstuff.Frame;
import org.bytedeco.copiedstuff.FrameGrabber;
import org.bytedeco.copiedstuff.Java2DFrameConverter;

import java.awt.image.BufferedImage;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

/// Uses JavaCV, which uses FFMPEG, to read in a video.
public class FfmpegVideoImageSequence<T extends ImageBase<T>> implements SimpleImageSequence<T> {
	String filename;
	FFmpegFrameGrabber frameGrabber;
	ImageType<T> imageType;

	Java2DFrameConverter converter;

	boolean finished = false;

	BufferedImage current;
	BufferedImage next;
	T currentBoof;
	int frameNumber;
	// Image dimensions. Cached in reset() because 'next' is the look ahead frame and could NPE
	@Getter int width, height;

	public FfmpegVideoImageSequence( String filename, ImageType<T> imageType ) {
		// Turn off that super annoying error message!
		av_log_set_level(AV_LOG_ERROR);

		this.filename = filename;
		this.imageType = imageType;
		converter = new Java2DFrameConverter();
		reset();
		if (finished)
			throw new RuntimeException("FFMPEG failed to open file. " + filename);
	}

	@Override public boolean hasNext() {
		return !finished;
	}

	@Override public T next() {
		if (finished)
			return null;

		current.createGraphics().drawImage(next, 0, 0, null);
		try {
			next = converter.convert(frameGrabber.grab());
			frameNumber++;
			// grab() returns null at the end of the stream.
			if (next == null)
				finished = true;
		} catch (FrameGrabber.Exception e) {
			finished = true;
		}
		if (frameNumber >= frameGrabber.getLengthInFrames())
			finished = true;
		ConvertBufferedImage.convertFrom(current, currentBoof, true);
		return currentBoof;
	}

	@Override public T getImage() {
		return currentBoof;
	}

	@Override public <InternalImage> InternalImage getGuiImage() {
		return (InternalImage)current;
	}

	@Override public void close() {
		try {
			frameGrabber.stop();
			finished = true;
		} catch (FrameGrabber.Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override public int getFrameNumber() {
		return frameNumber;
	}

	@Override public void setLoop( boolean loop ) {}

	@Override public ImageType<T> getImageType() {
		return imageType;
	}

	@Override public void reset() {
		filename = UtilIO.checkIfJarAndCopyToTemp(filename);

		// Release the native resources held by a previous grabber.
		if (frameGrabber != null) {
			try {
				frameGrabber.release();
			} catch (FrameGrabber.Exception ignore) {
			}
		}

		this.frameGrabber = new FFmpegFrameGrabber(filename);
		try {
			frameNumber = 0;
			finished = false;
			frameGrabber.start();
		} catch (FrameGrabber.Exception e) {
//			e.printStackTrace();
			finished = true;
			return;
		}

		try {
			Frame frame = frameGrabber.grab();
			next = converter.convert(frame);
			width = next.getWidth();
			height = next.getHeight();
			current = new BufferedImage(width, height, next.getType());
			currentBoof = imageType.createImage(width, height);
		} catch (FrameGrabber.Exception e) {
			finished = true;
		}
	}
}
