package com.example.feature.challenges

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.permissions.PermissionManager
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class CameraScannerStatus {
  READY,
  PERMISSION_REQUIRED,
  UNAVAILABLE,
  ERROR,
}

/**
 * Pure, unit-testable extractor that normalizes Y-plane luminance frames even when
 * Android CameraX buffers carry row padding (`rowStride > width`) or interleaved pixels (`pixelStride > 1`).
 */
object YuvLuminanceExtractor {
  fun extractNormalizedLuminance(
    width: Int,
    height: Int,
    rowStride: Int,
    pixelStride: Int,
    buffer: ByteBuffer,
  ): ByteArray? {
    if (width <= 0 || height <= 0 || rowStride <= 0 || pixelStride <= 0) return null
    val copy = buffer.duplicate()
    copy.rewind()
    val available = copy.remaining()
    if (available <= 0) return null

    // Fast contiguous copy when zero row padding & contiguous pixels
    if (rowStride == width && pixelStride == 1 && available >= width * height) {
      val tight = ByteArray(width * height)
      copy.get(tight, 0, tight.size)
      return tight
    }

    val output = ByteArray(width * height)
    val raw = ByteArray(available)
    copy.get(raw)

    for (row in 0 until height) {
      val rowOffset = row * rowStride
      if (rowOffset >= raw.size) break
      if (pixelStride == 1) {
        val len = minOf(width, raw.size - rowOffset)
        System.arraycopy(raw, rowOffset, output, row * width, len)
      } else {
        for (col in 0 until width) {
          val srcIdx = rowOffset + col * pixelStride
          if (srcIdx < raw.size) {
            output[row * width + col] = raw[srcIdx]
          }
        }
      }
    }
    return output
  }

  fun decodeFromNormalizedLuminance(
    reader: MultiFormatReader,
    luminance: ByteArray,
    width: Int,
    height: Int,
  ): String? {
    if (luminance.size < width * height || width <= 0 || height <= 0) return null
    return try {
      val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
      val bitmap = BinaryBitmap(HybridBinarizer(source))
      reader.decodeWithState(bitmap).text
    } catch (_: Exception) {
      null
    } finally {
      reader.reset()
    }
  }
}

