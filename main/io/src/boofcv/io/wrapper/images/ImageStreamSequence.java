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
import boofcv.io.video.CombineFilesTogether;
import boofcv.io.video.VideoMjpegCodec;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.GrowQueue_I8;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Given a sequence of images encoded with {@link CombineFilesTogether}, it will read the files from
 * the stream and decode them.  Only one image is read at a time and if it is initialized from a file
 * the sequence can be restarted.
 *
 * @author Peter Abeles
 */
public class ImageStreamSequence<T extends ImageBase>
		implements SimpleImageSequence<T>
{
	// If the data set was read from a file it can then be restarted
	String fileName;

	// used to read in the stream
	DataInputStream in;
	BufferedImage original;
	BufferedImage next;
	T image;
	int frameNumber;
	ImageType<T> imageType;
	GrowQueue_I8 buffer = new GrowQueue_I8();
	byte rawData[];

	public ImageStreamSequence(InputStream in, boolean storeData , ImageType<T> imageType) {
		if( storeData ) {
			try {
				rawData = VideoMjpegCodec.convertToByteArray(in);
				this.in = new DataInputStream(new ByteArrayInputStream(rawData));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.in = new DataInputStream(in);
		}

		this.imageType = imageType;
		image = imageType.createImage(1,1);
		readNext();
	}

	public ImageStreamSequence(String fileName, boolean storeData , ImageType<T> imageType) throws FileNotFoundException {
		this(new DataInputStream(new BufferedInputStream(new FileInputStream(fileName),1024*200)),storeData,imageType);
		this.fileName = fileName;
	}

	private void readNext() {
			try {
				CombineFilesTogether.readNext(in,buffer);
				next = ImageIO.read(new ByteArrayInputStream(buffer.data,0,buffer.size));
				frameNumber++;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	@Override
	public int getNextWidth() {
		return image.getWidth();
	}

	@Override
	public int getNextHeight() {
		return image.getWidth();
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public T next() {
		original = next;
		image.reshape(original.getWidth(),original.getHeight());
		ConvertBufferedImage.convertFrom(original,image,true);
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
		if( loop )
			throw new RuntimeException("Can't loop");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public void reset() {
		if( rawData != null ) {
			this.in = new DataInputStream(new ByteArrayInputStream(rawData));
			frameNumber = 0;
			readNext();
		} else if( fileName != null ) {
			try {
				in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName),1024*200));
				frameNumber = 0;
				readNext();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("Reset not supported");
		}
	}

	public static void main( String args[] ) throws FileNotFoundException {
		ImageStreamSequence stream = new ImageStreamSequence("combined.mpng",true, ImageType.single(GrayU16.class));

		while( stream.hasNext() ) {
			System.out.println("Image");
			stream.next();
		}
	}
}
