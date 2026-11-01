package com.financas.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FinflowProfileAvatar(
    photoUri: String?,
    userName: String = "",
    onClick: () -> Unit,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var avatarBitmap by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(photoUri) {
        withContext(Dispatchers.IO) {
            val localFile = context.filesDir.resolve("profile_photo.png")
            val cleanUri = photoUri?.substringBefore('?')

            // 1. Tentar ler ficheiro local guardado internamente
            if (localFile.exists() && localFile.length() > 0) {
                try {
                    val bmp = BitmapFactory.decodeFile(localFile.absolutePath)
                    if (bmp != null) {
                        avatarBitmap = bmp.asImageBitmap()
                        return@withContext
                    }
                } catch (_: Exception) {}
            }

            // 2. Se não existir no ficheiro local, tentar carregar do cleanUri e guardar localmente
            if (!cleanUri.isNullOrBlank()) {
                try {
                    if (cleanUri.startsWith("/")) {
                        val file = File(cleanUri)
                        if (file.exists() && file.length() > 0) {
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            if (bmp != null) {
                                avatarBitmap = bmp.asImageBitmap()
                                try {
                                    file.copyTo(localFile, overwrite = true)
                                } catch (_: Exception) {}
                                return@withContext
                            }
                        }
                    } else {
                        val uri = Uri.parse(cleanUri)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            localFile.outputStream().use { out ->
                                stream.copyTo(out)
                            }
                        }
                        if (localFile.exists() && localFile.length() > 0) {
                            val bmp = BitmapFactory.decodeFile(localFile.absolutePath)
                            if (bmp != null) {
                                avatarBitmap = bmp.asImageBitmap()
                                return@withContext
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            avatarBitmap = null
        }
    }

    val initials = remember(userName) {
        userName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FinflowPrimaryLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bmp = avatarBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = FinflowPrimary
            )
        }
    }
}
