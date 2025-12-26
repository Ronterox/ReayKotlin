package org.example

import java.awt.*
import java.awt.event.*
import javax.swing.*

const val WIN_WIDTH = 800
const val WIN_HEIGHT = 600
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

class Game : JPanel() {
    val gameLoop = Timer(1000 / TARGET_FPS, { update() })
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

    fun update() {
        if (input.isPressed("escape")) {
            System.exit(0)
        }

        if (input.isPressed("up")) {
            println("up")
        }

        if (input.isPressed("down")) {
            println("down")
        }

        if (input.isPressed("left")) {
            println("left")
        }

        if (input.isPressed("right")) {
            println("right")
        }

        if (input.isPressed("space")) {
            println("space")
        }

        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
    }

    init {
        this.background = Color.BLACK
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
