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

import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;


/**
 * Applet which does not require any images for input.
 *
 * @author Peter Abeles
 */
public class NoInputApplet extends JApplet {

	@Override
	public void init() {
		showStatus("Loading Parameters");
		final String panelPath = getParameter("panelPath");

		showStatus("Creating GUI component");
		Class inputType = ImageFloat32.class;

		final JComponent comp = FactoryVisualPanel.create(panelPath,inputType);

		if( comp == null ) {
			showStatus("Failed to create GUI component");
			return;
		}

		showStatus("Running");
		getContentPane().add(comp, BorderLayout.CENTER);
	}

	@Override
	public String getAppletInfo() {
		return "Shows gradient of an image";
	}
}
