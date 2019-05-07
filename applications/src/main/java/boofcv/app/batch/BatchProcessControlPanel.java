/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.app.batch;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Control panel for processing a set of images and saving the output in a single file
 *
 * @author Peter Abeles
 */
public abstract class BatchProcessControlPanel extends BatchControlPanel {

	protected JTextField textOutputFile = new JTextField();

	public void addStandardControls(Preferences prefs) {
		boolean recursive = Boolean.parseBoolean(prefs.get(KEY_RECURSIVE,"false"));

		textInputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
		textInputDirectory.setMaximumSize(textInputDirectory.getPreferredSize());
		textOutputFile.setPreferredSize(new Dimension(textWidth,textHeight));
		textOutputFile.setMaximumSize(textOutputFile.getPreferredSize());
		textRegex.setPreferredSize(new Dimension(textWidth+40,textHeight));
		textRegex.setMaximumSize(textRegex.getPreferredSize());
		textRegex.setText("([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)");
		checkRecursive.setSelected(recursive);

		textInputDirectory.setText(prefs.get(KEY_INPUT,textInputDirectory.getText()));
		textOutputFile.setText(prefs.get(KEY_OUTPUT,textOutputFile.getText()));

		bAction.addActionListener(a-> handleStart());

		addLabeled(createTextSelect(textInputDirectory,"Input Directory",true),"Input");
		addLabeled(createTextSelect(textOutputFile,"Output File",true),"Output");
		addAlignLeft(checkRecursive);
		addLabeled(textRegex,"Regex");
		addVerticalGlue();
		addAlignCenter(bAction);
	}
}
