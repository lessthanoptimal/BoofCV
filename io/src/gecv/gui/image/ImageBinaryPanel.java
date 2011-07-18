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

package gecv.gui.image;

import gecv.struct.image.ImageUInt8;
import sun.awt.image.ByteInterleavedRaster;

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
	protected ImageUInt8 binaryImage;

	public ImageBinaryPanel( ImageUInt8 binaryImage ) {
		this.binaryImage = binaryImage;
		img = new BufferedImage(binaryImage.getWidth(),binaryImage.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		renderImage();

		setPreferredSize(new Dimension(binaryImage.getWidth(), binaryImage.getHeight()));
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
	}

	protected ImageBinaryPanel() {
	}

	@Override
	public void paintComponent(Graphics g) {
		//draw the image
		if (img != null)
			g.drawImage(img, 0, 0, this);
	}

	public void renderImage() {
		ByteInterleavedRaster raster = (ByteInterleavedRaster)img.getRaster();

		int rasterIndex = 0;
		byte data[] = raster.getDataStorage();

		int w = binaryImage.getWidth();
		int h = binaryImage.getHeight();


		for( int y = 0; y < h; y++ ) {
			int indexSrc = binaryImage.startIndex + y*binaryImage.stride;
			for( int x = 0; x < w; x++ ) {
				 data[rasterIndex++] = binaryImage.data[indexSrc++] > 0 ? (byte)255 : (byte)0;
			}
		}
	}

	public void setBinaryImage(ImageUInt8 binaryImage) {
		this.binaryImage = binaryImage;
	}

	public BufferedImage getImage() {
		return img;
	}
}
