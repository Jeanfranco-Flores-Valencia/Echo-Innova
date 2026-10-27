package com.floresvalencia.baymaxecho.ui.navigation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.floresvalencia.baymaxecho.data.model.EmergencyMode
import com.floresvalencia.baymaxecho.data.model.Role
import com.floresvalencia.baymaxecho.ui.components.AlarmOverlay
import com.floresvalencia.baymaxecho.ui.components.EmergencyBanner
import com.floresvalencia.baymaxecho.ui.components.StartEmergencyDialog
import com.floresvalencia.baymaxecho.ui.components.StopEmergencyDialog
import com.floresvalencia.baymaxecho.ui.screens.alerts.AlertsScreen
import com.floresvalencia.baymaxecho.ui.screens.classroom.ClassroomScreen
import com.floresvalencia.baymaxecho.ui.screens.evacuation.EvacuationScreen
import com.floresvalencia.baymaxecho.ui.screens.fleet.FleetScreen
import com.floresvalencia.baymaxecho.ui.screens.fleet.RoverDetailScreen
import com.floresvalencia.baymaxecho.ui.screens.home.HomeScreen
import com.floresvalencia.baymaxecho.ui.screens.access.RegisterScreen
import com.floresvalencia.baymaxecho.ui.screens.access.StaffAccessScreen
import com.floresvalencia.baymaxecho.ui.screens.monitor.MonitorScreen
import com.floresvalencia.baymaxecho.ui.screens.route.RouteScreen
import com.floresvalencia.baymaxecho.ui.theme.EchoColors
import com.floresvalencia.baymaxecho.ui.theme.LocalEmergencyMode
import kotlinx.coroutines.launch

object Routes {
    const val REGISTER = "register"
    const val STAFF = "staff"
    const val HOME = "home"
    const val MONITOR = "monitor"
    const val FLEET = "fleet"
    const val ROVER = "rover/{id}"
    const val EVACUATION = "evacuation"
    const val CLASSROOM = "classroom"
    const val ROUTE = "route"
    const val ALERTS = "alerts"

    fun rover(id: String) = "rover/$id"
}

enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Inicio", Icons.Rounded.Home),
    MONITOR(Routes.MONITOR, "Cámaras", Icons.Rounded.Videocam),
    FLEET(Routes.FLEET, "Flota", Icons.Rounded.SmartToy),
    EVACUATION(Routes.EVACUATION, "Evacuar", Icons.AutoMirrored.Rounded.DirectionsRun),
    CLASSROOM(Routes.CLASSROOM, "Mi aula", Icons.Rounded.Groups),
    ROUTE(Routes.ROUTE, "Ruta", Icons.Rounded.Map),
    ALERTS(Routes.ALERTS, "Alertas", Icons.Rounded.Notifications),
}

/** Cada rol ve solo las secciones que necesita. */
fun tabsFor(role: Role): List<BottomTab> = when (role) {
    Role.BRIGADISTA, Role.DIRECTOR -> listOf(BottomTab.HOME, BottomTab.MONITOR, BottomTab.FLEET, BottomTab.EVACUATION, BottomTab.ALERTS)
    Role.DOCENTE -> listOf(BottomTab.HOME, BottomTab.CLASSROOM, BottomTab.ROUTE, BottomTab.ALERTS)
    Role.ALUMNO, Role.GENERAL -> listOf(BottomTab.HOME, BottomTab.ROUTE, BottomTab.ALERTS)
}

