package com.tuempresa.inventariovial.auth

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.R

/** Presentation only: the existing form and its callbacks are composed immediately. */
@Composable
internal fun AuthPage(configured: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val fontScale = LocalDensity.current.fontScale
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(configured) {
        entered = false
        withFrameNanos { }
        entered = true
    }
    // Compose's animation clock observes Android's animator duration scale, including 0.
    // The controls stay present and usable throughout this subtle 200 ms transition.
    val entrance by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(200), label = "auth-entrance"
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFF0F3F8)).testTag("gbo-auth-page")) {
        Image(
            painterResource(if (configured) R.drawable.gbo_login_background else R.drawable.gbo_setup_background),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        val sideBySide = landscape && maxWidth >= 840.dp && fontScale < 1.6f
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding().graphicsLayer { alpha = entrance }) {
            if (sideBySide) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp).testTag("auth-landscape"),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) { AuthBrand(Modifier.widthIn(max = 400.dp).fillMaxWidth()) }
                    AuthFormCard(Modifier.weight(1.35f).fillMaxHeight(), scrollable = true, content)
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp).testTag("auth-column"),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthBrand(Modifier.widthIn(max = 400.dp).fillMaxWidth())
                    AuthFormCard(Modifier.widthIn(max = 600.dp).fillMaxWidth(), scrollable = false, content)
                }
            }
        }
    }
}

@Composable
private fun AuthBrand(modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.gbo_auth_logo), contentDescription = "GBO Ingenieros Consultores",
                contentScale = ContentScale.Fit,
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1387f / 871f)
            )
            Spacer(Modifier.height(12.dp))
            Text("Inventario Vial", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF163F7B))
        }
    }
}

@Composable
private fun AuthFormCard(
    modifier: Modifier, scrollable: Boolean, content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier, shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content
        )
    }
}
