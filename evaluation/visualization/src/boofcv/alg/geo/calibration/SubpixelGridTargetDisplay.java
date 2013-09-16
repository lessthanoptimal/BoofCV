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

package boofcv.alg.geo.calibration;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * GUI display for {@link DebugSquaresSubpixelApp}.  Shows feature location, backgroudn image, and
 * allows the user to zoom in and out.
 * 
 * @author Peter Abeles
 */
public class SubpixelGridTargetDisplay<T extends ImageSingleBand> 
		extends JPanel {

	Class<T> imageType;
	T input;
	T transformed;
	BufferedImage workImage;

	// transform
	double scale=1;

	List<Point2D_I32> crudePoints;
	List<Point2D_F64> refinedPoints;

	boolean showCrude = true;
	boolean showRefined = true;

	public SubpixelGridTargetDisplay( Class<T> imageType ) {
		this.imageType = imageType;
		transformed = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	public void setShow( boolean showCrude , boolean showRefined ) {
		this.showCrude = showCrude;
		this.showRefined = showRefined;
	}

	public synchronized void setImage( T input ) {
		this.input = input;
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
		
		if( visibleRect.width == 0 || visibleRect.height == 0 )
			return;

		if( transformed.width != visibleRect.width || transformed.height != visibleRect.height ||
				workImage == null ) {
			transformed.reshape(visibleRect.width,visibleRect.height);
			workImage = new BufferedImage(visibleRect.width,visibleRect.height,BufferedImage.TYPE_INT_RGB);
		}
		double x = -visibleRect.x;
		double y = -visibleRect.y;

		DistortImageOps.affine(input,transformed,TypeInterpolate.NEAREST_NEIGHBOR,scale,0,0,scale,x,y);
		ConvertBufferedImage.convertTo(transformed,workImage,true);
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

		if( showCrude && crudePoints != null ) {
			for( Point2D_I32 p : crudePoints ) {
				// put it in the center of a pixel
				int x = (int)Math.round(p.x*scale+0.5*scale);
				int y = (int)Math.round(p.y*scale+0.5*scale);

				VisualizeFeatures.drawPoint(g2,x,y,Color.GRAY);
			}
		}

		if( showRefined && refinedPoints != null ) {
			for( Point2D_F64 p : refinedPoints ) {
				// put it in the center of a pixel
				int x = (int)Math.round(p.x*scale+0.5*scale);
				int y = (int)Math.round(p.y*scale+0.5*scale);

				VisualizeFeatures.drawPoint(g2,x,y,Color.RED);
			}
		}
	}

	public double getScale() {
		return scale;
	}

	public void setCrudePoints(List<Point2D_I32> crudePoints) {
		this.crudePoints = crudePoints;
	}

	public void setRefinedPoints(List<Point2D_F64> refinedPoints) {
		this.refinedPoints = refinedPoints;
	}
}
