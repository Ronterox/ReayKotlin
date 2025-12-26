package org.example

import java.awt.*
import java.awt.event.*
import java.lang.System
import javax.swing.*
import kotlin.math.*

data class Vec2(var x: Double = .0, var y: Double = .0) {
    override fun toString(): String {
        return "($x, $y)"
    }
}

data class Vec3(var x: Double = .0, var y: Double = .0, var z: Double = .0) {
    override fun toString(): String {
        return "($x, $y, $z)"
    }
}

const val WIN_WIDTH = 800
const val WIN_HEIGHT = 800
const val TARGET_FPS = 60

class Input(panel: JPanel, inputs: Map<String, List<Int>>) {
    enum class InputState {
        None,
        Pressed,
        Released
    }

    val mappedInput = mutableMapOf<String, InputState>()

    fun isPressed(eventKey: String): Boolean {
        return mappedInput[eventKey] == InputState.Pressed
    }

    fun isReleased(eventKey: String): Boolean {
        if (mappedInput[eventKey] != InputState.Released) {
            return false
        }
        mappedInput[eventKey] = InputState.None
        return true
    }

    init {
        val inputMap = panel.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = panel.getActionMap()

        val press = { key: String -> "press_$key" }
        val release = { key: String -> "release_$key" }

        val set_input = { code: Int, direction: String ->
            inputMap.put(KeyStroke.getKeyStroke(code, 0, false), press(direction))
            inputMap.put(KeyStroke.getKeyStroke(code, 0, true), release(direction))
        }

        val set_action = { name: String, action_fun: () -> Unit ->
            actionMap.put(
                    name,
                    object : AbstractAction() {
                        override fun actionPerformed(e: ActionEvent?) {
                            action_fun()
                        }
                    }
            )
        }

        inputs.forEach { (event, codes) ->
            codes.forEach { code -> set_input(code, event) }
            set_action(press(event), { mappedInput[event] = InputState.Pressed })
            set_action(release(event), { mappedInput[event] = InputState.Released })
        }
    }
}

class Game : JPanel(), Runnable {
    val input =
            Input(
                    this,
                    mapOf(
                            "up" to listOf(KeyEvent.VK_UP, KeyEvent.VK_W),
                            "down" to listOf(KeyEvent.VK_DOWN, KeyEvent.VK_S),
                            "left" to listOf(KeyEvent.VK_LEFT, KeyEvent.VK_A),
                            "right" to listOf(KeyEvent.VK_RIGHT, KeyEvent.VK_D),
                            "escape" to listOf(KeyEvent.VK_ESCAPE),
                            "space" to listOf(KeyEvent.VK_SPACE),
                    )
            )

    val points =
            listOf(
                    // Front
                    Vec3(0.25, 0.25, 0.25),
                    Vec3(-0.25, 0.25, 0.25),
                    Vec3(-0.25, -0.25, 0.25),
                    Vec3(0.25, -0.25, 0.25),

                    // Back
                    Vec3(0.25, 0.25, -0.25),
                    Vec3(-0.25, 0.25, -0.25),
                    Vec3(-0.25, -0.25, -0.25),
                    Vec3(0.25, -0.25, -0.25),
            )
    val faces =
            listOf(
                    listOf(0, 1, 2, 3),
                    listOf(4, 5, 6, 7),
                    listOf(0, 4),
                    listOf(1, 5),
                    listOf(2, 6),
                    listOf(3, 7),
            )

    var offset = Vec3(0.0, 0.0, 0.0)
    var angle = 0.0

    var dt = 0.0
    var gameThread: Thread

    override fun run() {
        var nsPerFrame = 1_000_000_000.0 / TARGET_FPS
        var lastTime = System.nanoTime()
        dt = 0.0

        while (true) {
            val now = System.nanoTime()
            dt += (now - lastTime) / nsPerFrame
            lastTime = now

            while (dt >= 1) {
                update()
                dt--
            }

            repaint()

            Thread.sleep(1)
        }
    }

    fun update() {
        if (input.isPressed("escape")) {
            System.exit(0)
        }

        if (input.isPressed("up")) {
            offset.z += dt * 0.01
        }

        if (input.isPressed("down")) {
            offset.z -= dt * 0.01
        }

        if (input.isPressed("left")) {
            offset.x -= dt * 0.01
        }

        if (input.isPressed("right")) {
            offset.x += dt * 0.01
        }

        if (input.isPressed("space")) {
            angle += PI * dt * 0.01
        }
    }

    fun point(g: Graphics, vec: Vec2) {
        val SIZE = 10
        val HSIZE = SIZE / 2
        val COLOR = Color.GREEN

        g.color = COLOR
        g.fillOval(vec.x.toInt() - HSIZE, vec.y.toInt() - HSIZE, SIZE, SIZE)
    }

    fun line(g: Graphics, v1: Vec2, v2: Vec2) {
        val SIZE = 10.0
        val HSIZE = SIZE / 2.0
        val COLOR = Color.GREEN

        g.color = COLOR
        g.drawLine(
                (v1.x - HSIZE).roundToInt(),
                (v1.y - HSIZE).roundToInt(),
                (v2.x - HSIZE).roundToInt(),
                (v2.y - HSIZE).roundToInt(),
        )
    }

    fun screen(vec: Vec3, out: Vec2) {
        out.x = (vec.x + 1) / 2.0 * WIN_WIDTH
        out.y = (1 - (vec.y + 1) / 2.0) * WIN_HEIGHT
    }

    fun project(vec: Vec3, offset: Vec3) {
        val z = vec.z + offset.z
        vec.x = vec.x / z + offset.x
        vec.y = vec.y / z + offset.y
    }

    fun rotate_xz(vec: Vec3, angle: Double) {
        val (x, _, z) = vec
        val c = cos(angle)
        val s = sin(angle)

        vec.x = x * c - z * s
        vec.z = x * s + z * c
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        var inp1 = Vec3(0.0, 0.0, 0.0)
        var out1 = Vec2(0.0, 0.0)

        var inp2 = Vec3(0.0, 0.0, 0.0)
        var out2 = Vec2(0.0, 0.0)

        faces.forEach { face ->
            face.forEachIndexed { i, _ ->
                val v1 = points[face[i]]
                inp1.x = v1.x
                inp1.y = v1.y
                inp1.z = v1.z

                rotate_xz(inp1, angle)
                project(inp1, offset)
                screen(inp1, out1)

                // point(g, out1)

                val v2 = points[face[(i + 1) % face.size]]
                inp2.x = v2.x
                inp2.y = v2.y
                inp2.z = v2.z

                rotate_xz(inp2, angle)
                project(inp2, offset)
                screen(inp2, out2)

                line(g, out1, out2)
            }
        }

        Toolkit.getDefaultToolkit().sync()
    }

    init {
        this.background = Color.BLACK
        gameThread = Thread(this)
        gameThread.start()
    }
}

fun main() {
    val frame = JFrame("Reay")

    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(WIN_WIDTH, WIN_HEIGHT)
    frame.setVisible(true)

    frame.setMinimumSize(frame.size)
    frame.setResizable(false)

    frame.add(Game())
    frame.pack()
}
