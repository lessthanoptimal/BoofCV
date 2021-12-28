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

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Loads and optionally scales all the images in a list.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class LoadFileImageSequence2<T extends ImageBase<T>> implements SimpleImageSequence<T> {

	int index;
	List<String> paths;

	// type of image it outputs
	ImageType<T> type;
	// the output image
	T image;

	// workspace if it needs to resize the image
	T work;

	// reference to output GUI image
	@Nullable BufferedImage imageGUI;

	boolean loop = false;
	boolean forwards = true;

	/** If > 0 then the input image will be rescaled down */
	@Getter @Setter int targetPixels = 0;

	/**
	 * Will load an image sequence with no modification.
	 */
	public LoadFileImageSequence2( List<String> paths, ImageType<T> type ) {
		this.type = type;
		this.paths = paths;

		image = type.createImage(0, 0);
		work = type.createImage(0, 0);
	}

	@Override
	public void setLoop( boolean loop ) {
		this.loop = loop;
	}

	public boolean isLoop() {
		return loop;
	}

	@Override
	public int getWidth() {
		// lazy determining of size because the user could have set targetPixels after calling constructor
		if (image.width == 0) {
			// Load the first image so that image has a correct size
			loadImage(paths.get(0));
		}
		return image.getWidth();
	}

	@Override
	public int getHeight() {
		// lazy determining of size because the user could have set targetPixels after calling constructor
		if (image.width == 0) {
			// Load the first image so that image has a correct size
			loadImage(paths.get(0));
		}
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
			return index < paths.size();
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
				if (index >= paths.size()) {
					index = paths.size() - 1;
					forwards = false;
				}
			} else {
				if (index < 0) {
					index = 0;
					forwards = true;
				}
			}
		}

		String path;

		if (forwards)
			path = paths.get(index++);
		else
			path = paths.get(index--);

		return loadImage(path);
	}

	public T loadImage( String path ) {
		imageGUI = UtilImageIO.loadImage(path);

		if (imageGUI == null)
			throw new RuntimeException("Could not load image at " + path);

		// If it doesn't need to scale the image just convert it
		int numPixels = imageGUI.getWidth()*imageGUI.getHeight();
		if (targetPixels <= 0 || numPixels <= targetPixels) {
			ConvertBufferedImage.convertFrom(imageGUI, image, true);
			return image;
		}

		// Convert the image
		ConvertBufferedImage.convertFrom(imageGUI, work, true);

		// Scale it
		double scale = Math.sqrt(targetPixels)/Math.sqrt(numPixels);
		image.reshape((int)(scale*work.width), (int)(scale*work.height));
		AverageDownSampleOps.down(work, image);

		return image;
	}

	public void seek( int index ) {
		this.index = index;
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

	@SuppressWarnings({"NullAway"})
	@Override
	public void reset() {
		index = 0;
		forwards = true;
		image = null;
		imageGUI = null;
	}

	public int getTotalImages() {
		return paths.size();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex( int index ) {
		this.index = index;
	}
}
