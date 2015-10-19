/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.image;

import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


/**
 * Simple JPanel for displaying buffered images allows images to be zoomed in and out of
 *
 * @author Peter Abeles
 */
public class ImageZoomPanel extends JScrollPane {
	// the image being displayed
	protected BufferedImage img;
	protected ImagePanel panel = new ImagePanel();

	protected double scale=1;

	public ImageZoomPanel(final BufferedImage img ) {
		this.img = img;
		setScale(1);
	}

	public ImageZoomPanel() {
		getViewport().setView(panel);
	}

	public synchronized void setScale( double scale ) {

		Rectangle r = panel.getVisibleRect();
		double centerX = (r.x + r.width/2.0)/this.scale;
		double centerY = (r.y + r.height/2.0)/this.scale;

		this.scale = scale;
		int w=0,h=0;
		if( img != null ) {
			w = (int)Math.ceil(img.getWidth()*scale);
			h = (int)Math.ceil(img.getHeight()*scale);
		}
		panel.setPreferredSize(new Dimension(w, h));
		getViewport().setView(panel);

		centerView(centerX, centerY);
	}

	public synchronized void centerView( double cx , double cy ) {
		Rectangle r = panel.getVisibleRect();
		int x = (int)(cx*scale-r.width/2);
		int y = (int)(cy*scale-r.height/2);

		getHorizontalScrollBar().setValue(x);
		getVerticalScrollBar().setValue(y);
	}


	/**
	 * Change the image being displayed.
	 *
	 * @param image The new image which will be displayed.
	 */
	public synchronized void setBufferedImage(BufferedImage image) {
		this.img = image;

		Dimension prev = getPreferredSize();

		int w=0,h=0;
		if( img != null ) {
			w = (int)Math.ceil(img.getWidth()*scale);
			h = (int)Math.ceil(img.getHeight()*scale);
		}

		if( prev.getWidth() != w || prev.getHeight() != h ) {
			panel.setPreferredSize(new Dimension(w,h));
			getViewport().setView(panel);
		}
	}

	public BufferedImage getImage() {
		return img;
	}

	public Point2D_F64 pixelToPoint(int x, int y) {
		Point2D_F64 ret = new Point2D_F64();
		ret.x = x/scale;
		ret.y = y/scale;

		return ret;
	}

	/**
	 * Paint inside the image panel. Useful for overlays
	 */
	protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {
		// intentionally empty
	}

	protected class ImagePanel extends JPanel
	{
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			if( img == null ) return;

			Graphics2D g2 = (Graphics2D)g;

			synchronized ( ImageZoomPanel.this ) {
				AffineTransform tran = AffineTransform.getScaleInstance(scale, scale);
				g2.drawImage(img, tran, null);

				paintInPanel(tran, g2);
			}
		}
	}

	public double getScale() {
		return scale;
	}
}
