package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.random.Random

class SnakeGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface GameEventListener {
        fun onScoreChanged(score: Int)
        fun onGameOver(score: Int)
    }

    private val gridSize = 20
    private val updateDelayMs = 180L
    private val snake = ArrayDeque<Point>()
    private var direction = Direction.RIGHT
    private var pendingDirection = direction
    private var food = Point(0, 0)
    private var score = 0
    private var cellSize = 0f
    private var startX = 0f
    private var startY = 0f
    private var isRunning = false
    private var isGameOver = false
    private var listener: GameEventListener? = null

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.board_bg)
    }
    private val snakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.snake_green)
    }
    private val snakeHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.snake_dark)
    }
    private val foodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.food_red)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        textAlign = Paint.Align.CENTER
        textSize = 48f
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!isRunning) return
            step()
            invalidate()
            postDelayed(this, updateDelayMs)
        }
    }

    init {
        isClickable = true
        resetGame()
    }

    fun setGameEventListener(listener: GameEventListener) {
        this.listener = listener
    }

    fun resume() {
        if (!isRunning && !isGameOver) {
            isRunning = true
            post(gameLoop)
        }
    }

    fun pause() {
        isRunning = false
        removeCallbacks(gameLoop)
    }

    fun startNewGame() {
        resetGame()
        isGameOver = false
        isRunning = true
        post(gameLoop)
    }

    fun isRunning(): Boolean = isRunning

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boardSize = minOf(w, h).toFloat()
        cellSize = boardSize / gridSize
        resetGame()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boardPaint)
        if (cellSize == 0f) return

        drawFood(canvas)
        drawSnake(canvas)

        if (isGameOver) {
            canvas.drawText(
                context.getString(R.string.status_game_over),
                width / 2f,
                height / 2f,
                textPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isGameOver || !isRunning) return true
                val deltaX = event.x - startX
                val deltaY = event.y - startY
                if (abs(deltaX) > abs(deltaY)) {
                    if (deltaX > 0) setDirection(Direction.RIGHT) else setDirection(Direction.LEFT)
                } else {
                    if (deltaY > 0) setDirection(Direction.DOWN) else setDirection(Direction.UP)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun drawSnake(canvas: Canvas) {
        snake.forEachIndexed { index, point ->
            val left = point.x * cellSize
            val top = point.y * cellSize
            val paint = if (index == 0) snakeHeadPaint else snakePaint
            canvas.drawRoundRect(
                left,
                top,
                left + cellSize,
                top + cellSize,
                cellSize * 0.2f,
                cellSize * 0.2f,
                paint
            )
        }
    }

    private fun drawFood(canvas: Canvas) {
        val left = food.x * cellSize
        val top = food.y * cellSize
        canvas.drawOval(
            left + cellSize * 0.15f,
            top + cellSize * 0.15f,
            left + cellSize * 0.85f,
            top + cellSize * 0.85f,
            foodPaint
        )
    }

    private fun step() {
        direction = pendingDirection
        val head = snake.first()
        val next = Point(head.x + direction.dx, head.y + direction.dy)

        if (next.x < 0 || next.y < 0 || next.x >= gridSize || next.y >= gridSize) {
            gameOver()
            return
        }
        if (snake.any { it == next }) {
            gameOver()
            return
        }

        snake.addFirst(next)
        if (next == food) {
            score += 1
            listener?.onScoreChanged(score)
            placeFood()
        } else {
            snake.removeLast()
        }
    }

    private fun setDirection(newDirection: Direction) {
        if (newDirection.isOpposite(direction)) return
        pendingDirection = newDirection
    }

    private fun resetGame() {
        snake.clear()
        val mid = gridSize / 2
        snake.add(Point(mid, mid))
        snake.add(Point(mid - 1, mid))
        snake.add(Point(mid - 2, mid))
        direction = Direction.RIGHT
        pendingDirection = direction
        score = 0
        listener?.onScoreChanged(score)
        placeFood()
        invalidate()
    }

    private fun placeFood() {
        val available = mutableListOf<Point>()
        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                val candidate = Point(x, y)
                if (snake.none { it == candidate }) {
                    available.add(candidate)
                }
            }
        }
        food = if (available.isNotEmpty()) {
            available[Random.nextInt(available.size)]
        } else {
            Point(0, 0)
        }
    }

    private fun gameOver() {
        pause()
        isGameOver = true
        listener?.onGameOver(score)
        invalidate()
    }

    enum class Direction(val dx: Int, val dy: Int) {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        fun isOpposite(other: Direction): Boolean {
            return dx + other.dx == 0 && dy + other.dy == 0
        }
    }
}
