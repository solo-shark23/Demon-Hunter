package com.example.snake

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.snake.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SnakeGameView.GameEventListener {
    private lateinit var binding: ActivityMainBinding
    private var highScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        highScore = loadHighScore()
        updateScore(0)
        updateHighScore(highScore)

        binding.gameView.setGameEventListener(this)

        binding.startPauseButton.setOnClickListener {
            if (binding.gameView.isRunning()) {
                binding.gameView.pause()
                binding.statusText.setText(R.string.status_paused)
                binding.startPauseButton.setText(R.string.start)
            } else {
                binding.gameView.resume()
                binding.statusText.setText(R.string.status_running)
                binding.startPauseButton.setText(R.string.pause)
            }
        }

        binding.restartButton.setOnClickListener {
            binding.gameView.startNewGame()
            binding.statusText.setText(R.string.status_running)
            binding.startPauseButton.setText(R.string.pause)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!binding.gameView.isRunning()) {
            binding.statusText.setText(R.string.status_ready)
            binding.startPauseButton.setText(R.string.start)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.pause()
        binding.statusText.setText(R.string.status_paused)
        binding.startPauseButton.setText(R.string.start)
    }

    override fun onScoreChanged(score: Int) {
        updateScore(score)
    }

    override fun onGameOver(score: Int) {
        if (score > highScore) {
            highScore = score
            saveHighScore(highScore)
            updateHighScore(highScore)
        }
        binding.statusText.setText(R.string.status_game_over)
        binding.startPauseButton.setText(R.string.start)
    }

    private fun updateScore(score: Int) {
        binding.scoreText.text = getString(R.string.score_label, score)
    }

    private fun updateHighScore(score: Int) {
        binding.highScoreText.text = getString(R.string.high_score_label, score)
    }

    private fun loadHighScore(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HIGH_SCORE, 0)
    }

    private fun saveHighScore(score: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    companion object {
        private const val PREFS_NAME = "snake_prefs"
        private const val KEY_HIGH_SCORE = "high_score"
    }
}
