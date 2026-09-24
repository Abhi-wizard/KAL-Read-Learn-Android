package com.example.kal.ui.promotions_feed.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kal.data.Post
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun PostCard(
    post: Post,
    currentUserId: String?,
    onLikeClick: (String) -> Unit
) {
    val context = LocalContext.current
    val isLiked = post.likedBy.contains(currentUserId)
    val purchaseLink = post.purchaseLink

    // FIX 1: State to manage synopsis expansion
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- 1. Author Info Row ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Image Placeholder (You'd replace with AsyncImage for real use)
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Author Profile",
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    post.authorPenName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. Book Cover and Synopsis ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                // Book Cover
                AsyncImage(
                    model = post.bookCoverUrl,
                    contentDescription = post.bookTitle,
                    modifier = Modifier
                        .size(100.dp, 150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        post.bookTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Synopsis:",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )
                    // FIX 1: Synopsis text made clickable to toggle expansion
                    Text(
                        post.bookSynopsis,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. Author's Comment ---
            Text(
                post.authorComment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))


            // --- 4. Actions: Like & Buy ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLikeClick(post.id) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Post",

                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(post.likesCount.toString(), style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Buy Button (Only visible if link exists)
                if (purchaseLink != null) {

                    Button(
                        onClick = {
                            // Open the external link
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(purchaseLink))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),

                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Buy", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))

                        Text("Buy Physical Book", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }
}