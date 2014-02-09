/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.binary;

import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.Contour;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class VisualizeBinaryData {

	public static BufferedImage renderContours( List<EdgeContour> edges , int colors[] ,
												int width , int height , BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		} else {
			Graphics2D g2 = out.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,width,height);
		}

		colors = checkColors(colors,edges.size());

		for( int i = 0; i < edges.size(); i++ ) {
			EdgeContour e = edges.get(i);
			int color = colors[i];

			for( EdgeSegment s : e.segments ) {
				for( Point2D_I32 p : s.points ) {
					out.setRGB(p.x,p.y,color);
				}
			}
		}

		return out;
	}

	/**
	 * Draws contours. Internal and external contours are different user specified colors.
	 *
	 * @param contours List of contours
	 * @param colorExternal RGB color
	 * @param colorInternal RGB color
	 * @param width Image width
	 * @param height Image height
	 * @param out (Optional) storage for output image
	 * @return Rendered contours
	 */
	public static BufferedImage renderContours( List<Contour> contours , int colorExternal, int colorInternal ,
												int width , int height , BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		} else {
			Graphics2D g2 = out.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,width,height);
		}

		for( Contour c : contours ) {
			for(Point2D_I32 p : c.external ) {
				out.setRGB(p.x,p.y,colorExternal);
			}
			for( List<Point2D_I32> l : c.internal ) {
				for( Point2D_I32 p : l ) {
					out.setRGB(p.x,p.y,colorInternal);
				}
			}
		}

		return out;
	}

	/**
	 * Draws contours. Internal and external contours are different user specified colors.
	 *
	 * @param contours List of contours
	 * @param colorExternal (Optional) Array of RGB colors for each external contour
	 * @param colorInternal RGB color
	 * @param width Image width
	 * @param height Image height
	 * @param out (Optional) storage for output image
	 * @return Rendered contours
	 */
	public static BufferedImage renderContours( List<Contour> contours , int colorExternal[], int colorInternal ,
												int width , int height , BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		} else {
			Graphics2D g2 = out.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,width,height);
		}

		colorExternal = checkColors(colorExternal,contours.size());

		int index = 0;
		for( Contour c : contours ) {
			int color = colorExternal[index++];
			for(Point2D_I32 p : c.external ) {
				out.setRGB(p.x,p.y,color);
			}
			for( List<Point2D_I32> l : c.internal ) {
				for( Point2D_I32 p : l ) {
					out.setRGB(p.x,p.y,colorInternal);
				}
			}
		}

		return out;
	}

	/**
	 * Renders only the external contours.  Each contour is individually colored as specified by 'colors'
	 *
	 * @param contours List of contours
	 * @param colors List of RGB colors for each element in contours.  If null then random colors will be used.
	 * @param width Width of input image.
	 * @param height Height of input image.
	 * @param out (Optional) Storage for output
	 * @return Rendered image for display.
	 */
	public static BufferedImage renderExternal( List<Contour> contours , int colors[] ,
												int width , int height , BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		}

		colors = checkColors(colors,contours.size());

		for( Contour c : contours ) {
			int color = colors[c.id-1];

			for(Point2D_I32 p : c.external ) {
				out.setRGB(p.x,p.y,color);
			}
		}

		return out;
	}

	public static int[] checkColors(  int[] colors , int size ) {
		if( colors == null ) {
			colors = new int[ size ];
			Random rand = new Random(123);
			for( int i = 0; i < size; i++ ) {
				colors[i] = rand.nextInt();
			}
		}
		return colors;
	}

	public static BufferedImage renderLabeled(ImageSInt32 labelImage, int colors[], BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(labelImage.getWidth(),labelImage.getHeight(),BufferedImage.TYPE_INT_RGB);
		}

		try {
			if( out.getRaster() instanceof IntegerInterleavedRaster) {
				renderLabeled(labelImage, colors, (IntegerInterleavedRaster)out.getRaster());
			} else {
				_renderLabeled(labelImage, out, colors);
			}
		} catch( SecurityException e ) {
			_renderLabeled(labelImage, out, colors);
		}
		return out;
	}

	/**
	 * Renders a labeled image where label=0 is assumed to be the background and is always set to black.  All
	 * other labels are assigned a random color.
	 *
	 * @param labelImage Labeled image with background having a value of 0
	 * @param numRegions Number of labeled in the image, excluding the background.
	 * @param out Output image.  If null a new image is declared
	 * @return Colorized labeled image
	 */
	public static BufferedImage renderLabeledBG(ImageSInt32 labelImage, int numRegions, BufferedImage out) {

		int colors[] = new int[numRegions+1];

		Random rand = new Random(123);
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}
		colors[0] = 0;

		return renderLabeled(labelImage, colors, out);
	}

	/**
	 * Renders a labeled where each region is assigned a random color.
	 *
	 * @param labelImage Labeled image with labels from 0 to numRegions-1
	 * @param numRegions Number of labeled in the image
	 * @param out Output image.  If null a new image is declared
	 * @return Colorized labeled image
	 */
	public static BufferedImage renderLabeled(ImageSInt32 labelImage, int numRegions, BufferedImage out) {

		int colors[] = new int[numRegions];

		Random rand = new Random(123);
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}

		return renderLabeled(labelImage, colors, out);
	}

	private static void _renderLabeled(ImageSInt32 labelImage, BufferedImage out, int[] colors) {
		int w = labelImage.getWidth();
		int h = labelImage.getHeight();

		for( int y = 0; y < h; y++ ) {
			int indexSrc = labelImage.startIndex + y*labelImage.stride;
			for( int x = 0; x < w; x++ ) {
				int rgb = colors[labelImage.data[indexSrc++]];
				out.setRGB(x,y,rgb);
			}
		}
	}

	private static void renderLabeled(ImageSInt32 labelImage, int[] colors, IntegerInterleavedRaster raster) {
		int rasterIndex = 0;
		int data[] = raster.getDataStorage();

		int w = labelImage.getWidth();
		int h = labelImage.getHeight();


		for( int y = 0; y < h; y++ ) {
			int indexSrc = labelImage.startIndex + y*labelImage.stride;
			for( int x = 0; x < w; x++ ) {
				data[rasterIndex++] = colors[labelImage.data[indexSrc++]];
			}
		}
	}

	public static BufferedImage renderBinary( ImageUInt8 binaryImage , BufferedImage out ) {

		if( out == null ) {
			out = new BufferedImage(binaryImage.getWidth(),binaryImage.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		}

		try {
			if( out.getRaster() instanceof ByteInterleavedRaster ) {
				renderBinary(binaryImage, (ByteInterleavedRaster)out.getRaster());
			} else {
				_renderBinary(binaryImage, out);
			}
		} catch( SecurityException e ) {
			_renderBinary(binaryImage, out);
		}
		return out;
	}

	private static void _renderBinary(ImageUInt8 binaryImage, BufferedImage out) {
		int w = binaryImage.getWidth();
		int h = binaryImage.getHeight();

		for( int y = 0; y < h; y++ ) {
			int indexSrc = binaryImage.startIndex + y*binaryImage.stride;
			for( int x = 0; x < w; x++ ) {
				int rgb = binaryImage.data[indexSrc++] > 0 ? 0x00FFFFFF : 0;
				out.setRGB(x,y,rgb);
			}
		}
	}

	private static void renderBinary(ImageUInt8 binaryImage, ByteInterleavedRaster raster) {
		int rasterIndex = 0;
		byte data[] = raster.getDataStorage();

		int w = binaryImage.getWidth();
		int h = binaryImage.getHeight();

		for( int y = 0; y < h; y++ ) {
			int indexSrc = binaryImage.startIndex + y*binaryImage.stride;
			for( int x = 0; x < w; x++ ) {
				data[rasterIndex++] = binaryImage.data[indexSrc++] > 0 ? (byte)255 : (byte)0;
			}
		}
	}
}
