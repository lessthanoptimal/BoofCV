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

package boofcv.io.wrapper.images;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * Loads all the images in a directory that have the specified suffix. If requested it can
 * scale the images down. The order in which the images are returned is determined by
 * Collections.sort() by file name.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public class LoadFileImageSequence<T extends ImageBase<T>> implements SimpleImageSequence<T> {

	String directoryName;
	@Nullable String suffix;

	int index;
	java.util.List<String> fileNames = new ArrayList<>();

	// type of image it outputs
	ImageType<T> type;
	// the output image
	T image;
	int scalefactor;
	BufferedImage scaled;
	// reference to output GUI image
	@Nullable BufferedImage imageGUI;

	boolean loop = false;
	boolean forwards = true;

	/**
	 * Will load an image sequence with no modification.
	 *
	 * @param directory The directory containing the images.
	 * @param suffix The suffix that the images have.
	 */
	public LoadFileImageSequence( ImageType<T> type, String directory, @Nullable String suffix ) {
		this(type, directory, suffix, 1);
	}

	/**
	 * Will load an image sequence and then scale the images
	 *
	 * @param directory The directory containing the images.
	 * @param suffix The suffix that the images have.
	 * @param scalefactor How much the images will be scaled down by.
	 */
	public LoadFileImageSequence( ImageType<T> type, String directory, @Nullable String suffix, int scalefactor ) {
		this.directoryName = directory;
		this.suffix = suffix;
		this.scalefactor = scalefactor;
		this.type = type;

		findImages();

		// load the first image so that we get the size
		next();
		index = 0; // go back to the first image again
	}

	@Override
	public void setLoop( boolean loop ) {
		this.loop = loop;
	}

	public boolean isLoop() {
		return loop;
	}

	private void findImages() {
		File dir = new File(directoryName);

		if (!dir.isDirectory())
			throw new IllegalArgumentException("directory must specify a directory. path = " + directoryName);

		fileNames.clear();

		File[] files;
		if (suffix != null)
			files = dir.listFiles(new Filter());
		else
			files = dir.listFiles();

		if (files == null)
			return;

		for (File f : files) {
			if (UtilImageIO.isImage(f))
				fileNames.add(f.getAbsolutePath());
		}

		Collections.sort(fileNames);
	}

	@Override
	public int getWidth() {
		return image.getWidth();
	}

	@Override
	public int getHeight() {
		return image.getHeight();
	}

	/**
	 * True if there is another image to read and false if there are no more.
	 */
	@Override
	public boolean hasNext() {
		if (loop)
			return true;
		else
			return index < fileNames.size();
	}

	/**
	 * Loads the next image into a BufferedImage and returns it. The same instance
	 * or a new instance of a BufferedImage might be returned each time. Don't rely
	 * on either behavior being consistent.
	 */
	@Override
	public T next() {
		if (loop) {
			if (forwards) {
				if (index >= fileNames.size()) {
					index = fileNames.size() - 1;
					forwards = false;
				}
			} else {
				if (index < 0) {
					index = 0;
					forwards = true;
				}
			}
		}

		int indexbefore = index;
		if (forwards)
			imageGUI = UtilImageIO.loadImage(fileNames.get(index++));
		else
			imageGUI = UtilImageIO.loadImage(fileNames.get(index--));

		if (imageGUI == null)
			throw new RuntimeException("Could not load image at index " + indexbefore);

		image = type.createImage(imageGUI.getWidth(), imageGUI.getHeight());
		ConvertBufferedImage.convertFrom(imageGUI, image, true);

		// no changes needed so return the original
		if (scalefactor == 1)
			return image;

		// scale down the image
		int width = image.getWidth()/scalefactor;
		int height = image.getHeight()/scalefactor;

		if (scaled == null || scaled.getWidth() != width || scaled.getHeight() != height) {
			scaled = new BufferedImage(width, height, imageGUI.getType());
		}
		Graphics2D g2 = scaled.createGraphics();

		AffineTransform affine = new AffineTransform();
		affine.setToScale(1.0/scalefactor, 1.0/scalefactor);

		g2.drawImage(imageGUI, affine, null);
		imageGUI = scaled;
		return image;
	}

	@Override
	public T getImage() {
		return image;
	}

	@Override
	public BufferedImage getGuiImage() {
		return Objects.requireNonNull(imageGUI);
	}

	@Override
	public ImageType<T> getImageType() {
		return type;
	}

	@Override
	public int getFrameNumber() {
		return index - 1;
	}

	@Override
	public void close() {
	}

	@SuppressWarnings("NullAway")
	@Override
	public void reset() {
		index = 0;
		forwards = true;
		image = null;
		imageGUI = null;
	}

	public int getTotalImages() {
		return fileNames.size();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex( int index ) {
		this.index = index;
	}

	private class Filter implements FilenameFilter {

		@Override
		public boolean accept( File dir, String name ) {
			return name.contains(suffix);
		}
	}
}
