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

package boofcv.gui;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;

/**
 * Circular cache for output from {@link SimpleImageSequence}.  Stores both the boofcv image and the associated
 * BufferedImage.
 *
 * @author Peter Abeles
 */
public class CacheSequenceStream<T extends ImageBase<T>> {

	SimpleImageSequence<T> sequence;

	T queueBoof[];
	BufferedImage queueBuff[];

	int offset;
	int size;

	public CacheSequenceStream( int length , ImageType<T> imageType ) {
		queueBoof = imageType.createArray(length);
		queueBuff = new BufferedImage[length];

		for (int i = 0; i < length; i++) {
			queueBoof[i] = imageType.createImage(1,1);
			queueBuff[i] = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
		}
	}

	public void setSequence(SimpleImageSequence<T> sequence) {
		this.sequence = sequence;
	}

	public void reset() {
		offset = 0;
		size = 0;
		sequence = null;
	}

	public boolean isCacheFull() {
		return( size >= queueBoof.length );
	}

	public boolean hasNext() {
		return sequence.hasNext();
	}

	public void cacheNext() {
		if( isCacheFull() )
			throw new IllegalArgumentException("Cache is already full.  Should have checked first");
		int index = (offset+size)%queueBoof.length;

		T sBoof = sequence.next();
		BufferedImage sBuff = sequence.getGuiImage();

		queueBoof[index].setTo(sBoof);
		queueBuff[index] = ConvertBufferedImage.checkCopy(sBuff,queueBuff[index]);

		size++;
	}

	public T getBoofImage() {
		int index = (offset+size)%queueBoof.length;
		return queueBoof[index];
	}

	public BufferedImage getBufferedImage() {
		int index = (offset+size)%queueBoof.length;
		return queueBuff[index];
	}

	public void release() {
		offset = (offset+1) % queueBoof.length;
	}

}
