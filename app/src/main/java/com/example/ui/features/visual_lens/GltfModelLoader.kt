package com.example.ui.features.visual_lens

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Lightweight glTF 2.0 / GLB triangle-mesh reader for POSITION + optional NORMAL attributes.
 * It expands indexed triangles to a GLES-friendly interleaved [x,y,z,nx,ny,nz] float array.
 * Materials/textures are intentionally handled by the renderer, not this file parser.
 */
object GltfModelLoader {
    data class Mesh(val interleaved: FloatArray, val triangleCount: Int, val sourceName: String)

    fun load(contentResolver: ContentResolver, uri: Uri): Mesh {
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_MODEL_FILE_BYTES) { "Model file exceeds the 64 MB mobile import limit" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("Model file cannot be opened")
        val name = uri.lastPathSegment ?: "selected model"
        val jsonText: String
        val binaryChunks = mutableListOf<ByteArray>()
        if (bytes.size >= 12 && bytes[0] == 'g'.code.toByte() && bytes[1] == 'l'.code.toByte() && bytes[2] == 'T'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(b.int == 0x46546C67) { "Invalid GLB header" }
            require(b.int == 2) { "Only glTF 2.0 GLB is supported" }
            val declaredTotal = b.int
            require(declaredTotal in 12..bytes.size) { "Invalid GLB declared file length" }
            val total = declaredTotal
            var json: String? = null
            while (b.position() + 8 <= total) {
                val length = b.int
                val type = b.int
                if (length < 0 || b.position() + length > total) break
                val chunk = ByteArray(length); b.get(chunk)
                when (type) {
                    0x4E4F534A -> json = chunk.toString(Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r', '\t')
                    0x004E4942 -> binaryChunks.add(chunk)
                }
            }
            jsonText = json ?: throw IllegalArgumentException("GLB JSON chunk missing")
        } else {
            jsonText = bytes.toString(Charsets.UTF_8)
        }
        val root = JSONObject(jsonText)
        val buffersJson = root.optJSONArray("buffers") ?: throw IllegalArgumentException("No buffers found")
        val buffers = ArrayList<ByteArray>()
        for (i in 0 until buffersJson.length()) {
            val item = buffersJson.getJSONObject(i)
            val uriValue = item.optString("uri", "")
            val data = when {
                uriValue.startsWith("data:") -> {
                    val comma = uriValue.indexOf(',')
                    require(comma > 0) { "Invalid embedded buffer URI" }
                    Base64.decode(uriValue.substring(comma + 1), Base64.DEFAULT)
                }
                uriValue.isEmpty() -> binaryChunks.getOrNull(i) ?: binaryChunks.firstOrNull()
                    ?: throw IllegalArgumentException("GLB binary chunk missing")
                else -> throw IllegalArgumentException("External .gltf buffers are not supported; use a self-contained .glb or embedded .gltf")
            }
            buffers.add(data)
        }
        val views = root.getJSONArray("bufferViews")
        val accessors = root.getJSONArray("accessors")
        val meshes = root.getJSONArray("meshes")
        val output = ArrayList<Float>()
        var triangles = 0
        // Respect glTF scene graph node transforms (matrix or translation/rotation/scale).
        val meshInstances = collectSceneMeshInstances(root, meshes.length())
        for ((mi, worldTransform) in meshInstances) {
            val primitives = meshes.getJSONObject(mi).getJSONArray("primitives")
            for (pi in 0 until primitives.length()) {
                val primitive = primitives.getJSONObject(pi)
                if (primitive.optInt("mode", 4) != 4) continue
                val attrs = primitive.optJSONObject("attributes") ?: continue
                val posIndex = attrs.optInt("POSITION", -1)
                if (posIndex < 0) continue
                val positions = readVec3(posIndex, accessors, views, buffers)
                require(positions.size / 3 <= MAX_MODEL_VERTICES) { "Model has too many vertices for mobile preview" }
                val indexAccessor = primitive.optInt("indices", -1)
                val indices = if (indexAccessor >= 0) readIndices(indexAccessor, accessors, views, buffers) else IntArray(positions.size / 3) { it }
                val normalIndex = attrs.optInt("NORMAL", -1)
                val normals = if (normalIndex >= 0) readVec3(normalIndex, accessors, views, buffers).also {
                    require(it.size == positions.size) { "NORMAL count must match POSITION count" }
                } else generateNormals(positions, indices)
                val count = indices.size - indices.size % 3
                for (i in 0 until count) {
                    val index = indices[i]
                    require(index in 0 until positions.size / 3) { "Mesh index out of bounds" }
                    val n = index * 3
                    val pos = transformPoint(worldTransform, positions[n], positions[n + 1], positions[n + 2])
                    val normal = transformNormal(worldTransform, normals[n], normals[n + 1], normals[n + 2])
                    output.add(pos[0]); output.add(pos[1]); output.add(pos[2])
                    output.add(normal[0]); output.add(normal[1]); output.add(normal[2])
                }
                triangles += count / 3
                require(output.size <= 1_800_000) { "Model is too large for the mobile preview (limit: 100,000 triangles)" }
            }
        }
        require(output.isNotEmpty()) { "No triangle POSITION mesh found in this model" }
        // Bound memory/fragment cost on mobile devices.
        require(output.size <= 1_800_000) { "Model is too large for the mobile preview (limit: 100,000 triangles)" }
        val packed = output.toFloatArray()
        // Center and normalize positions so imported assets fit the preview camera.
        var minX = Float.POSITIVE_INFINITY; var minY = Float.POSITIVE_INFINITY; var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY; var maxZ = Float.NEGATIVE_INFINITY
        var vi = 0
        while (vi < packed.size) {
            minX = minOf(minX, packed[vi]); maxX = maxOf(maxX, packed[vi])
            minY = minOf(minY, packed[vi + 1]); maxY = maxOf(maxY, packed[vi + 1])
            minZ = minOf(minZ, packed[vi + 2]); maxZ = maxOf(maxZ, packed[vi + 2])
            vi += 6
        }
        val cx = (minX + maxX) * 0.5f; val cy = (minY + maxY) * 0.5f; val cz = (minZ + maxZ) * 0.5f
        val maxDim = maxOf(maxX - minX, maxY - minY, maxZ - minZ).coerceAtLeast(0.0001f)
        val scale = 1.8f / maxDim
        vi = 0
        while (vi < packed.size) {
            packed[vi] = (packed[vi] - cx) * scale
            packed[vi + 1] = (packed[vi + 1] - cy) * scale
            packed[vi + 2] = (packed[vi + 2] - cz) * scale
            vi += 6
        }
        return Mesh(packed, triangles, name)
    }

    private fun collectSceneMeshInstances(root: JSONObject, meshCount: Int): List<Pair<Int, FloatArray>> {
        val nodes = root.optJSONArray("nodes")
        if (nodes == null || nodes.length() == 0) return (0 until meshCount).map { it to identityMatrix() }
        val scenes = root.optJSONArray("scenes")
        val sceneIndex = root.optInt("scene", 0)
        val sceneRoots = scenes?.optJSONObject(sceneIndex)?.optJSONArray("nodes")
        val output = mutableListOf<Pair<Int, FloatArray>>()
        val visitedPath = mutableSetOf<Int>()
        fun visit(nodeIndex: Int, parent: FloatArray) {
            if (nodeIndex !in 0 until nodes.length() || !visitedPath.add(nodeIndex)) return
            val node = nodes.getJSONObject(nodeIndex)
            val world = multiplyMatrices(parent, nodeLocalMatrix(node))
            val mesh = node.optInt("mesh", -1)
            if (mesh in 0 until meshCount) output.add(mesh to world)
            val children = node.optJSONArray("children")
            if (children != null) for (i in 0 until children.length()) visit(children.optInt(i, -1), world)
            visitedPath.remove(nodeIndex)
        }
        if (sceneRoots != null) {
            for (i in 0 until sceneRoots.length()) visit(sceneRoots.optInt(i, -1), identityMatrix())
        } else {
            // Files without a valid scene root still preview their mesh nodes.
            for (i in 0 until nodes.length()) {
                val node = nodes.getJSONObject(i)
                val mesh = node.optInt("mesh", -1)
                if (mesh in 0 until meshCount) output.add(mesh to nodeLocalMatrix(node))
            }
        }
        return output.ifEmpty { (0 until meshCount).map { it to identityMatrix() } }
    }

    private fun nodeLocalMatrix(node: JSONObject): FloatArray {
        val matrix = node.optJSONArray("matrix")
        if (matrix != null && matrix.length() == 16) return FloatArray(16) { matrix.optDouble(it, if (it % 5 == 0) 1.0 else 0.0).toFloat() }
        val t = node.optJSONArray("translation")
        val r = node.optJSONArray("rotation")
        val sc = node.optJSONArray("scale")
        val tx = t?.optDouble(0, 0.0)?.toFloat() ?: 0f
        val ty = t?.optDouble(1, 0.0)?.toFloat() ?: 0f
        val tz = t?.optDouble(2, 0.0)?.toFloat() ?: 0f
        val x = r?.optDouble(0, 0.0)?.toFloat() ?: 0f
        val y = r?.optDouble(1, 0.0)?.toFloat() ?: 0f
        val z = r?.optDouble(2, 0.0)?.toFloat() ?: 0f
        val w = r?.optDouble(3, 1.0)?.toFloat() ?: 1f
        val sx = sc?.optDouble(0, 1.0)?.toFloat() ?: 1f
        val sy = sc?.optDouble(1, 1.0)?.toFloat() ?: 1f
        val sz = sc?.optDouble(2, 1.0)?.toFloat() ?: 1f
        val out = identityMatrix()
        out[0] = (1 - 2*y*y - 2*z*z) * sx; out[1] = (2*x*y + 2*w*z) * sx; out[2] = (2*x*z - 2*w*y) * sx
        out[4] = (2*x*y - 2*w*z) * sy; out[5] = (1 - 2*x*x - 2*z*z) * sy; out[6] = (2*y*z + 2*w*x) * sy
        out[8] = (2*x*z + 2*w*y) * sz; out[9] = (2*y*z - 2*w*x) * sz; out[10] = (1 - 2*x*x - 2*y*y) * sz
        out[12] = tx; out[13] = ty; out[14] = tz
        return out
    }

    private fun identityMatrix() = FloatArray(16).also { for (i in 0..3) it[i * 5] = 1f }

    private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (column in 0..3) for (row in 0..3) {
            var value = 0f
            for (k in 0..3) value += a[k * 4 + row] * b[column * 4 + k]
            out[column * 4 + row] = value
        }
        return out
    }

