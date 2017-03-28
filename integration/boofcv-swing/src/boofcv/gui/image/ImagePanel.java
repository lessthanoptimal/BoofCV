/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

	protected SaveImageOnClick mouseListener;

	private AffineTransform transform = new AffineTransform();
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

		//draw the image
		BufferedImage img = this.img;
		if (img != null) {
			computeOffsetAndScale();
			if( scale == 1 ) {
				if( offsetX == 0 && offsetY == 0 ) {
					g2.drawImage(img,transform,null);
				} else {
					g2.drawImage(img, (int)offsetX, (int)offsetY, this);
				}
			} else {
				transform.setTransform(scale,0,0,scale,offsetX,offsetY);
				g2.drawImage(img,transform,null);
			}
		}
	}

	private void computeOffsetAndScale() {
		if( scaling != ScaleOptions.NONE ) {
			double ratioW = (double)getWidth()/(double)img.getWidth();
			double ratioH = (double)getHeight()/(double)img.getHeight();

			scale = Math.min(ratioW,ratioH);
			if( scaling == ScaleOptions.DOWN && scale >= 1 )
				scale = 1;

			if( center ) {
				offsetX = (getWidth()-img.getWidth()*scale)/2;
				offsetY = (getHeight()-img.getHeight()*scale)/2;
			} else {
				offsetX = 0;
				offsetY = 0;
			}

			if( scale == 1 ) {
				offsetX = (int)offsetX;
				offsetY = (int)offsetY;
			}
		} else {
			offsetX = (img.getWidth() - getWidth())/2;
			offsetY = (img.getHeight() - getHeight())/2;

			scale = 1;
		}
	}

	/**
	 * Change the image being displayed. If panel is active then don't call unless inside the GUI thread.  Repaint()
    * is not automatically called.
	 *
	 * @param image The new image which will be displayed.
	 */
	public void setBufferedImage(BufferedImage image) {
		// if image is larger before  than the new image then you need to make sure you repaint
		// the entire image otherwise a ghost will be left
		repaintJustImage();
		this.img = image;
		repaintJustImage();
	}

   /**
    * Change the image being displayed. Can be called at any time and automatically called repaint().
    *
    * @param image The new image which will be displayed.
    */
   public void setBufferedImageSafe(final BufferedImage image) {
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
			 setBufferedImage(image);
         }
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
		if( img == null ) {
			repaint();
			return;
		}
		computeOffsetAndScale();

		repaint((int)Math.round(offsetX)-1,(int)Math.round(offsetY)-1,
				(int)(img.getWidth()*scale+0.5)+2,(int)(img.getHeight()*scale+0.5)+2);
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
}
