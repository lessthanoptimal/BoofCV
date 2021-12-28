/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.gui;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;

/**
 * Cache for output from {@link SimpleImageSequence}. Stores both the boofcv image and the associated
 * BufferedImage. Storage for two sets of images are stored. One for the IO thread and one for the algorithm.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CacheSequenceStream<T extends ImageBase<T>> {

	SimpleImageSequence<T> sequence;
	ImageType<T> imageType;
	T[] queueBoof;
	BufferedImage[] queueBuff;

	int selected;

	public CacheSequenceStream( ImageType<T> imageType ) {

		queueBoof = imageType.createArray(2);
		queueBuff = new BufferedImage[2];

		for (int i = 0; i < 2; i++) {
			queueBoof[i] = imageType.createImage(1, 1);
			queueBuff[i] = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		}
		this.imageType = imageType;
	}

	public void setSequence( SimpleImageSequence<T> sequence ) {
		this.sequence = sequence;
	}

	public void reset() {
		selected = 0;
		if (sequence != null)
			sequence.reset();
	}

	public boolean hasNext() {
		return sequence.hasNext();
	}

	public void cacheNext() {
		selected = (selected + 1)%queueBoof.length;
		sequence.next();
		T sBoof = sequence.getImage();
		BufferedImage sBuff = sequence.getGuiImage();

		queueBoof[selected].setTo(sBoof);
		queueBuff[selected] = ConvertBufferedImage.checkCopy(sBuff, queueBuff[selected]);
	}

	public T getBoofImage() {
		return queueBoof[selected];
	}

	public BufferedImage getBufferedImage() {
		return queueBuff[selected];
	}

	public void setBufferedImage( BufferedImage buff ) {
		queueBuff[selected] = ConvertBufferedImage.checkCopy(buff, queueBuff[selected]);
	}

	public int getWidth() {
		if (sequence == null)
			return queueBoof[selected].getWidth();
		return sequence.getWidth();
	}

	public int getHeight() {
		if (sequence == null)
			return queueBoof[selected].getHeight();
		return sequence.getHeight();
	}

	public ImageType<T> getImageType() {
		return imageType;
	}
}
