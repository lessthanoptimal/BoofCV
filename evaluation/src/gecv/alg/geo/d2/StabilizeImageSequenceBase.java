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

package gecv.alg.geo.d2;

import gecv.alg.geo.d2.stabilization.PointImageStabilization;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.geo.DrawAssociatedPairs;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.struct.image.ImageBase;

import java.awt.*;
import java.awt.image.BufferedImage;

// todo evaluation metrics
//       area of overlap with keyframe
//       difference to key frame
//       number of resets
//       frames before reset
/**
 * @author Peter Abeles
 */
// todo Draw used/unused features
// todo count number of resets
public class StabilizeImageSequenceBase <I extends ImageBase>
		extends ProcessImageSequence<I>
{
	DrawAssociatedPairs drawFeatures = new DrawAssociatedPairs(3);
	PointImageStabilization<I> stabilizer;

	ImagePanel panelStabilized;
	ImagePanel panelOriginal;

	BufferedImage stabilizedBuff;

	public StabilizeImageSequenceBase(SimpleImageSequence<I> imageSequence ) {
		super(imageSequence);
	}

	public void setStabilizer(PointImageStabilization<I> stabilizer) {
		this.stabilizer = stabilizer;
	}

	@Override
	public void processFrame(I image) {
		stabilizer.process(image);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, I origImage) {

		I stabilizedImage = stabilizer.getStabilizedImage();
		stabilizedBuff = ConvertBufferedImage.convertTo(stabilizedImage,stabilizedBuff);

		Graphics2D g2 = guiImage.createGraphics();
		drawFeatures.setColor(Color.red);
		drawFeatures.drawCurrent(g2,stabilizer.getTracker().getActiveTracks());
		drawFeatures.setColor(Color.blue);
		drawFeatures.drawCurrent(g2,stabilizer.getInlierFeatures());
		drawFeatures.drawNumber(g2,stabilizer.getInlierFeatures());

		if (panelStabilized == null) {
			panelStabilized = ShowImages.showWindow(stabilizedBuff, "Stabilized Image");
			addComponent(panelStabilized);
			panelOriginal = ShowImages.showWindow(guiImage,"Original");
		} else {
			panelStabilized.setBufferedImage(stabilizedBuff);
			panelStabilized.repaint();
			panelOriginal.setBufferedImage(guiImage);
			panelOriginal.repaint();
		}
	}
}
