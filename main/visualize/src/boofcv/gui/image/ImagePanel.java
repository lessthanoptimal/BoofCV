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

import javax.swing.*;
import java.awt.*;
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

	public ImagePanel(BufferedImage img) {
		this(img,ScaleOptions.NONE);
	}

	public ImagePanel(final BufferedImage img , ScaleOptions scaling ) {
		this.img = img;
		this.scaling = scaling;
		autoSetPreferredSize();
	}

	public ImagePanel( int width , int height ) {
		setPreferredSize(new Dimension(width,height));
	}

	public ImagePanel() {
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		//draw the image
		BufferedImage img = this.img;
		if (img != null) {
			if( scaling != ScaleOptions.NONE ) {
				double ratioW = (double)getWidth()/(double)img.getWidth();
				double ratioH = (double)getHeight()/(double)img.getHeight();

				double ratio = Math.min(ratioW,ratioH);
				if( scaling == ScaleOptions.DOWN && ratio >= 1 )
					ratio = 1;

				if( ratio == 1 ) {
					g.drawImage(img, 0, 0, this);
				} else {
					AffineTransform tran = AffineTransform.getScaleInstance(ratio, ratio);
//					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//							RenderingHints.VALUE_ANTIALIAS_ON);
					g2.drawImage(img,tran,null);
				}

			} else {
				g2.drawImage(img, 0, 0, this);
			}
		}
	}

	/**
	 * Change the image being displayed. If panel is active then don't call unless inside the GUI thread.  Repaint()
    * is not automatically called.
	 *
	 * @param image The new image which will be displayed.
	 */
	public void setBufferedImage(BufferedImage image) {
		this.img = image;
		this.repaint();
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

	public BufferedImage getImage() {
		return img;
	}

	public void setScaling(ScaleOptions scaling) {
		this.scaling = scaling;
	}

	public void autoSetPreferredSize() {
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
	}
}
