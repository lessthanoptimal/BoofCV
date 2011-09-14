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
	protected boolean resize = true;

	public ImagePanel(BufferedImage img) {
		this(img,false);
	}

	public ImagePanel(BufferedImage img , boolean resize ) {
		this.img = img;
		this.resize = resize;
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
	}

	public ImagePanel() {
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		//draw the image
		if (img != null) {
			if( resize ) {
				double ratioW = (double)getWidth()/(double)img.getWidth();
				double ratioH = (double)getHeight()/(double)img.getHeight();

				double ratio = Math.min(ratioW,ratioH);
				if( ratio >= 1 ) {
					g.drawImage(img, 0, 0, this);
				} else {
					AffineTransform tran = new AffineTransform();
					tran.setToScale(ratio,ratio);
					g2.drawImage(img,tran,null);
				}

			} else {
				g2.drawImage(img, 0, 0, this);
			}
		}
	}

	/**
	 * Change the image being displayed.
	 *
	 * @param image The new image which will be displayed.
	 */
	public void setBufferedImage(BufferedImage image) {
		this.img = image;
	}

	public BufferedImage getImage() {
		return img;
	}

	public void setResize(boolean resize) {
		this.resize = resize;
	}
}
