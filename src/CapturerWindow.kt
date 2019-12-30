import ar.com.hjg.pngj.PngReader
import ar.com.hjg.pngj.PngWriter
import ar.com.hjg.pngj.chunks.PngChunkTextVar
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXColorPicker
import com.jfoenix.controls.JFXSlider
import com.jfoenix.controls.JFXToolbar
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.scene.effect.Light
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.transform.Transform
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Duration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.round

class CapturerWindow(inputImg : Image) : Stage() {
    var isProoved = true

    var copyCanvas : Canvas? = null


    val extFilter = FileChooser.ExtensionFilter("Image files (*.png)", "*.png")
    val canvasWidth = 1280.0 //img.width
    val canvasHeight = 720.0 //img.height
    var saver : Button? = null
    var img = inputImg
    var isShrinking = false

    val selectionRect = Rectangle(0.0, 0.0, canvasWidth, canvasHeight)

    private var successWindow: SuccessWindow? = null

    private fun initSuccessWindow(file : File) {
        if (successWindow != null && successWindow!!.isShowing)
            successWindow!!.hide()
        successWindow = SuccessWindow(file)
        successWindow!!.show(this, this.width - 125.0,
                this.height - 235.0)
    }

    fun drawSelectionRect(selectionRect : Rectangle, gc : GraphicsContext, sgc : GraphicsContext){
        gc.stroke = selectionRect.stroke
        gc.lineWidth = selectionRect.strokeWidth

        gc.beginPath()
        gc.moveTo(selectionRect.x, selectionRect.y)
        gc.stroke()
        gc.lineTo(selectionRect.x + selectionRect.width, selectionRect.y)
        gc.stroke()
        gc.lineTo(selectionRect.x + selectionRect.width, selectionRect.y + selectionRect.height)
        gc.stroke()
        gc.lineTo(selectionRect.x, selectionRect.y + selectionRect.height)
        gc.stroke()
        gc.lineTo(selectionRect.x, selectionRect.y)
        gc.stroke()
        gc.closePath()

        gc.stroke = sgc.stroke
        gc.lineWidth = sgc.lineWidth
    }

    private fun checkCorrectness(saver : Button) : Boolean {
        if (selectionRect.width == 0.0 || selectionRect.height == 0.0) {
            val tooltip = Tooltip("Cant save zero-width/height image")
            saver.tooltip = tooltip
            tooltip.isAutoHide = true
            tooltip.show(this, this.x + saver.layoutX, this.y + saver.layoutY)
            return false
        }
        return true
    }

    private fun writeImg(file : File, canvas : Canvas) {
        Main.settings["defaultDirectory"] = file.parent
        val wImage = takeSnapshot(canvas)
        var bufImg = SwingFXUtils.fromFXImage(wImage!!, null)
        bufImg = bufImg.getSubimage((selectionRect.x / canvasWidth * bufImg.width).toInt(),
                (selectionRect.y / canvasHeight * bufImg.height ).toInt(),
                (selectionRect.width / canvasWidth * bufImg.width).toInt(),
                (selectionRect.height / canvasHeight * bufImg.height).toInt())

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufImg, "png", outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        val pngReader = PngReader(inputStream)
        val pngWriter = PngWriter(file, pngReader.imgInfo, true)

        pngWriter.metadata.setText(PngChunkTextVar.KEY_Author, "ScreenCapturer")

        for (row in 0 until pngReader.imgInfo.rows){
            val line = pngReader.readRow()
            pngWriter.writeRow(line)
        }
        pngReader.end()
        pngWriter.end()
    }

    fun fastSave(saver : Button, canvas : Canvas) {
        if (!checkCorrectness(saver))
            return
        val file = File(Main.settings["defaultDirectory"].toString() + "\\" + "capture.png")
        writeImg(file, canvas)
        initSuccessWindow(file)
    }


    fun saveCapture(saver : Button, canvas : Canvas) {
        if (!checkCorrectness(saver))
            return

        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(extFilter)
        fileChooser.initialDirectory = File(Main.settings["defaultDirectory"].toString())
        val file : File? = fileChooser.showSaveDialog(this)
        if (file != null){
            writeImg(file, canvas)
            initSuccessWindow(file)
        }
    }

    fun cloneCanvas(canvas : Canvas, copyCanvas : Canvas) {
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        val image = canvas.snapshot(params, null)
        copyCanvas.graphicsContext2D.drawImage(image, 0.0, 0.0, canvasWidth, canvasHeight)
    }

