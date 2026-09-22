package com.collectes.reset

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Icône émulateur : pose un drapeau dans le stockage privé de l’app.
 * tools/watch-collectes-reset.ps1 le détecte (adb root) et fait uninstall + reinstall.
 */
class ResetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val label = TextView(this).apply {
            text = "Reset Collectes…"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
        }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.parseColor("#C62828"))
                addView(
                    label,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        )

        Thread {
            val flag = File(filesDir, FLAG_NAME)
            val ok = runCatching {
                flag.writeText(System.currentTimeMillis().toString())
            }.isSuccess

            Handler(Looper.getMainLooper()).post {
                if (ok) {
                    toast("Reset demandé — laisse le watcher PC tourner")
                } else {
                    toast("Échec. Lance tools/reset-collectes.ps1 depuis le PC")
                }
                finish()
            }
        }.start()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val FLAG_NAME = "collectes-reset.flag"
    }
}
