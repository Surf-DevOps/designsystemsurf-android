package com.surf.surfhubds.util

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Port do `DSSImagePickerManager.swift` — abre câmera ou galeria, lida com permissões,
 * e devolve um [Bitmap] via callback.
 *
 * Diferença pro iOS: no Android usa o ActivityResultContracts API. Registre os launchers
 * no `Activity.onCreate()` ou `Fragment.onCreate()` antes de chamar [show].
 *
 * Paridade com iOS:
 * - Permissão negada (câmera/galeria) abre o mesmo alerta "Permissão Necessária" com a
 *   ação "Configurações", que aqui leva às Configurações do app
 *   (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) — espelha o `openSettingsURLString`.
 * - A câmera captura em resolução cheia gravando num `Uri` do MediaStore via `EXTRA_OUTPUT`
 *   e decodifica esse `Uri` depois (o iOS entrega a `UIImage` original; o thumbnail de
 *   `extras["data"]` ficava em baixa resolução).
 * - Antes de abrir a galeria, pede a permissão de leitura de mídia (READ_MEDIA_IMAGES no
 *   API 33+, senão READ_EXTERNAL_STORAGE) — o iOS pede `PHPhotoLibrary`.
 */
class DSSImagePickerManager private constructor(
    private val activity: AppCompatActivity?,
    private val fragment: Fragment?,
) {
    interface Delegate {
        fun didSelectImage(bitmap: Bitmap)
        fun didFailToSelectImage(error: String)
    }

    var delegate: Delegate? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>

    /** Uri de saída (MediaStore) onde a câmera grava a foto em resolução cheia. */
    private var cameraOutputUri: Uri? = null

    constructor(activity: AppCompatActivity) : this(activity, null) { register() }
    constructor(fragment: Fragment) : this(null, fragment) { register() }

    private val ctx get() = activity ?: fragment?.requireActivity()!!

    private fun <I, O> registerLauncher(
        contract: androidx.activity.result.contract.ActivityResultContract<I, O>,
        callback: androidx.activity.result.ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> = activity?.registerForActivityResult(contract, callback)
        ?: fragment!!.registerForActivityResult(contract, callback)

    private fun register() {
        cameraLauncher = registerLauncher(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != AppCompatActivity.RESULT_OK) {
                cameraOutputUri?.let { runCatching { ctx.contentResolver.delete(it, null, null) } }
                cameraOutputUri = null
                return@registerLauncher
            }
            val uri = cameraOutputUri
            if (uri == null) {
                delegate?.didFailToSelectImage(
                    AppStrings.brand(ctx, "image_picker_load_image_failed", "Não foi possível carregar a imagem"),
                )
                return@registerLauncher
            }
            decodeUri(uri)
            cameraOutputUri = null
        }

        galleryLauncher = registerLauncher(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) return@registerLauncher
            decodeUri(uri)
        }

        cameraPermissionLauncher = registerLauncher(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) openCamera() else showCameraPermissionAlert()
        }

        galleryPermissionLauncher = registerLauncher(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) openGallery() else showGalleryPermissionAlert()
        }
    }

    fun show() {
        val options = arrayOf(
            AppStrings.brand(ctx, "image_picker_camera", "Câmera"),
            AppStrings.brand(ctx, "image_picker_gallery", "Galeria"),
            AppStrings.brand(ctx, "common_cancel", "Cancelar"),
        )
        val d = AlertDialog.Builder(ctx)
            .setTitle(AppStrings.brand(ctx, "image_picker_select_photo", "Selecionar Foto"))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> ensureCameraPermissionAndOpen()
                    1 -> ensureGalleryPermissionAndOpen()
                }
                dialog.dismiss()
            }
            .create()
        d.show()
        // Tematiza pelo DSS (fundo+título; sem isso fica branco e o texto some no dark/black).
        d.applyDssTheme()
    }

    // MARK: - Camera

    private fun ensureCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        // Grava a foto em resolução cheia num Uri do MediaStore (EXTRA_OUTPUT). Sem isso, a
        // câmera só devolve um thumbnail de baixa resolução em result.data.extras["data"].
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "dss_capture_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            delegate?.didFailToSelectImage(
                AppStrings.brand(ctx, "image_picker_camera_unavailable", "Câmera não disponível neste dispositivo"),
            )
            return
        }
        cameraOutputUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        cameraLauncher.launch(intent)
    }

    private fun showCameraPermissionAlert() = showPermissionDeniedAlert(
        title = AppStrings.brand(ctx, "image_picker_camera_permission_title", "Permissão da Câmera Necessária"),
        message = AppStrings.brand(
            ctx,
            "image_picker_camera_permission_message",
            "Para usar a câmera, é necessário permitir o acesso nas Configurações do dispositivo.",
        ),
        fallbackError = AppStrings.brand(ctx, "image_picker_camera_permission_denied", "Permissão da câmera negada"),
    )

    // MARK: - Gallery

    private val galleryReadPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun ensureGalleryPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(ctx, galleryReadPermission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            galleryPermissionLauncher.launch(galleryReadPermission)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showGalleryPermissionAlert() = showPermissionDeniedAlert(
        title = AppStrings.brand(ctx, "image_picker_gallery_permission_title", "Permissão da Galeria Necessária"),
        message = AppStrings.brand(
            ctx,
            "image_picker_gallery_permission_message",
            "Para acessar a galeria, é necessário permitir o acesso nas Configurações do dispositivo.",
        ),
        fallbackError = AppStrings.brand(ctx, "image_picker_gallery_permission_denied", "Permissão da galeria negada"),
    )

    // MARK: - Shared

    /**
     * Espelha o `showCameraPermissionAlert`/`showPhotoLibraryPermissionAlert` do iOS: um alerta
     * com a ação "Configurações" que aqui abre as Configurações do app
     * (`ACTION_APPLICATION_DETAILS_SETTINGS`) e um "Cancelar". Se as Configurações não puderem
     * ser abertas, cai no callback de erro pra não deixar o fluxo mudo.
     */
    private fun showPermissionDeniedAlert(title: String, message: String, fallbackError: String) {
        val d = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(AppStrings.brand(ctx, "image_picker_settings", "Configurações")) { dialog, _ ->
                openAppSettings(fallbackError)
                dialog.dismiss()
            }
            .setNegativeButton(AppStrings.brand(ctx, "common_cancel", "Cancelar")) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        d.show()
        d.applyDssTheme()
    }

    private fun openAppSettings(fallbackError: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(ctx.packageManager) != null) {
            ctx.startActivity(intent)
        } else {
            delegate?.didFailToSelectImage(fallbackError)
        }
    }

    private fun decodeUri(uri: Uri) {
        try {
            val input = ctx.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            if (bmp != null) {
                delegate?.didSelectImage(bmp)
            } else {
                delegate?.didFailToSelectImage(
                    AppStrings.brand(ctx, "image_picker_load_image_failed", "Não foi possível carregar a imagem"),
                )
            }
        } catch (e: Exception) {
            delegate?.didFailToSelectImage(
                e.localizedMessage
                    ?: AppStrings.brand(ctx, "image_picker_load_image_failed", "Não foi possível carregar a imagem"),
            )
        }
    }
}
