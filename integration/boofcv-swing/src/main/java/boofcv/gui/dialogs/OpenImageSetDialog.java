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
import boofcv.gui.image.ImagePanel;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Opens a dialog which lets the user select multiple images as a set
 *
 * @author Peter Abeles
 */
public class OpenImageSetDialog extends JPanel {
	private static final int PREVIEW_LENGTH = 300;

	Listener listener;

	// GUI components
	FileBrowser browser; // file browser
	ImagePanel preview = new ImagePanel(); // shows preview of selected image
	SelectedList selected = new SelectedList(); // list of files selected to be in the set

	// Number of images it's required to select
	// <= 0 means one or more images. > 0 means that exact number
	int requiredSelect = 1;
	Mode modeSelect = Mode.MINIMUM;

	JDialog dialog;

	File defaultDirectory;

	// when pressed the image set is returned. Only enabled when selection criteria have been meet
	JButton bOK;

	// handling the image preview
	private final Object lockPreview = new Object();
	@Nullable PreviewThread previewThread;
	@Nullable String pendingPreview;

	public OpenImageSetDialog( JDialog dialog, Listener listener, File directory ) {
		setLayout(new BorderLayout());
		this.listener = listener;
		this.dialog = dialog;
		this.defaultDirectory = directory;

		browser = new FileBrowser(directory, null, new BrowserListener());
		browser.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		SelectPanel selectPanel = new SelectPanel();

		bOK = BoofSwingUtil.button("OK", e -> handleOK());
		bOK.setEnabled(false);
		JButton bCancel = BoofSwingUtil.button("Cancel", e -> handleCancel());
		JPanel bottomPanel = JSpringPanel.createLockedSides(bCancel, bOK, 35);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// it will draw the image centered in the split pane
		preview.setCentering(true);

		JSplitPane splitSelected = new JSplitPane(JSplitPane.VERTICAL_SPLIT, preview, selected);
		splitSelected.setDividerLocation(200);
		splitSelected.setResizeWeight(0.0);
		splitSelected.setContinuousLayout(true);

		JSplitPane splitContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, selectPanel, splitSelected);
		splitContent.setDividerLocation(300);
		splitContent.setResizeWeight(0.0);
		splitContent.setContinuousLayout(true);