    private fun transformPoint(m: FloatArray, x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
        m[0] * x + m[4] * y + m[8] * z + m[12],
        m[1] * x + m[5] * y + m[9] * z + m[13],
        m[2] * x + m[6] * y + m[10] * z + m[14]
    )

    private fun transformNormal(m: FloatArray, x: Float, y: Float, z: Float): FloatArray {
        var nx = m[0] * x + m[4] * y + m[8] * z
        var ny = m[1] * x + m[5] * y + m[9] * z
        var nz = m[2] * x + m[6] * y + m[10] * z
        val length = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.00001f)
        nx /= length; ny /= length; nz /= length
        return floatArrayOf(nx, ny, nz)
    }

    private fun readVec3(index: Int, accessors: org.json.JSONArray, views: org.json.JSONArray, buffers: List<ByteArray>): FloatArray {
        val a = accessors.getJSONObject(index)
        require(a.optInt("componentType") == 5126 && a.optString("type") == "VEC3") { "POSITION/NORMAL must use FLOAT VEC3" }
        val count = a.getInt("count")
        require(count in 1..MAX_MODEL_VERTICES) { "Invalid or excessive glTF accessor count" }
        val viewIndex = a.getInt("bufferView")
        val view = views.getJSONObject(viewIndex)
        val data = buffers[view.getInt("buffer")]
        val start = view.optInt("byteOffset", 0) + a.optInt("byteOffset", 0)
        val stride = view.optInt("byteStride", 12)
        require(stride >= 12 && count > 0) { "Invalid glTF vector layout" }
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val result = FloatArray(count * 3)
        for (i in 0 until count) for (c in 0..2) {
            val offset = start + i * stride + c * 4
            require(offset >= 0 && offset + 4 <= data.size) { "glTF accessor exceeds buffer" }
            result[i * 3 + c] = b.getFloat(offset)
        }
        return result
    }

    private fun readIndices(index: Int, accessors: org.json.JSONArray, views: org.json.JSONArray, buffers: List<ByteArray>): IntArray {
        val a = accessors.getJSONObject(index)
        require(a.optString("type") == "SCALAR") { "Indices must be SCALAR" }
        val view = views.getJSONObject(a.getInt("bufferView"))
        val data = buffers[view.getInt("buffer")]
        val start = view.optInt("byteOffset", 0) + a.optInt("byteOffset", 0)
        val component = a.getInt("componentType")
        val size = when (component) { 5121 -> 1; 5123 -> 2; 5125 -> 4; else -> throw IllegalArgumentException("Unsupported index component type") }
        val count = a.getInt("count")
        require(count > 0 && count <= data.size / size) { "Invalid or excessive glTF index count" }
        require(start >= 0 && start.toLong() + count.toLong() * size <= data.size.toLong()) { "Index accessor exceeds buffer" }
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        return IntArray(count) { i ->
            val offset = start + i * size
            require(offset >= 0 && offset + size <= data.size) { "Index accessor exceeds buffer" }
            when (component) { 5121 -> b.get(offset).toInt() and 0xff; 5123 -> b.getShort(offset).toInt() and 0xffff; else -> b.getInt(offset) }
        }
    }

    private fun generateNormals(positions: FloatArray, indices: IntArray): FloatArray {
        val out = FloatArray(positions.size)
        var i = 0
        while (i + 2 < indices.size) {
            val ia = indices[i] * 3
            val ib = indices[i + 1] * 3
            val ic = indices[i + 2] * 3
            require(ia >= 0 && ib >= 0 && ic >= 0 && ia + 2 < positions.size && ib + 2 < positions.size && ic + 2 < positions.size) {
                "Mesh index out of bounds while generating normals"
            }
            val ax = positions[ib] - positions[ia]; val ay = positions[ib + 1] - positions[ia + 1]; val az = positions[ib + 2] - positions[ia + 2]
            val bx = positions[ic] - positions[ia]; val by = positions[ic + 1] - positions[ia + 1]; val bz = positions[ic + 2] - positions[ia + 2]
            val nx = ay * bz - az * by; val ny = az * bx - ax * bz; val nz = ax * by - ay * bx
            for (k in intArrayOf(ia, ib, ic)) { out[k] += nx; out[k + 1] += ny; out[k + 2] += nz }
            i += 3
        }
        var v = 0
        while (v + 2 < out.size) {
            val length = kotlin.math.sqrt(out[v] * out[v] + out[v + 1] * out[v + 1] + out[v + 2] * out[v + 2]).coerceAtLeast(0.00001f)
            out[v] /= length; out[v + 1] /= length; out[v + 2] /= length
            v += 3
        }
        return out
    }

    private const val MAX_MODEL_FILE_BYTES = 64 * 1024 * 1024
    private const val MAX_MODEL_VERTICES = 300_000
}
