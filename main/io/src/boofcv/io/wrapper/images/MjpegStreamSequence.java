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

package boofcv.io.wrapper.images;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoMjpegCodec;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Instead of loading and decompressing the whole MJPEG at once, it loads the images
 * one at a time until it reaches the end of the file.
 *
 * @author Peter Abeles
 */
public class MjpegStreamSequence<T extends ImageBase>
		implements SimpleImageSequence<T>
{
	VideoMjpegCodec codec = new VideoMjpegCodec();
	DataInputStream in;
	BufferedImage original;
	BufferedImage next;
	T image;
	int frameNumber;
	ImageType<T> imageType;

	public MjpegStreamSequence( InputStream in , ImageType<T> imageType ) {
		this.in = new DataInputStream(in);
		this.imageType = imageType;
		image = imageType.createImage(1,1);
		readNext();
	}

	public MjpegStreamSequence( String fileName , ImageType<T> imageType ) throws FileNotFoundException {
		this(new DataInputStream(new BufferedInputStream(new FileInputStream(fileName),1024*200)),imageType);
	}

	private void readNext() {
		byte[] data = codec.readFrame(in);
		if( data == null ) {
			next = null;
		} else {
			try {
				next = ImageIO.read(new ByteArrayInputStream(data));
				frameNumber++;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public int getNextWidth() {
		return next.getWidth();
	}

	@Override
	public int getNextHeight() {
		return next.getHeight();
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public T next() {
		original = next;
		image.reshape(original.getWidth(),original.getHeight());
		ConvertBufferedImage.convertFrom(original,image, true);
		readNext();
		return image;
	}

	@Override
	public BufferedImage getGuiImage() {
		return original;
	}

	@Override
	public void close() {
		try {
			in.close();
		} catch (IOException e) {
		}
		in = null;
	}

	@Override
	public int getFrameNumber() {
		return frameNumber-1;
	}

	@Override
	public void setLoop(boolean loop) {
		throw new RuntimeException("Can't loop");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public void reset() {
		throw new RuntimeException("Reset not supported");
	}
}