    init {
        this.setOnCloseRequest {
            Main.mainWindow?.show()
        }
        this.icons.add(Image("file:icon.png"))
        this.title = "Save capture"
        val root = object : BorderPane() {
            init {
                val anchor = Light.Point()

                selectionRect.stroke = Color.RED
                selectionRect.strokeWidth = 5.0

                val canvas = Canvas(canvasWidth, canvasHeight)
                copyCanvas = Canvas(img.width, img.height)
                val gc = canvas.graphicsContext2D
                val sgc = copyCanvas?.graphicsContext2D
                gc.drawImage(img, 0.0, 0.0, canvasWidth, canvasHeight)
                sgc?.drawImage(img, 0.0, 0.0, img.width, img.height)

                drawSelectionRect(selectionRect, gc, sgc!!)

                val tools = VBox()
                val toolBox = object : JFXToolbar() {
                    init {
                        val proovedImg = object : ImageView() {
                            init {
                                this.image = Main.ico
                                val tooltip = Tooltip("This image was created using ScreenCapturer")
                                tooltip.showDelay = Duration.seconds(0.25)
                                Tooltip.install(this, tooltip)
                                this.fitWidth = 32.0
                                this.fitHeight = 32.0
                                isVisible = isProoved
                            }
                        }

                        val colorPicker = JFXColorPicker()
                        colorPicker.value = Color.BLACK
                        colorPicker.valueProperty().addListener { _, _, newValue ->
                            gc.stroke = newValue
                            sgc.stroke = newValue
                        }

                        val widthPicker = JFXSlider()
                        widthPicker.isShowTickLabels = true
                        widthPicker.isShowTickMarks = true
                        widthPicker.majorTickUnit = 1.0
                        widthPicker.minorTickCount = 3
                        widthPicker.min = 0.5
                        widthPicker.max = 10.0
                        widthPicker.value = 1.0
                        widthPicker.valueProperty().addListener { _, _, newValue ->
                            gc.lineWidth = newValue.toDouble()
                            sgc.lineWidth = newValue.toDouble()
                        }

                        val shrinker = JFXButton("Shrink")
                        shrinker.setOnAction {
                            isShrinking = true
                        }

                        saver = JFXButton("Save")
                        saver?.setOnAction {
                            saveCapture(saver!!, copyCanvas!!)
                        }

                        val back = JFXButton("Make another capture")
                        back.setOnAction {
                            close()
                            Main.mainWindow?.show()
                        }

                        val openImg = JFXButton("Open previous capture")
                        openImg.setOnAction {
                            val fileChooser = FileChooser()
                            fileChooser.title = "Open capture"
                            fileChooser.selectedExtensionFilter = extFilter
                            fileChooser.initialDirectory = File(Main.settings["defaultDirectory"].toString())
                            val file = fileChooser.showOpenDialog(this@CapturerWindow)

                            if (file != null) {
                                val pngReader = PngReader(file)
                                pngReader.readSkippingAllRows()
                                isProoved = pngReader.metadata.getTxtForKey(PngChunkTextVar.KEY_Author) == "ScreenCapturer"
                                proovedImg.isVisible = isProoved
                                pngReader.close()

                                gc.clearRect(0.0, 0.0, canvasWidth, canvasHeight)
                                sgc.clearRect(0.0, 0.0, img.width, img.height)

                                img = SwingFXUtils.toFXImage(ImageIO.read(file), null)

                                anchor.x = 0.0
                                anchor.y = 0.0

                                copyCanvas?.width = img.width
                                copyCanvas?.height = img.height
                                gc.drawImage(img, 0.0, 0.0, canvasWidth, canvasHeight)
                                sgc.drawImage(img, 0.0, 0.0, img.width, img.height)

                                selectionRect.x = 0.0
                                selectionRect.y = 0.0
                                selectionRect.width = canvasWidth
                                selectionRect.height = canvasHeight

                                drawSelectionRect(selectionRect, gc, sgc)
                            }
                        }

                        leftItems.addAll(colorPicker, widthPicker, shrinker, saver, back, openImg, proovedImg)
                    }
                }

                tools.children.addAll(toolBox)

                top = tools

                canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { e ->
                    if (isShrinking){
                        cloneCanvas(copyCanvas!!, canvas)
                        anchor.x = e.x
                        anchor.y = e.y
                        selectionRect.x = e.x
                        selectionRect.y = e.y
                        selectionRect.width = 0.0
                        selectionRect.height = 0.0
                        drawSelectionRect(selectionRect, gc, sgc)
                    } else {
                        gc.beginPath()
                        gc.moveTo(e.x, e.y)
                        gc.stroke()
                        sgc.beginPath()
                        sgc.moveTo(e.x / canvasWidth * img.width, e.y / canvasHeight * img.height)
                        sgc.stroke()
                    }
                }

                canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { e ->
                    if (isShrinking){
                        cloneCanvas(copyCanvas!!, canvas)
                        selectionRect.width = abs(e.x - anchor.x)
                        selectionRect.height = abs(e.y - anchor.y)
                        selectionRect.x = anchor.x.coerceAtMost(e.x)
                        selectionRect.y = anchor.y.coerceAtMost(e.y)
                        drawSelectionRect(selectionRect, gc, sgc)
                    } else {
                        gc.lineTo(e.x, e.y)
                        gc.stroke()
                        sgc.lineTo(e.x / canvasWidth * img.width, e.y / canvasHeight * img.height)
                        sgc.stroke()
                    }

                }

                canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) {
                    if (isShrinking) {
                        cloneCanvas(copyCanvas!!, canvas)
                        isShrinking = false
                        drawSelectionRect(selectionRect, gc, sgc)
                    } else {
                        gc.closePath()
                        sgc.closePath()
                        drawSelectionRect(selectionRect, gc, sgc)
                    }
                }

                center = canvas
            }
        }

        val scene = Scene(root, canvasWidth, canvasHeight + 100.0)
        scene.setOnKeyPressed { e ->
            if (e.code == KeyCode.C && e.isControlDown && copyCanvas != null){
                val clipboard = Clipboard.getSystemClipboard()
                val content = ClipboardContent()
                val wImage = takeSnapshot(copyCanvas!!)
                val bufImg = SwingFXUtils.fromFXImage(wImage, null).getSubimage((selectionRect.x / 1280.0 * img.width).toInt(),
                        (selectionRect.y / canvasHeight * img.height).toInt(),
                        (selectionRect.width / canvasWidth * img.width).toInt(),
                        (selectionRect.height / canvasHeight * img.height).toInt())

                content.putImage(SwingFXUtils.toFXImage(bufImg, null))
                clipboard.setContent(content)
            }
        }
        this.scene = scene

        this.show()
    }

    private fun takeSnapshot(canvas: Canvas): WritableImage? {
        val writableImage = WritableImage(round(canvas.width).toInt(), round(canvas.height).toInt())
        val spa = SnapshotParameters()
        spa.transform = Transform.scale(1.0, 1.0)
        return canvas.snapshot(spa, writableImage)
    }
}