/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Simple JPanel for displaying buffered images allows images to be zoomed in and out of
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ImageZoomPanel extends JScrollPane {
	// the image being displayed
	protected @Nullable BufferedImage img;
	protected ImagePanel panel = new ImagePanel();

	public boolean autoScaleCenterOnSetImage = true;
	private boolean hasImageChanged = false;

	protected double scale = 1;
	protected double transX = 0, transY = 0;

	boolean checkEventDispatch = true;

	protected ImageZoomListener listener;

	public ImageZoomPanel( final @Nullable BufferedImage img ) {
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

	public void autoScaleAndAlign() {
		this.autoScaleAndAlign(Objects.requireNonNull(img));
	}

	/**
	 * Automatically recomputes the scale to fit inside the window and centers it in the view
	 */
	private void autoScaleAndAlign( final BufferedImage img ) {
		double ratioW = (double)getWidth()/(double)img.getWidth();
		double ratioH = (double)getHeight()/(double)img.getHeight();

		double scale = Math.min(ratioW, ratioH);
		if (scale >= 1)
			scale = 1;

		boolean scaleChange = this.scale != scale;
		this.scale = scale;
		if (scaleChange) {
			if (listener != null) {
				listener.handleScaleChange(this.scale);
			}
			updateSize(img.getWidth(), img.getHeight());
		}
		// Scroll it to the top left (this is the align part)
		getHorizontalScrollBar().setValue(0);
		getVerticalScrollBar().setValue(0);
	}

	public synchronized void setScale( double scale ) {
		// do nothing if the scale has not changed
		if (this.scale == scale)
			return;

		Rectangle r = panel.getVisibleRect();
		double centerX = (r.x + r.width/2.0)/this.scale;
		double centerY = (r.y + r.height/2.0)/this.scale;

		this.scale = scale;
		if (img != null) {
			int w = (int)Math.ceil(img.getWidth()*scale);
			int h = (int)Math.ceil(img.getHeight()*scale);
			panel.setPreferredSize(new Dimension(w, h));
		}
		getViewport().setView(panel);

		centerView(centerX, centerY);

		{
			ImageZoomListener listener = this.listener;
			if (listener != null) {
				listener.handleScaleChange(this.scale);
			}
		}
	}

	public synchronized void setScaleAndCenter( double scale, double cx, double cy ) {
		boolean scaleChanged = false;
		if (scale != this.scale) {
			scaleChanged = true;
			this.scale = scale;
		}
		if (img != null) {
			int w = (int)Math.ceil(img.getWidth()*scale);
			int h = (int)Math.ceil(img.getHeight()*scale);
			panel.setPreferredSize(new Dimension(w, h));
		}
		getViewport().setView(panel);
		centerView(cx, cy);

		if (scaleChanged) {
			ImageZoomListener listener = this.listener;
			if (listener != null) {
				listener.handleScaleChange(this.scale);
			}
		}
	}

	public synchronized void centerView( double cx, double cy ) {
		Rectangle r = panel.getVisibleRect();
		int x = (int)(cx*scale - r.width/2);
		int y = (int)(cy*scale - r.height/2);

		getHorizontalScrollBar().setValue(x);
		getVerticalScrollBar().setValue(y);
	}

	/**
	 * Change the image being displayed.
	 *
	 * @param image The new image which will be displayed.
	 */
	public synchronized void setImage( BufferedImage image ) {
		// assume the image was initially set before the GUI was invoked
		if (checkEventDispatch && this.img != null) {
			if (!SwingUtilities.isEventDispatchThread())
				throw new RuntimeException("Changed image when not in GUI thread?");
		}

		hasImageChanged = this.img != image;
		int beforeWidth = -1, beforeHeight = -1;
		if (this.img != null) {
			beforeWidth = this.img.getWidth();
			beforeHeight = this.img.getHeight();
		}
		setBufferedImageNoChange(image);
		// only update size if the image size has changed
		if (image != null && hasImageChanged && beforeWidth != image.getWidth() && beforeHeight != image.getHeight()) {
			updateSize(image.getWidth(), image.getHeight());
		}
	}

	public void updateSize( int width, int height ) {
		Dimension prev = getPreferredSize();

		int w = (int)Math.ceil(width*scale);
		int h = (int)Math.ceil(height*scale);

		if (prev.getWidth() != w || prev.getHeight() != h) {
			panel.setPreferredSize(new Dimension(w, h));
			getViewport().setView(panel);
		}
	}

	public synchronized void setBufferedImageNoChange( @Nullable BufferedImage image ) {
		// assume the image was initially set before the GUI was invoked
		if (checkEventDispatch && this.img != null) {
			if (!SwingUtilities.isEventDispatchThread())
				throw new RuntimeException("Changed image when not in GUI thread?");
		}
		this.img = image;
	}

	public @Nullable BufferedImage getImage() {
		return img;
	}

	public Point2D_F64 pixelToPoint( int x, int y ) {
		Point2D_F64 ret = new Point2D_F64();
		ret.x = x/scale;
		ret.y = y/scale;

		return ret;
	}

	/**
	 * Paint inside the image panel. Useful for overlays
	 */
	protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
		// intentionally empty
	}

	/** Called very last when you don't want to draw in image coordinates */
	protected void paintOverPanel( Graphics2D g2 ) {}

	public class ImagePanel extends JPanel {
		@Nullable SaveImageOnClick mouseListener = new SaveImageOnClick(ImageZoomPanel.this.getViewport());

		BufferedImage buffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		public ImagePanel() {
			addMouseListener(mouseListener);
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			if (img == null) {
				paintOverPanel((Graphics2D)g);
				return;
			}

			if (hasImageChanged) {
				hasImageChanged = false;
				if (autoScaleCenterOnSetImage) {
					autoScaleAndAlign();
				}
			}
			Graphics2D panelGraphics = (Graphics2D)g;

			// render to a buffer first to improve performance. This is particularly evident when rendering
			// lines for some reason

			int w = ImageZoomPanel.this.getWidth(), h = ImageZoomPanel.this.getHeight();
			buffer = ConvertBufferedImage.checkDeclare(w, h, buffer, buffer.getType());
			Graphics2D bufferGraphics = this.buffer.createGraphics();
			bufferGraphics.setColor(getBackground());
			bufferGraphics.fillRect(0, 0, w, h);

			ImageZoomPanel.this.transX = -ImageZoomPanel.this.getHorizontalScrollBar().getValue();
			ImageZoomPanel.this.transY = -ImageZoomPanel.this.getVerticalScrollBar().getValue();

			AffineTransform tran = new AffineTransform(scale, 0, 0, scale, transX, transY);
			synchronized (ImageZoomPanel.this) {
				bufferGraphics.drawImage(img, tran, null);
			}

			AffineTransform orig = bufferGraphics.getTransform();
			// make the default behavior be a translate for backwards compatibility. Before it was rendered in a
			// buffered image it was rendered into the full scale panel
			tran = new AffineTransform(1, 0, 0, 1, transX, transY);
			bufferGraphics.setTransform(tran);
			paintInPanel(tran, bufferGraphics);
			bufferGraphics.setTransform(orig);

			panelGraphics.drawImage(this.buffer, (int)-transX, (int)-transY, null);

			paintOverPanel(panelGraphics);
		}
	}

	public void setScrollbarsVisible( boolean visible ) {
		if (visible) {
			setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		} else {
			setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		}
		repaint();
	}

	public double getScale() {
		return scale;
	}

	public ImagePanel getImagePanel() {
		return panel;
	}

	public ImageZoomListener getListener() {
		return listener;
	}

	public void setListener( ImageZoomListener listener ) {
		this.listener = listener;
	}

	public interface ImageZoomListener {
		void handleScaleChange( double scale );
	}
}
