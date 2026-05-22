package com.surf.surfhubds.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
            val bmp = result.data?.extras?.get("data") as? Bitmap
            if (bmp != null) delegate?.didSelectImage(bmp)
            else delegate?.didFailToSelectImage("Falha ao carregar imagem da câmera.")
        }

        galleryLauncher = registerLauncher(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) return@registerLauncher
            try {
                val input = ctx.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(input)
                input?.close()
                if (bmp != null) delegate?.didSelectImage(bmp)
                else delegate?.didFailToSelectImage("Falha ao decodificar imagem.")
            } catch (e: Exception) {
                delegate?.didFailToSelectImage(e.localizedMessage ?: "Erro desconhecido")
            }
        }

        cameraPermissionLauncher = registerLauncher(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> if (granted) openCamera() else delegate?.didFailToSelectImage("Permissão de câmera negada.") }
    }

    fun show() {
        val options = arrayOf("Câmera", "Galeria", "Cancelar")
        AlertDialog.Builder(ctx)
            .setTitle("Selecionar foto")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> ensureCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun ensureCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }
}
