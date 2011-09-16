package boofcv.gui;

import boofcv.io.image.SelectInputImageToolBar;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


/**
 * @author Peter Abeles
 */
public class UtilVisualize {

	/**
	 * calls the process() function in GUI when the toolbar
	 * selects a new image
	 * @param toolbar
	 * @param gui GUI with process( BufferedImage ) function.
	 * @param pauseProcess Pause until an image is selected.  Add images first if true.
	 */
	public static void manageSelectInput(SelectInputImageToolBar toolbar ,
										 ProcessImage gui ,
										 URL url ,
										 boolean pauseProcess ) {
		Manager m = new Manager(toolbar,gui,url);
		if( pauseProcess ) {
			// make sure it broadcasts the currently active item
			toolbar.actionPerformed(null);
			// now wait until an event comes through
			m.waitUntilProcessed();
		}
	}

	public static class Manager implements SelectInputImageToolBar.Listener
	{
		ProcessImage app;
		volatile boolean processedOnce = false;
		Object prevCookie;
		URL url;
		

		public Manager(SelectInputImageToolBar toolbar , ProcessImage app, URL url) {
			this.app = app;
			this.url = url;
			toolbar.setListener(this);
		}

		@Override
		public void selectedImage(Object cookie) {
			// avoid broadcasting multiple events that are the same
			if( prevCookie == cookie ) {
				return;
			}
			prevCookie = cookie;

			BufferedImage input;

			try {
				if( url == null )
					input = ImageIO.read(new URL((String)cookie ) );
				else
					input = ImageIO.read(new URL(url, (String)cookie ) );
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			if( input == null ) {
				System.err.print("Image can't be loaded:" +cookie);
				return;
			}
			app.process(input);
			processedOnce = true;

		}

		public void waitUntilProcessed() {
			while( processedOnce == false ) {
				Thread.yield();
			}
		}
	}
}
