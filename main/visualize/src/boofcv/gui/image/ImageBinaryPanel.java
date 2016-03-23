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

import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.struct.image.GrayU8;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * Used for displaying binary images.
 *
 * @author Peter Abeles
 */
public class ImageBinaryPanel extends JPanel {

	// the image being displayed
	protected BufferedImage img;
	protected GrayU8 binaryImage;

	public ImageBinaryPanel( GrayU8 binaryImage ) {
		this.binaryImage = binaryImage;
		img = new BufferedImage(binaryImage.getWidth(),binaryImage.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		VisualizeBinaryData.renderBinary(binaryImage,false,img);

		setPreferredSize(new Dimension(binaryImage.getWidth(), binaryImage.getHeight()));
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
	}

	protected ImageBinaryPanel() {
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		//draw the image
		if (img != null)
			g.drawImage(img, 0, 0, this);
	}

	public synchronized void setBinaryImage(GrayU8 binaryImage) {
		this.binaryImage = binaryImage;
		VisualizeBinaryData.renderBinary(binaryImage,false,img);
	}

	public BufferedImage getImage() {
		return img;
	}
}
