@file:Suppress("DEPRECATION")

package com.scape.capture.preview

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.scape.capture.extensions.CameraExtensions
import com.scape.capture.extensions.setCameraPreviewSize
import com.scape.capture.extensions.setFocusMode
import com.scape.capture.gles.*
import com.scape.capture.gles.shader.ShaderProgram
import com.scape.capture.graphics.Dimens
import com.scape.capture.graphics.XY
import com.scape.pixscape.utils.CameraIntrinsics
import com.scape.capture.utils.CameraUtils
import com.scape.pixscape.PixscapeApplication
import com.scape.scapekit.setByteBuffer
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.experimental.and

internal class CapturePreviewObserver(private val surfaceView: SurfaceView,
                                      private val deviceRotation: Int) : LifecycleObserver,
                                                                        SurfaceHolder.Callback,
                                                                        SurfaceTexture.OnFrameAvailableListener,
                                                                        Camera.PreviewCallback{
    private var luma: ByteBuffer? = null
    private var callbackBuffer: ByteArray = ByteArray(1920*1080*3)
    private val TAG = CapturePreviewObserver::class.java.simpleName

    companion object {
        private var prevSurfaceHolder: SurfaceHolder? = null
    }

    private val rectDrawable = ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE)
    private val rect = Sprite2d(rectDrawable)

    private var glExecutor: ExecutorService? = null
    private var frameProcessingExecutor: ExecutorService? = null
    private lateinit var eglCore: EglCore

    private var camera: Camera? = null
    private var cameraRotation: Int = -1
    private var cameraIntrinsics: CameraIntrinsics? = null
    private var isPreviewHidden = true

    private var windowSurface: WindowSurface? = null
    private var texProgram: ShaderProgram? = null

    private var cameraTexture: SurfaceTexture? = null
    private lateinit var windowSurfaceDimens: Dimens
    private lateinit var cameraPreviewDimens: Dimens

    private lateinit var centerPos: XY

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun setup() {
        Log.d(TAG, "setup@onCreate")
        surfaceView.holder.addCallback(this)
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun show() {
        Log.d(TAG, "show@onResume")

        if(!isPreviewHidden) return

        isPreviewHidden = false

        glExecutor = Executors.newSingleThreadExecutor()
        frameProcessingExecutor = Executors.newSingleThreadExecutor()

        glExecutor?.execute {
            Log.d(TAG, "Init")
            // Init
            eglCore = EglCore()
            CameraExtensions.openCamera(deviceRotation).let { (cam, rotation) ->
                camera = cam
                camera?.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                cameraRotation = rotation
            }
        }

        prevSurfaceHolder?.let {
            Log.d(TAG, "Previous Surface Available")

            glExecutor?.execute {
                it.surfaceAvailable(false)
            }
        }
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun hide() {
        Log.d(TAG, "hide@onPause")

        if(isPreviewHidden) return

        isPreviewHidden = true

        frameProcessingExecutor?.checkShutdown()?.execute {
            Log.d(TAG, "execute reset frame processing resources")

            cameraIntrinsics = null
            luma = null
            callbackBuffer = ByteArray(1920*1080*3)

            Log.d(TAG, "execute reset frame processing resources done")
        }
        frameProcessingExecutor?.shutdown()

        try {
            glExecutor?.checkShutdown()?.execute {
                try {
                    Log.d(TAG, "execute release camera")

                    releaseCamera()
                    releaseGL()
                    eglCore.release()

                    Log.d(TAG, "execute release camera done")
                } catch (t: Throwable) {
                    Log.e(TAG, t.toString())
                }
            }
            glExecutor?.shutdown()
        } catch (t: Throwable) {
            Log.e(TAG, t.toString())
        }
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.d(TAG, "destroy@onDestroy")
    }

    @WorkerThread
    private fun releaseCamera() {
        camera?.let {
            it.stopPreview()
            it.release()
            camera = null
            Log.d(TAG, "Camera Release Done")
        }
    }

    @MainThread
    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceCreated")

        prevSurfaceHolder?.let {
            throw RuntimeException("SurfaceHolder already created!")
        }

        prevSurfaceHolder = holder

        glExecutor?.checkShutdown()?.execute {
            holder?.surfaceAvailable(true)
        }

        // TODO : Log corner case(see grafika)
    }

    @MainThread
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged, width : $width, height : $height")

        glExecutor?.checkShutdown()?.execute {
            finishSurfaceSetup(Dimens(width, height))
        }
    }

    // Runs on arbitary thread(camera)
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glExecutor?.checkShutdown()?.execute {
            cameraTexture?.updateTexImage()
            draw()
        }
    }

    // Runs on arbitary thread(camera)
    override fun onPreviewFrame(yuvArray: ByteArray?, camera: Camera?) {
        camera?.addCallbackBuffer(callbackBuffer)

        try {
            frameProcessingExecutor?.checkShutdown()?.execute {
                val intrinsics = cameraIntrinsics ?: return@execute

                val width = this.cameraPreviewDimens.width
                val height = this.cameraPreviewDimens.height

                decodeYUV(callbackBuffer, width, height)

                val scapeClient = PixscapeApplication.sharedInstance?.scapeClient
                scapeClient?.scapeSession?.setCameraIntrinsics(intrinsics.focalLengthX,
                                                               intrinsics.focalLengthY,
                                                               intrinsics.principalPointX,
                                                               intrinsics.principalPointY)
                scapeClient?.scapeSession?.setByteBuffer(luma, width, height)
            }
        } catch (t: Throwable) {
            Log.e(TAG, t.toString())
        }
    }

    @Synchronized
    private fun decodeYUV(yuv: ByteArray, width: Int, height: Int) {
        val size = width * height

        if(luma == null) {
            luma = ByteBuffer.allocateDirect(size)
        }

        for (pixelPointer in 0 until size) {
            luma?.put(pixelPointer, yuv[pixelPointer] and 0xFF.toByte())
        }
    }

    @WorkerThread
    private fun draw() {
        GlUtil.checkGlError("Draw Start!")

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val (width, height) = windowSurfaceDimens

        val displayProjectionMatrix = FloatArray(16)
        Matrix.orthoM(displayProjectionMatrix, 0, 0F, width.toFloat(), 0F, height.toFloat(), -1F, 1F)

        GLES20.glViewport(0, 0, width, height)

        texProgram?.let { program ->
            rect.draw(program, displayProjectionMatrix)
        }

        checkNotNull(windowSurface, { Log.e(TAG, "WindowSurface is Null@draw") })
                .swapBuffers()

        GlUtil.checkGlError("Draw Done!")
    }

    @MainThread
    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "Surface Destroyed")

        glExecutor?.checkShutdown()?.execute {
            releaseGL()
        }
        prevSurfaceHolder = null
    }

    @WorkerThread
    private fun releaseGL() {
        GlUtil.checkGlError("releaseGl Start!")

        cameraTexture?.let {
            it.release()
            cameraTexture = null
        }

        windowSurface?.let {
            it.release()
            windowSurface = null
        }

        texProgram?.let {
            it.release()
            texProgram = null
        }

        GlUtil.checkGlError("releaseGl Done!")

        eglCore.makeNothingCurrent()
    }

    @WorkerThread
    private fun finishSurfaceSetup(windowDimens: Dimens) {
        windowSurfaceDimens = windowDimens
        cameraPreviewDimens = checkNotNull(camera, { Log.e(TAG, "Camera is Null@finishSurfaceSetup") })
                                .setCameraPreviewSize(windowSurfaceDimens, cameraRotation)

        Log.d(TAG, "finishSurfaceSetup size= $windowSurfaceDimens, Camera Preview Size : $cameraPreviewDimens")

        val (windowWidth, windowHeight) = windowSurfaceDimens

        centerPos = XY(windowWidth / 2F, windowHeight / 2F)

        updateGeometry()

        Log.d(TAG, "Start Camera Preview")

        checkNotNull(camera, { Log.e(TAG, "Camera is Null@finishSurfaceSetup") }).let {
            // Checked Exception Ignored

            //setPreviewCallbackWithBuffer needs to be called before startPreview(),
            // because setPreviewCallbackWithBuffer needs to specify a byte array as a buffer for previewing frame data,
            // so we need to call addCallbackBuffer before setPreviewCallbackWithBuffer, so that the data of onPreviewFrame has value.
            it.addCallbackBuffer(callbackBuffer)
            it.setPreviewCallbackWithBuffer(this)
            it.setPreviewTexture(cameraTexture)
            it.startPreview()

            cameraIntrinsics = CameraUtils.getCameraIntrinsics(it.parameters,
                                                               cameraPreviewDimens.width,
                                                               cameraPreviewDimens.height)
        }
    }

    private fun setRectScaleForDefaultDimensions() {
        val (windowWidth, windowHeight) = windowSurfaceDimens
        val smallDim = Math.min(windowWidth, windowHeight).toFloat()

        val cameraAspect = cameraPreviewDimens.width.toFloat() / cameraPreviewDimens.height.toFloat()
        // Camera aspect is assumed smaller than window aspect
        val newWidth = windowWidth//Math.round(smallDim)
        val newHeight =  windowHeight//Math.round(smallDim * cameraAspect)

        Log.d(TAG, "setRectScaleForDefaultDimensions with New Size: $newWidth x $newHeight")

        rect.setScale(newWidth.toFloat(), -newHeight.toFloat())
    }

    private fun setRectScaleForRotatedDimensions() {
        val (windowWidth, windowHeight) = windowSurfaceDimens
        val smallDim = Math.min(windowWidth, windowHeight).toFloat()

        val cameraAspect = cameraPreviewDimens.width.toFloat() / cameraPreviewDimens.height.toFloat()
        // Camera aspect is assumed smaller than window aspect
        val newWidth = windowWidth//Math.round(smallDim)
        val newHeight =  windowHeight//Math.round(smallDim * cameraAspect)

        Log.d(TAG, "setRectScaleForRotatedDimensions with New Size: $newWidth x $newHeight")

        rect.setScale(-newHeight.toFloat(), -newWidth.toFloat())
    }

    @WorkerThread
    private fun updateGeometry() {
        when (cameraRotation) {
            0, 180 -> {
                setRectScaleForDefaultDimensions()
            }
            90, 270 -> {
                setRectScaleForRotatedDimensions()
            }
            else -> {
                Log.d(TAG, "Wrong rotation : $cameraRotation")
                setRectScaleForDefaultDimensions()
            }
        }

        rect.setRotation(cameraRotation.toFloat())
        rect.setPosition(centerPos.x, centerPos.y)

        rectDrawable.setScale(1F)
    }

    @WorkerThread
    private fun SurfaceHolder.surfaceAvailable(newSurface: Boolean) {
        val surface: Surface = this.surface

        windowSurface = WindowSurface(eglCore, surface, false).apply {
            makeCurrent()
        }

        texProgram = ShaderProgram()

        val textureId = ShaderProgram.createTextureObject()
        textureId.let { id ->
            cameraTexture = SurfaceTexture(id)
            rect.setTexture(id)
        }

        if (!newSurface) {
            Log.d(TAG, "not a new surface")
            checkNotNull(windowSurface, { Log.e(TAG, "windowSurface is Null@surfaceAvailable") }).let {
                finishSurfaceSetup(Dimens(it.width, it.height))
            }
        }

        cameraTexture?.setOnFrameAvailableListener(this@CapturePreviewObserver)
    }

    @MainThread
    private fun ExecutorService.checkShutdown(): ExecutorService? {
        if (this.isShutdown) {
            Log.e(TAG, "already shutdown!")
            return null
        }
        return this
    }
}
