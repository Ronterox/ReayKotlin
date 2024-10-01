package org.example

import java.awt.*
import java.awt.event.*
import javax.swing.*

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

class Game : JPanel() {
    var player = Player()
    val speed = 10

    val gameLoop =
            Timer(
                    10,
                    {
                        player.position += player.velocity
                        repaint()
                    }
            )

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.color = Color.RED
        g?.fillRect(player.x, player.y, 20, 20)
    }

    init {
        this.background = Color.BLACK

        val inputMap = this.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = this.getActionMap()

        val press = { key: String -> "pressed_$key" }
        val release = { key: String -> "released_$key" }

        val set_input = { code: Int, direction: String ->
            inputMap.put(KeyStroke.getKeyStroke(code, 0, false), press(direction))
            inputMap.put(KeyStroke.getKeyStroke(code, 0, true), release(direction))
        }

        val set_action = { name: String, action_fun: () -> Unit ->
            actionMap.put(name, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    action_fun()
                }
            })
        }

        set_input(KeyEvent.VK_W, "up")
        set_input(KeyEvent.VK_S, "down")
        set_input(KeyEvent.VK_A, "left")
        set_input(KeyEvent.VK_D, "right")

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape")

        set_action(release("up"), { player.velocity.y = 0 })
        set_action(press("up"), { player.velocity.y = -speed })

        set_action(release("down"), { player.velocity.y = 0 })
        set_action(press("down"), { player.velocity.y = speed })

        set_action(release("left"), { player.velocity.x = 0 })
        set_action(press("left"), { player.velocity.x = -speed })

        set_action(release("right"), { player.velocity.x = 0 })
        set_action(press("right"), { player.velocity.x = speed })

        set_action("escape", { System.exit(0) })

        gameLoop.start()
    }
}

fun main() {
    val frame = JFrame("Reay")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(800, 600)
    frame.setVisible(true)

    frame.add(Game())
    frame.pack()
}
