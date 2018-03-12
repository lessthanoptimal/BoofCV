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

import boofcv.gui.BoofSwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


/**
 * Simple JPanel for displaying buffered images.
 *
 * @author Peter Abeles
 */
public class ImagePanel extends JPanel {
	// the image being displayed
	protected BufferedImage img;
	// should it re-size the image based on the panel's size
	protected ScaleOptions scaling = ScaleOptions.DOWN;

	public double scale = 1;
	public double offsetX=0;
	public double offsetY=0;

	//  this variable must only be touched inside the GUI thread
	private ScaleOffset adjustmentGUI = new ScaleOffset();

	protected SaveImageOnClick mouseListener;

	protected AffineTransform transform = new AffineTransform();
	private boolean center = false;

	public ImagePanel(BufferedImage img) {
		this(img,ScaleOptions.NONE);
	}

	public ImagePanel(final BufferedImage img , ScaleOptions scaling ) {
		this(true);
		this.img = img;
		this.scaling = scaling;
		autoSetPreferredSize();
	}

	public ImagePanel( int width , int height ) {
		this(true);
		setPreferredSize(new Dimension(width,height));
	}

	/**
	 * Adds the ability to save an image using the middle mouse button.  A dialog is shown to the user
	 * so that they know what has happened.  They can hide it in the future if they wish.
	 */

	public ImagePanel( boolean addMouseListener ) {
		if( addMouseListener ) {
			// Adds the ability to save an image using the middle mouse button.  A dialog is shown to the user
			// so that they know what has happened.  They can hide it in the future if they wish.
			mouseListener = new SaveImageOnClick(this);
			addMouseListener(mouseListener);
		}
	}

	public ImagePanel() {
		this(true);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform original = g2.getTransform();
		configureDrawImageGraphics(g2);

		//draw the image
		BufferedImage img = this.img;
		if (img != null) {
			computeOffsetAndScale(img,adjustmentGUI);
			scale = adjustmentGUI.scale;
			offsetX = adjustmentGUI.offsetX;
			offsetY = adjustmentGUI.offsetY;

			if( scale == 1 ) {
				g2.drawImage(img, (int)offsetX, (int)offsetY, this);
			} else {
				transform.setTransform(scale,0,0,scale,offsetX,offsetY);
				g2.drawImage(img,transform,null);
			}
		}

		g2.setTransform(original);
	}

	protected void configureDrawImageGraphics( Graphics2D g2 ) {}

	private void computeOffsetAndScale(BufferedImage img, ScaleOffset so ) {
		if( scaling != ScaleOptions.NONE ) {
			if( scaling == ScaleOptions.MANUAL ) {
				so.scale = scale;
			} else {
				double ratioW = (double) getWidth() / (double) img.getWidth();
				double ratioH = (double) getHeight() / (double) img.getHeight();

				so.scale = Math.min(ratioW, ratioH);
				if (scaling == ScaleOptions.DOWN && so.scale >= 1)
					so.scale = 1;
			}

			if( center ) {
				so.offsetX = (getWidth()-img.getWidth()*so.scale)/2;
				so.offsetY = (getHeight()-img.getHeight()*so.scale)/2;
			} else {
				so.offsetX = 0;
				so.offsetY = 0;
			}

			if( scale == 1 ) {
				so.offsetX = (int)so.offsetX;
				so.offsetY = (int)so.offsetY;
			}
		} else {
			if( center ) {
				so.offsetX = (getWidth()-img.getWidth())/2;
				so.offsetY = (getHeight()-img.getHeight())/2;
			} else {
				so.offsetX = 0;
				so.offsetY = 0;
			}

			so.scale = 1;
		}
	}

	/**
	 * Change the image being displayed. If panel is active then don't call unless inside the GUI thread.  Repaint()
	 * is not automatically called.
	 *
	 * @param image The new image which will be displayed.
	 */
	public void setImage(BufferedImage image) {
		this.img = image;
	}

	/**
	 * Changes the buffered image and calls repaint.  Does not need to be called in the UI thread.
	 */
	public void setImageRepaint(BufferedImage image) {
		// if image is larger before  than the new image then you need to make sure you repaint
		// the entire image otherwise a ghost will be left
		ScaleOffset workspace;
		if( SwingUtilities.isEventDispatchThread() ) {
			workspace = adjustmentGUI;
		} else {
			workspace = new ScaleOffset();
		}
		repaintJustImage(img,workspace);
		this.img = image;
		repaintJustImage(img,workspace);
	}

	/**
	 * Changes the image and will be invoked inside the UI thread at a later time.  repaint() is automatically
	 * called.
	 *
	 * @param image The new image which will be displayed.
	 */
	public void setImageUI(final BufferedImage image) {
		BoofSwingUtil.invokeNowOrLater(() -> {
			repaintJustImage(ImagePanel.this.img,adjustmentGUI);
			ImagePanel.this.img = image;
			repaintJustImage(ImagePanel.this.img,adjustmentGUI);
		});
	}

	public boolean isCentered() {
		return center;
	}

	public void setCentering(boolean center) {
		this.center = center;
	}

	/**
	 * Repaints just the region around the image.
	 */
	public void repaintJustImage() {
		repaintJustImage(img, new ScaleOffset());
	}

	protected void repaintJustImage(BufferedImage img,ScaleOffset workspace ) {
		if( img == null ) {
			repaint();
			return;
		}
		computeOffsetAndScale(img,workspace);

		repaint((int)Math.round(workspace.offsetX)-1,(int)Math.round(workspace.offsetY)-1,
				(int)(img.getWidth()*workspace.scale+0.5)+2,(int)(img.getHeight()*workspace.scale+0.5)+2);
	}

	public BufferedImage getImage() {
		return img;
	}

	public void setScaling(ScaleOptions scaling) {
		this.scaling = scaling;
	}

	public void autoSetPreferredSize() {
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
	}

	public MouseListener getMouseClickToSaveListener() {
		return mouseListener;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	private static class ScaleOffset {
		double scale,offsetX,offsetY;
	}
}
