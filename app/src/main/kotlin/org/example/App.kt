package org.example

import java.awt.*
import java.awt.event.*
import javax.swing.*

const val WIN_WIDTH = 800
const val WIN_HEIGHT = 600

open class Vec2(var x: Double = .0, var y: Double = .0) {
    override fun toString(): String {
        return "($x, $y)"
    }
}

operator fun Vec2.plus(other: Vec2): Vec2 {
    return Vec2(x + other.x, y + other.y)
}

operator fun Vec2.minus(other: Vec2): Vec2 {
    return Vec2(x - other.x, y - other.y)
}

operator fun Vec2.times(other: Double): Vec2 {
    return Vec2(x * other, y * other)
}

operator fun Vec2.times(other: Vec2): Vec2 {
    return Vec2(x * other.x, y * other.y)
}

class Player(var position: Vec2 = Vec2(.0, .0), var velocity: Vec2 = Vec2(.0, .0)) {
    val x: Double
        get() = position.x
    val y: Double
        get() = position.y
}

class Input(panel: JPanel) : Vec2() {
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

        val inputs =
                mapOf(
                        "up" to listOf(KeyEvent.VK_UP, KeyEvent.VK_W),
                        "down" to listOf(KeyEvent.VK_DOWN, KeyEvent.VK_S),
                        "left" to listOf(KeyEvent.VK_LEFT, KeyEvent.VK_A),
                        "right" to listOf(KeyEvent.VK_RIGHT, KeyEvent.VK_D),
                        "escape" to listOf(KeyEvent.VK_ESCAPE),
                        "space" to listOf(KeyEvent.VK_SPACE)
                )

        inputs.forEach { (event, codes) ->
            codes.forEach { code -> set_input(code, event) }
            set_action(press(event), { mappedInput[event] = InputState.Pressed })
            set_action(release(event), { mappedInput[event] = InputState.Released })
        }
    }
}

class Game : JPanel() {
    var player = Player()

    var rotX = Vec2(0.0, 0.0)
    var rotY = Vec2(0.0, 0.0)
    var rotS = Vec2(0.0, 0.0)

    var curr = 0

    val input = Input(this)
    val speed = 5

    val gameLoop = Timer(10, { update() })

    val tileSize = 20 * 5
    val tileMap =
            arrayOf(
                    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                    intArrayOf(0, 1, 0, 0, 0, 1, 1, 0, 0, 0),
                    intArrayOf(0, 1, 0, 0, 0, 1, 0, 1, 0, 0),
                    intArrayOf(0, 1, 0, 0, 0, 1, 1, 0, 0, 0),
                    intArrayOf(0, 1, 1, 1, 0, 1, 0, 1, 0, 0),
                    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                    intArrayOf(0, 1, 0, 0, 0, 1, 1, 0, 0, 0),
                    intArrayOf(0, 1, 0, 0, 0, 1, 0, 1, 0, 0),
                    intArrayOf(0, 1, 0, 0, 0, 1, 1, 0, 0, 0),
                    intArrayOf(0, 1, 1, 1, 0, 1, 0, 1, 0, 0),
                    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            )

    fun update() {
        player.velocity = Vec2(0.0, 0.0)

        if (input.isPressed("escape")) {
            System.exit(0)
        }

        if (input.isReleased("space")) {
            println(
                    when (curr++ % 3) {
                        0 -> rotX
                        1 -> rotY
                        2 -> rotS
                        else -> throw Exception("Unexpected")
                    }
            )
        }

        if (input.isPressed("up")) {
            player.velocity.y -= speed
        }

        if (input.isPressed("down")) {
            player.velocity.y += speed
        }

        if (input.isPressed("left")) {
            player.velocity.x -= speed
        }

        if (input.isPressed("right")) {
            player.velocity.x += speed
        }

        val amount = player.velocity * 0.01
        when (curr % 3) {
            0 -> rotX += amount
            1 -> rotY -= amount
            2 -> rotS += amount
        }

        player.position += player.velocity

        player.position.x = player.x.coerceIn(0.0, WIN_WIDTH - 20.0)
        player.position.y = player.y.coerceIn(0.0, WIN_HEIGHT - 20.0)

        repaint()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA)

        for (y in 0..WIN_HEIGHT / tileSize) {
            for (x in 0..WIN_WIDTH / tileSize) {
                val sx = tileSize * (rotS.x + 1.0)
                val sy = tileSize * (rotS.y + 1.0)

                val xs = (x * sx).toDouble()
                val ys = (y * sy).toDouble()

                val org = Vec2(xs, ys)

                val xT = Vec2(xs + sx * rotX.x, ys + sy * rotX.y)
                val yT = Vec2(xs - sx * rotY.x, ys + sy * rotY.y)

                g?.color = colors[curr % colors.size]
                g?.drawLine(org.x.toInt(), org.y.toInt(), xT.x.toInt(), xT.y.toInt())
                g?.drawLine(org.x.toInt(), org.y.toInt(), yT.x.toInt(), yT.y.toInt())
            }
        }
    }

    init {
        this.background = Color.BLACK
        player.position += Vec2(WIN_WIDTH / 2.0, WIN_HEIGHT / 2.0)
        gameLoop.start()
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
