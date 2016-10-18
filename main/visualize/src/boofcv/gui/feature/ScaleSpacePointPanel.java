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

package boofcv.gui.feature;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.gss.GaussianScaleSpace;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ScaleSpacePointPanel extends JPanel {

	GaussianScaleSpace ss;
	BufferedImage background;
	List<ScalePoint> points = new ArrayList<>();
	List<ScalePoint> unused = new ArrayList<>();
	BufferedImage levelImage;
	List<ScalePoint> levelPoints = new ArrayList<>();

	int activeLevel = 0;
	double scaleToRadius;

	public ScaleSpacePointPanel( GaussianScaleSpace ss , double scaleToRadius ) {
		this.ss = ss;
	}

	public void setBackground( BufferedImage background ) {
		this.background = background;
		final int width = background.getWidth();
		final int height = background.getHeight();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(width,height));
			}});
	}

	public synchronized void setPoints( List<ScalePoint> points ) {
		unused.addAll(this.points);
		this.points.clear();
		this.activeLevel = 0;

		for( ScalePoint p : points ) {
			if( unused.isEmpty() ) {
				this.points.add(p.copy());
			} else {
				ScalePoint c = unused.remove( unused.size()-1 );
				c.set(p);
			}
			this.points.add(p);
		}
	}

	public synchronized void setLevel( int level ) {
//		System.out.println("level "+level);
		if( level > 0 ) {
			ss.setActiveScale(level-1);
			// if the input image size has changed reallocate the levelImage
			if( levelImage != null &&
					(levelImage.getWidth() != background.getWidth() ||
					levelImage.getHeight() != background.getHeight()))
				levelImage = null;
			levelImage = ConvertBufferedImage.convertTo(ss.getScaledImage(),levelImage,true);

			double scale = ss.getCurrentScale();
			levelPoints.clear();
			for( ScalePoint p : points ) {
				if( p.scale == scale ) {
					levelPoints.add(p);
				}
			}
		}

		this.activeLevel = level;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		double scaleX = background.getWidth() / (double)getWidth();
		double scaleY = background.getHeight() / (double)getHeight();
		double scale = Math.max(scaleX,scaleY);

		// scale it down so that the whole image is visible
		if( scale > 1 ) {
			AffineTransform tran = g2.getTransform();
			tran.concatenate(AffineTransform.getScaleInstance(1/scale,1/scale));
			g2.setTransform(tran);
		}

		if( activeLevel == 0 )
			showAll(g);
		else {
			g2.drawImage(levelImage, 0, 0, levelImage.getWidth(), levelImage.getHeight(),null);
			VisualizeFeatures.drawScalePoints(g2,levelPoints,scaleToRadius);
		}

	}

	private void showAll(Graphics g) {
		//draw the image
		if (background != null)
			g.drawImage(background, 0, 0, background.getWidth(),background.getHeight(),null);

		VisualizeFeatures.drawScalePoints((Graphics2D)g,points,scaleToRadius);
	}

}
