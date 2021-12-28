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

package boofcv.gui.image;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides a list of input images which can be selected by the user. When a new image
 * is selected the listener will be notified. The notification will spawn a new thread
 * automatically.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SelectInputImageToolBar extends JPanel implements ActionListener {
	JComboBox<String> imageMenu;
	java.util.List<Object> cookies = new ArrayList<>();

	@Nullable Listener listener;

	public SelectInputImageToolBar( JComponent main ) {
		super(new BorderLayout());

		JToolBar toolbar = new JToolBar("Still draggable");
		toolbar.setFloatable(false);
		toolbar.setRollover(true);

		imageMenu = new JComboBox<>();
		imageMenu.addActionListener(this);

		toolbar.add(new JLabel("Image:"));
		toolbar.add(imageMenu);

		add(toolbar, BorderLayout.NORTH);
		add(main, BorderLayout.CENTER);
	}

	public void addImage( final String menuName, final Object cookie ) {
		cookies.add(cookie);

		SwingUtilities.invokeLater(() -> imageMenu.addItem(menuName));
	}

	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	@Override
	public void actionPerformed( @Nullable ActionEvent e ) {
		if (listener == null)
			return;

		int index = imageMenu.getSelectedIndex();
		if (index < 0)
			return;

		final Object cookie = cookies.get(imageMenu.getSelectedIndex());

		new Thread(() -> Objects.requireNonNull(listener).selectedImage(cookie)).start();
	}

	public void triggerEvent() {
		actionPerformed(null);
	}

	public interface Listener {
		void selectedImage( Object cookie );
	}
}
