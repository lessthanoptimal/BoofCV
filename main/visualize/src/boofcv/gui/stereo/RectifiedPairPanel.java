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

package boofcv.gui.stereo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Draws two images side by side and draws a line across the whole window where the user clicks.
 *
 * @author Peter Abeles
 */
public class RectifiedPairPanel extends JPanel implements MouseListener {

	// left image
	BufferedImage image1;
	// right image
	BufferedImage image2;

	// where the red line is drawn
	int mouseY = -10;
	int mouseX;

	// if true it will scale the images to fit the display
	boolean scaleToWindow;

	public RectifiedPairPanel( boolean scaleToWindow ) {
		addMouseListener(this);
		this.scaleToWindow = scaleToWindow;
	}

	public RectifiedPairPanel( boolean scaleToWindow , BufferedImage image1 , BufferedImage image2) {
		this(scaleToWindow);
		setImages(image1,image2);
		int h = Math.max(image1.getHeight(),image2.getHeight());
		int w = image1.getWidth() + image2.getWidth();
		setPreferredSize(new Dimension(w,h));
		setMinimumSize(new Dimension(w,h));
	}

	public void setImages( BufferedImage image1 , BufferedImage image2 ) {
		this.image1 = image1;
		this.image2 = image2;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		if( image1 == null || image2 == null )
			return;

		double scale = 1;
		if( scaleToWindow ) {
			int h = Math.max(image1.getHeight(),image2.getHeight());
			int w = image1.getWidth() + image2.getWidth();

			double scaleX = getWidth()/(double)w;
			double scaleY = getHeight()/(double)h;
			scale = Math.min(scaleX,scaleY);
			if( scale > 1 ) scale = 1;

			AffineTransform orig = g2.getTransform();
			AffineTransform combined = (AffineTransform)orig.clone();
			combined.concatenate(AffineTransform.getScaleInstance(scale, scale));
			g2.setTransform(combined);
			g2.drawImage(image1,0,0,null);
			g2.drawImage(image2,image1.getWidth(),0,null);
			g2.setTransform(orig);
		} else {
			g2.drawImage(image1,0,0,null);
			g2.drawImage(image2,image1.getWidth(),0,null);
		}

		// draw a vertical line to make it easy to see how
		if( mouseY >= 0 ) {
			int x = mouseX >= image1.getWidth() ? mouseX-image1.getWidth() : mouseX;
			x *= scale;
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.RED);
			g2.drawLine(x,0,x,getHeight());
			x += image1.getWidth();
			g2.drawLine(x,0,x,getHeight());
		}

		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(3));

		g2.drawLine(0,mouseY,getWidth(),mouseY);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		mouseY = e.getY();
		mouseX = e.getX();
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
