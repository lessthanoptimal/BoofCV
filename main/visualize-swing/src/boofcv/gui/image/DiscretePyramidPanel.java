/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidDiscrete;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


/**
 * Displays the entire image pyramid in a single panel.  With discrete pyramids it is possible
 * to show the whole pyramid inside a single window of a reasonable size.
 *
 * @author Peter Abeles
 */
public class DiscretePyramidPanel<T extends ImageGray> extends JPanel {

	BufferedImage img;
	ImagePyramid<T> pyramid;

	BufferedImage layers[];

	public DiscretePyramidPanel() {
	}

	public DiscretePyramidPanel(PyramidDiscrete<T> pyramid) {
		setPyramid(pyramid);
	}

	public void setPyramid( PyramidDiscrete<T> pyramid ) {
		this.pyramid = pyramid;

		// create temporary buffers for each layer in the pyramid
		layers = new BufferedImage[ pyramid.getNumLayers() ];
		for( int i = 0; i < layers.length; i++ ) {
			int width = pyramid.getWidth(i);
			int height = pyramid.getHeight(i);
			layers[i] = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
		}

		// compute the size of the panel
		int width = pyramid.getWidth(0);
		int height = pyramid.getHeight(0);

		if( pyramid.getNumLayers() > 1 ) {
			width += pyramid.getWidth(1);
		}

		img = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);

		setPreferredSize(new Dimension(width,height));
	}

	public void render() {
		for( int i = 0; i < layers.length; i++ ) {
			VisualizeImageData.standard(pyramid.getLayer(i),layers[i]);
		}

		Graphics2D g2 = (Graphics2D)img.getGraphics();

		g2.drawImage(layers[0],0,0,this);
		int height = 0;
		int width = layers[0].getWidth();
		for( int i = 1; i < layers.length; i++ ) {
			g2.drawImage(layers[i],width,height,this);
			height += layers[i].getHeight();
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		if( img != null) {
			Graphics2D g2 = (Graphics2D)g;
			g2.setColor(Color.WHITE);
			g2.fillRect(0,0,getWidth(),getHeight());
			
			double scale = img.getWidth()/(double)getWidth();
			if( scale > 1 ) {
				AffineTransform tran = new AffineTransform();
				tran.setToScale(1.0/scale,1.0/scale);
				g2.drawImage(img,tran,null);
			} else {
				g.drawImage(img, 0, 0, this);
			}
		}
	}
}
