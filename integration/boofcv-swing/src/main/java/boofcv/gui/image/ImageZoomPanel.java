/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.io.image.ConvertBufferedImage;
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
	protected double transX=0,transY=0;

	boolean checkEventDispatch = true;

	public ImageZoomPanel(final BufferedImage img ) {
		this.img = img;
		setScale(1);
	}

	public ImageZoomPanel() {
		getViewport().setView(panel);
	}

	/**
	 * Makes it so that the image is no longer saved when clicked.
	 */
	public void disableSaveOnClick() {
		panel.removeMouseListener(panel.mouseListener);
		panel.mouseListener = null;
	}

	public synchronized void setScale( double scale ) {
		// do nothing if the scale has not changed
		if( this.scale == scale )
			return;

		Rectangle r = panel.getVisibleRect();
		double centerX = (r.x + r.width/2.0)/this.scale;
		double centerY = (r.y + r.height/2.0)/this.scale;

		this.scale = scale;
		if( img != null ) {
			int w = (int)Math.ceil(img.getWidth()*scale);
			int h = (int)Math.ceil(img.getHeight()*scale);
			panel.setPreferredSize(new Dimension(w, h));
		}
		getViewport().setView(panel);

		centerView(centerX, centerY);
	}

	public synchronized void setScaleAndCenter( double scale,  double cx , double cy) {
		this.scale = scale;
		if( img != null ) {
			int w = (int)Math.ceil(img.getWidth()*scale);
			int h = (int)Math.ceil(img.getHeight()*scale);
			panel.setPreferredSize(new Dimension(w, h));
		}
		getViewport().setView(panel);
		centerView(cx,cy);
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
		// assume the image was initially set before the GUI was invoked
		if( checkEventDispatch && this.img != null ) {
			if( !SwingUtilities.isEventDispatchThread() )
				throw new RuntimeException("Changed image when not in GUI thread?");
		}

		this.img = image;
		if( image != null )
			updateSize(image.getWidth(),image.getHeight());
	}

	public void updateSize( int width , int height ) {
		Dimension prev = getPreferredSize();

		int w = (int)Math.ceil(width*scale);
		int h = (int)Math.ceil(height*scale);

		if( prev.getWidth() != w || prev.getHeight() != h ) {
			panel.setPreferredSize(new Dimension(w,h));
			getViewport().setView(panel);
		}
	}

	public synchronized void setBufferedImageNoChange(BufferedImage image) {
		// assume the image was initially set before the GUI was invoked
		if( checkEventDispatch && this.img != null ) {
			if( !SwingUtilities.isEventDispatchThread() )
				throw new RuntimeException("Changed image when not in GUI thread?");
		}
		this.img = image;
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

	public class ImagePanel extends JPanel
	{
		SaveImageOnClick mouseListener = new SaveImageOnClick(ImageZoomPanel.this.getViewport());

		BufferedImage buffer = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);

		public ImagePanel() {
			addMouseListener(mouseListener);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			if( img == null ) return;

			// render to a buffer first to improve performance. This is particularly evident when rendering
			// lines for some reason

			int w = ImageZoomPanel.this.getWidth(),h = ImageZoomPanel.this.getHeight();
			buffer = ConvertBufferedImage.checkDeclare(w,h,buffer,buffer.getType());
			Graphics2D g2 = buffer.createGraphics();
			g2.setColor(getBackground());
			g2.fillRect(0,0,w,h);

			ImageZoomPanel.this.transX = -ImageZoomPanel.this.getHorizontalScrollBar().getValue();
			ImageZoomPanel.this.transY = -ImageZoomPanel.this.getVerticalScrollBar().getValue();

			AffineTransform tran = new AffineTransform(scale,0,0,scale,transX,transY);
			synchronized ( ImageZoomPanel.this ) {
				g2.drawImage(img, tran, null);
			}

			AffineTransform orig = g2.getTransform();
			// make the default behavior be a translate for backwards compatibility. Before it was rendered in a
			// buffered image it was rendered into the full scale panel
			tran = new AffineTransform(1,0,0,1,transX,transY);
			g2.setTransform(tran);
			paintInPanel(tran, g2);
			g2.setTransform(orig);

			g.drawImage(buffer,(int)-transX,(int)-transY,null);
		}
	}

	public double getScale() {
		return scale;
	}

	public ImagePanel getImagePanel() {
		return panel;
	}
}
