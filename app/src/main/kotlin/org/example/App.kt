package org.example

import java.awt.*
import java.awt.event.*
import java.io.ObjectInputStream
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
    val verticesIso = arrayListOf<Polygon>()
    val verticesThird = arrayListOf<Polygon>()
    val verticesTop = arrayListOf<Polygon>()

    fun loadSprite(filepath: String, vertices: ArrayList<Polygon>) {
        val file = javaClass.classLoader.getResourceAsStream(filepath)

        if (file == null) {
            println("File not found $filepath!")
            System.exit(0)
        }

        ObjectInputStream(file).readObject().let {
            if (it is List<*>) {
                it.forEach { vertices.add(it as Polygon) }
            }
        }
    }

    init {
        loadSprite("player_iso.obj", verticesIso)
        loadSprite("player_third.obj", verticesThird)
        loadSprite("player_top.obj", verticesTop)
    }
}

class Input(panel: JPanel, inputs: Map<String, List<Int>>) : Vec2() {
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

class Game : JPanel() {
    val gameLoop = Timer(1000 / 60, { update() })
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
                            "inc" to listOf(KeyEvent.VK_E),
                            "dec" to listOf(KeyEvent.VK_Q),
                    )
            )
    val player = Player()
    val speed = 0.15
    val modes = listOf(Mode.Topdown, Mode.Isometric, Mode.ThirdPerson)

    var tileSize = 50
    var offset = Vec2(0.0, 0.0)

    var modeIndex = 2
    var inc = 1

    enum class Mode {
        Isometric,
        Topdown,
        ThirdPerson
    }

    fun update() {
        player.velocity = Vec2(0.0, 0.0)

        if (input.isPressed("escape")) {
            System.exit(0)
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

        if (input.isPressed("inc")) {
            tileSize++
        } else if (input.isPressed("dec")) {
            tileSize--
        }

        if (input.isReleased("space")) {
            inc = inc * -1 * (if (modeIndex == 0 || modeIndex == 2) 1 else -1)
            modeIndex += inc
        }

        offset += player.velocity
        player.position += player.velocity
        player.position.x = player.x.coerceIn(0.0, WIN_WIDTH - 20.0)
        player.position.y = player.y.coerceIn(0.0, WIN_HEIGHT - 20.0)

        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA)
        val isIso = if (modes[modeIndex] == Mode.Isometric) 1.0 else 0.0
        val isThird = if (modes[modeIndex] == Mode.ThirdPerson) 1.0 else 0.0

        val h = 10
        val w = 10
        for (y in 0 until h) {
            for (x in 0 until w) {
                val sx = tileSize
                val sy = tileSize

                val vx = x + (h - y) * isIso + x * 2 * isThird + offset.x
                val vy = y + (x * 0.5 - y * 0.5) * isIso + offset.y

                val x1 = vx * sx - (sx * y * isThird)
                val x2 = vx * sx + sx + (sx * y * isThird)
                val x3 = x2 - (sx * isIso) + (sx * y * isThird)
                val x4 = x1 - (sx * isIso) - (sx * y * isThird)

                val y1 = vy * sy
                val y2 = y1 + (sy * 0.5 * isIso)
                val y3 = y1 + sy + (sy * 0.1 * isIso)
                val y4 = y1 + sy - (sy * 0.5 * isIso)

                val xs = intArrayOf(x1.toInt(), x2.toInt(), x3.toInt(), x4.toInt())
                val ys = intArrayOf(y1.toInt(), y2.toInt(), y3.toInt(), y4.toInt())

                val tile = Polygon(xs, ys, xs.size)

                g.color = colors[x % colors.size]
                g.fillPolygon(tile)
                // g.drawPolygon(tile)
            }
        }

        g.color = Color.WHITE
        g.drawString(modes[modeIndex].toString(), 50, 50)

        val playerSprite = when (modes[modeIndex]) {
            Mode.Isometric -> player.verticesIso
            Mode.Topdown -> player.verticesTop
            Mode.ThirdPerson -> player.verticesThird
        }

        g.color = Color.WHITE
        playerSprite.forEach {
            // TODO: Increment X and Y for player to move
            g.fillPolygon(it)
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
