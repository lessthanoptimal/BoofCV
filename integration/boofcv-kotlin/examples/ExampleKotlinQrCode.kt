import boofcv.factory.fiducial.FactoryFiducial
import boofcv.gui.image.ImagePanel
import boofcv.gui.image.ShowImages
import boofcv.io.webcamcapture.UtilWebcamCapture
import boofcv.kotlin.asGrayU8
import boofcv.kotlin.draw
import boofcv.struct.image.GrayU8
import java.awt.BasicStroke
import java.awt.Color

/**
 * Simple example showing how to open a webcam and scan for a QR Code. When possible extensions available through
 * BoofCV are used.
 */
fun main() {
    // Open a webcam and create the detector
    val webcam = UtilWebcamCapture.openDefault(800, 600)
    val detector = FactoryFiducial.qrcode(null, GrayU8::class.java)

    // Create the panel used to display the image and
    val gui = ImagePanel()
    gui.preferredSize =  webcam.viewSize

    ShowImages.showWindow(gui, "QR Code", true)

    while (true) {
        // Load the image from the webcam
        val image = webcam.image ?: break

        // Convert to gray scale and detect QR codes inside
        detector.process(image.asGrayU8())

        // Draw where boxes around the QR Codes
        val g2 = image.createGraphics()
        g2.color = Color.RED
        g2.stroke = BasicStroke(4.0f)
        for (qr in detector.detections) {
            g2.draw(qr.bounds, true, 1.0)
        }

        // Update the display
        gui.setImageRepaint(image)
    }
}