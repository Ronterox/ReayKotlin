package org.example

import java.awt.Color
import java.awt.Graphics
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.system.exitProcess

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

		val press = { key: String -> "press_$key" }
		val release = { key: String -> "release_$key" }

		val setInput = { code: Int, direction: String ->
			inputMap.put(KeyStroke.getKeyStroke(code, 0, false), press(direction))
			inputMap.put(KeyStroke.getKeyStroke(code, 0, true), release(direction))
		}

		val setAction = { name: String, actionFun: () -> Unit ->
			panel.actionMap.put(
				name,
				object : AbstractAction() {
					override fun actionPerformed(e: ActionEvent?) {
						actionFun()
					}
				}
			)
		}

		inputs.forEach { (event, codes) ->
			codes.forEach { code -> setInput(code, event) }
			setAction(press(event)) { mappedInput[event] = InputState.Pressed }
			setAction(release(event)) { mappedInput[event] = InputState.Released }
		}
	}
}

class Game : JPanel(), Runnable {
	val input =
		Input(
			this,
			mapOf(
				"up" to listOf(KeyEvent.VK_W),
				"look_up" to listOf(KeyEvent.VK_UP),
				"down" to listOf(KeyEvent.VK_S),
				"look_down" to listOf(KeyEvent.VK_DOWN),
				"left" to listOf(KeyEvent.VK_A),
				"look_left" to listOf(KeyEvent.VK_LEFT),
				"right" to listOf(KeyEvent.VK_D),
				"look_right" to listOf(KeyEvent.VK_RIGHT),
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
			// Connection faces
			listOf(0, 4, 5, 1),
			listOf(1, 5, 6, 2),
			listOf(2, 6, 7, 3),
			listOf(3, 7, 4, 0),
		)

	var offset = Vec3(0.0, 0.0, 1.0)
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
			exitProcess(0)
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

		if (input.isPressed("look_right")) {
			angle += PI * dt * 0.01
		}

		if (input.isPressed("look_left")) {
			angle -= PI * dt * 0.01
		}

		if (input.isPressed("look_up")) {
			offset.y += dt * 0.01
		}

		if (input.isPressed("look_down")) {
			offset.y -= dt * 0.01
		}

		if (input.isPressed("right")) {
			offset.x += dt * 0.01
		}
	}

	fun point(g: Graphics, vec: Vec2) {
		val size = 10
		val hsize = size / 2
		val color = Color.GREEN

		g.color = color
		g.fillOval(vec.x.toInt() - hsize, vec.y.toInt() - hsize, size, size)
	}

	fun line(g: Graphics, v1: Vec2, v2: Vec2) {
		val size = 10.0
		val hsize = size / 2.0
		val color = Color.GREEN

		g.color = color
		g.drawLine(
			(v1.x - hsize).roundToInt(),
			(v1.y - hsize).roundToInt(),
			(v2.x - hsize).roundToInt(),
			(v2.y - hsize).roundToInt(),
		)
	}

	fun fillFaceManually(g: Graphics, projectedPoints: List<Vec2>, color: Color) {
		// Limits of drawings
		val minY = projectedPoints.minOf { it.y }.toInt()
		val maxY = projectedPoints.maxOf { it.y }.toInt()

		g.color = color

		// Horizontal scanlines
		for (y in minY..maxY) {
			val intersections = mutableListOf<Double>()

			// Find where the scanline crosses each edge
			for (i in projectedPoints.indices) {
				val p1 = projectedPoints[i]
				val p2 = projectedPoints[(i + 1) % projectedPoints.size]

				// Check if the scanline 'y' is between the two vertices
				if ((p1.y <= y && p2.y > y) || (p2.y <= y && p1.y > y)) {
					// Calculate X coordinate using linear interpolation
					// Formula: x = x1 + (y - y1) * (x2 - x1) / (y2 - y1)
					val intersectX = p1.x + (y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y)
					intersections.add(intersectX)
				}
			}

			// Sort intersections from left to right
			intersections.sort()

			// Fill pixels between pairs of intersections
			for (i in 0 until intersections.size - 1 step 2) {
				val startX = intersections[i].toInt()
				val endX = intersections[i + 1].toInt()
				g.drawLine(startX, y, endX, y)
			}
		}
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

	fun rotateXZ(vec: Vec3, angle: Double) {
		val (x, _, z) = vec
		val c = cos(angle)
		val s = sin(angle)

		vec.x = x * c - z * s
		vec.z = x * s + z * c
	}

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)

		var inp = Vec3(0.0, 0.0, 0.0)

		faces.forEach { face ->
			val facePoints = mutableListOf<Vec2>()

			face.forEachIndexed { i, _ ->
				val out = Vec2(0.0, 0.0)
				val v1 = points[face[i]]
				inp.x = v1.x
				inp.y = v1.y
				inp.z = v1.z

				rotateXZ(inp, angle)
				project(inp, offset)
				screen(inp, out)

				facePoints.add(out)
			}

			fillFaceManually(g, facePoints, Color.GREEN)

			for (i in facePoints.indices) {
				line(g, facePoints[i], facePoints[(i + 1) % facePoints.size])
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
	frame.isVisible = true

	frame.minimumSize = frame.size
	frame.isResizable = false

	frame.add(Game())
	frame.pack()
}
