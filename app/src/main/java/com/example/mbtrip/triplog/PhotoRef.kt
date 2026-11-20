package com.example.mbtrip.triplog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load

/** 앱 안 그림을 가리키는 사진 주소 앞부분 — 예: "res:photo1" = res/drawable/photo1 */
const val RES_PHOTO_PREFIX = "res:"

/**
 * 사진 주소 → 이미지 라이브러리(Coil)가 읽을 수 있는 값.
 *  - "res:이름"   → 그림 리소스 번호(Int)
 *  - "https://…" → 그대로 (인터넷 사진)
 *  - 빈 값·없는 그림 → null
 */
@SuppressLint("DiscouragedApi")
fun photoModel(context: Context, ref: String?): Any? = when {
    ref.isNullOrBlank() -> null
    // 앱에서 쓴 후기 사진: "data:image/jpeg;base64,…" (Firestore 문서 안에 작게 줄여 저장)
    ref.startsWith(DATA_PHOTO_PREFIX) -> decodeDataPhoto(ref)
    ref.startsWith(RES_PHOTO_PREFIX) ->
        context.resources.getIdentifier(ref.removePrefix(RES_PHOTO_PREFIX), "drawable", context.packageName)
            .takeIf { it != 0 }
    else -> ref
}

/** 문서 안에 넣은 사진 주소 앞부분 */
const val DATA_PHOTO_PREFIX = "data:image/"

/** "data:image/jpeg;base64,…" → 그림 (Coil 이 Bitmap 을 바로 그린다) */
private val dataPhotoCache = android.util.LruCache<Int, android.graphics.Bitmap>(24)
fun decodeDataPhoto(ref: String): android.graphics.Bitmap? {
    val key = ref.hashCode()
    dataPhotoCache.get(key)?.let { return it }
    return runCatching {
        val bytes = android.util.Base64.decode(ref.substringAfter(","), android.util.Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()?.also { dataPhotoCache.put(key, it) }
}

/** ImageView 에 사진 주소를 띄운다. 못 읽으면 fallback 그림 */
fun ImageView.loadPhotoRef(ref: String?, @DrawableRes fallback: Int) {
    val model = photoModel(context, ref) ?: fallback
    load(model) {
        crossfade(true)
        placeholder(ColorDrawable(context.getColor(com.example.mbtrip.R.color.log_placeholder)))
        error(fallback)
    }
}
