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

import boofcv.gui.BoofSwingUtil;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Configuration widget for specifying the number of levels in a discrete pyramid
 *
 * @author Peter Abeles
 */
public class JPyramidLevels extends JPanel implements ActionListener {
	public final ConfigDiscreteLevels config;

	private boolean isFixedLevels;
	private int numberOfLevels = 4;
	private int minPixels = 40;

	private final JButton bType = new JButton("TEXTAAA"); // initial name so that a reasonable length is set
	// Numeric values for selected number of pyramid levels and pixel values
	private final JFormattedTextField fieldLevels = BoofSwingUtil.createTextField(1, 1, Integer.MAX_VALUE);
	private final JFormattedTextField fieldPixels = BoofSwingUtil.createTextField(1, 1, Integer.MAX_VALUE);

	protected final Listener listener;

	public JPyramidLevels( @Nullable ConfigDiscreteLevels config, Listener listener ) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		config = config == null ? ConfigDiscreteLevels.minSize(40) : config;
		this.config = config;
		this.listener = listener;

		bType.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		bType.setMaximumSize(bType.getPreferredSize());
		bType.setToolTipText("Toggles between fixed level pyramid and pixels in top of pyramid");
		fieldLevels.setToolTipText("Number of levels in the pyramid");
		fieldPixels.setToolTipText("Target width/height in the top of pyramid");
		fieldLevels.setPreferredSize(new Dimension(80, 22));
		fieldLevels.setMaximumSize(fieldLevels.getPreferredSize());
		fieldPixels.setPreferredSize(fieldLevels.getPreferredSize());
		fieldPixels.setMaximumSize(fieldPixels.getPreferredSize());

		isFixedLevels = config.isFixedLevels();
		if (isFixedLevels) {
			numberOfLevels = config.numLevelsRequested;
		} else {
			minPixels = Math.max(config.minWidth, config.minHeight);
		}
		fieldLevels.setValue(numberOfLevels);
		fieldPixels.setValue(minPixels);
		updateType(isFixedLevels);

		// Now that everything has been configured set up the listeners
		fieldLevels.addPropertyChangeListener(e -> {
			if (e.getPropertyName().equals("value"))
				JPyramidLevels.this.actionPerformed(fieldLevels);
		});
		fieldPixels.addPropertyChangeListener(e -> {
			if (e.getPropertyName().equals("value"))
				JPyramidLevels.this.actionPerformed(fieldPixels);
		});
		bType.addActionListener(this);
		fieldLevels.addActionListener(this);
		fieldPixels.addActionListener(this);
	}

	protected void updateType( boolean levels ) {
		this.isFixedLevels = levels;
		bType.setText(isFixedLevels ? "Levels" : "Pixels");
		removeAll();
		add(bType);
		if (levels) {
			add(fieldLevels);
		} else {
			add(fieldPixels);
		}
		validate();
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		actionPerformed(e.getSource());
	}

	public void actionPerformed( final Object source ) {
		if (bType == source) {
			updateType(!isFixedLevels);
		} else if (fieldLevels == source) {
			numberOfLevels = ((Number)fieldLevels.getValue()).intValue();
		} else if (fieldPixels == source) {
			minPixels = ((Number)fieldPixels.getValue()).intValue();
		}

		if (isFixedLevels) {
			config.numLevelsRequested = numberOfLevels;
			config.minWidth = config.minHeight = -1;
		} else {
			config.numLevelsRequested = -1;
			config.minWidth = config.minHeight = minPixels;
		}

		listener.changePyramidLevels();
	}

	public interface Listener {
		void changePyramidLevels();
	}
}
