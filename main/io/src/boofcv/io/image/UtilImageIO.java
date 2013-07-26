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

package boofcv.io.image;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.GrowQueue_I8;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import sun.awt.image.IntegerInterleavedRaster;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.security.AccessControlException;

/**
 * Class for loading and saving images.
 *
 * @author Peter Abeles
 */
public class UtilImageIO {

	/**
	 * A function that load the specified image.  If anything goes wrong it returns a
	 * null.
	 */
	public static BufferedImage loadImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));

			if( img == null && fileName.endsWith("ppm") || fileName.endsWith("PPM") ) {
				return loadPPM(fileName,null);
			}
		} catch (IOException e) {
			return null;
		}

		return img;
	}

	/**
	 * A function that load the specified image.  If anything goes wrong it returns a
	 * null.
	 */
	public static BufferedImage loadImage(URL fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(fileName);
		} catch (IOException e) {
			return null;
		}

		return img;
	}

	/**
	 * Loads the image and converts into the specified image type.
	 *
	 * @param fileName Path to image file.
	 * @param imageType Type of image that should be returned.
	 * @return The image or null if the image could not be loaded.
	 */
	public static <T extends ImageSingleBand> T loadImage(String fileName, Class<T> imageType ) {
		BufferedImage img = loadImage(fileName);
		if( img == null )
			return null;

		return ConvertBufferedImage.convertFromSingle(img, (T) null, imageType);
	}

	public static void saveImage(BufferedImage img, String fileName) {
		try {
			String type;
			String a[] = fileName.split("[.]");
			if (a.length > 0) {
				type = a[a.length - 1];
			} else {
				type = "jpg";
			}

			if( !ImageIO.write(img, type, new File(fileName)) ) {
				if( fileName.endsWith("ppm") || fileName.endsWith("PPM") ) {
					MultiSpectral<ImageUInt8> color = ConvertBufferedImage.convertFromMulti(img,null,ImageUInt8.class);
					ConvertBufferedImage.orderBandsIntoRGB(color,img);
					savePPM(color, fileName, null);
				}else
					throw new IllegalArgumentException("No writter appropriate found");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Loads a PPM image from a file.
	 *
	 * @param fileName Location of PPM image
	 * @param storage (Optional) Storage for output image.  Must be the width and height of the image being read.
	 *                Better performance of type BufferedImage.TYPE_INT_RGB.  If null or width/height incorrect a new image
	 *                will be declared.
	 * @return The read in image
	 * @throws IOException
	 */
	public static BufferedImage loadPPM( String fileName , BufferedImage storage ) throws IOException {
		return loadPPM(new FileInputStream(fileName),storage);
	}

	/**
	 * Loads a PPM image from an {@link InputStream}.
	 *
	 * @param inputStream InputStream for PPM image
	 * @param storage (Optional) Storage for output image.  Must be the width and height of the image being read.
	 *                Better performance of type BufferedImage.TYPE_INT_RGB.  If null or width/height incorrect a new image
	 *                will be declared.
	 * @return The read in image
	 * @throws IOException
	 */
	public static BufferedImage loadPPM( InputStream inputStream , BufferedImage storage ) throws IOException {
		DataInputStream in = new DataInputStream(inputStream);

		readLine(in);
		String s[] = readLine(in).split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);
		readLine(in);

		if( storage == null || storage.getWidth() != w || storage.getHeight() != h )
			storage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB );

		int length = w*h*3;
		byte[] data = new byte[length];

		in.read(data,0,length);

		boolean useFailSafe = storage.getType() != BufferedImage.TYPE_INT_RGB;
		// try using the internal array for better performance
		try {
			int rgb[] =  ((IntegerInterleavedRaster)storage.getRaster()).getDataStorage();

			int indexIn = 0;
			int indexOut = 0;
			for( int y = 0; y < h; y++ ) {
				for( int x = 0; x < w; x++ ) {
					rgb[indexOut++] = ((data[indexIn++] & 0xFF) << 16) |
							((data[indexIn++] & 0xFF) << 8) | (data[indexIn++] & 0xFF);
				}
			}
		} catch( AccessControlException e ) {
			useFailSafe = true;

		}

		if( useFailSafe ) {
			// use the slow setRGB() function
			int indexIn = 0;
			for( int y = 0; y < h; y++ ) {
				for( int x = 0; x < w; x++ ) {
					storage.setRGB(x, y, ((data[indexIn++] & 0xFF) << 16) |
							((data[indexIn++] & 0xFF) << 8) | (data[indexIn++] & 0xFF));
				}
			}
		}

		return storage;
	}

	/**
	 * Reads a PPM image file directly into a MultiSpectral<ImageUInt8> image.   To improve performance when reading
	 * many images, the user can provide work space memory in the optional parameters
	 *
	 * @param fileName Location of PPM file
	 * @param storage (Optional) Where the image is written in to.  Will be resized if needed.
	 *                   If null or the number of bands isn't 3, a new instance is declared.
	 * @param temp (Optional) Used internally to store the image.  Can be null.
	 * @return The image.
	 * @throws IOException
	 */
	public static MultiSpectral<ImageUInt8> loadPPM_U8( String fileName , MultiSpectral<ImageUInt8> storage , GrowQueue_I8 temp )
			throws IOException
	{
		return loadPPM_U8(new FileInputStream(fileName),storage,temp);
	}

	/**
	 * Reads a PPM image file directly into a MultiSpectral<ImageUInt8> image.   To improve performance when reading
	 * many images, the user can provide work space memory in the optional parameters
	 *
	 * @param inputStream InputStream for PPM image
	 * @param storage (Optional) Where the image is written in to.  Will be resized if needed.
	 *                   If null or the number of bands isn't 3, a new instance is declared.
	 * @param temp (Optional) Used internally to store the image.  Can be null.
	 * @return The image.
	 * @throws IOException
	 */
	public static MultiSpectral<ImageUInt8> loadPPM_U8( InputStream inputStream, MultiSpectral<ImageUInt8> storage , GrowQueue_I8 temp )
			throws IOException
	{
		DataInputStream in = new DataInputStream(inputStream);

		readLine(in);
		String s[] = readLine(in).split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);
		readLine(in);

		if( storage == null || storage.getNumBands() != 3 )
			storage = new MultiSpectral<ImageUInt8>(ImageUInt8.class,w,h,3 );
		else
			storage.reshape(w,h);

		int length = w*h*3;
		if( temp == null )
			temp = new GrowQueue_I8(length);
		temp.resize(length);

		byte data[] = temp.data;
		in.read(data,0,length);

		ImageUInt8 band0 = storage.getBand(0);
		ImageUInt8 band1 = storage.getBand(1);
		ImageUInt8 band2 = storage.getBand(2);

		int indexIn = 0;
		for( int y = 0; y < storage.height; y++ ) {
			int indexOut = storage.startIndex + y*storage.stride;
			for( int x = 0; x < storage.width; x++ , indexOut++ ) {
				band0.data[indexOut] = data[indexIn++];
				band1.data[indexOut] = data[indexIn++];
				band2.data[indexOut] = data[indexIn++];
			}
		}

		return storage;
	}

	/**
	 * Saves an image in PPM format.
	 *
	 * @param rgb 3-band RGB image
	 * @param fileName Location where the image is to be written to.
	 * @param temp (Optional) Used internally to store the image.  Can be null.
	 * @throws IOException
	 */
	public static void savePPM( MultiSpectral<ImageUInt8> rgb , String fileName , GrowQueue_I8 temp ) throws IOException {
		File out = new File(fileName);
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("P6\n%d %d\n255\n", rgb.width, rgb.height);
		os.write(header.getBytes());

		int length = rgb.width*rgb.height*3;
		if( temp == null )
			temp = new GrowQueue_I8(length);
		temp.resize(length);

		byte data[] = temp.data;

		ImageUInt8 band0 = rgb.getBand(0);
		ImageUInt8 band1 = rgb.getBand(1);
		ImageUInt8 band2 = rgb.getBand(2);

		int indexOut = 0;
		for( int y = 0; y < rgb.height; y++ ) {
			int index = rgb.startIndex + y*rgb.stride;
			for( int x = 0; x < rgb.width; x++ , index++) {
				data[indexOut++] = band0.data[index];
				data[indexOut++] = band1.data[index];
				data[indexOut++] = band2.data[index];
			}
		}

		os.write(data,0,rgb.width*rgb.height*3);

		os.close();
	}

	private static String readLine( DataInputStream in ) throws IOException {
		String s = "";
		while( true ) {
			int b = in.read();

			if( b == '\n' )
				return s;
			else
				s += (char)b;
		}
	}

}
