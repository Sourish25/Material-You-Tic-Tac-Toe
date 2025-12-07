package com.example.tiktac

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color
)

@Composable
fun ConfettiView() {
    val particles = remember {
        List(100) {
            Particle(
                x = Random.nextFloat(),
                y = -0.1f, // Start above screen
                vx = Random.nextFloat() * 0.01f - 0.005f,
                vy = Random.nextFloat() * 0.02f + 0.01f,
                color = Color(Random.nextLong(0xFFFFFFFF))
            )
        }
    }
    
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(1f, animationSpec = tween(3000, easing = LinearEasing))
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = time.value
        
        particles.forEach { p ->
            // Physics simulation approximated by time
            val cx = (p.x + p.vx * t * 100) % 1.0f * w
            val cy = (p.y + p.vy * t * 100 + 0.5f * t * t) * h // Gravity
            
            drawCircle(
                color = p.color,
                radius = 10f,
                center = Offset(cx, cy)
            )
        }
    }
}
