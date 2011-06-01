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

import gecv.alg.pyramid.GradientPyramid;
import gecv.alg.pyramid.PyramidUpdater;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ImagePyramidPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */

public abstract class TrackVideoPyramidKLT<I extends ImageBase, D extends ImageBase>
		extends ProcessImageSequence<I> {

	private PkltManager<I, D> tracker;

	ImagePanel panel;
	ImagePyramidPanel pyramidPanel;
	int totalRespawns;

	PyramidUpdater<I> pyramidUpdater;
	GradientPyramid<I,D> updateGradient;

	ImagePyramid<I> basePyramid;
	ImagePyramid<D> derivX;
	ImagePyramid<D> derivY;


	@SuppressWarnings({"unchecked"})
	public TrackVideoPyramidKLT(SimpleImageSequence<I> sequence,
								PkltManager<I, D> tracker ,
								PyramidUpdater<I> pyramidUpdater ,
								GradientPyramid<I,D> updateGradient) {
		super(sequence);
		this.tracker = tracker;
		this.pyramidUpdater = pyramidUpdater;
		this.updateGradient = updateGradient;
		PkltManagerConfig<I, D> config = tracker.getConfig();

		// declare the image pyramid
		basePyramid = ImagePyramidFactory.create(
				config.imgWidth,config.imgHeight,true,config.typeInput);
		derivX = ImagePyramidFactory.create(
				config.imgWidth,config.imgHeight,false,config.typeDeriv);
		derivY = ImagePyramidFactory.create(
				config.imgWidth,config.imgHeight,false,config.typeDeriv);

		basePyramid.setScaling(config.pyramidScaling);
		derivX.setScaling(config.pyramidScaling);
		derivY.setScaling(config.pyramidScaling);

		pyramidUpdater.setPyramid(basePyramid);
	}


	@Override
	public void processFrame(I image) {

		pyramidUpdater.update(image);
		updateGradient.update(basePyramid,derivX,derivY);

		tracker.processFrame(basePyramid,derivX,derivY);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, I origImage) {
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

//		if( pyramidPanel == null ) {
//			pyramidPanel = new ImagePyramidPanel(tracker.getPyramid());
//			ShowImages.showWindow(pyramidPanel,"Pyramid");
//			addComponent(pyramidPanel);
//		} else {
//			pyramidPanel.render();
//			pyramidPanel.repaint();
//		}

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
