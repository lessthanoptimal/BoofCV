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

package boofcv.io.image;

import boofcv.struct.image.*;
import org.ddogleg.struct.GrowQueue_I8;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

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

			if(  img == null) {
				if( fileName.endsWith("ppm") || fileName.endsWith("PPM") ) {
					return loadPPM(fileName,null);
				} else if( fileName.endsWith("pgm") || fileName.endsWith("PGM") ) {
					return loadPGM(fileName, null);
				}
			}
		} catch (IOException e) {
			return null;
		}

		return img;
	}

	public static BufferedImage loadImage(String directory , String fileName) {
		return loadImage(new File(directory,fileName).getPath());
	}

	/**
	 * Loads all the image in the specified directory which match the provided regex
	 * @param directory File directory
	 * @param regex Regex used to match file names
	 * @return List of found images.
	 */
	public static List<BufferedImage> loadImages( String directory , final String regex ) {

		File[] files = new File(directory).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(regex);
			}
		});

		List<BufferedImage> ret = new ArrayList<>();

		for( File f : files ) {
			BufferedImage img = loadImage(f.getAbsolutePath());
			if( img != null )
				ret.add( img );
		}

		return ret;
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
	public static <T extends ImageGray> T loadImage(String fileName, Class<T> imageType ) {
		BufferedImage img = loadImage(fileName);
		if( img == null )
			return null;

		return ConvertBufferedImage.convertFromSingle(img, (T) null, imageType);
	}

	public static <T extends ImageGray> T loadImage(String directory , String fileName, Class<T> imageType ) {
		return loadImage(new File(directory,fileName).getPath(),imageType);
	}

	public static <T extends ImageBase> T loadImage( File image, boolean orderRgb, ImageType<T> imageType ) {
		BufferedImage img = loadImage(image.getAbsolutePath());
		if( img == null )
			return null;

		T output = imageType.createImage(img.getWidth(),img.getHeight());
		ConvertBufferedImage.convertFrom(img, orderRgb, output);
		return output;
	}

	/**
	 * Saves the {@link BufferedImage} to the specified file.  The image type of the output is determined by
	 * the name's extension.  By default the file is saved using {@link ImageIO#write(RenderedImage, String, File)}}
	 * but if that fails then it will see if it can save it using BoofCV native code for PPM and PGM.
	 *
	 * @param img Image which is to be saved.
	 * @param fileName Name of the output file.  The type is determined by the extension.
	 */
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
					Planar<GrayU8> color = ConvertBufferedImage.convertFromMulti(img,null,true,GrayU8.class);
					savePPM(color, fileName, null);
				} else if( fileName.endsWith("pgm") || fileName.endsWith("PGM") ) {
					GrayU8 gray = ConvertBufferedImage.convertFrom(img, (GrayU8) null);
					savePGM(gray, fileName);
				}else
					throw new IllegalArgumentException("No writer appropriate found");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 *
	 * <p>Saves the BoofCV formatted image.  This is identical to the following code:</p>
	 *
	 * <pre>
	 * BufferedImage out = ConvertBufferedImage.convertTo(image,null,true);
	 * saveImage(out,fileName);
	 * </pre>
	 *
	 * @param image Image which is to be saved.
	 * @param fileName Name of the output file.  The type is determined by the extension.
	 */
	public static void saveImage( ImageBase image , String fileName ) {
		BufferedImage out = ConvertBufferedImage.convertTo(image,null,true);
		saveImage(out,fileName);
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
	 * Loads a PGM image from a file.
	 *
	 * @param fileName Location of PGM image
	 * @param storage (Optional) Storage for output image.  Must be the width and height of the image being read.
	 *                Better performance of type BufferedImage.TYPE_BYTE_GRAY.  If null or width/height incorrect a new image
	 *                will be declared.
	 * @return The read in image
	 * @throws IOException
	 */
	public static BufferedImage loadPGM( String fileName , BufferedImage storage ) throws IOException {
		return loadPGM(new FileInputStream(fileName), storage);
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
		String line = readLine(in);
		while( line.charAt(0) == '#')
			line = readLine(in);
		String s[] = line.split(" ");
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
	 * Loads a PGM image from an {@link InputStream}.
	 *
	 * @param inputStream InputStream for PGM image
	 * @param storage (Optional) Storage for output image.  Must be the width and height of the image being read.
	 *                Better performance of type BufferedImage.TYPE_BYTE_GRAY.  If null or width/height incorrect a new image
	 *                will be declared.
	 * @return The read in image
	 * @throws IOException
	 */
	public static BufferedImage loadPGM( InputStream inputStream , BufferedImage storage ) throws IOException {
		DataInputStream in = new DataInputStream(inputStream);

		readLine(in);
		String line = readLine(in);
		while( line.charAt(0) == '#')
			line = readLine(in);
		String s[] = line.split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);
		readLine(in);

		if( storage == null || storage.getWidth() != w || storage.getHeight() != h )
			storage = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY );

		int length = w*h;
		byte[] data = new byte[length];

		in.read(data,0,length);

		boolean useFailSafe = storage.getType() != BufferedImage.TYPE_BYTE_GRAY;
		// try using the internal array for better performance
		try {
			byte gray[] =  ((ByteInterleavedRaster)storage.getRaster()).getDataStorage();

			int indexIn = 0;
			int indexOut = 0;
			for( int y = 0; y < h; y++ ) {
				for( int x = 0; x < w; x++ ) {
					gray[indexOut++] =  data[indexIn++];
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
					int gray = data[indexIn++] & 0xFF;
					storage.setRGB(x, y, gray << 16 | gray << 8 | gray );
				}
			}
		}

		return storage;
	}

	/**
	 * Reads a PPM image file directly into a Planar<GrayU8> image.   To improve performance when reading
	 * many images, the user can provide work space memory in the optional parameters
	 *
	 * @param fileName Location of PPM file
	 * @param storage (Optional) Where the image is written in to.  Will be resized if needed.
	 *                   If null or the number of bands isn't 3, a new instance is declared.
	 * @param temp (Optional) Used internally to store the image.  Can be null.
	 * @return The image.
	 * @throws IOException
	 */
	public static Planar<GrayU8> loadPPM_U8(String fileName , Planar<GrayU8> storage , GrowQueue_I8 temp )
			throws IOException
	{
		return loadPPM_U8(new FileInputStream(fileName),storage,temp);
	}

	/**
	 * Reads a PPM image file directly into a Planar<GrayU8> image.   To improve performance when reading
	 * many images, the user can provide work space memory in the optional parameters
	 *
	 * @param inputStream InputStream for PPM image
	 * @param storage (Optional) Where the image is written in to.  Will be resized if needed.
	 *                   If null or the number of bands isn't 3, a new instance is declared.
	 * @param temp (Optional) Used internally to store the image.  Can be null.
	 * @return The image.
	 * @throws IOException
	 */
	public static Planar<GrayU8> loadPPM_U8(InputStream inputStream, Planar<GrayU8> storage , GrowQueue_I8 temp )
			throws IOException
	{
		DataInputStream in = new DataInputStream(inputStream);

		readLine(in);
		String line = readLine(in);
		while( line.charAt(0) == '#')
			line = readLine(in);
		String s[] = line.split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);
		readLine(in);

		if( storage == null || storage.getNumBands() != 3 )
			storage = new Planar<>(GrayU8.class,w,h,3 );
		else
			storage.reshape(w,h);

		int length = w*h*3;
		if( temp == null )
			temp = new GrowQueue_I8(length);
		temp.resize(length);

		byte data[] = temp.data;
		in.read(data,0,length);

		GrayU8 band0 = storage.getBand(0);
		GrayU8 band1 = storage.getBand(1);
		GrayU8 band2 = storage.getBand(2);

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
	 * Loads a PGM image from an {@link InputStream}.
	 *
	 * @param fileName InputStream for PGM image
	 * @param storage (Optional) Storage for output image.  Must be the width and height of the image being read.
	 *                If null a new image will be declared.
	 * @return The read in image
	 * @throws IOException
	 */
	public static GrayU8 loadPGM_U8(String fileName , GrayU8 storage )
			throws IOException
	{
		return loadPGM_U8(new FileInputStream(fileName),storage);
	}

	/**
	 * Loads a PGM image from an {@link InputStream}.
	 *
	 * @param inputStream InputStream for PGM image
	 * @param storage (Optional) Storage for output image.  Must be the width and height of the image being read.
	 *                If null a new image will be declared.
	 * @return The read in image
	 * @throws IOException
	 */
	public static GrayU8 loadPGM_U8(InputStream inputStream , GrayU8 storage ) throws IOException {
		DataInputStream in = new DataInputStream(inputStream);

		readLine(in);
		String line = readLine(in);
		while( line.charAt(0) == '#')
			line = readLine(in);
		String s[] = line.split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);
		readLine(in);

		if( storage == null )
			storage = new GrayU8(w,h);

		in.read(storage.data,0,w*h);

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
	public static void savePPM(Planar<GrayU8> rgb , String fileName , GrowQueue_I8 temp ) throws IOException {
		File out = new File(fileName);
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("P6\n%d %d\n255\n", rgb.width, rgb.height);
		os.write(header.getBytes());

		if( temp == null )
			temp = new GrowQueue_I8();
		temp.resize(rgb.width*rgb.height*3);
		byte data[] = temp.data;

		GrayU8 band0 = rgb.getBand(0);
		GrayU8 band1 = rgb.getBand(1);
		GrayU8 band2 = rgb.getBand(2);

		int indexOut = 0;
		for( int y = 0; y < rgb.height; y++ ) {
			int index = rgb.startIndex + y*rgb.stride;
			for( int x = 0; x < rgb.width; x++ , index++) {
				data[indexOut++] = band0.data[index];
				data[indexOut++] = band1.data[index];
				data[indexOut++] = band2.data[index];
			}
		}

		os.write(data,0,temp.size);

		os.close();
	}

	/**
	 * Saves an image in PGM format.
	 *
	 * @param gray Gray scale image
	 * @param fileName Location where the image is to be written to.
	 * @throws IOException
	 */
	public static void savePGM(GrayU8 gray , String fileName ) throws IOException {
		File out = new File(fileName);
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("P5\n%d %d\n255\n", gray.width, gray.height);
		os.write(header.getBytes());

		os.write(gray.data,0,gray.width*gray.height);

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
