/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.io.image.UtilImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Renders what's currently visible in the component and saves to disk.  Opens a dialog to let the user know what's going
 * on and provides the open to silently save in the future.
 *
 * @author Peter Abeles
 */
public class SaveImageOnClick extends MouseAdapter {

	private static int saveCounter = 0;
	private static boolean hideSaveDialog = false;

	Component parent;

	public SaveImageOnClick(Component parent) {
		this.parent = parent;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		boolean clicked = SwingUtilities.isMiddleMouseButton(e);

		// This is for Mac OS X. Checks to see if control-command are held down with the mouse press
		clicked |= e.isControlDown() && ((e.getModifiersEx() & 256) != 0);

		if( clicked ) {
			String fileName = String.format("saved_image%03d.png",saveCounter++);
			System.out.println("Image saved to "+fileName);
			BufferedImage output = getBufferedImage();
			UtilImageIO.saveImage(output, fileName);
			if( hideSaveDialog )
				return;

			Object[] options = {"Hide in Future","OK"};
			int n = JOptionPane.showOptionDialog(parent,
					"Saved image to "+fileName,
					"Middle Mouse Click Image Saving",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[1]);

			if( n == 0 )
				hideSaveDialog = true;
		}
	}

	protected BufferedImage getBufferedImage() {
		BufferedImage output = new BufferedImage(parent.getWidth(),parent.getHeight(), BufferedImage.TYPE_INT_BGR);
		parent.paint(output.createGraphics());
		return output;
	}
}
