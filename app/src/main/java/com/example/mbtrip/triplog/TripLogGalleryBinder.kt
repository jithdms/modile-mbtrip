package com.example.mbtrip.triplog

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.dispose
import com.example.mbtrip.R

/** 사진 모아보기 화면 상태 */
sealed interface GalleryState {
    data object Loading : GalleryState
    data class Ready(val logs: List<TripLog>, val isSample: Boolean) : GalleryState
    data class Failed(val message: String) : GalleryState
}

/** 비었을 때 가운데 안내 + 버튼 */
data class EmptyInfo(
    val title: String,
    val desc: String,
    val primary: Pair<String, () -> Unit>? = null,
    val secondary: Pair<String, () -> Unit>? = null,
)

/**
 * res/layout/activity_trip_log_gallery.xml 과 데이터를 이어 준다.
 * Compose(TripLogScreens.kt)가 상태를 들고, 바뀔 때마다 render() 를 부른다.
 */
class TripLogGalleryBinder(val root: View, initialQuery: String) {
    private val ctx = root.context
    private val etSearch: EditText = root.findViewById(R.id.et_search)
    private val btnClearSearch: ImageButton = root.findViewById(R.id.btn_clear_search)
    private val btnCalendar: ImageButton = root.findViewById(R.id.btn_calendar)
    private val chipPeriod: View = root.findViewById(R.id.chip_period)
    private val tvPeriod: TextView = root.findViewById(R.id.tv_period)
    private val tvSummary: TextView = root.findViewById(R.id.tv_summary)
    private val btnSummaryAction: TextView = root.findViewById(R.id.btn_summary_action)
    private val rv: RecyclerView = root.findViewById(R.id.rv_photos)
    private val layoutEmpty: View = root.findViewById(R.id.layout_empty)
    private val tvEmptyTitle: TextView = root.findViewById(R.id.tv_empty_title)
    private val tvEmptyDesc: TextView = root.findViewById(R.id.tv_empty_desc)
    private val btnEmptyPrimary: TextView = root.findViewById(R.id.btn_empty_primary)
    private val btnEmptySecondary: TextView = root.findViewById(R.id.btn_empty_secondary)
    private val progress: ProgressBar = root.findViewById(R.id.progress)

    // Compose 쪽에서 매번 최신 동작으로 바꿔 끼운다
    var onBack: () -> Unit = {}
    var onQueryChange: (String) -> Unit = {}
    var onCalendarClick: () -> Unit = {}
    var onClearPeriod: () -> Unit = {}
    var onPhotoClick: (TripLog, Int) -> Unit = { _, _ -> }
    var onSummaryAction: () -> Unit = {}

    private val adapter = GalleryAdapter { log, index -> onPhotoClick(log, index) }
    private val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    private var pendingScroll: Parcelable? = null

