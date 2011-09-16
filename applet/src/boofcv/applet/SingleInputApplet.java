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
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;


/**
 * @author Peter Abeles
 */
public class SingleInputApplet extends JApplet {

	@Override
	public void init() {
		showStatus("Loading Parameters");
		String directory = getParameter("directory");

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

		showStatus("Loading Image");
		ProcessImage p = (ProcessImage)comp;

		AppletImageListManager manager = new AppletImageListManager(getCodeBase());
		manager.add("shapes",directory+"data/shapes01.png");
		manager.add("sunflowers",directory+"data/sunflowers.jpg");
		manager.add("room",directory+"data/indoors01.jpg");

		p.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !p.getHasProcessedImage() ) {
			Thread.yield();
		}

		showStatus("Running");
		getContentPane().add(comp, BorderLayout.CENTER);
	}

	@Override
	public String getAppletInfo() {
		return "Shows gradient of an image";
	}
}
