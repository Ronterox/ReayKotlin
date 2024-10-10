package org.example

import java.awt.*
import java.awt.event.*
import java.io.ObjectOutputStream
import javax.swing.*

const val APP_NAME = "Object Designer"

class Designer : JPanel() {
    val appLoop = Timer(1000 / 60, { update() })
    val input =
            Input(
                    this,
                    mapOf(
                            "escape" to listOf(KeyEvent.VK_ESCAPE),
                            "space" to listOf(KeyEvent.VK_SPACE),
                            "delete" to listOf(KeyEvent.VK_Q),
                    )
            )

    val figures = arrayListOf<Polygon>()
    var curr = Polygon()

    var verticesTotal = 0

    fun update() {
        if (input.isPressed("escape")) {
            System.exit(0)
        }

        if (input.isReleased("space") && curr.npoints > 0) {
            figures.add(curr)
            curr = Polygon()
        }

        if (input.isPressed("delete")) {
            verticesTotal -= curr.npoints
            curr = Polygon()
        }

        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        g.color = Color.RED
        figures.forEach { g.drawPolygon(it) }

        g.color = Color.WHITE
        g.drawPolygon(curr)

        g.drawString("Vertices: ${curr.npoints}", 10, 80)
        g.drawString("Total Vertices: $verticesTotal", 10, 100)
    }

    init {
        this.background = Color.BLACK
        this.appLoop.start()

        this.addMouseListener(
                object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        this@Designer.grabFocus()
                        curr.addPoint(e.x, e.y)
                        verticesTotal++
                    }
                }
        )

        val exitButton = JButton("Exit")
        val saveButton = JButton("Save")
        val resetButton = JButton("Reset")

        exitButton.addActionListener { System.exit(0) }
        resetButton.addActionListener {
            verticesTotal = 0
            figures.clear()
            curr = Polygon()
        }
        saveButton.addActionListener {
            val fileDialog = JFileChooser("./src/main/resources")
            // TODO: Save in a way so that the first point is at point (0,0)
            if (fileDialog.showDialog(this, "Save") == JFileChooser.APPROVE_OPTION) {
                ObjectOutputStream(fileDialog.selectedFile.outputStream()).use {
                    it.writeObject(figures)
                }
            }
        }

        val titleBar = JPanel()
        titleBar.background = Color(128, 0, 0, 128)
        titleBar.preferredSize = Dimension(WIN_WIDTH, 50)

        val text = JLabel(APP_NAME)
        text.font = Font("Arial", Font.BOLD, 20)
        text.foreground = Color.WHITE

        titleBar.add(exitButton)
        titleBar.add(text)
        titleBar.add(saveButton)
        titleBar.add(resetButton)

        add(titleBar, BorderLayout.NORTH)
    }
}

fun main() {
    val frame = JFrame(APP_NAME)

    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(WIN_WIDTH, WIN_HEIGHT)
    frame.setVisible(true)

    frame.setMinimumSize(frame.size)
    frame.setResizable(false)

    frame.add(Designer())
    frame.pack()
}
