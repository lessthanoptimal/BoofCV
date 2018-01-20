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

package boofcv.io.jcodec;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Picture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Reads movie files using JCodec
 *
 * @author Peter Abeles
 */
public class JCodecSimplified<T extends ImageBase<T>> implements SimpleImageSequence<T> {

	FrameGrab grabber;

	T image;
	// type of output image
	ImageType<T> typeOutput;

	Picture frameCurrent;
	Picture frameNext;
	int frame = 0;

	String filename;

	public JCodecSimplified(String filename, ImageType<T> typeOutput) {
		image = typeOutput.createImage(1,1);
		this.typeOutput = typeOutput;
		this.filename = filename;
		reset();
	}

	@Override
	public int getNextWidth() {
		return frameNext.getWidth();
	}

	@Override
	public int getNextHeight() {
		return frameNext.getHeight();
	}

	@Override
	public boolean hasNext() {
		return frameNext != null;
	}

	@Override
	public T next() {
		frameCurrent = frameNext;
		try {
			frameNext = grabber.getNativeFrame();
		} catch (IOException e) {
			frameNext = null;
		}

		image.reshape(frameCurrent.getWidth(), frameCurrent.getHeight());
		UtilJCodec.convertToBoof(frameCurrent, image);
		frame++;
		return image;
	}

	@Override
	public <InternalImage> InternalImage getGuiImage() {
		Planar<GrayU8> boofColor = new Planar<>(GrayU8.class,
				frameCurrent.getWidth(),frameCurrent.getHeight(),3);

		BufferedImage output = new BufferedImage(boofColor.width,boofColor.height,BufferedImage.TYPE_INT_RGB);

		UtilJCodec.convertToBoof(frameCurrent,boofColor);
		ConvertBufferedImage.convertTo(boofColor,output,true);
		return (InternalImage)output;
	}

	@Override
	public void close() {
	}

	@Override
	public int getFrameNumber() {
		return frame;
	}

	@Override
	public void setLoop(boolean loop) {
		if( loop )
			throw new IllegalArgumentException("Not supported");
	}

	@Override
	public ImageType<T> getImageType() {
		return typeOutput;
	}

	@Override
	public void reset() {
		try {
			grabber = new FrameGrab(NIOUtils.readableFileChannel(new File(filename)));
		} catch (IOException | JCodecException e) {
			throw new RuntimeException(e);
		}
		try {
			frameCurrent = null;
			frameNext = grabber.getNativeFrame();
		} catch (IOException e) {
			frameNext = null;
		}
	}
}
