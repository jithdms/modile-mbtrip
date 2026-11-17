package com.example.mbtrip.friends

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.mbtrip.timeline.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MemberAvatar(member: Member, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(null, member.photoUrl) {
        value = null
        val address = member.photoUrl ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                require(address.startsWith("https://"))
                val connection = URL(address).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000; connection.readTimeout = 5000
                connection.instanceFollowRedirects = false
                try {
                    require(connection.responseCode == 200)
                    val bytes = connection.inputStream.use { it.readNBytes(2 * 1024 * 1024 + 1) }
                    require(bytes.size <= 2 * 1024 * 1024)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0)
                    val options = BitmapFactory.Options()
                    while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 256) options.inSampleSize *= 2
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                } finally { connection.disconnect() }
            }.getOrNull()
        }
    }
    Box(modifier.clip(CircleShape).background(Color(0xFFE5E7E8)), contentAlignment = Alignment.Center) {
        val photo = bitmap
        if (photo == null) Text(member.nickname.take(1))
        else Image(photo.asImageBitmap(), "${member.nickname} 프로필 사진", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
}
