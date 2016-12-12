import boofcv.io.UtilIO;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Example of how to display an image using JavaFX
 *
 * @author Peter Abeles
 */
public class ExampleFxShowImage extends Application  {

	@Override
	public void start(Stage stage) throws Exception {
		Image image = new Image("file://"+UtilIO.pathExample("standard/lena512.jpg"));
		ImageView imageView = new ImageView();
		imageView.setImage(image);

		StackPane root = new StackPane();
		root.getChildren().add(imageView);
		Scene scene = new Scene(root);
		stage.setTitle("Show Image Example");
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
