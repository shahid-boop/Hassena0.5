package com.example.ui.features.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.memory.KnowledgeItemEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceGlass
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RagKnowledgeScreen(
    knowledgeItems: List<KnowledgeItemEntity>,
    onAddKnowledge: (title: String, type: String, content: String, sourcePath: String) -> Unit = { _, _, _, _ -> },
    onDeleteKnowledge: (Long) -> Unit = {},
    onAskGroundedQuestion: (query: String, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToResearch: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var groundedResult by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state
    var newTitle by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("DOCUMENT") }
    var newContent by remember { mutableStateOf("") }
    var newSourcePath by remember { mutableStateOf("") }

    val docCount = remember(knowledgeItems) { knowledgeItems.count { it.type == "DOCUMENT" } }
    val linkCount = remember(knowledgeItems) { knowledgeItems.count { it.type == "WEB_LINK" } }
    val noteCount = remember(knowledgeItems) { knowledgeItems.count { it.type == "NOTE" } }

    val filteredList = remember(knowledgeItems, searchQuery) {
        if (searchQuery.isBlank()) {
            knowledgeItems
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            knowledgeItems.filter {
                it.title.lowercase(Locale.US).contains(q) ||
                it.contentSnippet.lowercase(Locale.US).contains(q) ||
                it.fullText.lowercase(Locale.US).contains(q)
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = ObsidianSurfaceElevated,
            titleContentColor = TextPrimary,
            title = {
                Text(
                    text = "Add to Knowledge Base",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Index local documents, web URLs, or notes into JARVIS RAG context.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Type Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurface, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("DOCUMENT", "WEB_LINK", "NOTE").forEach { type ->
                            val isSelected = newType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CyberCyan else Color.Transparent)
                                    .clickable { newType = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (type) {
                                        "DOCUMENT" -> "Document"
                                        "WEB_LINK" -> "Link"
                                        else -> "Note"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text(if (newType == "WEB_LINK") "URL & Description" else "Content / Notes") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                            onAddKnowledge(newTitle, newType, newContent, newSourcePath)
                            newTitle = ""
                            newContent = ""
                            newSourcePath = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text("Index Item", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp)
            .testTag("rag_knowledge_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .testTag("rag_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JARVIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "RAG / Knowledge",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_knowledge_header_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Tabs: "My Knowledge" vs "Web Search"
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyan
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("My Knowledge", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    onNavigateToResearch()
                },
                text = { Text("Web Search", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Card: "Build Your Knowledge Base"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyanDim.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(CyberCyan.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Build Your Knowledge Base",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Add files, websites or documents to give JARVIS more context and better source-grounded answers.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("hero_add_knowledge_button")
                ) {
                    Text("Add Knowledge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stats Summary Row (Documents 12 files, Web Links 5 links, Notes 8 notes)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KnowledgeStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Description,
                count = "$docCount files",
                label = "Documents",
                tint = CyberCyan
            )
            KnowledgeStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Link,
                count = "$linkCount links",
                label = "Web Links",
                tint = QuantumViolet
            )
            KnowledgeStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Note,
                count = "$noteCount notes",
                label = "Notes",
                tint = SolarGold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar with Citation Retrieval
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Ask or search indexed knowledge...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSearching = true
                                onAskGroundedQuestion(searchQuery) { result ->
                                    groundedResult = result
                                    isSearching = false
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Query RAG", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                groundedResult = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianSurface,
                unfocusedContainerColor = ObsidianSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = BorderSubtle
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rag_search_field")
        )

        // Grounded Answer Card (if available)
        if (groundedResult != null || isSearching) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Source-Grounded Response",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSearching) "Searching indexed documents & synthesizing cited answer..." else (groundedResult ?: ""),
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Knowledge Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { item ->
                KnowledgeItemRow(
                    item = item,
                    onDelete = { onDeleteKnowledge(item.id) },
                    onSelect = {
                        searchQuery = item.title
                        isSearching = true
                        onAskGroundedQuestion(item.title) { result ->
                            groundedResult = result
                            isSearching = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun KnowledgeStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    label: String,
    tint: Color
) {
    Card(
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun KnowledgeItemRow(
    item: KnowledgeItemEntity,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val icon = when (item.type) {
        "DOCUMENT" -> Icons.Default.Description
        "WEB_LINK" -> Icons.Default.Link
        else -> Icons.AutoMirrored.Filled.Note
    }
    val iconColor = when (item.type) {
        "DOCUMENT" -> CyberCyan
        "WEB_LINK" -> QuantumViolet
        else -> SolarGold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = item.contentSnippet,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${item.chunkCount} chunks",
                        fontSize = 10.sp,
                        color = CyberCyan
                    )
                    Text(
                        text = "${item.wordCount} words",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete knowledge",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
