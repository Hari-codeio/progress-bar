package app.becoming.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Identity photos. A picked image is first saved as a *pending* PNG in the cache (so Cancel leaves
 * nothing behind), then committed to filesDir/identity/journey_<id>_<epochMillis>.png on save.
 * Stored paths are relative to filesDir so they survive Auto Backup restores on a new phone.
 */
class IdentityPhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val photoDir: File get() = File(appContext.filesDir, PHOTO_DIR)
    private val pendingDir: File get() = File(appContext.cacheDir, PENDING_DIR)

    /** Decodes [uri], centre-crops to a square, scales to 256 px and saves a pending PNG. Returns its absolute path. */
    suspend fun importPending(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val decoded = decode(uri) ?: return@withContext null
            val square = squareThumbnail(decoded, SIZE_PX)
            pendingDir.mkdirs()
            val file = File(pendingDir, "pending_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out -> square.compress(Bitmap.CompressFormat.PNG, 100, out) }
            file.absolutePath
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun isPending(path: String?): Boolean {
        if (path == null) return false
        val file = File(path)
        return file.isAbsolute && file.parentFile?.absolutePath == pendingDir.absolutePath
    }

    /** Moves a pending photo into permanent storage and returns its stored path; other paths pass through. */
    fun commitIfPending(path: String?, journeyId: Long): String? {
        if (path == null || !isPending(path)) return path
        val source = File(path)
        if (!source.exists()) return null
        photoDir.mkdirs()
        val name = "journey_${journeyId}_${System.currentTimeMillis()}.png"
        val target = File(photoDir, name)
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
        return "$PHOTO_DIR/$name"
    }

    /** Absolute file for a stored (relative) or pending (absolute) path. */
    fun resolve(path: String): File {
        val file = File(path)
        return if (file.isAbsolute) file else File(appContext.filesDir, path)
    }

    /** The photo as a bitmap, or null when there is none or the file is missing. Call off the main thread. */
    fun loadBitmap(path: String?): Bitmap? {
        if (path == null) return null
        val file = resolve(path)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Deletes a stored or pending photo. Paths outside our two folders are ignored. */
    fun delete(path: String?) {
        if (path == null) return
        val file = resolve(path)
        val parent = file.parentFile?.absolutePath ?: return
        if (parent == photoDir.absolutePath || parent == pendingDir.absolutePath) file.delete()
    }

    fun deleteIfPending(path: String?) {
        if (isPending(path)) delete(path)
    }

    private fun decode(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeWithImageDecoder(uri) else decodeWithBitmapFactory(uri)

    /** ImageDecoder applies the EXIF orientation itself. */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(appContext.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
        }
    }

    private fun decodeWithBitmapFactory(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val degrees = resolver.openInputStream(uri)?.use { exifRotation(it) } ?: 0
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun exifRotation(input: InputStream): Int = try {
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: IOException) {
        0
    }

    /** Power-of-two sample size that keeps the short side at least twice the output size. */
    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (minOf(width, height) / (sample * 2) >= SIZE_PX * 2) sample *= 2
        return sample
    }

    private fun squareThumbnail(source: Bitmap, size: Int): Bitmap {
        val side = minOf(source.width, source.height)
        val cropped = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        return Bitmap.createScaledBitmap(cropped, size, size, true)
    }

    private companion object {
        const val PHOTO_DIR = "identity"
        const val PENDING_DIR = "identity_pending"
        const val SIZE_PX = 256
    }
}
