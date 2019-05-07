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
 * Control panel for convert one file into another file
 *
 * @author Peter Abeles
 */
public abstract class BatchConvertControlPanel extends BatchControlPanel {

	protected JTextField textOutputDirectory = new JTextField();
	protected JCheckBox checkRename = new JCheckBox("Rename");

	public void addStandardControls(Preferences prefs) {
		textInputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
		textInputDirectory.setMaximumSize(textInputDirectory.getPreferredSize());
		textOutputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
		textOutputDirectory.setMaximumSize(textOutputDirectory.getPreferredSize());
		textRegex.setPreferredSize(new Dimension(textWidth+40,textHeight));
		textRegex.setMaximumSize(textRegex.getPreferredSize());
		textRegex.setText("([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)");
		checkRecursive.setSelected(false);

		textInputDirectory.setText(prefs.get(KEY_INPUT,""));
		textOutputDirectory.setText(prefs.get(KEY_OUTPUT,""));

		bAction.addActionListener(a-> handleStart());

		addLabeled(createTextSelect(textInputDirectory,"Input Directory",true),"Input");
		addLabeled(createTextSelect(textOutputDirectory,"Output Directory",true),"Output");
		addAlignLeft(checkRecursive);
		addAlignLeft(checkRename);
		addLabeled(textRegex,"Regex");
		addVerticalGlue();
		addAlignCenter(bAction);
	}
}
