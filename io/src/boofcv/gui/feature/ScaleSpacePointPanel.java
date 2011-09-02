/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.feature;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.gss.GaussianScaleSpace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ScaleSpacePointPanel extends JPanel implements MouseListener {

	GaussianScaleSpace ss;
	double radius;
	BufferedImage background;
	List<ScalePoint> points = new ArrayList<ScalePoint>();
	List<ScalePoint> unused = new ArrayList<ScalePoint>();
	BufferedImage levelImage;
	List<ScalePoint> levelPoints = new ArrayList<ScalePoint>();

	int activeLevel = 0;

	public ScaleSpacePointPanel( GaussianScaleSpace ss , double radius) {
		this.ss = ss;
		this.radius = radius;
		addMouseListener(this);
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
	}

	private synchronized void setLevel( int level ) {
		System.out.println("level "+level);
		if( level > 0 ) {
			ss.setActiveScale(level-1);
			levelImage = ConvertBufferedImage.convertTo(ss.getScaledImage(),levelImage);

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
		if( activeLevel == 0 )
			showAll(g);
		else {
			g.drawImage(levelImage, 0, 0, this);
			VisualizeFeatures.drawScalePoints((Graphics2D)g,levelPoints,radius);
		}

	}

	private void showAll(Graphics g) {
		//draw the image
		if (background != null)
			g.drawImage(background, 0, 0, this);

		VisualizeFeatures.drawScalePoints((Graphics2D)g,points,radius);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int level = activeLevel + 1;
		if( level > ss.getTotalScales() ) {
			level = 0;
		}
		setLevel(level);
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void mouseExited(MouseEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
