package com.scape.capture.gles.shader

import com.scape.capture.gles.shader.GLSLVars.aPosition
import com.scape.capture.gles.shader.GLSLVars.aTextureCoord
import com.scape.capture.gles.shader.GLSLVars.sTexture
import com.scape.capture.gles.shader.GLSLVars.uMVPMatrix
import com.scape.capture.gles.shader.GLSLVars.uTexMatrix
import com.scape.capture.gles.shader.GLSLVars.vTextureCoord

/**
 * Shaders for OpenGL drawing.
 * Uses Kotlin's String Templates for readability.
 *
 *
 */
internal object GLSLVars {

    const val uMVPMatrix = "uMVPMatrix"
    const val uTexMatrix = "uTexMatrix"
    const val aPosition = "aPosition"
    const val aTextureCoord = "aTextureCoord"

    const val vTextureCoord = "vTextureCoord"

    const val sTexture = "sTexture"
}

internal enum class Shader(val code: String) {

    VERTEX("uniform mat4 $uMVPMatrix;\n" +
            "uniform mat4 $uTexMatrix;\n" +
            "attribute vec4 $aPosition;\n" +
            "attribute vec4 $aTextureCoord;\n" +
            "varying vec2 $vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = $uMVPMatrix * $aPosition;\n" +
            "    $vTextureCoord = ($uTexMatrix * $aTextureCoord).xy;\n" +
            "}\n"
    ),
    FRAGMENT_EXT(
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 $vTextureCoord;\n" +
                    "uniform samplerExternalOES $sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D($sTexture, $vTextureCoord);\n" +
                    "}\n"
    )
}