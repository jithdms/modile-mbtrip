package com.example.mbtrip.timeline.ui

import com.example.mbtrip.BuildConfig

/*
 * ★ 브이월드 인증키는 여기 한 곳에만 넣는다 ★
 *
 * 방법 1 (권장, git 에 안 올라감): 프로젝트 맨 위 local.properties 에
 *      VWORLD_KEY=발급받은키
 * 방법 2 (간단, git 에 올라감): 아래 VWORLD_KEY_DIRECT 따옴표 안에 붙여넣기
 *
 * 브이월드 인증키 관리에서 활용 API 로 아래 3개를 체크해야 한다.
 *   - 2D지도 API (WMTS/TMS) → 지도 그림
 *   - 검색 API              → 가게·장소 이름 검색
 *   - 지오코더 API          → 지도 누른 곳의 상세 주소
 */
private const val VWORLD_KEY_DIRECT = "1FCCD895-6ABB-4671-93CF-50CA420AB98B"

/**
 * 키를 신청할 때 적은 '서비스 URL'. 브이월드는 요청에 이 주소가 같이 와야 응답한다.
 * 신청서에 다른 주소를 적었다면 똑같이 바꿔야 한다.
 */
internal const val VWORLD_SERVICE_URL = "http://localhost"

/** 실제로 쓸 키. 비어 있으면 지도 대신 안내 문구를 보여준다 */
internal val vworldKey: String
    get() = BuildConfig.VWORLD_KEY.trim().trim('"').ifBlank { VWORLD_KEY_DIRECT.trim() }