/**
 * 100% offline QR & Barcode scanner using CameraX + pure-Java ZXing MultiFormatReader.
 * - Handles row-strided YUV buffers safely without ArrayIndexOutOfBoundsException.
 * - Verifies camera hardware availability and permission prior to binding.
 * - Releases ProcessCameraProvider cleanly via DisposableEffect.
 * - Includes torch toggle, error/retry UI, permission/settings CTA, and manual token entry.
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
  val hasCameraHardware = remember {
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
  }
  var scannerStatus by remember(hasCameraPermission, hasCameraHardware) {
    mutableStateOf(
      when {
        !hasCameraPermission -> CameraScannerStatus.PERMISSION_REQUIRED
        !hasCameraHardware -> CameraScannerStatus.UNAVAILABLE
        else -> CameraScannerStatus.READY
      }
    )
  }
  var cameraErrorText by remember { mutableStateOf<String?>(null) }
  var retryCounter by remember { mutableIntStateOf(0) }
  var torchEnabled by remember { mutableStateOf(false) }
  var hasFlashUnit by remember { mutableStateOf(false) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }
  var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var manualCode by remember { mutableStateOf("") }

  val scanDispatched = remember { AtomicBoolean(false) }
  val executor = remember { Executors.newSingleThreadExecutor() }

  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
      scannerStatus =
        when {
          !granted -> CameraScannerStatus.PERMISSION_REQUIRED
          !hasCameraHardware -> CameraScannerStatus.UNAVAILABLE
          else -> CameraScannerStatus.READY
        }
      cameraErrorText = null
    }

  DisposableEffect(Unit) {
    onDispose {
      runCatching { boundProvider?.unbindAll() }
      executor.shutdown()
    }
  }

  LaunchedEffect(scannerStatus, retryCounter, previewViewRef, lifecycleOwner) {
    val pv = previewViewRef ?: return@LaunchedEffect
    if (scannerStatus != CameraScannerStatus.READY) return@LaunchedEffect
    if (
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      scannerStatus = CameraScannerStatus.PERMISSION_REQUIRED
      return@LaunchedEffect
    }

    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener(
      {
        runCatching {
          val provider = providerFuture.get()
          boundProvider = provider
          val selector =
            when {
              provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                CameraSelector.DEFAULT_BACK_CAMERA
              provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                CameraSelector.DEFAULT_FRONT_CAMERA
              else -> {
                scannerStatus = CameraScannerStatus.UNAVAILABLE
                cameraErrorText = "No usable camera lens reported by CameraX on this device."
                return@addListener
              }
            }

          val preview =
            Preview.Builder().build().also {
              it.surfaceProvider = pv.surfaceProvider
            }
          val reader = MultiFormatReader()
          val analyzer =
            ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
              .also { analysis ->
                analysis.setAnalyzer(executor) { image ->
                  try {
                    if (!scanDispatched.get()) {
                      decodeZxingFrame(reader, image)?.let { code ->
                        if (scanDispatched.compareAndSet(false, true)) {
                          pv.post { onCodeScanned(code) }
                        }
                      }
                    }
                  } finally {
                    image.close()
                  }
                }
              }

          provider.unbindAll()
          val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
          boundCamera = cam
          hasFlashUnit = cam.cameraInfo.hasFlashUnit()
          if (hasFlashUnit) {
            cam.cameraControl.enableTorch(torchEnabled)
          }
          cameraErrorText = null
        }.onFailure { err ->
          scannerStatus = CameraScannerStatus.ERROR
          cameraErrorText = err.message ?: "Unable to initialize CameraX preview."
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  AlertDialog(
    onDismissRequest = {
      runCatching { boundProvider?.unbindAll() }
      onDismiss()
    },
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(title, modifier = Modifier.weight(1f))
        if (scannerStatus == CameraScannerStatus.READY && hasFlashUnit) {
          IconButton(
            onClick = {
              val next = !torchEnabled
              torchEnabled = next
              runCatching { boundCamera?.cameraControl?.enableTorch(next) }
            },
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor =
                  if (torchEnabled) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant,
              ),
            modifier = Modifier.testTag("scanner_torch_btn"),
          ) {
            Icon(
              imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
              contentDescription = if (torchEnabled) "Turn flashlight off" else "Turn flashlight on",
            )
          }
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (scannerStatus) {
          CameraScannerStatus.READY -> {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(220.dp)
                  .clip(MaterialTheme.shapes.medium)
                  .testTag("scanner_camera_box"),
              contentAlignment = Alignment.Center,
            ) {
              AndroidView(
                factory = { ctx -> PreviewView(ctx).also { previewViewRef = it } },
                modifier = Modifier.fillMaxSize(),
              )
              val frameColor = MaterialTheme.colorScheme.primary
              Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = size.minDimension * 0.68f
                val left = (size.width - boxSize) / 2f
                val top = (size.height - boxSize) / 2f
                drawRoundRect(
                  color = frameColor,
                  topLeft = Offset(left, top),
                  size = Size(boxSize, boxSize),
                  cornerRadius = CornerRadius(24f, 24f),
                  style = Stroke(width = 5f),
                )
              }
            }
            Text(
              "Align QR code or barcode inside the reticle. Scanning runs 100% offline.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          CameraScannerStatus.PERMISSION_REQUIRED -> {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth().testTag("scanner_permission_card"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(36.dp))
                Text(
                  "Grant Camera permission to scan unlock QR codes or barcodes offline, or enter the token manually below.",
                  style = MaterialTheme.typography.bodyMedium,
                  textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  Button(
                    onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.weight(1f).testTag("scanner_enable_camera_btn"),
                  ) {
                    Text("Enable Camera")
                  }
                  OutlinedButton(
                    onClick = {
                      PermissionManager.safeStartSettings(
                        context,
                        PermissionManager.appDetailsIntent(context),
                      )
                    },
                    modifier = Modifier.testTag("scanner_app_settings_btn"),
                  ) {
                    Icon(Icons.Default.Settings, contentDescription = "Open App Settings")
                  }
                }
              }
            }
          }

          CameraScannerStatus.UNAVAILABLE -> {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth().testTag("scanner_unavailable_card"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(Icons.Default.VideocamOff, contentDescription = null, modifier = Modifier.size(34.dp))
                Text(
                  "Camera Unavailable",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  cameraErrorText ?: "No camera hardware is available on this device. Enter your unlock token below.",
                  style = MaterialTheme.typography.bodySmall,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }

          CameraScannerStatus.ERROR -> {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth().testTag("scanner_error_card"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  "Camera Preview Failed",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                  cameraErrorText ?: "Unable to open camera stream.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
                OutlinedButton(
                  onClick = {
                    scannerStatus = CameraScannerStatus.READY
                    retryCounter++
                  },
                  modifier = Modifier.testTag("scanner_retry_btn"),
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Retry Camera")
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = manualCode,
          onValueChange = { manualCode = it },
          label = { Text("Or enter token value manually") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("scanner_manual_input"),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = manualCode.isNotBlank(),
        onClick = {
          runCatching { boundProvider?.unbindAll() }
          onCodeScanned(manualCode.trim())
        },
        modifier = Modifier.testTag("scanner_verify_manual_btn"),
      ) {
        Text("Verify Token")
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          runCatching { boundProvider?.unbindAll() }
          onDismiss()
        },
        modifier = Modifier.testTag("scanner_close_btn"),
      ) {
        Text("Close")
      }
    },
  )
}

internal fun decodeZxingFrame(reader: MultiFormatReader, image: ImageProxy): String? {
  val plane = image.planes.firstOrNull() ?: return null
  val luminance =
    YuvLuminanceExtractor.extractNormalizedLuminance(
      width = image.width,
      height = image.height,
      rowStride = plane.rowStride,
      pixelStride = plane.pixelStride,
      buffer = plane.buffer,
    ) ?: return null
  return YuvLuminanceExtractor.decodeFromNormalizedLuminance(reader, luminance, image.width, image.height)
}
