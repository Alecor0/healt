package com.healmate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.healmate.ui.navigation.Screen
import com.healmate.ui.theme.Motion

private data class NavigationItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val navigationItems = listOf(
    NavigationItem(Screen.Dashboard, "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    NavigationItem(Screen.Medications, "Medicación", Icons.Filled.Medication, Icons.Outlined.Medication),
    NavigationItem(Screen.Appointments, "Citas", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NavigationItem(Screen.Health, "Salud", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    NavigationItem(Screen.Profile, "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun HealthMateBottomBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    // Al elegirla, el icono rebota y pasa de contorno a relleno.
                    val bounce by animateFloatAsState(
                        if (isSelected) 1.18f else 1f, Motion.playful(), label = "navBounce"
                    )
                    Crossfade(
                        isSelected,
                        animationSpec = Motion.standard(Motion.SHORT),
                        label = "navIcon",
                        modifier = Modifier.graphicsLayer { scaleX = bounce; scaleY = bounce }
                    ) { selected ->
                        Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthMateTopBar(
    title: String,
    isOffline: Boolean = false,
    syncPending: Boolean = false,
    // null oculta el interruptor del modo sin conexión simulado (solo existe en la demostración).
    onOfflineToggle: (() -> Unit)? = {}
) {
    TopAppBar(
        title = {
            Column {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn(Motion.standard(Motion.SHORT)) togetherWith fadeOut(Motion.standard(Motion.SHORT)) },
                    label = "title"
                ) { value ->
                    Text(text = value, style = MaterialTheme.typography.titleLarge)
                }
                // El aviso de conexión solo aparece cuando hace falta.
                AnimatedVisibility(
                    visible = isOffline || syncPending,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = if (syncPending) "Cambios pendientes de sincronizar" else "Sin conexión",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        actions = {
            if (onOfflineToggle != null) IconButton(onClick = onOfflineToggle) {
                Crossfade(isOffline, label = "wifi") { offline ->
                    Icon(
                        imageVector = if (offline) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = if (offline) "Desactivar modo sin conexión" else "Activar modo sin conexión",
                        tint = if (offline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