    init {
        rv.layoutManager = layoutManager
        rv.adapter = adapter
        rv.itemAnimator = null

        root.findViewById<View>(R.id.btn_back).setOnClickListener { onBack() }
        btnCalendar.setOnClickListener { onCalendarClick() }
        root.findViewById<View>(R.id.btn_clear_period).setOnClickListener { onClearPeriod() }
        btnSummaryAction.setOnClickListener { onSummaryAction() }

        // ── 움직임: 위쪽 줄들이 차례로 떠오르고, 버튼은 누르면 통통 ──
        listOf(R.id.btn_back, R.id.et_search, R.id.btn_calendar, R.id.tv_summary).forEachIndexed { i, id ->
            val v = root.findViewById<View>(id)
            (if (id == R.id.et_search) v.parent as View else v).riseIn(i)
        }
        listOf<View>(btnCalendar, btnSummaryAction, btnEmptyPrimary, btnEmptySecondary, root.findViewById<View>(R.id.btn_back))
            .forEach { it.bounceOnPress() }

        etSearch.setText(initialQuery)
        btnClearSearch.visibility = if (initialQuery.isEmpty()) View.GONE else View.VISIBLE
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                btnClearSearch.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                onQueryChange(text)
            }
        })
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        btnClearSearch.setOnClickListener { etSearch.setText("") }
        // 사진을 내리기 시작하면 자판을 내린다
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) hideKeyboard()
            }
        })
    }

    /** 화면 밖에서 검색어를 비웠을 때 글자칸도 비운다 */
    fun clearQueryIfNeeded() {
        if (!etSearch.text.isNullOrEmpty()) etSearch.setText("")
    }

    /** 상세로 넘어가기 전 보던 자리 저장 */
    fun saveScroll(): Parcelable? = layoutManager.onSaveInstanceState()

    fun restoreScrollLater(state: Parcelable?) {
        pendingScroll = state
    }

    fun render(
        state: GalleryState,
        filtered: List<TripLog>,
        range: LogDateRange?,
        filtering: Boolean,
        busy: Boolean,
        empty: EmptyInfo?,
    ) {
        // 달력 버튼: 기간이 골라져 있으면 파랗게
        btnCalendar.setBackgroundResource(
            if (range != null) R.drawable.bg_log_square_button_active else R.drawable.bg_log_square_button,
        )
        btnCalendar.imageTintList = ctx.getColorStateList(if (range != null) android.R.color.white else R.color.log_text)
        val chipWasVisible = chipPeriod.visibility == View.VISIBLE
        chipPeriod.visibility = if (range != null) View.VISIBLE else View.GONE
        if (!chipWasVisible && range != null) chipPeriod.popIn()
        range?.let { tvPeriod.text = formatDateRange(it) }

        progress.visibility = if (state is GalleryState.Loading || busy) View.VISIBLE else View.GONE

        // 요약 줄
        val ready = state as? GalleryState.Ready
        if (ready != null) {
            val photos = filtered.sumOf { it.photos.size }
            val base = "로그 ${filtered.size}개 · 사진 ${photos}장"
            tvSummary.text = when {
                ready.isSample -> "예시 로그 · $base"
                filtering -> "검색 결과 · $base"
                else -> base
            }
            tvSummary.setTextColor(ctx.getColor(if (ready.isSample) R.color.log_primary else R.color.log_text_sub))
            btnSummaryAction.visibility = if (ready.isSample && !busy) View.VISIBLE else View.GONE
            adapter.items = buildGalleryItems(filtered)
            pendingScroll?.let { s ->
                if (filtered.isNotEmpty()) {
                    layoutManager.onRestoreInstanceState(s)
                    pendingScroll = null
                }
            }
        } else {
            tvSummary.text = if (state is GalleryState.Loading) "로그를 불러오는 중…" else ""
            tvSummary.setTextColor(ctx.getColor(R.color.log_text_sub))
            btnSummaryAction.visibility = View.GONE
            if (state is GalleryState.Failed) adapter.items = emptyList()
        }

        // 가운데 안내
        if (empty != null && !busy) {
            if (layoutEmpty.visibility != View.VISIBLE) {
                // 안내가 처음 나타날 때: 아이콘은 톡, 글·버튼은 차례로 떠오름
                (layoutEmpty as? ViewGroup)?.let { g -> for (i in 0 until g.childCount) g.getChildAt(i).riseIn(i) }
            }
            layoutEmpty.visibility = View.VISIBLE
            tvEmptyTitle.text = empty.title
            tvEmptyDesc.text = empty.desc
            bindButton(btnEmptyPrimary, empty.primary)
            bindButton(btnEmptySecondary, empty.secondary)
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun bindButton(button: TextView, action: Pair<String, () -> Unit>?) {
        if (action == null) {
            button.visibility = View.GONE
            button.setOnClickListener(null)
        } else {
            button.visibility = View.VISIBLE
            button.text = action.first
            button.setOnClickListener { action.second() }
        }
    }

    private fun hideKeyboard() {
        etSearch.clearFocus()
        ctx.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }
}

/* ───────────── 사진 칸 어댑터 (두 줄 엇갈림 · 달마다 제목) ───────────── */

private class GalleryAdapter(
    private val onClick: (TripLog, Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<GalleryItem> = emptyList()
        set(value) {
            if (field == value) return
            field = value
            lastAnimated = -1 // 목록이 바뀌면(검색·기간) 다시 차례로 나타난다
            notifyDataSetChanged()
        }

    /** 이미 나타나는 효과를 준 마지막 칸 (스크롤로 다시 보일 땐 효과 없음) */
    private var lastAnimated = -1

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is GalleryItem.Header -> TYPE_HEADER
        is GalleryItem.Photo -> TYPE_PHOTO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_trip_log_header, parent, false)
            // 제목 줄은 두 칸을 다 쓴다
            view.layoutParams = StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { isFullSpan = true }
            HeaderHolder(view)
        } else {
            PhotoHolder(inflater.inflate(R.layout.item_trip_log_photo, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GalleryItem.Header -> (holder as HeaderHolder).bind(item)
            is GalleryItem.Photo -> (holder as PhotoHolder).bind(item, onClick)
        }
        // 처음 보이는 칸: 아래에서 살짝 커지며 떠오른다 (화면에 같이 보이는 칸끼리 조금씩 늦게)
        val v = holder.itemView
        if (position > lastAnimated) {
            lastAnimated = position
            val d = v.resources.displayMetrics.density
            v.animate().cancel()
            v.alpha = 0f
            v.translationY = 12f * d
            v.scaleX = 1f
            v.scaleY = 1f
            v.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay((position % 6) * 40L)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            v.alpha = 1f; v.translationY = 0f; v.scaleX = 1f; v.scaleY = 1f
        }
    }

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.tv_header_label)
        private val detail: TextView = view.findViewById(R.id.tv_header_detail)
        fun bind(item: GalleryItem.Header) {
            label.text = item.label
            detail.text = item.detail
        }
    }

    private class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tile: FrameLayout = view.findViewById(R.id.tile)
        private val photo: ImageView = view.findViewById(R.id.iv_photo)
        private val noPhoto: View = view.findViewById(R.id.iv_no_photo)
        private val scrim: View = view.findViewById(R.id.scrim)
        private val caption: LinearLayout = view.findViewById(R.id.layout_caption)
        private val title: TextView = view.findViewById(R.id.tv_tile_title)
        private val date: TextView = view.findViewById(R.id.tv_tile_date)
        private val count: TextView = view.findViewById(R.id.tv_tile_count)

        fun bind(item: GalleryItem.Photo, onClick: (TripLog, Int) -> Unit) {
            val density = itemView.resources.displayMetrics.density
            photo.layoutParams = photo.layoutParams.apply { height = (item.heightDp * density).toInt() }

            if (item.ref != null) {
                noPhoto.visibility = View.GONE
                photo.loadPhotoRef(item.ref, R.drawable.travel_main_photo)
            } else {
                noPhoto.visibility = View.VISIBLE
                photo.dispose()
                photo.setImageDrawable(null)
            }

            // 표지(로그의 첫 사진)에만 제목·날짜·사진 수
            val cover = item.isCover
            scrim.visibility = if (cover) View.VISIBLE else View.GONE
            caption.visibility = if (cover) View.VISIBLE else View.GONE
            count.visibility = if (cover && item.log.photos.size > 1) View.VISIBLE else View.GONE
            if (cover) {
                title.text = item.log.title
                date.text = listOf(formatDateRange(item.log.startDate, item.log.endDate).substring(5), item.log.destination)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                count.text = item.log.photos.size.toString()
            }

            tile.bounceOnPress(0.96f)
            tile.contentDescription = "${item.log.title} 사진 ${item.index + 1}"
            tile.setOnClickListener { onClick(item.log, item.index) }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_PHOTO = 1
    }
}


