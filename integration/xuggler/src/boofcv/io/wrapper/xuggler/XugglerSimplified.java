/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.io.wrapper.xuggler;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * Implementation of {@link SimpleImageSequence} for the Xuggler library.  Can ready many types of
 * video formats.
 *
 * @author Peter Abeles
 */
public class XugglerSimplified<T extends ImageBase> implements SimpleImageSequence<T> {

	IContainer container;
	IStreamCoder videoCoder;
	IPacket packet;
	int videoStreamId;
	IVideoResampler resampler;

	IConverter converter;

	// the output image
	T image;
	// type of output image
	ImageType<T> typeOutput;

	// read in buffered images
	BufferedImage bufferedImage;
	// if the image is reduced this is where it is put
	BufferedImage reducedImage;
	int factor;
	// reference to the output BufferedImage
	BufferedImage imageGUI;

	int frameID=-1;

	String fileName;

	public XugglerSimplified(String filename, ImageType<T> typeOutput) {
		image = typeOutput.createImage(1,1);
		open(filename,typeOutput);
	}

	public void open(String filename, ImageType<T> typeOutput) {
		this.fileName = filename;

		if (!IVideoResampler.isSupported(
				IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION))
			throw new RuntimeException(
					"you must install the GPL version of Xuggler (with IVideoResampler" +
							" support) for this demo to work");

		this.typeOutput = typeOutput;
		// create a Xuggler container object

		container = IContainer.make();

		// open up the container
		if (container.open(filename, IContainer.Type.READ, null) < 0)
			throw new IllegalArgumentException("could not open file: " + filename);

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// and iterate through the streams to find the first video stream
		videoStreamId = -1;
		videoCoder = null;
		for (int i = 0; i < numStreams; i++) {
			// find the stream object

			IStream stream = container.getStream(i);

			// get the pre-configured decoder that can decode this stream;

			IStreamCoder coder = stream.getStreamCoder();

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamId = i;
				videoCoder = coder;
				break;
			}
		}

		if (videoStreamId == -1)
			throw new RuntimeException("could not find video stream in container: " + filename);

		// Now we have found the video stream in this file.  Let's open up
		// our decoder so it can do work

		if (videoCoder.open() < 0)
			throw new RuntimeException(
					"could not open video decoder for container: " + filename);

		resampler = null;
		if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
			// if this stream is not in BGR24, we're going to need to
			// convert it.  The VideoResampler does that for us.

			resampler = IVideoResampler.make(
					videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24,
					videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
			if (resampler == null)
				throw new RuntimeException(
						"could not create color space resampler for: " + filename);
		}

		// Now, we start walking through the container looking at each packet.

		packet = IPacket.make();
		bufferedImage = new BufferedImage(videoCoder.getWidth(), videoCoder.getHeight(),
				BufferedImage.TYPE_3BYTE_BGR);

	}

	public void setReduce(int factor) {
		this.factor = factor;
		reducedImage = new BufferedImage(videoCoder.getWidth() / factor, videoCoder.getHeight() / factor,
				BufferedImage.TYPE_3BYTE_BGR);

	}

	@Override
	public boolean hasNext() {
		while (container.readNextPacket(packet) >= 0) {
			frameID++;

			// Now we have a packet, let's see if it belongs to our video stream

			if (packet.getStreamIndex() == videoStreamId) {
				// We allocate a new picture to get the data out of Xuggle

				IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
						videoCoder.getWidth(), videoCoder.getHeight());

				int offset = 0;
				while (offset < packet.getSize()) {
					// Now, we decode the video, checking for any errors.

					int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
					if (bytesDecoded < 0)
						throw new RuntimeException("got error decoding video");
					offset += bytesDecoded;

					// Some decoders will consume data in a packet, but will not
					// be able to construct a full video picture yet.  Therefore
					// you should always check if you got a complete picture from
					// the decode.

					if (picture.isComplete()) {
						IVideoPicture newPic = picture;

						// If the resampler is not null, it means we didn't get the
						// video in BGR24 format and need to convert it into BGR24
						// format.

						if (resampler != null) {
							// we must resample
							newPic = IVideoPicture.make(
									resampler.getOutputPixelFormat(), picture.getWidth(),
									picture.getHeight());
							if (resampler.resample(newPic, picture) < 0)
								throw new RuntimeException(
										"could not resample video.");
						}

						if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
							throw new RuntimeException(
									"could not decode video as BGR 24 bit data.");

						// convert the BGR24 to an Java buffered image
						if (converter == null) {
							converter = ConverterFactory.createConverter(bufferedImage, newPic.getPixelType());
						}

						bufferedImage = converter.toImage(newPic);
						return true;
					}
				}
			} else {
				// This packet isn't part of our video stream, so we just
				// silently drop it.
				do {
				} while (false);
			}
		}

		return false;
	}

	@Override
	public T next() {
		if (reducedImage != null) {
			Graphics2D g2 = reducedImage.createGraphics();

			g2.scale(1.0 / factor, 1.0 / factor);
			g2.drawImage(bufferedImage, 0, 0, null);

			imageGUI = reducedImage;
		} else {
			imageGUI = bufferedImage;
		}

		image.reshape(imageGUI.getWidth(),imageGUI.getHeight());
		ConvertBufferedImage.convertFrom(imageGUI, image,true);
		return image;
	}

	@Override
	public void setLoop(boolean loop) {
	}

	@Override
	public BufferedImage getGuiImage() {
		return imageGUI;
	}

	@Override
	public ImageType<T> getImageType() {
		return typeOutput;
	}

	@Override
	public int getFrameNumber() {
		return frameID;
	}

	@Override
	public void reset() {
		// todo this really is a brute force reset... has to be a faster way
		close();
		open(fileName,typeOutput);
	}

	@Override
	public void close() {
		if (videoCoder != null) {
			videoCoder.close();
			videoCoder = null;
		}
		if (container != null) {
			container.close();
			container = null;
		}

		bufferedImage = null;
		converter = null;
		resampler = null;
		packet = null;
	}
}
