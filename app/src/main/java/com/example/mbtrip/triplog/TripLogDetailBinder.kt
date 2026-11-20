package com.example.mbtrip.triplog

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.dispose
import com.example.mbtrip.R
import com.example.mbtrip.timeline.model.Member

/**
 * res/layout/activity_trip_log_detail.xml 과 데이터를 이어 준다.
 *  - 사진이 여러 장: 옆으로 넘기는 슬라이드(ViewPager2) + 아래 점 + 오른쪽 위 "1 / 3"
 *  - 오른쪽 아래 프로필: 겹쳐 놓고, 누르면 onMemberClick → 정보 모달 (Compose)
 */
class TripLogDetailBinder(val root: View) {
    private val ctx = root.context
    private val density = ctx.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private val tvToolbarTitle: TextView = root.findViewById(R.id.tv_toolbar_title)
    private val pager: ViewPager2 = root.findViewById(R.id.vp_photos)
    private val tvCounter: TextView = root.findViewById(R.id.tv_photo_counter)
    private val progress: ProgressBar = root.findViewById(R.id.progress)
    private val dots: LinearLayout = root.findViewById(R.id.layout_dots)
    private val tvDate: TextView = root.findViewById(R.id.tv_date)
    private val tvPlace: TextView = root.findViewById(R.id.tv_place)
    private val tvTitle: TextView = root.findViewById(R.id.tv_title)
    private val tvStars: TextView = root.findViewById(R.id.tv_stars)
    private val btnEdit: View = root.findViewById(R.id.btn_edit)
    private val btnDelete: View = root.findViewById(R.id.btn_delete)
    private val tvRatingValue: TextView = root.findViewById(R.id.tv_rating_value)
    private val tvAuthor: TextView = root.findViewById(R.id.tv_author)
    private val tvContent: TextView = root.findViewById(R.id.tv_content)
    private val tvMemberCount: TextView = root.findViewById(R.id.tv_member_count)
    private val avatars: LinearLayout = root.findViewById(R.id.layout_avatars)

    var onBack: () -> Unit = {}
    /** 내가 쓴 후기의 수정 · 삭제 아이콘 */
    var onEdit: () -> Unit = {}
    var onDelete: () -> Unit = {}
    var onMemberClick: (Member) -> Unit = {}

    private val photoAdapter = PhotoPagerAdapter()
    private var boundLog: TripLog? = null
    private var boundMembers: List<Member>? = null
    private var membersExpanded = false

