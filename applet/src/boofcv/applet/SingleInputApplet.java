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

package boofcv.applet;

import boofcv.gui.ProcessImage;
import boofcv.gui.UtilVisualize;
import boofcv.io.image.SelectInputImageToolBar;
import boofcv.struct.image.ImageFloat32;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


/**
 * @author Peter Abeles
 */
public class SingleInputApplet extends JApplet {

	@Override
	public void init() {
		showStatus("Loading Input Image");
		String directory = getParameter("directory");
		final BufferedImage image = loadImage(directory+"data/indoors01.jpg");


		final String panelPath = getParameter("panelPath");
		Class inputType = ImageFloat32.class;

		showStatus("Creating GUI component");
		final JComponent comp = FactoryVisualPanel.create(panelPath,inputType);

		if( comp == null ) {
			showStatus("Failed to create GUI component");
		} else if( !(comp instanceof ProcessImage) ) {
			showStatus("Loaded component is not of ProcessImage type");
			return;
		}

		SelectInputImageToolBar toolbar = new SelectInputImageToolBar(comp);
		toolbar.addImage("Room",directory+"data/indoors01.jpg");
		toolbar.addImage("Shapes",directory+"data/shapes01.png");
		toolbar.addImage("Sunflowers",directory+"data/sunflowers.jpg");

		UtilVisualize.manageSelectInput(toolbar,(ProcessImage)comp,getCodeBase(),true);

		getContentPane().add(toolbar, BorderLayout.CENTER);
	}


	public BufferedImage loadImage( String path ) {
		try {
		   URL url = new URL(getCodeBase(), path);
		   return ImageIO.read(url);
		} catch (IOException e) {
			System.err.println("Failed to load image: "+path);
			return null;
		}
	}

	@Override
	public String getAppletInfo() {
		return "Shows gradient of an image";
	}
}
