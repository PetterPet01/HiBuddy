package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hibuddy.ui.theme.HiBuddyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                    actions()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        content = content
    )
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    helper: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdminActionCard(
    title: String,
    description: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    urgent: Boolean = false
) {
    val accent = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = if (urgent) 0.34f else 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (urgent) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
                AdminStatusChip(
                    text = count.toString(),
                    tone = if (urgent) AdminChipTone.Error else AdminChipTone.Info
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2
            )
        }
    }
}

enum class AdminChipTone { Neutral, Info, Success, Warning, Error }

@Composable
fun AdminStatusChip(
    text: String,
    tone: AdminChipTone = AdminChipTone.Neutral,
    modifier: Modifier = Modifier
) {
    val container: Color
    val content: Color
    when (tone) {
        AdminChipTone.Success -> {
            container = HiBuddyColors.successContainer
            content = HiBuddyColors.onSuccessContainer
        }
        AdminChipTone.Warning -> {
            container = HiBuddyColors.warningContainer
            content = HiBuddyColors.onWarningContainer
        }
        AdminChipTone.Error -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
        AdminChipTone.Info -> {
            container = HiBuddyColors.infoContainer
            content = HiBuddyColors.onInfoContainer
        }
        AdminChipTone.Neutral -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = container
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AdminInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
fun AdminStateBanner(
    message: String,
    tone: AdminChipTone,
    modifier: Modifier = Modifier
) {
    val icon = when (tone) {
        AdminChipTone.Error -> Icons.Filled.ErrorOutline
        AdminChipTone.Success -> Icons.Filled.CheckCircle
        else -> Icons.Filled.Info
    }
    val container = when (tone) {
        AdminChipTone.Error -> MaterialTheme.colorScheme.errorContainer
        AdminChipTone.Success -> HiBuddyColors.successContainer
        AdminChipTone.Warning -> HiBuddyColors.warningContainer
        AdminChipTone.Info -> HiBuddyColors.infoContainer
        AdminChipTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (tone) {
        AdminChipTone.Error -> MaterialTheme.colorScheme.onErrorContainer
        AdminChipTone.Success -> HiBuddyColors.onSuccessContainer
        AdminChipTone.Warning -> HiBuddyColors.onWarningContainer
        AdminChipTone.Info -> HiBuddyColors.onInfoContainer
        AdminChipTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = content)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AdminEmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Filled.CheckCircle,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HiBuddyColors.success,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminLoadingState(message: String = "Loading admin workspace…") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AdminFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun AdminSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminDestructiveOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
    ) {
        Text(text)
    }
}
