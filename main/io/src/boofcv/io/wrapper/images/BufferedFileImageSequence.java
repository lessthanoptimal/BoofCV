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
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Loads or plays a sequence of buffered images.
 *
 * @author Peter Abeles
 */
public class BufferedFileImageSequence<T extends ImageBase> implements SimpleImageSequence<T> {

	BufferedImage orig[];
	T[] images;
	int index;

	BufferedImage imageGUI;

	// type of image it outputs
	ImageType<T> type;

	boolean loop = true;
	boolean forwards = true;

	/**
	 * Will load an image sequence with no modification.
	 *
	 * @param directory The directory containing the images.
	 * @param suffix	The suffix that the images have.
	 */
	public BufferedFileImageSequence(ImageType<T> type, File directory, String suffix) {
		this.type = type;

		if (!directory.isDirectory()) throw new IllegalArgumentException("directory must specify a directory");

		String[] files = directory.list(new Filter(suffix));

		List<String> listNames = new ArrayList<>();
		for( String s : files ) {
			listNames.add(s);
		}

		Collections.sort(listNames);

		orig = new BufferedImage[ files.length ];
		images = type.createArray( files.length );
		int index = 0;
		for (String s : listNames) {
			BufferedImage b = orig[index] = UtilImageIO.loadImage(directory.getPath()+"/"+s);
			T a = images[index++] = type.createImage(b.getWidth(),b.getHeight());
			ConvertBufferedImage.convertFrom(b, a, true);
		}
	}

	/**
	 *
	 */
	public BufferedFileImageSequence(ImageType<T> type, BufferedImage[] orig) {
		this.type = type;
		this.orig = orig;
		images = type.createArray( orig.length );

		for( int i = 0; i < orig.length; i++ ) {
			BufferedImage b = orig[i];
			images[i] = type.createImage(b.getWidth(),b.getHeight());
			ConvertBufferedImage.convertFrom(orig[i], images[i], true);
		}
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	@Override
	public int getNextWidth() {
		return imageGUI.getWidth();
	}

	@Override
	public int getNextHeight() {
		return imageGUI.getHeight();
	}

	/**
	 * True if there is another image to read and false if there are no more.
	 */
	public boolean hasNext() {
		if( loop )
			return true;
		else
			return index < images.length;
	}

	/**
	 * Loads the next image into a BufferedImage and returns it. The same instance
	 * or a new instance of a BufferedImage might be returned each time.  Don't rely
	 * on either behavior being consistent.
	 *
	 * @return A BufferedImage containing the next image.
	 */
	public T next() {
		if( loop ) {
			if( forwards ) {
				if( index >= images.length ) {
					index = images.length-1;
					forwards = false;
				}
			} else {
				if( index < 0 ) {
					index = 0;
					forwards = true;
				}
			}
		}

		this.imageGUI = orig[index];

		if( forwards )
			return images[index++];
		else
			return images[index--];

	}

	@Override
	public BufferedImage getGuiImage() {
		return imageGUI;
	}

	@Override
	public ImageType<T> getImageType() {
		return type;
	}

	@Override
	public int getFrameNumber() {
		return index-1;
	}

	@Override
	public void close() {
	}

	private class Filter implements FilenameFilter {

		String suffix;

		private Filter(String suffix) {
			this.suffix = suffix;
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.contains(suffix);
		}
	}

	@Override
	public void reset() {
		index = 0;
		forwards = true;
	}
}