/* ───────────── 공용 View 움직임 (사진 모아보기 · 후기 상세가 같이 씀) ───────────── */

/** 아래에서 떠오르며 나타남 */
internal fun View.riseIn(order: Int, distanceDp: Float = 10f) {
    val d = resources.displayMetrics.density
    animate().cancel()
    alpha = 0f
    translationY = distanceDp * d
    animate().alpha(1f).translationY(0f)
        .setStartDelay(order.coerceAtMost(8) * 40L)
        .setDuration(300)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

/** 부드럽게 나타남 (살짝 작게 시작) */
internal fun View.popIn(delay: Long = 0L) {
    animate().cancel()
    alpha = 0f
    scaleX = 0.96f
    scaleY = 0.96f
    animate().alpha(1f).scaleX(1f).scaleY(1f)
        .setStartDelay(delay)
        .setDuration(220)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

/** 누르는 동안 아주 살짝 작아졌다가 놓으면 돌아옴 (클릭은 그대로) */
@SuppressLint("ClickableViewAccessibility")
internal fun View.bounceOnPress(scale: Float = 0.97f) {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(scale.coerceAtLeast(0.97f)).scaleY(scale.coerceAtLeast(0.97f)).setStartDelay(0).setDuration(80).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(160).setInterpolator(DecelerateInterpolator()).start()
        }
        false
    }
}
