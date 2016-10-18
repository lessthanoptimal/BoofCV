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
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Create a sequence from an array of jpeg images in byte[] array format.  Each image is decompressed
 * as need
 *
 * @author Peter Abeles
 */
public class JpegByteImageSequence<T extends ImageBase> implements SimpleImageSequence<T> {

	int index;
	List<byte[]> jpegData = new ArrayList<>();

	// type of image it outputs
	ImageType<T> imageType;

	BufferedImage imageGUI;
	T output;

	BufferedImage imageNext;

	// loop back and forth in the sequence
	boolean loop = false;
	// is it traversing in the forwards or backwards direction
	boolean forward = true;

	public JpegByteImageSequence(ImageType<T> imageType, List<byte[]> jpegData, boolean loop) {
		this.imageType = imageType;
		this.jpegData = jpegData;
		this.loop = loop;

		output = imageType.createImage(1,1);
		loadNext();
	}

	public JpegByteImageSequence(Class<T> imageType, List<byte[]> jpegData, boolean loop) {
		this(ImageType.single((Class)imageType), jpegData,loop);
	}

	@Override
	public int getNextWidth() {
		return imageNext.getWidth();
	}

	@Override
	public int getNextHeight() {
		return imageNext.getHeight();
	}

	@Override
	public boolean hasNext() {
		return loop || index < jpegData.size();
	}

	@Override
	public T next() {
		imageGUI = imageNext;
		output.reshape(imageGUI.getWidth(),imageGUI.getHeight());
		ConvertBufferedImage.convertFrom(imageGUI, output, true);

		if(forward) {
			index++;
			if( loop && index >= jpegData.size() ) {
				index = jpegData.size()-1;
				forward = false;
			}
		} else {
			index--;
			if( loop && index < 0) {
				index = 1;
				forward = true;
			}
		}
		if( hasNext())
			loadNext();

		return output;
	}

	private void loadNext() {
		try {
			imageNext = ImageIO.read(new ByteArrayInputStream(jpegData.get(index)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	@Override
	public BufferedImage getGuiImage() {
		return imageGUI;
	}

	@Override
	public void close() {
	}

	@Override
	public int getFrameNumber() {
		return index;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public void reset() {
		index = 0;
		forward = true;
	}
}
