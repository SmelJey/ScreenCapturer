import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Popup
import java.awt.Desktop
import java.io.File

class SuccessWindow(file : File) : Popup() {
    val root = object : VBox() {
        init {
            alignment = Pos.CENTER
            children.add(Label("ScreenCapturer"))
            children.add(ImageView(Main.ico))
            children.addAll(Label("Your image successfully saved!"),
                    Label("Path: ${file.absolutePath}"))
            val openBtn = JFXButton("Open in Explorer")
            openBtn.setOnAction {
                Desktop.getDesktop().open(file.parentFile)
            }
            children.add(openBtn)

        }
    }

    init {

        root.style = "-fx-background-color: grey;\n" +
                "    -fx-padding: 10;\n" +
                "    -fx-border-color: black; \n" +
                "    -fx-border-width: 2;\n" +
                "    -fx-font-size: 16;"
        this.isAutoHide = true
        this.content.add(root)
    }
}