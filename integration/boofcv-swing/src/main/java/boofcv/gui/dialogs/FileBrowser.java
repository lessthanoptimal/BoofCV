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

package boofcv.gui.dialogs;

import boofcv.gui.BoofSwingUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;

/**
 * Dialog which lets the user selected a known file type and navigate the file system
 *
 * @author Peter Abeles
 */
public class FileBrowser extends JSpringPanel {
	// field containing the file name
	JTextArea textFileName;
	// Path from root to current directory
	JComboBox<String> comboPath;
	// list of child files and directories
	JList<File> fileList;
	DefaultListModel<File> listModel = new DefaultListModel<>();

	// directory path
	List<File> directories = new ArrayList<>();
	SortDirectoryFirst sorter = new SortDirectoryFirst();

	ActionListener directoryListener;

	Listener listener;

	// Used to specify which files should be displayed
	java.util.List<javax.swing.filechooser.FileFilter> filters = new ArrayList<>();

	/**
	 * @param providedFileName If not null then this "provided" will be updated but not added to the browser's GUI
	 */
	public FileBrowser( File directory, @Nullable JTextArea providedFileName, Listener listener ) {
		this.listener = listener;

		directory = directory.getAbsoluteFile();
		if (directory.isDirectory() && directory.getName().equals(".")) {
			directory = directory.getParentFile();
		}
		if (providedFileName == null) {
			textFileName = new JTextArea();
			DefaultCaret caret = (DefaultCaret)textFileName.getCaret();
			caret.setUpdatePolicy(ALWAYS_UPDATE);
			textFileName.setRows(1);
			textFileName.setEditable(false);
			textFileName.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		} else {
			textFileName = providedFileName;
		}

		comboPath = new JComboBox<>();
		fileList = new JList<>(listModel);
		fileList.setCellRenderer(new FileListCellRenderer());
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fileList.setLayoutOrientation(JList.VERTICAL);
		fileList.addListSelectionListener(new FileSelectionListener(this));
		fileList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked( MouseEvent evt ) {
				if (evt.getClickCount() == 2) {
					File selected = listModel.get(fileList.getSelectedIndex());
					if (selected.isDirectory()) {
						setDirectory(selected);
					} else {
						setSelectedName(selected);
						listener.handleDoubleClickedFile(selected);
					}
				}
			}
		});

		JScrollPane scrollList = new JScrollPane(fileList);
		scrollList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


		JPanel navigationPanel = createNavigationPanel();

		JPanel directoryRow = new JPanel();
		directoryRow.setLayout(new BoxLayout(directoryRow, BoxLayout.X_AXIS));
		directoryRow.add(new JLabel("Location"));
		directoryRow.add(Box.createHorizontalStrut(5));
		directoryRow.add(comboPath);

		if (providedFileName == null) {
			// Put it into a horizontal panel and have text letting the user know this is just the file name
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JScrollPane nameScrollPane = new JScrollPane(textFileName);
			nameScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			nameScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			panel.add(new JLabel("Name"));
			panel.add(Box.createHorizontalStrut(5));
			panel.add(nameScrollPane);
			constrainWestNorthEast(panel, null, 5, 5);
			constrainWestNorthEast(directoryRow, panel, 5, 5);
		} else {
			constrainWestNorthEast(directoryRow, null, 5, 5);
		}
		constrainWestNorthEast(navigationPanel, directoryRow, 5, 5);
		constrainWestNorthEast(scrollList, navigationPanel, 5, 5);
		layout.putConstraint(SpringLayout.SOUTH, scrollList, -5, SpringLayout.SOUTH, this);

		setDirectory(directory);
		directoryListener = e -> {
			if (comboPath.getSelectedIndex() >= 0) {
				File f = directories.get(comboPath.getSelectedIndex());
				setDirectory(f);
			}
		};
		comboPath.addActionListener(directoryListener);
	}

	public void addFileFilter( javax.swing.filechooser.FileFilter filter ) {
		this.filters.add(filter);
	}

	/**
	 * Sets the selected file to be the specified one
	 */
	public void setSelectedFile( File file ) {
		if (file.isDirectory()) {
			setDirectory(file);
		} else {
			// File provided, set the directory to the parent
			setDirectory(file.getParentFile());
			// Then search for the in the list of files and select it
			for (int i = 0; i < listModel.size(); i++) {
				File f = listModel.get(i);
				if (f.getName().equals(file.getName())) {
					fileList.setSelectedIndex(i);
					fileList.ensureIndexIsVisible(i);
					setSelectedName(file);
					return;
				}
			}
		}
	}

	/**
	 * Specifies file selection mode for the browser.
	 *
	 * @see ListSelectionModel#SINGLE_SELECTION
	 * @see ListSelectionModel#SINGLE_INTERVAL_SELECTION
	 * @see ListSelectionModel#MULTIPLE_INTERVAL_SELECTION
	 */
	public void setSelectionMode( int mode ) {
		fileList.setSelectionMode(mode);
	}

	private JPanel createNavigationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		JButton bHome = BoofSwingUtil.createButtonIconGUI("Home24.gif", 26, 26);
		bHome.setToolTipText("User Home");
		bHome.addActionListener(e -> setDirectory(BoofSwingUtil.directoryUserHome()));

		JButton bSystem = BoofSwingUtil.createButtonIconGUI("Host24.gif", 26, 26);
		bSystem.setToolTipText("System");
		bSystem.addActionListener(e -> setDirectory(null));

		JButton bPrevious = BoofSwingUtil.createButtonIconGUI("AlignCenter24.gif", 26, 26);
		bPrevious.setToolTipText("Previous");
