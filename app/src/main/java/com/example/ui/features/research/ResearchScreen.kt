package com.example.ui.features.research

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.models.ResearchMode
import com.example.models.ResearchSource
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResearchScreen(
    selectedMode: ResearchMode,
    isResearching: Boolean,
    researchStatus: String,
    researchSummary: String,
    researchSources: List<ResearchSource>,
    onSelectMode: (ResearchMode) -> Unit,
    onExecuteResearch: (String) -> Unit,
    onOpenSourceUrl: (String) -> Unit,
    onSpeakSummary: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val sampleTopics = listOf(
        "Quantum computing error correction",
        "Android AccessibilityService architecture",
        "Solid-state battery breakthroughs",
        "James Webb Space Telescope discoveries"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Web & Deep Research",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Live multi-source retrieval, synthesis & verified clickable citations",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Mode Selector Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResearchMode.entries.forEach { mode ->
                    val active = selectedMode == mode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) CyberCyan.copy(alpha = 0.18f) else ObsidianSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) CyberCyan else BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMode(mode) }
                            .testTag("research_mode_${mode.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) CyberCyan else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Search Input Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Enter topic for ${selectedMode.title}...", color = TextMuted)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                onExecuteResearch(searchQuery)
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("research_query_input")
                )

                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onExecuteResearch(searchQuery)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberCyan)
                        .testTag("research_execute_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Run research",
                        tint = Color.Black
                    )
                }
            }
        }

        // Quick Research Topics
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sampleTopics.forEach { topic ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                searchQuery = topic
                                onExecuteResearch(topic)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (isResearching || researchStatus.isNotBlank()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isResearching) {
                        LinearProgressIndicator(
                            color = CyberCyan,
                            trackColor = ObsidianSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = researchStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isResearching) AmberThinking else EmeraldReady
                    )
                }
            }
        }

        // Synthesized Report Card
        if (researchSummary.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("research_summary_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYNTHESIZED BRIEFING (${selectedMode.title.uppercase()})",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                            IconButton(
                                onClick = { onSpeakSummary(researchSummary) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak research summary",
                                    tint = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = researchSummary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Verified Citations List
        if (researchSources.isNotEmpty()) {
            item {
                Text(
                    text = "VERIFIED SOURCES & CITATIONS (${researchSources.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
            itemsIndexed(researchSources) { idx, source ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenSourceUrl(source.url) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = QuantumViolet.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "[${idx + 1}]",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = source.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open source URL",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = EmeraldReady,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = source.sourceDomain,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldReady
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = source.snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
