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

import boofcv.abst.distort.FDistort;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ScaleSpacePyramidPointPanel extends JPanel implements MouseListener {

	private PyramidFloat ss;
	BufferedImage background;
	List<ScalePoint> points = new ArrayList<>();
	List<ScalePoint> unused = new ArrayList<>();
	BufferedImage levelImage;
	List<ScalePoint> levelPoints = new ArrayList<>();

	int activeLevel = 0;

	double scaleToRadius;

	public ScaleSpacePyramidPointPanel( double scaleToRadius ) {
		this.scaleToRadius = scaleToRadius;
		addMouseListener(this);
	}

	public void setSs(PyramidFloat ss) {
		this.ss = ss;
	}

	public void setBackground( BufferedImage background ) {
		this.background = background;
		setPreferredSize(new Dimension(background.getWidth(),background.getHeight()));
	}

	public synchronized void setPoints( List<ScalePoint> points ) {
		unused.addAll(this.points);
		this.points.clear();

		for( ScalePoint p : points ) {
			if( unused.isEmpty() ) {
				this.points.add(p.copy());
			} else {
				ScalePoint c = unused.remove( unused.size()-1 );
				c.set(p);
			}
			this.points.add(p);
		}
		setLevel(0);
	}

	private synchronized void setLevel( int level ) {
//		System.out.println("level "+level);
		if( level > 0 && ss != null ) {

			ImageGray small = ss.getLayer(level-1);
			ImageGray enlarge = GeneralizedImageOps.createSingleBand(small.getClass(), ss.getInputWidth(), ss.getInputHeight());
			new FDistort(small,enlarge).interpNN().apply();

			// if the size isn't the same null it so a new image will be declared
			if( levelImage != null &&
					(levelImage.getWidth() != enlarge.width || levelImage.getHeight() != enlarge.height )) {
				levelImage = null;
			}
			levelImage = ConvertBufferedImage.convertTo(enlarge,levelImage,true);

			double scale = ss.getScale(level-1);
			levelPoints.clear();
			for( ScalePoint p : points ) {
				if( p.scale == scale ) {
					levelPoints.add(p);
				}
			}
		} else {
			levelPoints.clear();
			levelPoints.addAll(points);
		}

		this.activeLevel = level;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		double scaleX = ss.getInputWidth() / (double)getWidth();
		double scaleY = ss.getInputHeight() / (double)getHeight();
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
			g.drawImage(levelImage, 0, 0, levelImage.getWidth(), levelImage.getHeight(),null);
			VisualizeFeatures.drawScalePoints((Graphics2D)g,levelPoints,scaleToRadius);
		}

	}

	private void showAll(Graphics g) {
		//draw the image
		if (background != null)
			g.drawImage(background, 0, 0, background.getWidth(), background.getHeight(),null);

		VisualizeFeatures.drawScalePoints((Graphics2D)g,levelPoints,scaleToRadius);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int level = activeLevel + 1;
		if( level > ss.getNumLayers() ) {
			level = 0;
		}
		setLevel(level);
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
