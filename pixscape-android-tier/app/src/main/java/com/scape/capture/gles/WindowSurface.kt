package com.scape.capture.gles

import android.view.Surface

/**
 * Recordable EGL window surface.
 *
 *
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
class WindowSurface(eglCore: EglCore, private var mSurface: Surface?, private val mReleaseSurface: Boolean) : EglSurfaceBase(eglCore) {

    init {
        mSurface?.let {
            createWindowSurface(it)
        }
    }

    /**
     * Releases any resources associated with the EGL surface (and, if configured to do so,
     * with the Surface as well).
     *
     *
     * Does not require that the surface's EGL context be current.
     */
    fun release() {
        releaseEglSurface()
        if (mReleaseSurface) {
            mSurface?.release()
        }
        mSurface = null
    }
}
