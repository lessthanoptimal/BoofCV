/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.applet;

import boofcv.gui.VisualizeApp;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.io.File;


/**
 * Applet for visualizations which take in as input a single image.
 *
 * @author Peter Abeles
 */
public class MediaApplet extends JApplet {

	@Override
	public void init() {
		showStatus("Loading Parameters");
		String dataset = getParameter("dataset");
		String config = getParameter("config");

		if( dataset == null ) {
			showStatus("dataset parameter not set!");
			return;
		}

		final String panelPath = getParameter("panelPath");
		Class inputType = ImageFloat32.class;

		showStatus("Creating GUI component");
		final JComponent comp = FactoryVisualPanel.create(panelPath,inputType);

		if( comp == null ) {
			showStatus("Failed to create GUI component");
			return;
		} else if( !(comp instanceof VisualizeApp) ) {
			showStatus("The loaded component is not of VisualizeApp type");
			return;
		}

		VisualizeApp p = (VisualizeApp)comp;

		AppletMediaManager amm = new AppletMediaManager(getCodeBase());
		amm.setOwnerGUI(this);

		p.setMediaManager(amm);
		String directory = getDirectory(dataset);
		amm.setHomeDirectory(directory);

		if( config != null )
			p.loadConfigurationFile(config);

		p.loadInputData(new File(dataset).getName());

		// wait for it to process one image so that the size isn't all screwed up
		while( !p.getHasProcessedImage() ) {
			Thread.yield();
		}

		showStatus("Running");
		getContentPane().add(comp, BorderLayout.CENTER);
	}

	/**
	 * Given the path to the file it extracts the directory containing the file
	 */
	public static String getDirectory( String fileName ) {
		int tailLength = 0;
		while( tailLength < fileName.length() ) {
			if( fileName.charAt(fileName.length()-tailLength-1) == '/') {
				break;
			} else {
				tailLength++;
			}
		}
		return fileName.substring(0,fileName.length()-tailLength);
	}

	@Override
	public String getAppletInfo() {
		return "Shows gradient of an image";
	}
}