		add(splitContent, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(600, 400));
	}

	/**
	 * Add all selected files
	 */
	void handleAdd() {
		BoofSwingUtil.checkGuiThread();
		java.util.List<File> paths = browser.getSelectedFiles();

		for (int i = 0; i < paths.size(); i++) {
			File f = paths.get(i);

			// if it's a directory add all the files in the directory
			if (f.isDirectory()) {
				java.util.List<String> files = UtilIO.listAll(f.getPath());
				Collections.sort(files);
				for (int j = 0; j < files.size(); j++) {
					File p = new File(files.get(j));
					// Note: Can't use mimetype to determine if it's an image or not since it isn't 100% reliable
					if (p.isFile()) {
						selected.addPath(p);
					}
				}
			} else {
				selected.addPath(f);
			}
		}
	}

	/**
	 * OK button pressed
	 */
	void handleOK() {
		String[] selected = this.selected.paths.toArray(new String[0]);
		listener.selectedImages(selected);
	}

	/**
	 * Cancel button pressed
	 */
	void handleCancel() {
		listener.userCanceled();
	}

	public void handleSelectedUpdate( int count ) {
		switch (modeSelect) {
			case EXACTLY: {
				bOK.setEnabled(count == requiredSelect);
			}
			break;

			case MINIMUM: {
				bOK.setEnabled(count >= requiredSelect);
			}
			break;
		}
	}

	private class BrowserListener implements FileBrowser.Listener {
		@Override
		public void handleSelectedFile( @Nullable File file ) {
			if (file == null) {
				showPreview("");
			} else {
				if (file.isFile()) {
//                    if( !BoofSwingUtil.isImage(file))
//                        return;
					showPreview(file.getPath());
				} else
					showPreview("");
			}
		}

		@Override
		public void handleDoubleClickedFile( File file ) {
//            if( BoofSwingUtil.isImage(file) )
			selected.addPath(file);
		}
	}

	private class SelectPanel extends JSpringPanel {
		public SelectPanel() {
			JButton bAdd = BoofSwingUtil.button("Add", e -> handleAdd());

			constrainWestNorthEast(browser, null, 0, 0);
			add(bAdd);
			layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, bAdd, 0, SpringLayout.HORIZONTAL_CENTER, this);
			layout.putConstraint(SpringLayout.SOUTH, browser, 0, SpringLayout.NORTH, bAdd);
			layout.putConstraint(SpringLayout.SOUTH, bAdd, -5, SpringLayout.SOUTH, this);
		}
	}

	/**
	 * Show list of selected items. Have a button that when clicked will remove the selected "selected" item from
	 * the list.
	 */
	private class SelectedList extends JSpringPanel implements ListSelectionListener {
		JList selectedList;
		JButton bRemove = new JButton("Remove");
		DefaultListModel listModel = new DefaultListModel();
		java.util.List<String> paths = new ArrayList<>();

		public SelectedList() {
			selectedList = new JList(listModel);
			selectedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			selectedList.setLayoutOrientation(JList.VERTICAL);
			selectedList.addListSelectionListener(this);

			JScrollPane scrollList = new JScrollPane(selectedList);
			scrollList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			bRemove.addActionListener(e -> {
				int index = selectedList.getSelectedIndex();
				if (index >= 0) {
					listModel.removeElementAt(index);
					paths.remove(index);
					handleSelectedUpdate(paths.size());
				}
			});

			constrainWestNorthEast(scrollList, null, 5, 0);
			add(bRemove);
			layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, bRemove, 0, SpringLayout.HORIZONTAL_CENTER, this);
			layout.putConstraint(SpringLayout.SOUTH, scrollList, -5, SpringLayout.NORTH, bRemove);
			layout.putConstraint(SpringLayout.SOUTH, bRemove, -5, SpringLayout.SOUTH, this);
		}

		public void addPath( File path ) {
			BoofSwingUtil.invokeNowOrLater(() -> {
				// ignore if it has reached its limit already
				if (modeSelect == Mode.EXACTLY && paths.size() == requiredSelect)
					return;

				// make sure it doesn't add the same image twice
				if (!paths.contains(path.getPath())) {
					listModel.addElement(path.getName());
					paths.add(path.getPath());
					handleSelectedUpdate(paths.size());
				}
			});
		}

		@Override
		public void valueChanged( ListSelectionEvent e ) {
			if (e.getValueIsAdjusting())
				return;

			int index = selectedList.getSelectedIndex();
			if (index >= 0) {
				showPreview(paths.get(index));
			} else {
				showPreview("");
			}
		}
	}

	/**
	 * Start a new preview thread if one isn't already running. Carefully manipulate variables due to threading
	 */
	void showPreview( String path ) {
		synchronized (lockPreview) {
			if (path == null) {
				pendingPreview = null;
			} else if (previewThread == null) {
				pendingPreview = path;
				previewThread = new PreviewThread();
				previewThread.start();
			} else {
				pendingPreview = path;
			}
		}
	}

	/**
	 * Loads a images, scales them down, and puts them in the image preview.
	 */
	private class PreviewThread extends Thread {
		public PreviewThread() {
			super("Image Preview");
		}

		@Override
		public void run() {
			while (true) {
				// see if there are any pending preview requests. If not exit and mark the thread as dead
				String path;
				synchronized (lockPreview) {
					if (pendingPreview == null) {
						previewThread = null;
						return;
					} else {
						path = pendingPreview;
						pendingPreview = null;
					}
				}

				BufferedImage full = UtilImageIO.loadImage(path);
//                System.out.println("Full is null " +(full==null));
				if (full == null) {
					preview.setImageRepaint(null);
				} else {
					// shrink the image down to preview size
					int w = full.getWidth(), h = full.getHeight();
					double scale;
					if (w > h) {
						scale = PREVIEW_LENGTH/(double)w;
						h = h*PREVIEW_LENGTH/w;
						w = PREVIEW_LENGTH;
					} else {
						scale = PREVIEW_LENGTH/(double)h;
						w = w*PREVIEW_LENGTH/h;
						h = PREVIEW_LENGTH;
					}
					BufferedImage small = new BufferedImage(w, h, full.getType());
					Graphics2D g2 = small.createGraphics();
					g2.setTransform(new AffineTransform(scale, 0, 0, scale, 0, 0));
					g2.drawImage(full, 0, 0, null);

					// don't need to run in the UI thread
					preview.setImageRepaint(small);
				}
			}
		}
	}

	/**
	 * Lets the listener know what the user has chosen to do.
	 */
	public interface Listener {
		void selectedImages( String... images );

		void userCanceled();
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class DefaultListener implements Listener {

		JDialog dialog;
		public String[] images;
		public boolean canceled = false;

		public DefaultListener( JDialog dialog ) {
			this.dialog = dialog;
		}

		@Override
		public void selectedImages( String... images ) {
			this.images = images;
			this.dialog.setVisible(false);
		}

		@Override
		public void userCanceled() {
			this.canceled = true;
			this.dialog.setVisible(false);
		}
	}

	public enum Mode {
		MINIMUM,
		EXACTLY
	}

	public static @Nullable String[] showDialog( File directory, Mode mode, int numberOfImages,
												 @Nullable Window owner ) {
		String title = switch (mode) {
			case EXACTLY -> "Select exactly " + numberOfImages + " images";
			case MINIMUM -> "Select at least " + numberOfImages + " images";
		};

		JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
		DefaultListener listener = new DefaultListener(dialog);
		OpenImageSetDialog panel = new OpenImageSetDialog(dialog, listener, directory);
		panel.modeSelect = Mode.EXACTLY;
		panel.requiredSelect = numberOfImages;

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				panel.handleCancel();
			}
		});
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		// should block at this point
		dialog.dispose();

		if (listener.canceled)
			return null;
		return listener.images;
	}

	public static void main( String[] args ) {
		String selected[] = showDialog(new File(""), Mode.EXACTLY, 3, null);

		if (selected == null)
			System.out.println("Canceled");
		else {
			for (int i = 0; i < selected.length; i++) {
				System.out.println(selected[i]);
			}
		}
	}
}
