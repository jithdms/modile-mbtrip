package com.example.mbtrip.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.mbtrip.diagnosis.DiagnosisResult
import com.example.mbtrip.diagnosis.DiagnosisScreen
import com.example.mbtrip.friends.FriendListScreen
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.ui.TimelineScreen
import com.example.mbtrip.travel.*
import com.example.mbtrip.createtrip.*
import com.example.mbtrip.ui.screens.MyPageScreen
import com.example.mbtrip.ui.component.TripExampleScreen
import com.example.mbtrip.ui.screen.LandingScreen
import com.example.mbtrip.ui.screens.LoginScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import com.example.mbtrip.auth.*

const val SHOW_DEV_MENU = false
sealed class Screen(val route: String) {
    object DevMenu : Screen("dev_menu")
    object TripSetup : Screen("trip_setup")
    object MyPage : Screen("mypage")
    object TripReview : Screen("trip_review")
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateTrip : Screen("create_trip")
    object Diagnosis : Screen("diagnosis")
    object Timeline : Screen("timeline")
    object Friends : Screen("friends")
}

private data class Loaded<T>(val value: T? = null, val error: String? = null)
private fun <T> Flow<T>.asLoaded(): Flow<Loaded<T>> = map { Loaded(value = it) }.catch {
    if (it is CancellationException) throw it
    emit(Loaded(error = it.message ?: "데이터를 불러오지 못했어요."))
}

