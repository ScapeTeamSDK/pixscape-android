package com.scape.capture.gles

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Tweaked version of Drawable2d that rescales the texture coordinates to provide a
 * "zoom" effect.
 */
class ScaledDrawable2d(shape: Drawable2d.Prefab) : Drawable2d(shape) {

    private var mScale = 1.0f
    private var mRecalculate: Boolean = true

    private val mTweakedTexCoordArray: FloatBuffer by lazy {
        val bb = ByteBuffer.allocateDirect(backingTexCoordArray.capacity() * SIZEOF_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        return@lazy bb.asFloatBuffer()
    }

    /**
     * Returns the array of texture coordinates.  The first time this is called, we generate
     * a modified version of the array from the parent class.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    override val texCoordArray: FloatBuffer
        get() {
            if (mRecalculate) {
                val fb = mTweakedTexCoordArray
                val scale = mScale

                val parentBuf = backingTexCoordArray
                val count = parentBuf.capacity()

                for (i in 0 until count) {
                    var fl = parentBuf.get(i)
                    fl = (fl - 0.5f) * scale + 0.5f
                    fb.put(i, fl)
                }

                mRecalculate = false
            }

            return mTweakedTexCoordArray
        }

    /**
     * Set the scale factor.
     */
    fun setScale(scale: Float) {
        if (scale < 0.0f || scale > 1.0f) {
            throw RuntimeException("invalid scale " + scale)
        }
        mScale = scale
        mRecalculate = true
    }

    companion object {
        private val SIZEOF_FLOAT = 4
    }
}
