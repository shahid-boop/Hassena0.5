package com.example.ui.features.visual_lens

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/** A genuine OpenGL ES 2.0 sphere renderer: perspective projection, depth buffer,
 * normals, diffuse/specular lighting, Fresnel rim light and continuous rotation. */
@Composable
fun OpenGlLensPreview(
    modifier: Modifier = Modifier,
    selectedScene: Int = 0,
    selectedEffect: Int = 0,
    modelMesh: FloatArray? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceView = remember(context) { HologramSurfaceView(context) }
    LaunchedEffect(surfaceView, selectedScene, selectedEffect, modelMesh) {
        surfaceView.configure(selectedScene, selectedEffect, modelMesh)
    }
    DisposableEffect(surfaceView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> surfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // A composable can be entered while its owner is already resumed.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            surfaceView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceView.onPause()
        }
    }
    Box(modifier = modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp))) {
        AndroidView(
            factory = { surfaceView },
            modifier = Modifier.fillMaxWidth().height(210.dp)
        )
        Text(
            text = "OPENGL ES • 3D LIGHTING • LIVE PREVIEW",
            color = Color(0xFF8BEFFF),
            modifier = Modifier.align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color(0xAA07111F), RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

private class HologramSurfaceView(context: Context) : GLSurfaceView(context) {
    private val hologramRenderer = HologramRenderer()
    fun configure(scene: Int, effect: Int, mesh: FloatArray?) {
        queueEvent { hologramRenderer.configure(scene, effect, mesh) }
    }
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 24, 8)
        setRenderer(hologramRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }
}

private class HologramRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private var width = 1
    private var height = 1
    private var angle = 0f
    @Volatile private var sceneMode = 0
    @Volatile private var effectMode = 0
    @Volatile private var customMesh: FloatArray? = null

    fun configure(scene: Int, effect: Int, mesh: FloatArray?) {
        sceneMode = scene.coerceIn(0, 3)
        effectMode = effect.coerceIn(0, 3)
        customMesh = mesh
        if (mesh != null) {
            vertexCount = mesh.size / 6
            vertexBuffer = ByteBuffer.allocateDirect(mesh.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(mesh); position(0) }
        } else {
            val data = createSphere(1f, 48, 64)
            vertexCount = data.size / 6
            vertexBuffer = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }
        }
    }
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mv = FloatArray(16)
    private val mvp = FloatArray(16)
    private val normalMatrix = FloatArray(16)

    private val vertexShader = """
        uniform mat4 uMvp;
        uniform mat4 uModel;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        varying vec3 vNormal;
        varying vec3 vPosition;
        void main() {
          vec4 world = uModel * vec4(aPosition, 1.0);
          vPosition = world.xyz;
          vNormal = normalize(mat3(uModel) * aNormal);
          gl_Position = uMvp * vec4(aPosition, 1.0);
        }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        varying vec3 vNormal;
        varying vec3 vPosition;
        uniform vec3 uCamera;
        uniform float uScene;
        uniform float uEffect;
        uniform float uMetallic;
        uniform float uRoughness;
        void main() {
          vec3 N = normalize(vNormal);
          vec3 V = normalize(uCamera - vPosition);
          vec3 L = normalize(vec3(-0.55, 0.75, 1.8));
          vec3 H = normalize(L + V);
          float NoL = max(dot(N, L), 0.0);
          float NoV = max(dot(N, V), 0.001);
          float NoH = max(dot(N, H), 0.0);
          float VoH = max(dot(V, H), 0.0);
          float rough = clamp(uRoughness, 0.08, 0.95);
          float a = rough * rough;
          float a2 = a * a;
          float denom = max(3.14159265 * pow(NoH * NoH * (a2 - 1.0) + 1.0, 2.0), 0.001);
          float D = a2 / denom;
          float k = pow(rough + 1.0, 2.0) / 8.0;
          float Gv = NoV / (NoV * (1.0 - k) + k);
          float Gl = NoL / (NoL * (1.0 - k) + k);
          float G = Gv * Gl;
          float rim = pow(1.0 - NoV, 2.2);
          float bands = 0.5 + 0.5 * sin(vPosition.y * 30.0 + vPosition.x * 8.0);
          vec3 cyan = vec3(0.12, 0.88, 1.0);
          vec3 violet = vec3(0.40, 0.12, 0.95);
          vec3 base = mix(vec3(0.025, 0.20, 0.42), vec3(0.30, 0.055, 0.62), bands * 0.48);
          if (uScene > 0.5 && uScene < 1.5) base = mix(vec3(0.08, 0.12, 0.16), vec3(0.65, 0.78, 0.90), NoL);
          else if (uScene > 1.5 && uScene < 2.5) base = mix(vec3(0.02, 0.30, 0.22), vec3(0.10, 0.95, 0.65), bands * 0.55);
          else if (uScene > 2.5) base = mix(vec3(0.32, 0.04, 0.08), vec3(1.0, 0.36, 0.08), bands * 0.55);
          // Schlick Fresnel + GGX-style microfacet highlight. The environment is a
          // procedural studio approximation; this is not a full HDR image sampler.
          vec3 F0 = mix(vec3(0.04), base, clamp(uMetallic, 0.0, 1.0));
          vec3 F = F0 + (1.0 - F0) * pow(1.0 - VoH, 5.0);
          vec3 specular = (D * G * F) / max(4.0 * NoV * NoL, 0.001);
          vec3 R = reflect(-V, N);
          float envBand = 0.5 + 0.5 * sin(R.y * 5.0 + R.x * 2.5);
          vec3 studioEnv = mix(vec3(0.025, 0.055, 0.11), mix(cyan, violet, envBand), pow(max(R.y, 0.0), 1.5));
          studioEnv += vec3(0.42, 0.48, 0.62) * pow(max(dot(R, normalize(vec3(-0.6, 0.8, 0.5))), 0.0), mix(48.0, 5.0, rough));
          vec3 diffuse = (1.0 - clamp(uMetallic, 0.0, 1.0)) * base * (0.18 + NoL * 0.82) / 3.14159265;
          vec3 color = diffuse + specular * NoL * 2.2 + studioEnv * (0.12 + 0.62 * clamp(uMetallic, 0.0, 1.0));
          color += cyan * rim * (uEffect > 1.5 ? 1.45 : 0.9);
          color += violet * pow(rim, 1.7) * (uEffect > 2.5 ? 0.9 : 0.5);
          if (uEffect > 0.5 && uEffect < 1.5) color += vec3(0.18,0.42,1.0) * pow(max(dot(N,H),0.0), 0.4) * 0.35;
          if (uEffect > 1.5 && uEffect < 2.5) color *= 0.84 + 0.16 * sin(vPosition.y * 48.0);
          // Bloom-style emissive shoulder and tone mapping for GLES2 devices.
          if (uEffect > 2.5) color += max(color - vec3(0.35), vec3(0.0)) * 0.8;
          color = color / (vec3(1.0) + max(color, vec3(0.0)));
          gl_FragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.012f, 0.025f, 0.065f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        // Keep both sides visible to avoid winding-order differences on vendor GLES drivers.
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        program = linkProgram(compileShader(GLES20.GL_VERTEX_SHADER, vertexShader), compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader))
        val data = customMesh ?: createSphere(1f, 48, 64)
        vertexCount = data.size / 6
        vertexBuffer = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w.coerceAtLeast(1); height = h.coerceAtLeast(1)
        GLES20.glViewport(0, 0, width, height)
        Matrix.perspectiveM(projection, 0, 42f, width.toFloat() / height.toFloat(), 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0 || vertexBuffer == null) return
        angle = (angle + 0.42f) % 360f
        Matrix.setLookAtM(view, 0, 0f, 0f, 3.45f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, angle, 0.18f, 1f, 0.12f)
        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0)
        Matrix.setIdentityM(normalMatrix, 0)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMvp"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModel"), 1, false, model, 0)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uCamera"), 0f, 0f, 3.45f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uScene"), sceneMode.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uEffect"), effectMode.toFloat())
        // Scene-dependent material presets: chrome is metallic/smooth, hologram is
        // less metallic and rougher to preserve the emissive visual identity.
        val metallic = when (sceneMode) { 1 -> 0.92f; 2 -> 0.55f; 3 -> 0.72f; else -> 0.28f }
        val roughness = when (sceneMode) { 1 -> 0.16f; 2 -> 0.28f; 3 -> 0.34f; else -> 0.42f }
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uMetallic"), metallic)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uRoughness"), roughness)
        val pos = GLES20.glGetAttribLocation(program, "aPosition")
        val norm = GLES20.glGetAttribLocation(program, "aNormal")
        val buffer = vertexBuffer!!
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(3)
        GLES20.glEnableVertexAttribArray(norm)
        GLES20.glVertexAttribPointer(norm, 3, GLES20.GL_FLOAT, false, 24, buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(norm)
    }

    private fun createSphere(radius: Float, latitudes: Int, longitudes: Int): FloatArray {
        val out = ArrayList<Float>(latitudes * longitudes * 36)
        fun vertex(lat: Int, lon: Int): FloatArray {
            val theta = Math.PI * lat / latitudes
            val phi = 2.0 * Math.PI * lon / longitudes
            val x = (sin(theta) * cos(phi)).toFloat()
            val y = cos(theta).toFloat()
            val z = (sin(theta) * sin(phi)).toFloat()
            return floatArrayOf(x * radius, y * radius, z * radius, x, y, z)
        }
        fun add(v: FloatArray) { v.forEach { out.add(it) } }
        for (lat in 0 until latitudes) for (lon in 0 until longitudes) {
            val a = vertex(lat, lon); val b = vertex(lat + 1, lon)
            val c = vertex(lat, lon + 1); val d = vertex(lat + 1, lon + 1)
            add(a); add(b); add(c); add(c); add(b); add(d)
        }
        return out.toFloatArray()
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source); GLES20.glCompileShader(shader)
        val status = IntArray(1); GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) { val log = GLES20.glGetShaderInfoLog(shader); GLES20.glDeleteShader(shader); throw IllegalStateException("OpenGL shader compile failed: $log") }
        return shader
    }

    private fun linkProgram(vertex: Int, fragment: Int): Int {
        val p = GLES20.glCreateProgram(); GLES20.glAttachShader(p, vertex); GLES20.glAttachShader(p, fragment); GLES20.glLinkProgram(p)
        val status = IntArray(1); GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) { val log = GLES20.glGetProgramInfoLog(p); GLES20.glDeleteProgram(p); throw IllegalStateException("OpenGL program link failed: $log") }
        GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment)
        return p
    }
}