@Composable
fun MBTripNavGraph(
    navController: NavHostController = rememberNavController(),
    sessionAccess: SessionAccess = remember { FirebaseSessionAccess() },
) {
    val session by remember(sessionAccess) { sessionAccess.observe() }.collectAsState(initial = sessionAccess.current())
    val initialDestination = remember { if (session?.anonymous == false) Screen.Home.route else Screen.Landing.route }
    val context = LocalContext.current
    val repository = remember { TravelRepository(context) }
    val invites = remember { InviteRepository() }
    var joining by remember { mutableStateOf(false) }
    val scope = remember(session?.uid) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }
    var active by remember { mutableStateOf(repository.restore()) }
    var travelSetup by remember(active?.id) { mutableStateOf<TripSetup?>(null) }
    var creating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var opening by remember { mutableStateOf(false) }
    var tripIdInput by remember { mutableStateOf("") }
    var openError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var previousUid by remember { mutableStateOf(session?.uid) }
    LaunchedEffect(session?.uid) {
        if (previousUid != session?.uid) {
            previousUid = session?.uid
            active = if (session == null) null else repository.restore()
            joining = false; opening = false; creating = false
            tripIdInput = ""; createError = null; openError = null; retry++

        }
    }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    LaunchedEffect(session, currentRoute) {
        if (session?.anonymous != false && currentRoute in listOf(
                Screen.Home.route, Screen.CreateTrip.route, Screen.Diagnosis.route,
                Screen.Friends.route, Screen.Timeline.route, Screen.TripSetup.route, Screen.MyPage.route, Screen.TripReview.route, Screen.DevMenu.route)) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val memberState by remember(active, retry) {
        active?.let { repository.observeMembers(it).asLoaded() }
            ?: flowOf(Loaded(value = emptyList<Member>()))
    }.collectAsState(initial = Loaded())
    val blockState by remember(active, retry) {
        active?.let { repository.observeBlocks(it).asLoaded() }
            ?: flowOf(Loaded(value = emptyList<Block>()))
    }.collectAsState(initial = Loaded())

    NavHost(navController, startDestination = initialDestination) {
        composable(Screen.DevMenu.route) { DevMenuScreen(onOpen = { route ->
            navController.navigate(if (route == Screen.CreateTrip.route) Screen.TripSetup.route else route)
        }) }
        composable(Screen.MyPage.route) { MyPageScreen(
            onBackClick = { navController.popBackStack() },
            onReTestClick = { navController.navigate(Screen.Diagnosis.route) }) }
        composable(Screen.TripReview.route) { TripReviewXmlPreview() }
        composable(Screen.TripSetup.route) {
            val trip = active
            if (trip == null) StatusPage("먼저 여행을 만들어주세요.", onBack = { navController.popBackStack() })
            else TripSetupScreen(members = memberState.value.orEmpty(),
                tripStart = trip.input.start, tripEnd = trip.input.end,
                initial = travelSetup ?: TripSetup(dayTrip = trip.input.start == trip.input.end),
                onBack = { navController.popBackStack() },
                onInviteClick = { navController.navigate(Screen.Friends.route) },
                onSave = { setup -> travelSetup = setup; navController.navigate(Screen.Timeline.route) })
        }
        composable(Screen.Landing.route) { LandingScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController, onAuthenticated = { active = repository.restore(); retry++ }) }
        composable(Screen.Home.route) {
            if (joining) InviteJoinDialog(onDismiss = { joining = false }, onJoin = { code ->
                val tripId = invites.join(code)
                active = repository.openTrip(tripId)
                joining = false
                navController.navigate(Screen.CreateTrip.route)
            })
            if (opening) AlertDialog(
                onDismissRequest = { if (!creating) opening = false },
                title = { Text("참여 중인 여행 불러오기") },
                text = {
                    Column {
                        OutlinedTextField(tripIdInput, onValueChange = { tripIdInput = it }, label = { Text("여행 ID") }, singleLine = true)
                        Text("이미 동행자로 등록된 여행만 불러올 수 있어요.", style = MaterialTheme.typography.bodySmall)
                        openError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = { TextButton(enabled = !creating && tripIdInput.isNotBlank(), onClick = {
                    creating = true; openError = null
                    scope.launch {
                        try { active = repository.openTrip(tripIdInput); opening = false; navController.navigate(Screen.CreateTrip.route) }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { openError = e.message }
                        finally { creating = false }
                    }
                }) { Text(if (creating) "불러오는 중" else "불러오기") } },
                dismissButton = { TextButton(enabled = !creating, onClick = { opening = false }) { Text("취소") } },
            )
            TripExampleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = { navController.navigate(Screen.CreateTrip.route) },
                isCreating = creating,
                error = createError,
                onDiagnosisClick = { navController.navigate(Screen.Diagnosis.route) },
                onOpenTripClick = { openError = null; opening = true },
                onJoinTripClick = { joining = true },
                onMyPageClick = { navController.navigate(Screen.MyPage.route) },
                accountContent = {
                    AccountSessionBar(session, creating, onSignOut = { sessionAccess.signOut() },
                        onLink = { navController.navigate(Screen.Login.route) })
                },
                onCurrentTripClick = active?.let { { navController.navigate(Screen.CreateTrip.route) } },
                onCreateTrip = { departure, destination, dates, count ->
                    val input = runCatching { TripInput.parse(departure, destination, dates, count) }
                    if (input.isFailure) createError = input.exceptionOrNull()?.message
                    else scope.launch {
                        creating = true; createError = null
                        try {
                            active = repository.create(input.getOrThrow())
                            navController.navigate(Screen.CreateTrip.route)
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { createError = "여행을 저장하지 못했어요: ${e.message}" }
                        finally { creating = false }
                    }
                },
            )
        }
        composable(Screen.CreateTrip.route) {
            val trip = active
            if (trip == null) {
                StatusPage("먼저 여행을 만들어주세요.", onBack = { navController.popBackStack() })
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${trip.input.destination} 여행", style = MaterialTheme.typography.headlineSmall)
                Text("${trip.input.start} ~ ${trip.input.end} · ${trip.input.headcount}명")
                Text("여행 ID: ${trip.id}", style = MaterialTheme.typography.bodySmall)
                Text("동행자 ${memberState.value?.size ?: 0}명")
                Button(onClick = { navController.navigate(Screen.TripSetup.route) }) { Text("숙소·교통 입력") }
                if (repository.currentUid() == trip.ownerId) InviteOwnerPanel(
                    onIssue = { invites.issue(trip) }, onRevoke = { invites.revoke(trip) })
                Button(onClick = { navController.navigate(Screen.Diagnosis.route) }) { Text("J-P 진단하기") }
                Button(onClick = { navController.navigate(Screen.Friends.route) }) { Text("여행 동행자 보기") }
                Button(onClick = { navController.navigate(Screen.Timeline.route) }) { Text("일정표·추천 보기") }
                TextButton(onClick = { navController.popBackStack() }) { Text("뒤로") }
            }
        }
        composable(Screen.Diagnosis.route) {
            var lastResult by remember { mutableStateOf<DiagnosisResult?>(null) }
            var saving by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            var saved by remember { mutableStateOf(false) }
            val save: (DiagnosisResult) -> Unit = { result ->
                lastResult = result; saving = true; error = null; saved = false
                scope.launch {
                    try { repository.saveDiagnosis(result.firestoreJpScore, active); saved = true }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = "진단 결과를 저장하지 못했어요: ${e.message}" }
                    finally { saving = false }
                }
            }
            DiagnosisScreen(
                onContinue = { navController.popBackStack() }, onResult = save,
                isSaving = saving, saveError = error, saved = saved,
                onRetry = { lastResult?.let(save) },
            )
        }
        composable(Screen.Friends.route) {
            val trip = active
            when {
                trip == null -> StatusPage("먼저 여행을 만들어주세요.", onBack = { navController.popBackStack() })
                memberState.error != null -> StatusPage(memberState.error!!, { retry++ }, { navController.popBackStack() })
                memberState.value == null -> StatusPage("동행자를 불러오는 중이에요.", onBack = { navController.popBackStack() })
                else -> FriendListScreen(
                    friends = memberState.value.orEmpty(), onBack = { navController.popBackStack() },
                    canRemove = { repository.currentUid() == trip.ownerId && it.userId != trip.ownerId },
                    onDeleteFriend = { repository.removeMember(trip, it.userId) },
                )
            }
        }
        composable(Screen.Timeline.route) {
            val trip = active
            val error = memberState.error ?: blockState.error
            when {
                trip == null -> StatusPage("먼저 여행을 만들어주세요.", onBack = { navController.popBackStack() })
                error != null -> StatusPage(error, { retry++ }, { navController.popBackStack() })
                memberState.value == null || blockState.value == null -> StatusPage("여행 일정을 불러오는 중이에요.", onBack = { navController.popBackStack() })
                else -> TimelineScreen(
                    initialBlocks = blockState.value.orEmpty(), members = memberState.value.orEmpty(),
                    tripId = trip.id, tripStart = trip.input.start, tripEnd = trip.input.end, markers = travelSetup?.toMarkers().orEmpty(),
                    onInviteClick = { navController.navigate(Screen.Friends.route) },
                    onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route); launchSingleTop = true } },
                    onBlocksChange = { before, after -> repository.saveBlocks(trip, before, after) },
                )
            }
        }
    }
}

@Composable
private fun StatusPage(message: String, onRetry: (() -> Unit)? = null, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message)
        if (onRetry != null) Button(onClick = onRetry) { Text("다시 불러오기") }
        TextButton(onClick = onBack) { Text("뒤로") }
    }
}
