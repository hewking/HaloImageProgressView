package com.hewking.halo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import com.example.library.BuildConfig
import com.example.library.R

/**
 * Created by hewking on 2017/11/26.
 */
class HaloProgressView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

    companion object {
        private const val READY = 1
        private const val PROGRESS = 2
        private const val FINISH = 3
    }

    private var status: Int = PROGRESS

    var outRadius = dp2px(50f).toFloat()
    var innRaduus = dp2px(40f).toFloat()
    var round = outRadius - innRaduus

    var progress: Int = 0
        set(value) {
            field = value
            postInvalidate()
        }

    private var finishAnimValue = 0f

    private var paint: Paint = Paint()
    private val textPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            textSize = dp2px(16f).toFloat()
            color = getColor(R.color.main_gray)
        }
    }

    private val path by lazy {
        Path()
    }

    init {
        val typedArray = context?.obtainStyledAttributes(attrs,R.styleable.HaloProgressView)
        outRadius = typedArray?.getDimension(R.styleable.HaloProgressView_outRadius,outRadius)?:outRadius
        innRaduus = typedArray?.getDimension(R.styleable.HaloProgressView_innRaduus,innRaduus)?:innRaduus
        progress = typedArray?.getInteger(R.styleable.HaloProgressView_progress,progress)?:progress
        typedArray?.recycle()
        paint.isAntiAlias = true
        debugProgressStart()
    }

    private fun debugProgressStart() {
        if (BuildConfig.DEBUG) {
            status = PROGRESS
            ObjectAnimator.ofInt(this, "progress", 0, 100).apply {
                duration = 3000
                interpolator = BounceInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        status = FINISH
                        startFinishAnim()
                    }
                })
                start()
            }
        }
    }

    private fun startFinishAnim() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                finishAnimValue = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    if (BuildConfig.DEBUG) {
                        debugProgressStart()
                    }
                }
            })
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        anim?.start()

    }

    private var animatorValue: Float = 0f

    private val anim by lazy {
        ObjectAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                animatorValue = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        canvas.save()
        path.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat())
                , round, round, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
        paint.color = getColor(R.color.bantouming)
//        canvas.drawColor(getColor(R.color.top_forward_background_color))
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        when (status) {
            PROGRESS -> {
                canvas.drawPaint(paint)
                val sc = canvas.saveLayer(-outRadius, -outRadius, outRadius, outRadius, paint, Canvas.ALL_SAVE_FLAG)
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.setShader(RadialGradient(0f, 0f, outRadius
                        , intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.WHITE, Color.TRANSPARENT)
                        , floatArrayOf(0.1f, 0.4f, 0.8f, 1f), Shader.TileMode.CLAMP))
                paint.alpha = (animatorValue * 255).toInt()
                canvas.drawCircle(0f, 0f, innRaduus + (outRadius - innRaduus) * animatorValue, paint)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
                paint.setShader(null)
                paint.color = Color.WHITE
                canvas.drawCircle(0f, 0f, innRaduus, paint)
                paint.setXfermode(null)

                // draw progress
                val text = "${progress}%"
                canvas.drawText(text, 0 - textPaint.measureText(text).div(2), textPaint.textHeight().div(2) - textPaint.descent(), textPaint)

                canvas.restoreToCount(sc)

            }

            FINISH -> {
                val sc = canvas.saveLayer(-width.div(2f), -height.div(2f), width.div(2f), height.div(2f), paint, Canvas.ALL_SAVE_FLAG)
                canvas.drawPaint(paint)
                val maxRadius = Math.sqrt(Math.pow(width.toDouble(), 2.0) + Math.pow(height.toDouble(), 2.0)).div(2)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                paint.color = Color.WHITE
                canvas.drawCircle(0f, 0f, (outRadius + (maxRadius - outRadius) * finishAnimValue).toFloat(), paint)
                paint.xfermode = null
                canvas.restoreToCount(sc)
            }
        }
        canvas.restore()
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        anim?.end()
    }

}