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

package boofcv.gui.tracker;

import boofcv.struct.feature.NccFeature;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author Peter Abeles
 */
public class TldTemplatePanel extends JPanel {

	int featureWidth;

	int scale = 2;

	List<BufferedImage> templates = new ArrayList<>();

	Stack<BufferedImage> unused = new Stack<>();

	public TldTemplatePanel(int featureWidth) {
		this.featureWidth = featureWidth;
		setAutoscrolls(true);

		setPreferredSize(new Dimension(featureWidth*scale,30));
		setMinimumSize(getPreferredSize());
	}

	public synchronized void update( List<NccFeature> features , boolean gray ) {

		unused.addAll(templates);
		templates.clear();

		for( NccFeature f : features ) {
			BufferedImage img;
			if( unused.isEmpty() ) {
				img = new BufferedImage(featureWidth,featureWidth,BufferedImage.TYPE_INT_RGB);
			} else {
				img = unused.pop();
			}
			templates.add(img);

			int index = 0;
			int rgb;

			if( gray ) {
				for( int y = 0; y < featureWidth; y++ ) {
					for( int x = 0; x < featureWidth; x++ ) {
						int v = (int)(f.value[ index++ ] + f.mean);
						rgb = v << 16 | v << 8 | v;

						img.setRGB(x,y,rgb);
					}
				}
			} else {
				double maxAbs = 0;
				for( int i = 0; i < f.value.length; i++ ) {
					double v = Math.abs(f.value[i]);
					if( v > maxAbs )
						maxAbs = v;
				}
				if( maxAbs == 0 )
					continue;
				for( int y = 0; y < featureWidth; y++ ) {
					for( int x = 0; x < featureWidth; x++ ) {
						int v = (int)(255.0*f.value[ index++ ]/maxAbs);
						if( v < 0 )
							rgb = -v;
						else
							rgb = v << 16;

						img.setRGB(x,y,rgb);
					}
				}
			}
		}

		setPreferredSize(new Dimension(featureWidth*scale,featureWidth*features.size()*scale));
		setMinimumSize(getPreferredSize());
		revalidate();
	}

	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		g2.scale(scale,scale);

		for( int i = 0; i < templates.size(); i++ ) {
			int y = i * featureWidth;
			g2.drawImage(templates.get(i),0,y,featureWidth,featureWidth,null);
		}
	}
}
