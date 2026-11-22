package com.example.feature.challenges

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

/**
 * 100% offline QR & Barcode scanner using CameraX + pure-Java ZXing MultiFormatReader.
 * Also supports manual token entry if a device camera is unavailable.
 */
@Composable
fun OfflineQrBarcodeScannerDialog(
  title: String,
  onCodeScanned: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var manualCode by remember { mutableStateOf("") }
  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }
  val executor = remember { Executors.newSingleThreadExecutor() }
  DisposableEffect(Unit) { onDispose { executor.shutdown() } }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hasCameraPermission) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
          ) {
            AndroidView(
              factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener(
                  {
                    runCatching {
                      val provider = providerFuture.get()
                      val preview =
                        Preview.Builder().build().also {
                          it.surfaceProvider = previewView.surfaceProvider
                        }
                      val analyzer =
                        ImageAnalysis.Builder()
                          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                          .build()
                          .also { analysis ->
                            val reader = MultiFormatReader()
                            analysis.setAnalyzer(executor) { image ->
                              decodeZxingFrame(reader, image)?.let { code ->
                                previewView.post { onCodeScanned(code) }
                              }
                              image.close()
                            }
                          }
                      provider.unbindAll()
                      provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer,
                      )
                    }
                  },
                  ContextCompat.getMainExecutor(ctx),
                )
                previewView
              },
              modifier = Modifier.fillMaxWidth().height(220.dp),
            )
          }
        } else {
          Text(
            "Grant Camera permission to scan QR codes or barcodes offline, or enter the token value below.",
            style = MaterialTheme.typography.bodyMedium,
          )
          Button(onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) }) {
            Text("Enable Camera")
          }
        }
        OutlinedTextField(
          value = manualCode,
          onValueChange = { manualCode = it },
          label = { Text("Or enter token value manually") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = manualCode.isNotBlank(),
        onClick = { onCodeScanned(manualCode.trim()) },
      ) {
        Text("Verify Token")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

private fun decodeZxingFrame(reader: MultiFormatReader, image: ImageProxy): String? {
  return try {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    val source =
      PlanarYUVLuminanceSource(
        data,
        image.width,
        image.height,
        0,
        0,
        image.width,
        image.height,
        false,
      )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    reader.decodeWithState(bitmap).text
  } catch (_: Exception) {
    null
  } finally {
    reader.reset()
  }
}
