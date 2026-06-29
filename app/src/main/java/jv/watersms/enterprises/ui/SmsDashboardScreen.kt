package jv.watersms.enterprises.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jv.watersms.enterprises.data.Campaign
import jv.watersms.enterprises.data.Recipient
import jv.watersms.enterprises.ui.components.WaterSmsLogo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SmsDashboardScreen(
    viewModel: SmsViewModel,
    onCreateNewCampaign: () -> Unit,
    onCampaignClick: (Long) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val campaigns by viewModel.campaigns.collectAsState()
    val selectedCampaignId by viewModel.selectedCampaignId.collectAsState()
    val selectedCampaign by viewModel.selectedCampaign.collectAsState()
    val recipients by viewModel.selectedCampaignRecipients.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        if (selectedCampaignId != null && selectedCampaign != null && onBack != null) {
            CampaignDetailsView(
                campaign = selectedCampaign!!,
                recipients = recipients,
                onBack = onBack,
                onPause = { viewModel.pauseCampaignSending(selectedCampaign!!.id) },
                onResume = { viewModel.startCampaignSending(selectedCampaign!!.id) },
                onDelete = {
                    viewModel.deleteCampaign(selectedCampaign!!.id)
                    onBack()
                }
            )
        } else {
            if (campaigns.isEmpty()) {
                EmptyCampaignsState(onCreateNewCampaign)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Analytics Dashboard",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Aggregated campaign dispatch telemetry from SQLite local database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        GlobalAggregateTelemetryCard(campaigns = campaigns)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Campaigns",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            TextButton(onClick = onCreateNewCampaign) {
                                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New")
                            }
                        }
                    }

                    items(campaigns) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            recipientsFlow = viewModel.getRecipientsFlow(campaign.id),
                            onClick = { onCampaignClick(campaign.id) },
                            onPause = { viewModel.pauseCampaignSending(campaign.id) },
                            onResume = { viewModel.startCampaignSending(campaign.id) },
                            onDelete = { viewModel.deleteCampaign(campaign.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalAggregateTelemetryCard(campaigns: List<Campaign>) {
    var totalCampaigns = campaigns.size
    var activeCampaigns = campaigns.count { it.status == "SENDING" }
    var completedCampaigns = campaigns.count { it.status == "COMPLETED" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Stats",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "System Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "SQLite Local Engine",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TelemetryStatBlock("Total Campaigns", totalCampaigns.toString(), MaterialTheme.colorScheme.primary)
                TelemetryStatBlock("Sending Bulk", activeCampaigns.toString(), Color(0xFF60A5FA))
                TelemetryStatBlock("Completed", completedCampaigns.toString(), Color(0xFF34D399))
            }
        }
    }
}

@Composable
fun TelemetryStatBlock(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignCard(
    campaign: Campaign,
    recipientsFlow: kotlinx.coroutines.flow.Flow<List<Recipient>>,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(campaign.createdAt) { dateFormat.format(Date(campaign.createdAt)) }

    val recipients by recipientsFlow.collectAsState(initial = emptyList())
    val totalCount = recipients.size
    val sentCount = recipients.count { it.status == "SENT" }
    val failedCount = recipients.count { it.status == "FAILED" }
    val processedCount = sentCount + failedCount
    val progress = if (totalCount > 0) processedCount.toFloat() / totalCount else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val (badgeColor, textColor, label) = when (campaign.status) {
                    "SENDING" -> Triple(MaterialTheme.colorScheme.primary, Color.White, "SENDING")
                    "PAUSED" -> Triple(Color(0xFFE5A93C), Color.White, "PAUSED")
                    "COMPLETED" -> Triple(Color(0xFF4CAF50), Color.White, "COMPLETED")
                    else -> Triple(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.onSurface, campaign.status)
                }

                Surface(
                    color = if (campaign.status == "SENDING") badgeColor.copy(alpha = alpha) else badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (campaign.status == "SENDING") Color.White else badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = campaign.originalMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (totalCount > 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dispatched: $sentCount / $totalCount successfully ($failedCount failed)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = "Delay",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${campaign.minDelaySeconds}-${campaign.maxDelaySeconds}s delay",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (campaign.status == "SENDING") {
                        IconButton(
                            onClick = onPause,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                        }
                    } else if (campaign.status == "PAUSED" || campaign.status == "PENDING") {
                        IconButton(
                            onClick = onResume,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CampaignDetailsView(
    campaign: Campaign,
    recipients: List<Recipient>,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedVariations by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val totalCount = recipients.size
    val sentCount = recipients.count { it.status == "SENT" }
    val failedCount = recipients.count { it.status == "FAILED" }
    val pendingCount = recipients.count { it.status == "PENDING" || it.status == "SENDING" }

    val progress = if (totalCount > 0) (sentCount + failedCount).toFloat() / totalCount else 0f

    val filteredRecipients = remember(recipients, selectedFilter) {
        when (selectedFilter) {
            "SENT" -> recipients.filter { it.status == "SENT" }
            "FAILED" -> recipients.filter { it.status == "FAILED" }
            "PENDING" -> recipients.filter { it.status == "PENDING" || it.status == "SENDING" }
            else -> recipients
        }
    }

    val averageSendingTimeStr = remember(recipients, campaign.status) {
        val processed = recipients
            .filter { (it.status == "SENT" || it.status == "FAILED") && it.sentAt != null }
            .sortedBy { it.sentAt }

        if (processed.size > 1) {
            val first = processed.first().sentAt!!
            val last = processed.last().sentAt!!
            val durationMs = last - first
            val intervals = processed.size - 1
            val avgMs = durationMs.toDouble() / intervals
            String.format("%.1f s", avgMs / 1000.0)
        } else {
            val avgDelay = (campaign.minDelaySeconds + campaign.maxDelaySeconds) / 2.0
            String.format("%.1f s (est.)", avgDelay)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(campaign.name, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (campaign.status == "SENDING") {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5A93C)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause Dispatch")
                        }
                    } else if (campaign.status == "PAUSED" || campaign.status == "PENDING") {
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume Dispatch")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Dispatch Analytics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Real-time performance analytics metrics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Metrics",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 18f
                                    if (totalCount == 0) {
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.1f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth)
                                        )
                                    } else {
                                        val sweepPending = (pendingCount.toFloat() / totalCount) * 360f
                                        val sweepSent = (sentCount.toFloat() / totalCount) * 360f
                                        val sweepFailed = (failedCount.toFloat() / totalCount) * 360f

                                        var startAngle = -90f
                                        if (sweepSent > 0f) {
                                            drawArc(
                                                color = Color(0xFF34D399),
                                                startAngle = startAngle,
                                                sweepAngle = sweepSent,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            )
                                            startAngle += sweepSent
                                        }
                                        if (sweepFailed > 0f) {
                                            drawArc(
                                                color = Color(0xFFF87171),
                                                startAngle = startAngle,
                                                sweepAngle = sweepFailed,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            )
                                            startAngle += sweepFailed
                                        }
                                        if (sweepPending > 0f) {
                                            drawArc(
                                                color = Color(0xFFC084FC),
                                                startAngle = startAngle,
                                                sweepAngle = sweepPending,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            )
                                        }
                                    }
                                }

                                val successRate = if (totalCount > 0) ((sentCount.toFloat() / totalCount) * 100).toInt() else 0
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$successRate%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "success",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TelemetryMetricLine(
                                    dotColor = Color(0xFF34D399),
                                    label = "Successful Deliv.",
                                    value = sentCount.toString()
                                )
                                TelemetryMetricLine(
                                    dotColor = Color(0xFFF87171),
                                    label = "Failed Deliveries",
                                    value = failedCount.toString()
                                )
                                TelemetryMetricLine(
                                    dotColor = Color(0xFFC084FC),
                                    label = "Pending/Sending",
                                    value = pendingCount.toString()
                                )
                                TelemetryMetricLine(
                                    dotColor = Color.White,
                                    label = "Avg Sending Time",
                                    value = averageSendingTimeStr
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total dispatched: ${sentCount + failedCount} / $totalCount messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% Done",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedVariations = !expandedVariations },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Original & AI Variations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                if (expandedVariations) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Original: ${campaign.originalMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = expandedVariations) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "AI Paraphrasings used for anti-spam variety:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val variations = remember(campaign.variationsJson) {
                                    try {
                                        val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                                        val type = Types.newParameterizedType(List::class.java, String::class.java)
                                        moshi.adapter<List<String>>(type).fromJson(campaign.variationsJson) ?: emptyList()
                                    } catch (e: Exception) {
                                        emptyList<String>()
                                    }
                                }

                                if (variations.isEmpty()) {
                                    Text("No variations generated. Using original text only.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                } else {
                                    variations.forEachIndexed { idx, varText ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "${idx + 1}.",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Text(
                                                text = varText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All ($totalCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Pending ($pendingCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "SENT",
                        onClick = { selectedFilter = "SENT" },
                        label = { Text("Sent ($sentCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF34D399)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "FAILED",
                        onClick = { selectedFilter = "FAILED" },
                        label = { Text("Failed ($failedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF87171).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFF87171)
                        )
                    )
                }
            }

            if (filteredRecipients.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No contacts in this list.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredRecipients) { recipient ->
                    RecipientItem(recipient)
                }
            }
        }
    }
}

@Composable
fun TelemetryMetricLine(
    dotColor: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
fun RecipientItem(recipient: Recipient, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initial = recipient.name.firstOrNull()?.uppercase() ?: "?"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initial,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = recipient.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = recipient.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val (color, label) = when (recipient.status) {
                    "SENT" -> Pair(Color(0xFF34D399), "SENT")
                    "FAILED" -> Pair(Color(0xFFF87171), "FAILED")
                    "SENDING" -> Pair(Color(0xFF60A5FA), "SENDING")
                    else -> Pair(MaterialTheme.colorScheme.outline, "PENDING")
                }

                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!recipient.sentMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Phrasing selected:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = recipient.sentMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!recipient.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Error: ${recipient.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyCampaignsState(onCreateNewCampaign: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaterSmsLogo(size = 100.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Campaigns Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Send bulk text messages efficiently with custom delay intervals and AI rephrasing to prevent spam blocks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateNewCampaign,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First Campaign")
        }
    }
}
