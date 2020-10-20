package com.anwesh.uiprojects.bisideconeview

/**
 * Created by anweshmishra on 21/10/20.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF

val parts : Int = 6
val scGap : Float = 0.02f / parts
val strokeFactor : Float = 90f
val sizeFactor : Float = 3.4f
val delay : Long = 20
val deg : Float = 90f
val backColor : Int = Color.parseColor("#BDBDBD")
val colors : Array<Int> = arrayOf(
        "#F44336",
        "#4CAF50",
        "#2196F3",
        "#FF9800",
        "#3F51B5"
).map {
    Color.parseColor(it)
}.toTypedArray()

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawConeFillPath(scale : Float, r : Float, paint : Paint) {
    paint.style = Paint.Style.FILL
    save()
    val path : Path = Path()
    path.moveTo(0f, 0f)
    path.lineTo(-r, r)
    path.arcTo(RectF(-2 * r, -r, 0f, r), deg * 2, deg * 2, false)
    path.lineTo(0f, 0f)
    clipPath(path)
    drawRect(RectF(-2 * r * scale, -r, 0f, r), paint)
    restore()
}

fun Canvas.drawBiSideCone(scale : Float, w : Float, h : Float, paint : Paint) {
    val sf : Float = scale.sinify()
    val sf2 : Float = sf.divideScale(1, parts)
    val sf4 : Float = sf.divideScale(3, parts)
    val sf5 : Float = sf.divideScale(4, parts)
    val size : Float = Math.min(w, h) / sizeFactor
    val r : Float = size / 2
    save()
    translate(w / 2, h / 2)
    rotate(deg * sf5)
    for (j in 0..1) {
        save()
        scale(1f - 2 * j, 1f)
        paint.style = Paint.Style.STROKE
        for (i in 0..1) {
            val sfi : Float = sf.divideScale(i * 2, parts)
            drawLine(-r * sfi, r * (1f - 2 * i) * sfi, 0f, 0f, paint)
        }
        drawArc(RectF(-2 * r, -r, 0f, r), deg * 2, deg * 2 * sf2, false, paint)
        drawConeFillPath(sf4, r, paint)
        restore()
    }
    restore()
}

fun Canvas.drawBSCNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawBiSideCone(scale, w, h, paint)
}

class BiSideConeView(ctx : Context) : View(ctx) {

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class BSCNode(var i : Int, val state : State = State()) {

        private var next : BSCNode? = null
        private var prev : BSCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = BSCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawBSCNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BSCNode {
            var curr : BSCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BiSideCone(var i : Int) {

        private var curr : BSCNode = BSCNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BiSideConeView) {

        private val animator : Animator = Animator(view)
        private val bsc : BiSideCone = BiSideCone(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            bsc.draw(canvas, paint)
            animator.animate {
                bsc.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            bsc.startUpdating {
                animator.start()
            }
        }
    }
}