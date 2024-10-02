package org.example

import java.awt.*
import java.awt.event.*
import javax.swing.*

const val WIN_WIDTH = 800
const val WIN_HEIGHT = 600

open class Vec2(var x: Int = 0, var y: Int = 0)

operator fun Vec2.plus(other: Vec2): Vec2 {
    return Vec2(x + other.x, y + other.y)
}

class Player(var position: Vec2 = Vec2(0, 0), var velocity: Vec2 = Vec2(0, 0)) {
    val x: Int
        get() = position.x
    val y: Int
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
                        "escape" to listOf(KeyEvent.VK_ESCAPE)
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
    val input = Input(this)
    val speed = 5

    val gameLoop = Timer(10, { update() })

    fun update() {
        player.velocity = Vec2(0, 0)

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

        player.position += player.velocity

        repaint()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.color = Color.RED
        g?.fillRect(player.x, player.y, 20, 20)
    }

    init {
        this.background = Color.BLACK

        player.position += Vec2(WIN_WIDTH / 2, WIN_HEIGHT / 2)
        gameLoop.start()
    }
}

fun main() {
    val frame = JFrame("Reay")

    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(WIN_WIDTH, WIN_HEIGHT)
    frame.setVisible(true)

    frame.add(Game())
    frame.pack()
}