private val lightTopRoutes = setOf(Routes.EVACUATION, Routes.ALERTS, Routes.ROVER, Routes.CLASSROOM, Routes.ROUTE, Routes.STAFF)
private val accessRoutes = setOf(Routes.REGISTER, Routes.STAFF)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EchoApp(appVm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val app by appVm.ui.collectAsStateWithLifecycle()
    val user = app.user

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg)
        }
    }
    var confirmStop by rememberSaveable { mutableStateOf(false) }

    // Permiso de notificaciones (Android 13+) para la alarma con el teléfono bloqueado.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(user?.username) {
        if (user != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // La sesión decide la navegación:
    //  · sin perfil → registro (una sola vez)
    //  · al registrarse, entrar con PIN o salir del modo personal → Inicio (las pestañas cambian según el rol)
    var lastRole by remember { mutableStateOf(user?.role) }
    LaunchedEffect(user?.username, user?.role, route) {
        if (route == null) return@LaunchedEffect
        val goHome = user != null && (route in accessRoutes || lastRole != user.role)
        when {
            user == null && route !in accessRoutes ->
                nav.navigate(Routes.REGISTER) { popUpTo(nav.graph.id) { inclusive = true } }
            goHome ->
                nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } }
        }
        lastRole = user?.role
    }

    val loggedIn = user != null && route != null && route !in accessRoutes
    val showBanner = loggedIn && app.emergency.active
    val real = app.emergency.mode == EmergencyMode.REAL

    val view = LocalView.current
    val darkIcons = !showBanner && route != null && route in lightTopRoutes
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
    }

    val tabs = user?.let { tabsFor(it.role) } ?: emptyList()
    val navigateTab: (String) -> Unit = { target ->
        nav.navigate(target) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigate: (String) -> Unit = { target ->
        if (tabs.any { it.route == target }) navigateTab(target) else nav.navigate(target)
    }

    // Marco rojo pulsante alrededor de toda la app durante una emergencia REAL.
    val frame = rememberInfiniteTransition(label = "frame")
    val frameAlpha by frame.animateFloat(0.45f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "frameAlpha")

    CompositionLocalProvider(LocalEmergencyMode provides if (loggedIn) app.emergency.mode else EmergencyMode.NONE) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = EchoColors.Bg,
                topBar = {
                    if (showBanner) {
                        EmergencyBanner(
                            mode = app.emergency.mode,
                            elapsed = app.elapsed,
                            instruction = app.instruction,
                            canFinish = user?.role?.canStartEvacuation == true,
                            onFinish = { confirmStop = true },
                        )
                    }
                },
                bottomBar = {
                    if (loggedIn) {
                        EchoBottomBar(tabs, currentRoute = route, alertCount = app.activeAlerts, alarm = real, onSelect = navigateTab)
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { inner ->
                NavHost(
                    navController = nav,
                    startDestination = appVm.startDestination,
                    modifier = Modifier
                        .padding(inner)
                        .consumeWindowInsets(inner),
                ) {
                    composable(Routes.REGISTER) {
                        RegisterScreen(onStaffAccess = { nav.navigate(Routes.STAFF) }, onMessage = showMessage)
                    }
                    composable(Routes.STAFF) {
                        // Al validar el PIN cambia el rol y el efecto de arriba lleva a Inicio.
                        StaffAccessScreen(onBack = { nav.popBackStack() }, onSuccess = { showMessage("Modo personal activado") })
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            onNavigate = navigate,
                            onStartEvacuation = appVm::requestStart,
                            onStaffAccess = { nav.navigate(Routes.STAFF) },
                            onExitStaff = {
                                appVm.exitStaffMode()
                                showMessage("Volviste a tu perfil")
                            },
                            onClearProfile = appVm::clearProfile,
                            onMessage = showMessage,
                        )
                    }
                    composable(Routes.MONITOR) { MonitorScreen() }
                    composable(Routes.FLEET) { FleetScreen(onOpenRover = { nav.navigate(Routes.rover(it)) }, onMessage = showMessage) }
                    composable(Routes.ROVER, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        RoverDetailScreen(
                            onBack = { nav.popBackStack() },
                            onOpenCameras = { navigateTab(Routes.MONITOR) },
                            onMessage = showMessage,
                        )
                    }
                    composable(Routes.EVACUATION) { EvacuationScreen(onMessage = showMessage) }
                    composable(Routes.CLASSROOM) { ClassroomScreen(onMessage = showMessage) }
                    composable(Routes.ROUTE) { RouteScreen() }
                    composable(Routes.ALERTS) {
                        AlertsScreen(onStartEvacuation = appVm::requestStart, onOpenRover = { nav.navigate(Routes.rover(it)) })
                    }
                }
            }
            if (showBanner && real) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(4.dp, EchoColors.Red.copy(alpha = frameAlpha))
                )
            }
        }

        // Alarma a pantalla completa (contenido y botones según rol)
        val alarm = app.alarm
        if (loggedIn && alarm != null && app.startRequest == null) {
            AlarmOverlay(
                title = alarm.title,
                location = alarm.location,
                instruction = alarm.instruction,
                primaryText = alarm.primaryText,
                onPrimary = {
                    appVm.acknowledge(alarm.alertId)
                    appVm.requestStart(alarm.cause)
                },
                dismissText = if (alarm.primaryText != null) "Revisar luego" else "Entendido",
                onDismiss = { appVm.acknowledge(alarm.alertId) },
                sign = alarm.sign,
                signText = alarm.signText,
            )
        }

        // Elegir Real / Simulacro (solo roles autorizados llegan aquí)
        app.startRequest?.let { req ->
            StartEmergencyDialog(
                cause = req.cause,
                onDismiss = appVm::cancelStart,
                onStart = { mode ->
                    if (appVm.start(mode)) navigate(Routes.EVACUATION) else showMessage("Tu rol no puede iniciar evacuaciones")
                },
            )
        }

        if (confirmStop) {
            StopEmergencyDialog(
                real = real,
                onDismiss = { confirmStop = false },
                onConfirm = {
                    confirmStop = false
                    showMessage(appVm.stopEmergency() ?: "Tu rol no puede finalizar la evacuación")
                },
            )
        }
    }
}

@Composable
private fun EchoBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    alertCount: Int,
    alarm: Boolean,
    onSelect: (String) -> Unit,
) {
    // El detalle de un rover pertenece a la pestaña Flota.
    val selectedRoute = if (currentRoute == Routes.ROVER) Routes.FLEET else currentRoute
    NavigationBar(containerColor = if (alarm) Color(0xFF140404) else Color.White, tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    if (tab == BottomTab.ALERTS && alertCount > 0) {
                        BadgedBox(badge = { Badge(containerColor = EchoColors.RedText) { Text("$alertCount") } }) {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = if (alarm) NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = EchoColors.Red,
                    unselectedIconColor = Color(0xFFFCA5A5),
                    unselectedTextColor = Color(0xFFFCA5A5),
                ) else NavigationBarItemDefaults.colors(
                    selectedIconColor = EchoColors.Blue,
                    selectedTextColor = EchoColors.Blue,
                    indicatorColor = EchoColors.BlueSoft,
                    unselectedIconColor = EchoColors.Soft,
                    unselectedTextColor = EchoColors.Soft,
                ),
            )
        }
    }
}
