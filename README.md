> 之前看到qq 的图片发送效果很酷炫，很吸引人，不过现在这个效果好像没有了。试了几次,决定试试实现。大致想了下，实现效果还不错

##### 需要实现的效果
一图胜千言，看图如下：
![20181229_112329.gif](https://upload-images.jianshu.io/upload_images/1394860-82903e0a180898d6.gif?imageMogr2/auto-orient/strip)
##### 怎样实现呢？
首先从图中看分两部分，一部分是进度条带光晕得效果。第二部分是圆圈扩散到整个图片，到显示完整图片的过程。接下来一步一步跟着代码分析实现。

1.绘制的范围包括图片显示都在圆角矩形内，所以首先要裁剪canvas到圆角矩形。
```
        val path = Path()
        canvas.save()
        path.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat())
                , round, round, Path.Direction.CW)
        canvas.clipPath(path)
```

先保存画布，save()到最后要canvas.restore().因为显示图片，可以有两种选择，第一种：自己绘制图片，通过drawable得方式。第二种：继承ImageView 同时还可以获得ImageView提供的各种属性，scaleType之类。本质上ImageView也是通Drawable实现。IamgeView还帮我们处理了测量的狂傲，所以有什么理由不选择继承呢。然后绘制图片只有简单一行代码，再裁剪画布之后：
```
        super.onDraw(canvas)
```
2.绘制背景
可以看到效果图，图片在黑色半透明的下方。并且在最后显示出来。着一点都是跟canvas绘制背景相关的。不多说，先设置画笔。
```
    private var paint: Paint = Paint()
    paint.isAntiAlias = true
    paint.color = getColor(R.color.bantouming)

```
背景怎么绘制，直接通过canvas.drawPaint方法即可实现。把paint的颜色绘制到整个画布。并且再图片后边绘制，所以在上方。
```
                canvas.drawPaint(paint)
```
3.绘制进度
这里根据每一阶段状态的不同，通过三个状态值区分：
```
    companion object {
        private const val READY = 1
        private const val PROGRESS = 2
        private const val FINISH = 3
    }
```

为了方便的绘制，并且整个view是对称的。所以坐标点移动到view中心，非常有利于实现。
```
     canvas.save()
        canvas.translate(width / 2f, height / 2f)
```
当然最后别忘了canvas.restore().
在中间都好说了。先看百分比的实现。主要是drawText()的x,y比较不好掌握，不过搞明白基线之类的，就没问题了。

先看百分比的paint
```
    private val textPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            textSize = dp2px(16f).toFloat()
            color = getColor(R.color.main_gray)
        }
    }
```

接下来绘制，这行代码可能比较长。需要优化，到这里就先别吐槽。有点偷懒。
可以看到根据文字的宽，高。来绘制的。高这里需要额外注意
```
textPaint.textHeight().div(2) - textPaint.descent()
```
需要减去textPiat.descent(),如果不减绘制会偏下。
```
  val text = "${progress}%"
                canvas.drawText(text, 0 - textPaint.measureText(text).div(2), textPaint.textHeight().div(2) - textPaint.descent(), textPaint)
```

4.绘制光晕
这算是实现比较疑难的地方。要注意3个地方。1.光晕的实现 2.呼吸效果 3.PorterDuffXmode 使用。

先看呼吸效果如何实现。可能简单的想到的是通过圆环实现。但这样挺麻烦的，如果通过两个圆叠加，并设置paint.xfermode(PorterDuff.Mode.DST_OUT),可实现把内部圆裁剪掉。关于怎么使用，请看之前的关于xfermode的文章。光晕的实现需要依赖shader,这里通过RadilGradient 实现。具体用法也可看之前文章。
设置shader
```
     paint.setShader(RadialGradient(0f, 0f, outRadius
                        , intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.WHITE, Color.TRANSPARENT)
                        , floatArrayOf(0.1f, 0.4f, 0.8f, 1f), Shader.TileMode.CLAMP))
```

接下来的呼吸效果通过动画设置大圆半径的变化来实现。
```
                canvas.drawCircle(0f, 0f, innRaduus + (outRadius - innRaduus) * animatorValue, paint)

```

完整代码如下
```
             canvas.drawCircle(0f, 0f, innRaduus + (outRadius - innRaduus) * animatorValue, paint)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
                paint.setShader(null)
                paint.color = Color.WHITE
                canvas.drawCircle(0f, 0f, innRaduus, paint)
                paint.setXfermode(null)
```

如果仅仅是这样那么绘制出来中间喝一个黑洞。因为背景是透明的。所以这时候在绘制之前需要canvas.savlayer。如下
```
                val sc = canvas.saveLayer(-outRadius, -outRadius, outRadius, outRadius, paint, Canvas.ALL_SAVE_FLAG)

```
保存的范围包括大圆小圆
最后要restore
```
                canvas.restoreToCount(sc)

```
再加上animatorValue 从0到1的动画就完成PROGRES阶段的动画了。

5.绘制FINISH动画，揭露图片效果
同样这里也需要使用PorterDuff.Mode.DST_OUT，不过这里需要的是对整个圆角画布范围进行操作。DST 是canvas.drawPaint绘制的背景。SRC 是一整个圆角矩形对角线的一半为最大半径，从PROGRES 状态大圆的半径的范围，到最大范围的动画变化。如下：
```
   val sc = canvas.saveLayer(-width.div(2f), -height.div(2f), width.div(2f), height.div(2f), paint, Canvas.ALL_SAVE_FLAG)
                canvas.drawPaint(paint)
                val maxRadius = Math.sqrt(Math.pow(width.toDouble(), 2.0) + Math.pow(height.toDouble(), 2.0)).div(2)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                paint.color = Color.WHITE
                canvas.drawCircle(0f, 0f, (outRadius + (maxRadius - outRadius) * finishAnimValue).toFloat(), paint)
                paint.xfermode = null
                canvas.restoreToCount(sc)
```

6.动画的使用，与交替。这一点比较简单的ValueAnimator的使用，设置属性，Listener即可。
