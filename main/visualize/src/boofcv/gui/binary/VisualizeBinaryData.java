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

package boofcv.gui.binary;

import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.Contour;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.*;
import java.awt.geom.Line2D;
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
	 * @param out (Optional) Storage for output
	 */
	public static void render(List<Contour> contours , int colors[] , BufferedImage out) {

		colors = checkColors(colors,contours.size());

		for( Contour c : contours ) {
			int color = colors[c.id-1];

			for(Point2D_I32 p : c.external ) {
				out.setRGB(p.x,p.y,color);
			}
		}
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

	public static BufferedImage render(List<Contour> contours , Color color , BufferedImage out) {
		for( Contour c : contours ) {
			for(Point2D_I32 p : c.external ) {
				out.setRGB(p.x,p.y,color.getRGB());
			}
		}

		return out;
	}

	public static void render(List<Contour> contours , Color internal , Color external , double scale , Graphics2D g2 ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Line2D.Double l = new Line2D.Double();

		g2.setStroke(new BasicStroke(Math.max(1, (float) scale)));
		for( Contour c : contours ) {

			if( external != null) {
				g2.setColor(external);
				renderContour(scale, g2, l, c.external);
			}

			if( internal != null) {
				g2.setColor(internal);
				for (List<Point2D_I32> inner : c.internal) {
					renderContour(scale, g2, l, inner);
				}
			}
		}

		if( scale > 4 ) {
			Color before = g2.getColor();
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.LIGHT_GRAY);

			for( Contour c : contours ) {
				if( external != null ) {
					renderContour(scale, g2, l, c.external);
				}

				if( internal != null) {
					for (List<Point2D_I32> inner : c.internal) {
						renderContour(scale, g2, l, inner);
					}
				}
			}
			g2.setColor(before);
		}
	}

	private static void renderContour(double scale, Graphics2D g2, Line2D.Double l, List<Point2D_I32> list) {
		for (int i = 0, j = list.size()-1; i < list.size(); j=i, i++) {
			Point2D_I32 p0 = list.get(i);
			Point2D_I32 p1 = list.get(j);

			// draw it in the middle
			l.setLine((p0.x+0.5)*scale,(p0.y+0.5)*scale,(p1.x+0.5)*scale,(p1.y+0.5)*scale);
			g2.draw(l);
		}
	}

	public static BufferedImage renderLabeled(GrayS32 labelImage, int colors[], BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(labelImage.getWidth(),labelImage.getHeight(),BufferedImage.TYPE_INT_RGB);
		}

		try {
			if( out.getRaster() instanceof IntegerInterleavedRaster) {
				renderLabeled(labelImage, colors, (IntegerInterleavedRaster)out.getRaster());
			} else {
				_renderLabeled(labelImage, out, colors);
			}
			// hack so that it knows the image has been modified
			out.setRGB(0,0,out.getRGB(0,0));
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
	public static BufferedImage renderLabeledBG(GrayS32 labelImage, int numRegions, BufferedImage out) {

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
	public static BufferedImage renderLabeled(GrayS32 labelImage, int numRegions, BufferedImage out) {

		int colors[] = new int[numRegions];

		Random rand = new Random(123);
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}

		return renderLabeled(labelImage, colors, out);
	}

	private static void _renderLabeled(GrayS32 labelImage, BufferedImage out, int[] colors) {
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

	private static void renderLabeled(GrayS32 labelImage, int[] colors, IntegerInterleavedRaster raster) {
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

	/**
	 * Renders a binary image.  0 = black and 1 = white.
	 *
	 * @param binaryImage (Input) Input binary image.
	 * @param invert (Input) if true it will invert the image on output
	 * @param out (Output) optional storage for output image
	 * @return Output rendered binary image
	 */
	public static BufferedImage renderBinary(GrayU8 binaryImage, boolean invert, BufferedImage out) {

		if( out == null ) {
			out = new BufferedImage(binaryImage.getWidth(),binaryImage.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		}

		try {
			if( out.getRaster() instanceof ByteInterleavedRaster ) {
				renderBinary(binaryImage, invert, (ByteInterleavedRaster) out.getRaster());
			} else if( out.getRaster() instanceof  IntegerInterleavedRaster ) {
				renderBinary(binaryImage, invert, (IntegerInterleavedRaster) out.getRaster());
			} else {
				_renderBinary(binaryImage, invert,  out);
			}
		} catch( SecurityException e ) {
			_renderBinary(binaryImage, invert, out);
		}
		// hack so that it knows the buffer has been modified
		out.setRGB(0,0,out.getRGB(0,0));
		return out;
	}

	private static void _renderBinary(GrayU8 binaryImage, boolean invert, BufferedImage out) {
		int w = binaryImage.getWidth();
		int h = binaryImage.getHeight();

		if( invert ) {
			for (int y = 0; y < h; y++) {
				int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
				for (int x = 0; x < w; x++) {
					int rgb = binaryImage.data[indexSrc++] > 0 ? 0 : 0x00FFFFFF;
					out.setRGB(x, y, rgb);
				}
			}
		} else {
			for (int y = 0; y < h; y++) {
				int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
				for (int x = 0; x < w; x++) {
					int rgb = binaryImage.data[indexSrc++] > 0 ? 0x00FFFFFF : 0;
					out.setRGB(x, y, rgb);
				}
			}
		}
	}

	private static void renderBinary(GrayU8 binaryImage, boolean invert, ByteInterleavedRaster raster) {
		int rasterIndex = 0;
		byte data[] = raster.getDataStorage();

		int w = binaryImage.getWidth();
		int h = binaryImage.getHeight();

		int numBands = raster.getNumBands();
		if( numBands == 1 ) {
			if (invert) {
				for (int y = 0; y < h; y++) {
					int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
					for (int x = 0; x < w; x++) {
						data[rasterIndex++] = (byte) ((1 - binaryImage.data[indexSrc++]) * 255);
					}
				}
			} else {
				for (int y = 0; y < h; y++) {
					int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
					for (int x = 0; x < w; x++) {
						data[rasterIndex++] = (byte) (binaryImage.data[indexSrc++] * 255);
					}
				}
			}
		} else {
			if (invert) {
				for (int y = 0; y < h; y++) {
					int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
					for (int x = 0; x < w; x++) {
						byte val = (byte) ((1 - binaryImage.data[indexSrc++]) * 255);
						for (int i = 0; i < numBands; i++) {
							data[rasterIndex++] = val;
						}
					}
				}
			} else {
				for (int y = 0; y < h; y++) {
					int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
					for (int x = 0; x < w; x++) {
						byte val = (byte) (binaryImage.data[indexSrc++] * 255);
						for (int i = 0; i < numBands; i++) {
							data[rasterIndex++] = val;
						}
					}
				}
			}
		}
	}

	private static void renderBinary(GrayU8 binaryImage, boolean invert, IntegerInterleavedRaster raster) {
		int rasterIndex = 0;
		int data[] = raster.getDataStorage();

		int w = binaryImage.getWidth();
		int h = binaryImage.getHeight();

		if (invert) {
			for (int y = 0; y < h; y++) {
				int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
				for (int x = 0; x < w; x++) {
					data[rasterIndex++] = binaryImage.data[indexSrc++] > 0 ? 0 : 0xFFFFFFFF;
				}
			}
		} else {
			for (int y = 0; y < h; y++) {
				int indexSrc = binaryImage.startIndex + y * binaryImage.stride;
				for (int x = 0; x < w; x++) {
					data[rasterIndex++] = binaryImage.data[indexSrc++] > 0 ? 0xFFFFFFFF : 0;
				}
			}
		}

	}
}