    init {
        root.findViewById<View>(R.id.btn_back).setOnClickListener { onBack() }
        pager.adapter = photoAdapter
        pager.offscreenPageLimit = 1
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showPage(position)
        })
        // 사진을 넘길 때 옆 사진은 살짝 흐리게
        pager.setPageTransformer { page, pos ->
            val a = kotlin.math.abs(pos).coerceAtMost(1f)
            page.alpha = 1f - 0.3f * a
        }
        root.findViewById<View>(R.id.btn_back).bounceOnPress()
        btnEdit.setOnClickListener { onEdit() }
        btnDelete.setOnClickListener { onDelete() }
        btnEdit.bounceOnPress()
        btnDelete.bounceOnPress()
    }

    fun render(log: TripLog?, members: List<Member>, startPhotoIndex: Int) {
        progress.visibility = if (log == null) View.VISIBLE else View.GONE
        if (log == null) return

        // 수정하고 돌아오면 같은 로그라도 내용이 바뀌어 있어서, 내용까지 비교한다
        if (log != boundLog) {
            val sameLog = boundLog?.key == log.key
            boundLog = log
            if (!sameLog) membersExpanded = false
            bindLog(log, startPhotoIndex)
        }
        // 내가 쓴 후기만 수정 · 삭제 아이콘
        val mine = log.isMine()
        btnEdit.visibility = if (mine) View.VISIBLE else View.GONE
        btnDelete.visibility = if (mine) View.VISIBLE else View.GONE
        if (members != boundMembers) {
            boundMembers = members
            bindMembers(log, members)
        }
    }

    private fun bindLog(log: TripLog, startPhotoIndex: Int) {
        tvToolbarTitle.text = log.destination.ifBlank { "여행 로그" }

        // 사진 슬라이드
        val photos: List<String?> = log.photos.ifEmpty { listOf(null) }
        photoAdapter.photos = photos
        buildDots(photos.size)
        val start = startPhotoIndex.coerceIn(0, photos.lastIndex)
        pager.setCurrentItem(start, false)
        showPage(start)

        tvDate.text = "${formatDateRange(log.startDate, log.endDate)} · ${nightsAndDays(log.startDate, log.endDate)}"
        tvPlace.text = log.destination
        tvPlace.visibility = if (log.destination.isBlank()) View.GONE else View.VISIBLE

        tvTitle.text = log.title
        // 별점: 후기 쓰기 화면과 같은 검은 별 + 한마디
        val n = log.satisfaction.coerceIn(0, 5)
        if (n > 0) {
            tvStars.visibility = View.VISIBLE
            tvStars.text = "★".repeat(n) + "☆".repeat(5 - n)
            tvStars.contentDescription = "만족도 5점 중 ${n}점"
            tvRatingValue.text = satisfactionLabel(n)
        } else {
            tvStars.visibility = View.GONE
            tvRatingValue.text = "아직 평가 없음"
        }
        tvContent.text = log.content.ifBlank { "남긴 글이 없어요." }
        tvContent.setTextColor(ctx.getColor(if (log.content.isBlank()) R.color.log_text_hint else R.color.log_text_body))

        // ── 움직임: 사진은 서서히, 아래 글은 차례로 나타난다 ──
        root.findViewById<View>(R.id.card_photos).apply {
            alpha = 0f
            animate().alpha(1f).setDuration(300)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        listOf(dots, tvDate.parent as View, tvTitle, tvStars.parent as View, tvContent)
            .forEachIndexed { i, v -> v.riseIn(i + 2) }
    }

    /* ───── 사진 슬라이드 · 점 ───── */

    private fun buildDots(count: Int) {
        dots.removeAllViews()
        // 사진이 1장이면 점과 숫자를 숨긴다
        val many = count > 1
        dots.visibility = if (many) View.VISIBLE else View.GONE
        tvCounter.visibility = if (many) View.VISIBLE else View.GONE
        if (!many) return
        repeat(count) { i ->
            dots.addView(
                View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(6), dp(6)).apply {
                        marginStart = if (i == 0) 0 else dp(6)
                    }
                    setBackgroundResource(R.drawable.bg_log_dot)
                    // 점을 눌러도 그 사진으로
                    setOnClickListener { pager.currentItem = i }
                },
            )
        }
    }

    private fun showPage(position: Int) {
        val total = photoAdapter.itemCount
        tvCounter.text = "${position + 1} / $total"
        for (i in 0 until dots.childCount) {
            val dot = dots.getChildAt(i)
            val active = i == position
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            val target = dp(if (active) 18 else 6)
            dot.setBackgroundResource(if (active) R.drawable.bg_log_dot_active else R.drawable.bg_log_dot)
            // 지금 사진 점은 길쭉하게 늘어나고, 이전 점은 줄어든다
            if (lp.width != target) {
                ValueAnimator.ofInt(lp.width, target).apply {
                    duration = 260
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        lp.width = it.animatedValue as Int
                        dot.layoutParams = lp
                    }
                    start()
                }
            }
        }
    }

    /* ───── 함께한 사람 (오른쪽 아래) ───── */

    private fun bindMembers(log: TripLog, members: List<Member>) {
        val author = members.firstOrNull { it.userId == log.authorId }
        tvAuthor.text = author?.let { "· ${it.nickname} 님의 기록" }.orEmpty()

        tvMemberCount.text = if (members.isEmpty()) "불러오는 중…" else "${members.size}명 · 사진을 누르면 프로필"
        avatars.removeAllViews()

        val limit = if (membersExpanded) members.size else MAX_STACKED
        val shown = members.take(limit)
        val hidden = members.size - shown.size
        val gap = if (membersExpanded) dp(6) else -dp(12) // 접혀 있으면 살짝 겹쳐 놓기

        shown.forEachIndexed { i, m ->
            avatars.addView(avatarView(m), avatarParams(if (i == 0) 0 else gap))
        }
        if (hidden > 0) {
            avatars.addView(moreView(hidden), avatarParams(if (shown.isEmpty()) 0 else gap))
        }
        popAvatars()
    }

    /** 프로필이 차례로 나타난다 */
    private fun popAvatars() {
        for (i in 0 until avatars.childCount) {
            avatars.getChildAt(i).apply { bounceOnPress(); popIn(i * 40L + 150L) }
        }
    }

    private fun avatarParams(marginStart: Int) =
        LinearLayout.LayoutParams(dp(AVATAR_DP), dp(AVATAR_DP)).apply { this.marginStart = marginStart }

    private fun avatarView(member: Member): View = FrameLayout(ctx).apply {
        setBackgroundResource(R.drawable.bg_log_avatar_ring)
        setPadding(dp(2), dp(2), dp(2), dp(2))
        contentDescription = "${member.nickname} 프로필 보기"
        isClickable = true
        foreground = ctx.getDrawable(R.drawable.bg_log_icon_button)
        setOnClickListener { onMemberClick(member) }
        addView(
            ImageView(ctx).apply {
                setBackgroundResource(R.drawable.bg_log_avatar_inner)
                clipToOutline = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                loadPhotoRef(member.photoUrl, R.drawable.default_avatar)
            },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    /** +N — 누르면 겹친 걸 펼쳐서 모두 보여준다 */
    private fun moreView(hidden: Int): View = FrameLayout(ctx).apply {
        setBackgroundResource(R.drawable.bg_log_avatar_ring)
        setPadding(dp(2), dp(2), dp(2), dp(2))
        contentDescription = "${hidden}명 더 보기"
        setOnClickListener {
            membersExpanded = true
            boundMembers?.let { bindMembersExpanded(it) }
        }
        addView(
            TextView(ctx).apply {
                setBackgroundResource(R.drawable.bg_log_avatar_more)
                gravity = Gravity.CENTER
                text = "+$hidden"
                textSize = 12f
                setTextColor(ctx.getColor(android.R.color.white))
                paint.isFakeBoldText = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    private fun bindMembersExpanded(members: List<Member>) {
        avatars.removeAllViews()
        members.forEachIndexed { i, m -> avatars.addView(avatarView(m), avatarParams(if (i == 0) 0 else dp(6))) }
        popAvatars()
    }

    private companion object {
        const val AVATAR_DP = 40
        const val MAX_STACKED = 4
    }
}

/** 슬라이드 한 장 = 사진 한 장 */
private class PhotoPagerAdapter : RecyclerView.Adapter<PhotoPagerAdapter.Holder>() {
    var photos: List<String?> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = photos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val image = ImageView(parent.context).apply {
            // ViewPager2 의 한 장은 꽉 채워야 한다
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return Holder(image)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val ref = photos[position]
        holder.image.contentDescription = "사진 ${position + 1} / ${photos.size}"
        if (ref == null) {
            holder.image.dispose()
            holder.image.scaleType = ImageView.ScaleType.CENTER
            holder.image.setImageResource(R.drawable.ic_log_photos)
            holder.image.imageTintList = holder.image.context.getColorStateList(R.color.log_sky)
        } else {
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.image.imageTintList = null
            holder.image.loadPhotoRef(ref, R.drawable.travel_main_photo)
        }
    }

    class Holder(val image: ImageView) : RecyclerView.ViewHolder(image)
}

/** 별 수 → 한마디 (후기 쓰기 화면과 같은 말) */
internal fun satisfactionLabel(n: Int): String = when (n) {
    1 -> "아쉬웠어요"
    2 -> "그저 그랬어요"
    3 -> "괜찮았어요"
    4 -> "좋았어요"
    5 -> "최고였어요!"
    else -> "아직 평가 없음"
}
