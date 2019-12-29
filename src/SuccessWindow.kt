import Main
import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.awt.Desktop
import java.io.File

class SuccessWindow(file : File) : Stage() {
    init {
        this.title = "Image saved!"
        this.icons.add(Main.ico)
        val root = object : VBox() {
            init {
                alignment = Pos.CENTER
                children.add(Label("ScreenCapturer"))
                children.add(ImageView(Main.ico))
                children.addAll(Label("Your image successfully saved!"),
                        Label("Path: ${file.absolutePath}"))
                val openBtn = JFXButton("Open in Explorer")
                openBtn.setOnAction { _ ->
                    Desktop.getDesktop().open(file.parentFile)
                }
                children.add(openBtn)
            }

        }
        val scene = Scene(root, 350.0, 200.0)
        this.scene = scene
    }
}