//		bPrevious.addActionListener(e->setDirectory(defaultDirectory)); // TODO implement

		JButton bUp = BoofSwingUtil.createButtonIconGUI("Up24.gif", 26, 26);
		bUp.setToolTipText("Up Directory");
		bUp.addActionListener(e -> {
			if (directories.isEmpty())
				return;
			File d = directories.get(directories.size() - 1);
			setDirectory(d.getParentFile());
		});

		panel.add(Box.createHorizontalGlue());
		panel.add(bHome);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(bSystem);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(bPrevious);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(bUp);
		panel.add(Box.createHorizontalGlue());

		return panel;
	}

	/**
	 * The selected file/directory has changed. Just update the text
	 */
	private void setSelectedName( File file ) {
		textFileName.setText(file.getName());
	}

	/**
	 * The parent directory has changed. Update the file list. If file is null then it's assumed to be
	 * the list of all devices. On unix there's only one which is / but on windows there can be multiple
	 */
	public void setDirectory( @Nullable File file ) {

		List<File> roots = null;
		if (file == null) {
			// Create a list of roots with something in them. Windows list to list non-existant devices
			roots = new ArrayList<>(Arrays.asList(File.listRoots()));
			for (int i = roots.size() - 1; i >= 0; i--) {
				File[] files = roots.get(i).listFiles();
				if (files == null || files.length == 0) {
					roots.remove(i);
				}
			}

			if (roots.size() == 1) {
				file = roots.get(0);
				roots = null;
			}
		}

		if (roots == null) {
			setDirectoryNormal(Objects.requireNonNull(file));
		} else {
			// Present the user with a list of file system roots
			textFileName.setText("");

			listModel.clear();
			for (File f : roots) {
				listModel.addElement(f);
			}

			comboPath.removeActionListener(directoryListener);
			comboPath.removeAllItems();
			comboPath.addActionListener(directoryListener);

			listener.handleSelectedFile(null);
		}
	}

	private void setDirectoryNormal( File file ) {
		if (file.isFile())
			textFileName.setText(file.getName());
		else
			textFileName.setText("");

		listModel.clear();
		List<File> files = filterChildren(file);
		files.sort(sorter);
		for (File f : files) {
			if (f.isHidden())
				continue;

			listModel.addElement(f);
		}

		file = file.getAbsoluteFile();
		if (file.isFile())
			file = file.getParentFile();
		files = new ArrayList<>();
		while (file != null) {
			files.add(file);
			file = file.getParentFile();
		}

		comboPath.removeActionListener(directoryListener);
		comboPath.removeAllItems();
		directories.clear();
		for (int i = files.size() - 1; i >= 0; i--) {
			File f = files.get(i);
			if (f.getParentFile() == null) {
				try {
					comboPath.addItem(f.getCanonicalPath());
				} catch (IOException e) {
					comboPath.addItem("/");
				}
			} else
				comboPath.addItem(files.get(i).getName());
			directories.add(f);
		}
		comboPath.setSelectedIndex(files.size() - 1);
		comboPath.addActionListener(directoryListener);

		listener.handleSelectedFile(null);
	}

	private List<File> filterChildren( File file ) {
		File[] fileArray = file.listFiles();
		List<File> files = new ArrayList<>();
		if (fileArray != null) {
			for (int i = 0; i < fileArray.length; i++) {
				File f = fileArray[i];
				boolean filtered;
				if (f.isDirectory()) {
					filtered = false;
				} else {
					// If there is at least one filter, only accept the file if at least one filter accepts it
					filtered = filters.size() > 0;
					for (int j = 0; j < filters.size(); j++) {
						if (filters.get(j).accept(f)) {
							filtered = false;
							break;
						}
					}
				}

				if (!filtered)
					files.add(f);
			}
		}
		return files;
	}

	public List<File> getSelectedFiles() {
		List<File> selected = fileList.getSelectedValuesList();

		return new ArrayList<>(selected);
	}

	/**
	 * Needed to add System icons for each type of file
	 */
	private static class FileListCellRenderer extends DefaultListCellRenderer {

		private FileSystemView fileSystemView;
		private JLabel label;
		private Color textSelectionColor;
		private Color backgroundSelectionColor;
		private Color textNonSelectionColor;
		private Color backgroundNonSelectionColor;

		FileListCellRenderer() {
			label = new JLabel();
			label.setBorder(new EmptyBorder(2, 4, 2, 4));
			label.setOpaque(true);
			fileSystemView = FileSystemView.getFileSystemView();

			UIDefaults defaults = UIManager.getDefaults();
			backgroundSelectionColor = defaults.getColor("List.selectionBackground");
			textSelectionColor = defaults.getColor("List.selectionForeground");
			backgroundNonSelectionColor = defaults.getColor("List.background");
			textNonSelectionColor = defaults.getColor("List.foreground");
		}

		@Override
		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean selected,
				boolean expanded ) {


			File file = (File)value;
			String name = fileSystemView.getSystemDisplayName(file);
			if (name.length() == 0)
				name = file.getAbsolutePath();
			label.setIcon(fileSystemView.getSystemIcon(file));
			label.setText(name);
			label.setToolTipText(file.getPath());

			if (selected) {
				label.setBackground(backgroundSelectionColor);
				label.setForeground(textSelectionColor);
			} else {
				label.setBackground(backgroundNonSelectionColor);
				label.setForeground(textNonSelectionColor);
			}

			return label;
		}
	}

	/**
	 * Handles changes in which file is selected
	 */
	private class FileSelectionListener implements ListSelectionListener {

		FileBrowser browser;

		public FileSelectionListener( FileBrowser browser ) {
			this.browser = browser;
		}

		@Override
		public void valueChanged( ListSelectionEvent e ) {
			if (e.getValueIsAdjusting())
				return;

			JList<File> fileList = (JList)e.getSource();
			DefaultListModel<File> listModel = (DefaultListModel<File>)fileList.getModel();

			int index = fileList.getSelectedIndex();
			if (index >= 0) {
				File f = listModel.getElementAt(index);
				browser.setSelectedName(f);
				listener.handleSelectedFile(f);
			} else {
				listener.handleSelectedFile(null);
			}
		}
	}

	private static class SortDirectoryFirst implements Comparator<File> {

		@Override
		public int compare( File a, File b ) {
			if (a.isDirectory()) {
				if (b.isDirectory()) {
					return a.getName().compareToIgnoreCase(b.getName());
				} else {
					return -1;
				}
			} else if (b.isDirectory()) {
				return 1;
			} else {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		}
	}

	public interface Listener {
		void handleSelectedFile( @Nullable File file );

		void handleDoubleClickedFile( File file );
	}
}
