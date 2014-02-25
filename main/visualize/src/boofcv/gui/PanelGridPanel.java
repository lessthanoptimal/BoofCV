/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.gui;

import boofcv.gui.image.ImagePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class PanelGridPanel extends JPanel {
	public PanelGridPanel( int numColumns , JPanel ...panels ) {

		int numRows = panels.length/numColumns;

		GridLayout experimentLayout = new GridLayout(numRows,numColumns);

		setLayout(experimentLayout);

		for( JPanel p : panels )
			add(p);
	}

	public PanelGridPanel( int numRows , int numColumns ) {
		setLayout( new GridLayout(numRows,numColumns));
	}

	public void add( BufferedImage image ) {
		add(new ImagePanel(image));
	}
}
