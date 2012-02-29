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

import boofcv.gui.ProcessInput;
import boofcv.io.ConfigureFileInterface;
import boofcv.io.MediaManagerInput;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static boofcv.applet.VideoInputApplet.getDirectory;


/**
 * Applet for visualizations which take in as input a single image.
 *
 * @author Peter Abeles
 */
public class ImageInputApplet extends JApplet {

	@Override
	public void init() {
		showStatus("Loading Parameters");
		String dataset = getParameter("dataset");

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
		} else if( !(comp instanceof ProcessInput) ) {
			showStatus("The loaded component is not of ProcessInput type");
			return;
		}

		ProcessInput p = (ProcessInput)comp;

		if( p instanceof MediaManagerInput) {
			AppletMediaManager amm = new AppletMediaManager(getCodeBase());

			((MediaManagerInput)p).setMediaManager(amm);

			// todo total hack
			final String configPath = getParameter("config");
			if( configPath != null ) {
				String directory = getDirectory(configPath);
				amm.setHomeDirectory(directory);
			}
		}
		
		if( p instanceof ConfigureFileInterface)
			handleConfig(p);
		else
			System.out.println("NOT LOADING CONFIG");

		showStatus("Loading Image");
		AppletImageListManager manager = parseImageInput(dataset);

		p.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !p.getHasProcessedImage() ) {
			Thread.yield();
		}

		showStatus("Running");
		getContentPane().add(comp, BorderLayout.CENTER);
	}

	/**
	 * If one is specified load the configuration from a file
	 */
	protected boolean handleConfig(ProcessInput p) {

		final String configPath = getParameter("config");
		if( configPath == null ) {
			System.err.println("Class takes a config file but no 'config' param was found");
			return true;
		}

		showStatus("Loading Config File");
		((ConfigureFileInterface)p).configure(configPath);

		return false;
	}

	public AppletImageListManager parseImageInput( String fileName )
	{
		String directory = getDirectory(fileName);

		AppletImageListManager ret = new AppletImageListManager(getCodeBase());
		try {
			InputStreamReader isr = new InputStreamReader(new URL(getCodeBase(),fileName).openStream());
			BufferedReader reader = new BufferedReader(isr);

			String line;
			while( (line = reader.readLine()) != null ) {

				String[]z = line.split(":");
				String[] names = new String[z.length-1];
				for( int i = 1; i < z.length; i++ ) {
					names[i-1] = directory+z[i];
				}

				ret.add(z[0],names);
			}

			return ret;

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getAppletInfo() {
		return "Shows gradient of an image";
	}
}
