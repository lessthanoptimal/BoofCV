/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author Peter Abeles
 */

public abstract class TrackVideoPyramidKLT<InputImage extends ImageBase, DerivativeImage extends ImageBase>
		extends ProcessImageSequence<InputImage> {

	private PkltManager<InputImage,DerivativeImage> tracker;

	ImagePanel panel;
	int totalRespawns;

	public TrackVideoPyramidKLT(SimpleImageSequence<InputImage> sequence,
								PkltManager<InputImage,DerivativeImage> tracker ) {
		super(sequence);
		this.tracker = tracker;
	}


	@Override
	public void processFrame(InputImage image) {

		tracker.processFrame(image);


	}

	@Override
	public void updateGUI(BufferedImage guiImage, InputImage origImage) {
		Graphics2D g2 = guiImage.createGraphics();
		
		drawFeatures(g2, tracker.getFeatures(), Color.RED);
		drawFeatures(g2, tracker.getSpawned(), Color.BLUE);

		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "KLT Pyramidal Tracker");
			addComponent(panel);
		} else {
			panel.setBufferedImage(guiImage);
			panel.repaint();
		}

		if( tracker.getSpawned().size() != 0 )
			totalRespawns++;
		System.out.println(" total features: "+tracker.getFeatures().size()+" totalRespawns "+totalRespawns);
	}

	private void drawFeatures(Graphics2D g2,
							  java.util.List<PyramidKltFeature> list,
							  Color color ) {
		int r = 3;
		int w = r*2+1;
		int ro = r+2;
		int wo = ro*2+1;

		for (int i = 0; i < list.size(); i++) {
			PyramidKltFeature pt = list.get(i);

			int x = (int)pt.x;
			int y = (int)pt.y;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - ro, y - ro, wo, wo);
			g2.setColor(color);
			g2.fillOval(x - r, y - r, w, w);
		}
	}
}
