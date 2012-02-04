/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class SubpixelGridTargetDisplay<T extends ImageSingleBand> 
		extends JPanel {

	Class<T> imageType;
	T input;
	T transformed;
	BufferedImage workImage;

	// transform
	double scale;

	List<Point2D_I32> crudePoints;
	List<Point2D_F32> refinedPoints;
	
	public SubpixelGridTargetDisplay( Class<T> imageType ) {
		this.imageType = imageType;
		transformed = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}
	
	public synchronized void setImage( T input ) {
		this.input = input;

		// reset the transform
		scale = 1;
	}

	public synchronized void setScale( double scale ) {
		if( scale < 0.1 )
			scale = 0.1;
		else if( scale > 1000 )
			scale = 1000;
		
		this.scale = scale;
		Dimension d = new Dimension((int)(input.width*scale),(int)(input.height*scale));
		setSize(d);
		setPreferredSize(d);
	}

	/**
	 * The center pixel in the current view at a scale of 1
	 */
	public Point2D_F64 getCenter() {
		Rectangle r = getVisibleRect();
		
		double x = (r.x+r.width/2)/scale;
		double y = (r.y+r.height/2)/scale;

		return new Point2D_F64(x,y);
	}
	
	private synchronized void render( Rectangle visibleRect ) {
		System.out.println("render scale = "+scale);
		
		if( visibleRect.width == 0 || visibleRect.height == 0 )
			return;

		if( transformed.width != visibleRect.width || transformed.height != visibleRect.height ||
				workImage == null ) {
			transformed.reshape(visibleRect.width,visibleRect.height);
			workImage = new BufferedImage(visibleRect.width,visibleRect.height,BufferedImage.TYPE_INT_RGB);
		}
		double x = -visibleRect.x;
		double y = -visibleRect.y;

		System.out.println("   x = "+x+" y = "+y+"   workimage.width "+workImage.getWidth()+"  vis.w = "+visibleRect.getWidth());

		DistortImageOps.affine(input,transformed,TypeInterpolate.NEAREST_NEIGHBOR,scale,0,0,scale,x,y);
		ConvertBufferedImage.convertTo(transformed,workImage);
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		if( input == null ) {
			return;
		}
		Graphics2D g2 = (Graphics2D)g;

		Rectangle r = getVisibleRect();

		render(r);
		g2.drawImage(workImage,r.x,r.y,null);
		
		for( Point2D_I32 p : crudePoints ) {
			// put it in the center of a pixel
			int x = (int)Math.round(p.x*scale+0.5*scale);
			int y = (int)Math.round(p.y*scale+0.5*scale);

			VisualizeFeatures.drawPoint(g2,x,y,Color.GRAY);
		}

		for( Point2D_F32 p : refinedPoints ) {
			// put it in the center of a pixel
			int x = (int)Math.round(p.x*scale+0.5*scale);
			int y = (int)Math.round(p.y*scale+0.5*scale);

			VisualizeFeatures.drawPoint(g2,x,y,Color.RED);
		}
	}

	public double getScale() {
		return scale;
	}

	public void setCrudePoints(List<Point2D_I32> crudePoints) {
		this.crudePoints = crudePoints;
	}

	public void setRefinedPoints(List<Point2D_F32> refinedPoints) {
		this.refinedPoints = refinedPoints;
	}
}
