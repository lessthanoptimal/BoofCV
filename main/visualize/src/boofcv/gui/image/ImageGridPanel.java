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
import java.awt.image.BufferedImage;

/**
 * Displays images in a grid pattern
 *
 * @author Peter Abeles
 */
public class ImageGridPanel extends JPanel {
	BufferedImage images[];

	int numRows;
	int numCols;

	public ImageGridPanel( int numRows , int numCols ) {
		images = new BufferedImage[numRows*numCols];

		this.numRows = numRows;
		this.numCols = numCols;
	}

	public ImageGridPanel( int numRows , int numCols , BufferedImage ...images  ) {
		this.images = new BufferedImage[numRows*numCols];
		for( int i = 0; i < images.length;i++ ) {
			this.images[i] = images[i];
		}
		this.numRows = numRows;
		this.numCols = numCols;

		autoSetPreferredSize();
	}

	public synchronized void autoSetPreferredSize() {
		int width = 0;
		int height = 0;

		for( int row = 0; row < numRows; row++ ) {
			int h = 0;
			int w = 0;
			for( int col = 0; col < numCols; col++ ) {
				BufferedImage img = images[row*numCols+col];
				if( img == null )
					continue;

				w += img.getWidth();
				h = Math.max(h,img.getHeight());
			}
			width = Math.max(w,width);
			height += h;
		}

		setPreferredSize( new Dimension(width,height));
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		int y = 0;
		for( int row = 0; row < numRows; row++ ) {
			int rowHeight = 0;
			int x = 0;

			for( int col = 0; col < numCols; col++ ) {
				BufferedImage img = images[row*numCols+col];
				if( img == null )
					continue;

				g2.drawImage(img, x,y,null);

				x += img.getWidth();

				rowHeight = Math.max(rowHeight,img.getHeight());
			}
			y += rowHeight;
		}
	}

	public synchronized void setImage( int row , int col , BufferedImage image ) {
		this.images[row*numCols+col] = image;
	}

	public synchronized BufferedImage getImage( int row , int col ) {
		return images[row*numCols+col];
	}

	public synchronized void setImages( BufferedImage ...images ) {
		for( int i = 0; i < images.length; i++ ) {
			this.images[i] = images[i];
		}
		autoSetPreferredSize();
	}
}
