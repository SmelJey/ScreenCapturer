import com.jfoenix.controls.*
import javafx.application.Application
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.dispatcher.SwingDispatchService
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.util.logging.LogManager
import kotlin.system.exitProcess


class Main : Application() {
    var captureDelay : Long = 200
    var hideWindow = false

    var editorWindow : CapturerWindow? = null

    private fun initPrimaryStage(primaryStage: Stage){
        val screenBounds = Screen.getPrimary().bounds

        val root = object : BorderPane() {
            init {
                style = "-fx-background-color: transparent;"
            }
        }
        primaryStage.initStyle(StageStyle.TRANSPARENT)
        primaryStage.title = "Hello World"

        val scene = Scene(root, screenBounds.width, screenBounds.height)
        scene.fill = null
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun initNewEditorWindow(inputImg : Image){
        editorWindow?.close()
        if (hideWindow)
            mainWindow?.hide()


        editorWindow = CapturerWindow(inputImg)
    }

    private fun getCapture(){
        if (hideWindow)
            mainWindow?.hide()
        Thread.sleep(captureDelay)
        val screenRect = java.awt.Rectangle(Toolkit.getDefaultToolkit().screenSize)
        val capture: BufferedImage = Robot().createScreenCapture(screenRect)

        initNewEditorWindow(SwingFXUtils.toFXImage(capture, null))
    }

    private fun initMainWindow() {
        mainWindow?.title = "ScreenCapturer"

        val root = object : HBox() {

            init {
                val createScreenBtn = JFXButton("Capture!")
                createScreenBtn.setPrefSize(100.0, 60.0)
                createScreenBtn.setMinSize(70.0, 60.0)
                createScreenBtn.setOnAction {
                    getCapture()
                }

                val delayBox = object : VBox() {
                    init {
                        alignment = Pos.CENTER
                        val delaySlider = JFXSlider()
                        delaySlider.setPrefSize(150.0, 1.0)
                        delaySlider.setMaxSize(150.0, 1.0)
                        delaySlider.isShowTickLabels = true
                        delaySlider.isShowTickMarks = true
                        delaySlider.majorTickUnit = 1.0
                        delaySlider.minorTickCount = 5
                        delaySlider.min = 0.2
                        delaySlider.max = 10.0
                        delaySlider.valueProperty().addListener { _, _, newValue ->
                            captureDelay = ((newValue as Double) * 1000.0).toLong()
                        }
                        delaySlider.value = 0.2

                        children.addAll(delaySlider, Label("Delay"))
                    }
                }

                val isHiding = JFXCheckBox("Hide window")
                isHiding.setPrefSize(100.0, 60.0)
                isHiding.setOnAction {
                    hideWindow = !hideWindow
                }

                children.addAll(createScreenBtn, delayBox, isHiding)
            }


        }

        val scene = Scene(root, 400.0, 60.0)
        mainWindow?.scene = scene
        mainWindow?.show()
    }

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        GlobalScreen.setEventDispatcher(SwingDispatchService())
        try {
			GlobalScreen.registerNativeHook()
		}
		catch (ex : NativeHookException) {
			System.err.println("There was a problem registering the native hook.")
			System.err.println(ex.message)
			exitProcess(1)
		}

        LogManager.getLogManager().reset()

        if (File("settings.json").isFile){
            settings = JSONParser().parse(FileReader("settings.json")) as JSONObject
            if (!settings.containsKey("defaultDirectory"))
                settings["defaultDirectory"] = "C:\\Users"
        } else {
            settings["defaultDirectory"] = "C:\\Users"
        }

        initPrimaryStage(primaryStage)
        mainWindow = Stage()
        mainWindow?.icons?.add(Image("file:icon.png"))
        mainWindow!!.setOnCloseRequest {
            editorWindow?.close()

            val pw = PrintWriter("settings.json")
            pw.write(settings.toJSONString())
            pw.flush()
            pw.close()
            primaryStage.close()
            exitProcess(0)
        }
        primaryStage.hide()
        initMainWindow()

        GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
            override fun nativeKeyTyped(e : NativeKeyEvent) {}

            override fun nativeKeyPressed(e: NativeKeyEvent) {
                Platform.runLater {
                    if (e.keyCode == NativeKeyEvent.VC_F2){
                        if (mainWindow != null)
                            getCapture()
                    } else if (e.keyCode == NativeKeyEvent.VC_F5){
                        if (editorWindow != null && editorWindow?.saver != null && editorWindow?.copyCanvas != null)
                            editorWindow?.saveCapture(editorWindow!!.saver!!, editorWindow!!.copyCanvas!!)
                    } else if (e.keyCode == NativeKeyEvent.VC_F6){
                        if (editorWindow != null && editorWindow?.saver != null && editorWindow?.copyCanvas != null)
                            editorWindow?.fastSave(editorWindow!!.saver!!, editorWindow!!.copyCanvas!!)
                    }
                }
            }

            override fun nativeKeyReleased(e: NativeKeyEvent) {}
        })
    }

    companion object {
        var ico = Image("file:icon.png")
        var settings = JSONObject()
        var mainWindow : Stage? = null

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}