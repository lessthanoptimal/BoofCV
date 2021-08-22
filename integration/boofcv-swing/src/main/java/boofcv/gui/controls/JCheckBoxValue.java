/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.controls;

import javax.swing.*;

/**
 * Combines a spinner with a double value to reduce clutter.
 *
 * @author Peter Abeles
 */
public class JCheckBoxValue {
	public final JCheckBox check;
	/** Value. Safe to read outside the UI thread */
	public boolean value;

	public JCheckBoxValue( JCheckBox check, boolean value ) {
		this.check = check;
		this.value = value;
	}

	public JCheckBoxValue tt( String tooltip ) {
		check.setToolTipText(tooltip);
		return this;
	}

	public void updateValue() {
		value = check.isSelected();
	}
}
