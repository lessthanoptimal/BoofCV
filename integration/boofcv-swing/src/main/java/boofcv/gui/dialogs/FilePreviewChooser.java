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
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

import static boofcv.misc.BoofMiscOps.timeStr;

// TODO preview text documents
// TODO show size of input images and other meta data

/**
 * Opens a dialog which lets the user select a single file but shows a preview of whatever file is currently selected
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public class FilePreviewChooser extends JPanel {
	private static final int PREVIEW_PIXELS = 300;

	@Setter @Getter protected Listener listener;

	// GUI components
	@Getter FileBrowser browser; // file browser
	ImagePanel preview = new ImagePanel(); // shows preview of selected image
	JTextArea metadataText = new JTextArea();

	@Nullable File selected;

	// Indicates if it's opening files (true) or saving files (false)
	@Getter boolean openFile;

	// Clicks this when a file has been selected
	JButton bSelect;

	// handling the image preview
	private final Object lockPreview = new Object();
	@Nullable PreviewThread previewThread;
	@Nullable String pendingPreview;

	public FilePreviewChooser( boolean openFile ) {
		setLayout(new BorderLayout());
		this.openFile = openFile;

		String buttonText = openFile ? "Select" : "Save";

		bSelect = BoofSwingUtil.button(buttonText, e -> handlePressedSelect());
		bSelect.setEnabled(false);
		bSelect.setDefaultCapable(true);
		JButton bCancel = BoofSwingUtil.button("Cancel", e -> handlePressedCancel());
		JPanel bottomPanel = JSpringPanel.createLockedSides(bCancel, bSelect, 35);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// make so the user can copy but not edit the meta data text
		metadataText.setEditable(false);
		metadataText.setWrapStyleWord(true);
		metadataText.setLineWrap(true);

		// it will draw the image centered in the split pane
		preview.setCentering(true);
		// Need to specify a preferred size or the split pane does silly stuff
		preview.setPreferredSize(new Dimension(PREVIEW_PIXELS + 30, PREVIEW_PIXELS + 30));

		JSplitPane splitPreview = new JSplitPane(JSplitPane.VERTICAL_SPLIT, preview, metadataText);
		splitPreview.setDividerLocation(0.9);
//        splitPreview.setResizeWeight(0.0);
		splitPreview.setContinuousLayout(true);

		browser = new FileBrowser(new File("."), null, new BrowserListener());
		browser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(BorderLayout.CENTER, browser);
		leftPanel.add(BorderLayout.SOUTH, bottomPanel);

		JSplitPane splitBrowserPreview = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, splitPreview);
		splitBrowserPreview.setDividerLocation(300);
		splitBrowserPreview.setResizeWeight(0.0);
		splitBrowserPreview.setContinuousLayout(true);

		add(splitBrowserPreview, BorderLayout.CENTER);

		setPreferredSize(new Dimension(600, 400));

		// If the selected file is set before the chooser is visible then this is needed to set the selected button
		bSelect.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded( AncestorEvent event ) {
				setSelectEnabled(bSelect.isEnabled());
			}

			@Override public void ancestorRemoved( AncestorEvent event ) {}

			@Override public void ancestorMoved( AncestorEvent event ) {}
		});
	}

	/**
	 * Select button pressed
	 */
	void handlePressedSelect() {
		if (listener == null)
			System.err.println("You didn't set a listener!");
		else
			listener.selectedFile(Objects.requireNonNull(selected));
	}

	/**
	 * Cancel button pressed
	 */
	protected void handlePressedCancel() {
		if (listener == null) {
			System.err.println("You didn't set a listener!");
		} else {
			listener.userCanceled();
		}
	}

	public void setDirectory( File directory ) {
		BoofSwingUtil.checkGuiThread();
		if (directory.isFile())
			directory = directory.getParentFile();
		browser.setDirectory(directory);
	}

	private class BrowserListener implements FileBrowser.Listener {
		@Override
		public void handleSelectedFile( @Nullable File file ) {
			// Do nothing if this file is already selected
			if (selected == file)
				return;
			if (selected != null && file != null && file.getAbsolutePath().equals(selected.getAbsolutePath()))
				return;

			// Update the preview
			if (file == null) {
				selected = null;
				showPreview(null);
				setSelectEnabled(false);
			} else {
				if (file.isFile()) {
					selected = file;
					showPreview(file.getPath());
					setSelectEnabled(true);
				} else {
					if (selected != null) {
						selected = null;
						showPreview(null);
						setSelectEnabled(false);
					}
				}
			}
		}

		@Override
		public void handleDoubleClickedFile( File file ) {
			selected = file;
			handlePressedSelect();
		}
	}

	private void setSelectEnabled( boolean enabled ) {
		if (enabled) {
			bSelect.setEnabled(true);
			JRootPane rootPane = SwingUtilities.getRootPane(FilePreviewChooser.this);
			if (rootPane != null)
				rootPane.setDefaultButton(bSelect);
			// This only fails when the panel isn't visible. That's handled by setting the enabled flag
			// when the panel first becomes visible
		} else {
			bSelect.setEnabled(false);
		}
	}

	/**
	 * Start a new preview thread if one isn't already running. Carefully manipulate variables due to threading
	 */
	void showPreview( @Nullable String path ) {
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

				File file = new File(path);
				if (!file.exists() || file.isDirectory())
					continue;

				BufferedImage full = null;

				if (UtilImageIO.isImage(file)) {
					full = UtilImageIO.loadImage(path);
				}
				if (full == null) {
					// That failed, now assume that the file is a video sequence
					full = loadVideoPreview(path);
				}

				String date = timeStr(file.lastModified());

				if (full == null) {
					preview.setImageRepaint(null);
					metadataText.setText("File Size: " + file.length() + " B\nModified: " + date);
				} else {
					// shrink the image down to preview size
					int w = full.getWidth(), h = full.getHeight();
					double scale;
					if (w > h) {
						scale = PREVIEW_PIXELS/(double)w;
						h = h*PREVIEW_PIXELS/w;
						w = PREVIEW_PIXELS;
					} else {
						scale = PREVIEW_PIXELS/(double)h;
						w = w*PREVIEW_PIXELS/h;
						h = PREVIEW_PIXELS;
					}
					BufferedImage small = new BufferedImage(w, h, full.getType());
					Graphics2D g2 = small.createGraphics();
					g2.setTransform(new AffineTransform(scale, 0, 0, scale, 0, 0));
					g2.drawImage(full, 0, 0, null);

					// don't need to run in the UI thread
					preview.setImageRepaint(small);
					metadataText.setText(String.format("Shape: %d x %d\nModified: %s",
							full.getWidth(), full.getHeight(), date));
				}
			}
		}
	}

	private @Nullable BufferedImage loadVideoPreview( String path ) {
		BufferedImage full = null;
		try {
			SimpleImageSequence<InterleavedU8> sequence =
					Objects.requireNonNull(DefaultMediaManager.INSTANCE.openVideo(path, ImageType.IL_U8));

			if (sequence.hasNext()) {
				InterleavedU8 frame = sequence.next();
				full = ConvertBufferedImage.convertTo(frame, null, true);
			}
		} catch (RuntimeException ignore) {
		}
		return full;
	}

	/**
	 * Lets the listener know what the user has chosen to do.
	 */
	public interface Listener {
		void selectedFile( File file );

		void userCanceled();
	}

	@SuppressWarnings("NullAway.Init")
	public static class DefaultListener implements Listener {

		JDialog dialog;
		public File selectedFile;
		public boolean canceled = false;

		public DefaultListener( JDialog dialog ) {
			this.dialog = dialog;
		}

		@Override
		public void selectedFile( File file ) {
			this.selectedFile = file;
			this.dialog.setVisible(false);
		}

		@Override
		public void userCanceled() {
			this.canceled = true;
			this.dialog.setVisible(false);
		}
	}

	public @Nullable File showDialog( @Nullable Component parent ) {
		String title = openFile ? "Open File" : "Save File";

		JDialog dialog = new JDialog(null, title, Dialog.ModalityType.APPLICATION_MODAL);
		DefaultListener listener = new DefaultListener(dialog);
		this.listener = listener;

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				handlePressedCancel();
			}
		});
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		// should block at this point
		dialog.dispose();

		if (listener.canceled)
			return null;
		return listener.selectedFile;
	}

	public static void main( String[] args ) {
		var chooser = new FilePreviewChooser(true);
		File selected = chooser.showDialog(null);

		if (selected == null)
			System.out.println("Canceled");
		else {
			System.out.println(selected.getPath());
		}
	}
}
