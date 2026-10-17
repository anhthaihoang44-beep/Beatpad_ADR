package com.beatpad.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BackgroundDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPackSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Column {
                Text(
                    text = "Sound Packs",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Original BeatPad Studio Kits",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmberAccent, modifier = Modifier.size(36.dp))
            }
        } else {
            val packs = uiState.packs
            val newPacks = remember(packs) { packs.filter { it.isNew } }
            val genres = remember(packs) { packs.map { it.genre }.distinct() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section "New Releases" with "See All"
                if (newPacks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "New Releases",
                            actionText = "See All",
                            onActionClick = { viewModel.setFilter(null) }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            items(newPacks, key = { it.id }) { pack ->
                                PackCard(
                                    pack = pack,
                                    isPreviewing = uiState.previewingPackId == pack.id,
                                    onPackClick = {
                                        viewModel.onPackSelected(pack.id)
                                        onPackSelected(pack.id)
                                    },
                                    onPreviewToggle = { viewModel.togglePreview(pack) }
                                )
                            }
                        }
                    }
                }

                // Carousels by Genre
                items(genres, key = { it }) { genre ->
                    val genrePacks = packs.filter { it.genre == genre }
                    SectionHeader(
                        title = genre,
                        actionText = "${genrePacks.size} packs"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(genrePacks, key = { it.id }) { pack ->
                            PackCard(
                                pack = pack,
                                isPreviewing = uiState.previewingPackId == pack.id,
                                onPackClick = {
                                    viewModel.onPackSelected(pack.id)
                                    onPackSelected(pack.id)
                                },
                                onPreviewToggle = { viewModel.togglePreview(pack) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null) {
            Text(
                text = actionText,
                color = if (onActionClick != null) AmberAccent else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
