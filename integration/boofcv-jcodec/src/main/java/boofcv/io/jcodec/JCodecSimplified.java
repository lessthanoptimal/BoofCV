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

package boofcv.io.jcodec;

import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.jcodec.api.FrameGrab.createFrameGrab;

/**
 * Reads movie files using JCodec
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class JCodecSimplified<T extends ImageBase<T>> implements SimpleImageSequence<T> {
	// For information on frame order see https://github.com/jcodec/jcodec/issues/165
	// From issue:
	//   That's b-frame reordering. FrameGrab is low-delay so it doesn't reorder frames for you.
	//   Transcoder however does have the logic of frame reordering built-in.
	//
	//   In B-frame sequence frames are stored in weird order where a P-frame that's a future reference is transmitted
	//   first followed by all the B-frames that refer to it.
	private static final int REORDER_LENGTH = 5;

	FrameGrab grabber;

	T image;
	// type of output image
	ImageType<T> typeOutput;

	DogArray<PictureInfo> reorder = new DogArray<>(PictureInfo::new);
	int width, height;
	boolean endOfFile = false;
	int frame = -1;

	String filename;

	public JCodecSimplified( String filename, ImageType<T> typeOutput ) {
		image = typeOutput.createImage(1, 1);
		this.typeOutput = typeOutput;
		this.filename = filename;
		reset();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public boolean hasNext() {
		return reorder.size > 0;
	}

	@Override
	public T next() {
		// select the next image from what has already been loaded
		PictureInfo best = reorder.get(0);

		int bestIndex = 0;
		for (int i = 1; i < reorder.size; i++) {
			PictureInfo p = reorder.get(i);
			if (best.timestamp > p.timestamp) {
				best = p;
				bestIndex = i;
			}
		}
		// Convert the next image in the sequence into the output image
		UtilJCodec.convertToBoof(best.picture, image);

		// remove the next and recycle the data
		reorder.removeSwap(bestIndex);

		// load the next frame in the sequence
		if (!endOfFile) {
			try {
				grabAndCopy(reorder.grow());
			} catch (IOException e) {
				reorder.removeTail();
				endOfFile = true;
			}
		}

		frame++;
		return image;
	}

	@Override
	public T getImage() {
		return image;
	}

	@Override
	public <InternalImage> InternalImage getGuiImage() {
		BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(image, output, true);
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
	public void setLoop( boolean loop ) {}

	@Override
	public ImageType<T> getImageType() {
		return typeOutput;
	}

	@Override
	public void reset() {
		filename = UtilIO.checkIfJarAndCopyToTemp(filename);
		try {
			grabber = createFrameGrab(NIOUtils.readableChannel(new File(filename)));
		} catch (IOException | JCodecException e) {
			throw new RuntimeException(e);
		}
		frame = -1;

		// read in the full set of images to get correct order
		endOfFile = false;
		while (reorder.size < REORDER_LENGTH) {
			try {
				grabAndCopy(reorder.grow());
			} catch (IOException e) {
				reorder.removeTail();
				endOfFile = true;
				break;
			}
		}

		if (reorder.size > 0) {
			PictureInfo p = reorder.get(0);
			width = p.picture.getWidth();
			height = p.picture.getHeight();
		}
	}

	private void grabAndCopy( PictureInfo info ) throws IOException {
		PictureWithMetadata p = grabber.getNativeFrameWithMetadata();
		if (info.picture == null || !info.picture.compatible(p.getPicture())) {
			info.picture = p.getPicture().cloneCropped();
		} else {
			info.picture.copyFrom(p.getPicture());
		}
		info.timestamp = p.getTimestamp();
	}

	@SuppressWarnings({"NullAway.Init"})
	private static class PictureInfo {
		Picture picture;
		double timestamp;
	}
}
