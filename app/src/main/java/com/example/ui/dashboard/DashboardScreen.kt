package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Holding
import com.example.data.Transaction
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// Colors reflecting the Sleek Interface design HTML
val BrandBackground = Color(0xFFF8F9FF)
val DarkNavyText = Color(0xFF001E2F)
val SlateGray = Color(0xFF5C5E67)
val CardBackgroundNormal = Color(0xFFFFFFFF)
val PrimaryBlue = Color(0xFF0061A4)
val LightBlueAccent = Color(0xFFD1E4FF)
val DeepPrimaryText = Color(0xFF1A1C1E)
val LightBlueCardText = Color(0xFF001D36)
val GrayBorder = Color(0xFFE0E2EC)
val BottomBarBackground = Color(0xFFF3F4F9)

// Accent Category Colors
val AccentGreen = Color(0xFF1B6B55)
val AccentRed = Color(0xFFB12D00)
val ChartGreen = Color(0xFF4CAF50)
val ChartRed = Color(0xFFF44336)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    var showSplash by remember { mutableStateOf(true) }

    // Auto-transition splash after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        showSplash = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = showSplash,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) with
                            fadeOut(animationSpec = tween(500))
                },
                label = "SplashContainer"
            ) { isSplash ->
                if (isSplash) {
                    SplashScreen(onSkip = { showSplash = false })
                } else {
                    MainAppLayout(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: DashboardViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var showTradeDialogAsset by remember { mutableStateOf<TradeAssetInfo?>(null) }

    // Auto dismiss status snackbar
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(4000)
            viewModel.clearStatusMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
    ) {
        // Core Branding Header
        HeaderView()

        // Content Frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentTab) {
                "Home" -> HomeScreen(
                    viewModel = viewModel,
                    onOpenTrade = { showTradeDialogAsset = it }
                )
                "Markets" -> MarketsScreen(
                    viewModel = viewModel,
                    onOpenTrade = { showTradeDialogAsset = it }
                )
                "Portfolio" -> PortfolioScreen(
                    viewModel = viewModel,
                    onOpenTrade = { showTradeDialogAsset = it }
                )
                "Settings" -> SettingsScreen(viewModel = viewModel)
            }

            // Quick Status Message banner
            statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.contains("Error")) Color(0xFFFFDAD6) else Color(0xFFE2F0D9)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth()
                        .testTag("status_toast")
                        .clickable { viewModel.clearStatusMessage() },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (msg.contains("Error")) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = "Status Icon",
                            tint = if (msg.contains("Error")) Color(0xFFBA1A1A) else Color(0xFF386B11),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = if (msg.contains("Error")) Color(0xFF410002) else Color(0xFF102004),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Custom Navigation Bar to exactly match Design HTML and safe space
        BottomNavigationBar(
            currentTab = currentTab,
            onTabSelected = { viewModel.selectTab(it) }
        )
    }

    // Dynamic Trade Dialog Overlay
    showTradeDialogAsset?.let { asset ->
        TradeDialog(
            asset = asset,
            onDismiss = { showTradeDialogAsset = null },
            onExecute = { symbol, name, category, qty, price, isBuy ->
                viewModel.executeTrade(symbol, name, category, qty, price, isBuy)
                showTradeDialogAsset = null
            }
        )
    }
}

// ----------------- SPLASH SCREEN -----------------

@Composable
fun SplashScreen(onSkip: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashLogoAnim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1B24), Color(0xFF1E3545))
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .rotate(rotation)
                    .drawBehind {
                        // Ambient premium glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(PrimaryBlue.copy(alpha = 0.4f), Color.Transparent),
                                center = center,
                                radius = size.minDimension / 1.5f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                FinuraLogoHex(modifier = Modifier.size(90.dp * scale))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "FINURA",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "INTELLIGENT WEALTH SYSTEMS",
                color = LightBlueAccent.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = LightBlueAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }

        // Skip/Enter early action area
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .testTag("skip_splash_btn"),
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
        ) {
            Text(
                text = "Tap to Launch →",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ----------------- COMPONENT VIEWS -----------------

@Composable
fun FinuraLogoHex(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Colors
        val goldColor = Color(0xFFD4AF37) // Soft gold
        val tealColor = Color(0xFF1B6B55) // Emerald teal

        val center = Offset(w / 2, h / 2)
        val r = w * 0.42f

        // Draw hexagon edges using path
        val hexPath = Path().apply {
            for (i in 0..6) {
                val angle = i * Math.PI / 3 - Math.PI / 6
                val x = center.x + r * Math.cos(angle).toFloat()
                val y = center.y + r * Math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = hexPath,
            color = goldColor.copy(alpha = 0.7f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw inner geometric futuristic pointers directed upwards (exuding growth)
        val upwardArrowPath = Path().apply {
            moveTo(center.x, center.y + r * 0.6f)
            lineTo(center.x, center.y - r * 0.6f) // Centered spine

            // Left inner facet
            moveTo(center.x - r * 0.45f, center.y)
            lineTo(center.x, center.y - r * 0.5f)

            // Right inner facet
            moveTo(center.x + r * 0.45f, center.y)
            lineTo(center.x, center.y - r * 0.5f)

            // Split horizontal connection
            moveTo(center.x - r * 0.4f, center.y + r * 0.35f)
            lineTo(center.x, center.y)
            lineTo(center.x + r * 0.4f, center.y + r * 0.35f)
        }

        drawPath(
            path = upwardArrowPath,
            color = tealColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HeaderView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(colors = listOf(PrimaryBlue, Color(0xFF4FAAFF))),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "FINURA Icon",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "FINURA",
                    color = DarkNavyText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "WEALTH MANAGEMENT",
                    color = SlateGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Circular premium profile badge with dynamic letter representation
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(2.dp, LightBlueAccent, CircleShape)
                .padding(2.dp)
                .background(Color(0xFFE0E2EC), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "FX",
                color = PrimaryBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun BottomNavigationBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf(
        NavigationTabItem("Home", Icons.Default.GridView),
        NavigationTabItem("Markets", Icons.Default.TrendingUp),
        NavigationTabItem("Portfolio", Icons.Default.Wallet),
        NavigationTabItem("Settings", Icons.Default.Settings)
    )

    Surface(
        color = BottomBarBackground,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        border = BorderStroke(1.dp, GrayBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = currentTab == tab.name

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab.name) }
                        .padding(vertical = 8.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isActive) LightBlueAccent else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.name,
                            tint = if (isActive) LightBlueCardText else SlateGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.name,
                        color = if (isActive) LightBlueCardText else SlateGray,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// ----------------- HOME SCREEN -----------------

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onOpenTrade: (TradeAssetInfo) -> Unit
) {
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val chartFilter by viewModel.chartFilter.collectAsStateWithLifecycle()

    // Aggregate portfolio values
    val values = remember(holdings, marketPrices) {
        calculatePortfolioValues(holdings, marketPrices)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Net Worth Hero Card
        item {
            NetWorthCard(totalNetWorth = values.totalValue, last30DaysPct = 12.4)
        }

        // Performance Chart Card
        item {
            PerformanceChartCard(
                filter = chartFilter,
                onFilterSelected = { viewModel.selectChartFilter(it) },
                chartValues = getPerformanceCoordinates(chartFilter, values.totalValue)
            )
        }

        // Asset category header
        item {
            Text(
                text = "Asset Class Allocation",
                color = DarkNavyText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // Asset Allocation breakdown grid
        item {
            AssetAllocationGrid(
                stocksVal = values.stocksValue,
                fundsVal = values.fundsValue,
                retireVal = values.retirementValue
            )
        }

        // Interactive stock tick highlight row
        item {
            QuickTickTicker(marketPrices = marketPrices)
        }
    }
}

@Composable
fun NetWorthCard(totalNetWorth: Double, last30DaysPct: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightBlueAccent),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("net_worth_card")
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Total Net Worth",
                color = LightBlueCardText.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                val wholePart = totalNetWorth.toLong()
                val decimalPart = String.format("%.2f", totalNetWorth - wholePart).drop(1)
                Text(
                    text = "$${formatCostValue(wholePart.toDouble())}",
                    color = LightBlueCardText,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = decimalPart,
                    color = LightBlueCardText.copy(alpha = 0.6f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEAF1FF), RoundedCornerShape(100))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Upward trend",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+$last30DaysPct%",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Last 30 days portfolio growth",
                    color = Color(0xFF43474E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PerformanceChartCard(
    filter: String,
    onFilterSelected: (String) -> Unit,
    chartValues: List<Float>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GrayBorder),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("performance_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio Performance",
                    color = DeepPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = SlateGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth vector-drawn canvas chart representing asset performance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Background reference grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineSpacing = size.height / 4
                    for (i in 1..3) {
                        val y = i * lineSpacing
                        drawLine(
                            color = GrayBorder.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // Smooth growth line path
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 12.dp)
                ) {
                    if (chartValues.isNotEmpty()) {
                        val width = size.width
                        val height = size.height
                        val minVal = chartValues.minOrNull() ?: 0f
                        val maxVal = chartValues.maxOrNull() ?: 100f
                        val range = if (maxVal == minVal) 100f else maxVal - minVal

                        val points = chartValues.mapIndexed { index, value ->
                            val x = (index.toFloat() / (chartValues.size - 1)) * width
                            val y = height - (((value - minVal) / range) * height)
                            Offset(x, y)
                        }

                        val strokePath = Path().apply {
                            if (points.isNotEmpty()) {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val current = points[i]
                                    val previous = points[i - 1]
                                    val cpX = (current.x + previous.x) / 2
                                    cubicTo(
                                        cpX, previous.y,
                                        cpX, current.y,
                                        current.x, current.y
                                    )
                                }
                            }
                        }

                        // Gradient brush for filling under the curve
                        val gradientPath = Path().apply {
                            addPath(strokePath)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }

                        drawPath(
                            path = gradientPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimaryBlue.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )

                        drawPath(
                            path = strokePath,
                            color = PrimaryBlue,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Highlight end point node
                        if (points.isNotEmpty()) {
                            val endPoint = points.last()
                            drawCircle(
                                color = PrimaryBlue,
                                radius = 5.dp.toPx(),
                                center = endPoint
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = endPoint
                            )
                        }
                    }
                }

                // Ultimate visual price marker
                val maxEst = chartValues.maxOrNull() ?: 842000f
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(PrimaryBlue, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "$${formatCostValue(maxEst.toDouble(), precision = 0)}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time filters
            val filters = listOf("1D", "1W", "1M", "3M", "1Y", "ALL")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.forEach { item ->
                    val isSelected = item == filter
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) PrimaryBlue else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onFilterSelected(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            color = if (isSelected) Color.White else SlateGray,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetAllocationGrid(stocksVal: Double, fundsVal: Double, retireVal: Double) {
    val items = listOf(
        AssetGridData("STOCKS", formatCostValue(stocksVal), Icons.Default.ShowChart, PrimaryBlue),
        AssetGridData("MUTUAL FUNDS", formatCostValue(fundsVal), Icons.Default.PieChart, AccentGreen),
        AssetGridData("RETIREMENT", formatCostValue(retireVal), Icons.Default.PendingActions, AccentRed)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asset_allocation_grid"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { data ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(105.dp),
                border = BorderStroke(1.dp, GrayBorder),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = data.title,
                        tint = data.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = data.title,
                        color = SlateGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${data.value}",
                        color = DeepPrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

data class AssetGridData(
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun QuickTickTicker(marketPrices: Map<String, Double>) {
    Card(
        border = BorderStroke(1.dp, GrayBorder),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Simulated Real-Time Price Stream:",
                color = SlateGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            marketPrices.forEach { (sym, price) ->
                if (!sym.startsWith("RET-")) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF1F3F5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sym,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkNavyText
                        )
                        Text(
                            text = "$${String.format("%.2f", price)}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = DeepPrimaryText
                        )
                    }
                }
            }
        }
    }
}

// ----------------- MARKETS SCREEN -----------------

@Composable
fun MarketsScreen(
    viewModel: DashboardViewModel,
    onOpenTrade: (TradeAssetInfo) -> Unit
) {
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val priceChanges by viewModel.priceChanges.collectAsStateWithLifecycle()

    val availableInvestments = listOf(
        MarketAsset("AAPL", "Apple Inc.", "STOCKS"),
        MarketAsset("GOOG", "Alphabet Inc.", "STOCKS"),
        MarketAsset("TSLA", "Tesla Inc.", "STOCKS"),
        MarketAsset("VTSAX", "Vanguard TSM Index", "FUNDS"),
        MarketAsset("VTIAX", "Vanguard Total Intl", "FUNDS"),
        MarketAsset("RET-401K", "Employer Core 401(k)", "RETIREMENT"),
        MarketAsset("RET-IRA", "Target Retirement 2060", "RETIREMENT")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("markets_screen")
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial Markets",
                    color = DarkNavyText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Tap on any asset to execute simulated trade logs",
                    color = SlateGray,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(availableInvestments) { asset ->
                val currentPrice = marketPrices[asset.symbol] ?: 0.0
                val isUp = priceChanges[asset.symbol] ?: true

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_asset_${asset.symbol.lowercase()}")
                        .clickable {
                            onOpenTrade(
                                TradeAssetInfo(
                                    symbol = asset.symbol,
                                    name = asset.name,
                                    currentPrice = currentPrice,
                                    category = asset.category
                                )
                            )
                        },
                    border = BorderStroke(1.dp, GrayBorder),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = when (asset.category) {
                                            "STOCKS" -> PrimaryBlue.copy(alpha = 0.1f)
                                            "FUNDS" -> AccentGreen.copy(alpha = 0.1f)
                                            else -> AccentRed.copy(alpha = 0.1f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (asset.category) {
                                        "STOCKS" -> Icons.Default.ShowChart
                                        "FUNDS" -> Icons.Default.PieChart
                                        else -> Icons.Default.EventNote
                                    },
                                    contentDescription = asset.category,
                                    tint = when (asset.category) {
                                        "STOCKS" -> PrimaryBlue
                                        "FUNDS" -> AccentGreen
                                        else -> AccentRed
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = asset.symbol,
                                    color = DeepPrimaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = asset.name,
                                    color = SlateGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Sparkline visual cue or mock tiny graph
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(25.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val coordinates = if (isUp) {
                                    listOf(0.8f, 0.6f, 0.7f, 0.4f, 0.5f, 0.2f)
                                } else {
                                    listOf(0.2f, 0.4f, 0.3f, 0.6f, 0.5f, 0.8f)
                                }
                                val path = Path()
                                path.moveTo(0f, size.height * coordinates[0])
                                val step = size.width / (coordinates.size - 1)
                                for (i in 1 until coordinates.size) {
                                    path.lineTo(i * step, size.height * coordinates[i])
                                }
                                drawPath(
                                    path = path,
                                    color = if (isUp) ChartGreen else ChartRed,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "$${String.format("%.2f", currentPrice)}",
                                color = DeepPrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUp) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Trend",
                                    tint = if (isUp) ChartGreen else ChartRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isUp) "+0.45%" else "-0.32%",
                                    color = if (isUp) ChartGreen else ChartRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MarketAsset(val symbol: String, val name: String, val category: String)

// ----------------- PORTFOLIO SCREEN -----------------

@Composable
fun PortfolioScreen(
    viewModel: DashboardViewModel,
    onOpenTrade: (TradeAssetInfo) -> Unit
) {
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("Holdings") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_screen")
            .padding(16.dp)
    ) {
        Text(
            text = "Your Investment Portfolio",
            color = DarkNavyText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Track your shares, returns and order logs globally",
            color = SlateGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom Toggle Buttons for holdings vs transactions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BottomBarBackground, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { activeSubTab = "Holdings" },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("portfolio_holdings_subtab"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "Holdings") Color.White else Color.Transparent,
                    contentColor = if (activeSubTab == "Holdings") PrimaryBlue else SlateGray
                ),
                shape = RoundedCornerShape(10.dp),
                elevation = if (activeSubTab == "Holdings") ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "Asset Holdings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeSubTab = "Transactions" },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("portfolio_transactions_subtab"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "Transactions") Color.White else Color.Transparent,
                    contentColor = if (activeSubTab == "Transactions") PrimaryBlue else SlateGray
                ),
                shape = RoundedCornerShape(10.dp),
                elevation = if (activeSubTab == "Transactions") ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "Transaction History", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeSubTab == "Holdings") {
            if (holdings.isEmpty()) {
                EmptyPortfolioState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(holdings) { holding ->
                        val currentPrice = marketPrices[holding.symbol] ?: holding.averagePrice
                        val totalOriginalCost = holding.quantity * holding.averagePrice
                        val currentMarketValue = holding.quantity * currentPrice
                        val returns = currentMarketValue - totalOriginalCost
                        val isGain = returns >= 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenTrade(
                                        TradeAssetInfo(
                                            symbol = holding.symbol,
                                            name = holding.name,
                                            currentPrice = currentPrice,
                                            category = holding.category
                                        )
                                    )
                                },
                            border = BorderStroke(1.dp, GrayBorder),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = when (holding.category) {
                                                        "STOCKS" -> PrimaryBlue
                                                        "FUNDS" -> AccentGreen
                                                        else -> AccentRed
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = holding.symbol,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = holding.name,
                                            color = DeepPrimaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "$${String.format("%,.2f", currentMarketValue)}",
                                        color = DeepPrimaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Divider(color = GrayBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Shares Owned", fontSize = 11.sp, color = SlateGray)
                                        Text(
                                            text = if (holding.symbol.startsWith("RET-401")) "1.00 Unit" else String.format("%,.2f", holding.quantity),
                                            fontSize = 13.sp,
                                            color = DeepPrimaryText,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Avg Cost / Current", fontSize = 11.sp, color = SlateGray)
                                        Text(
                                            text = "$${String.format("%,.2f", holding.averagePrice)} / $${String.format("%,.2f", currentPrice)}",
                                            fontSize = 12.sp,
                                            color = DeepPrimaryText,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Total Return", fontSize = 11.sp, color = SlateGray)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isGain) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = if (isGain) "Gain" else "Loss",
                                                tint = if (isGain) ChartGreen else ChartRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${if (isGain) "+" else ""}$${String.format("%.2f", returns)}",
                                                color = if (isGain) ChartGreen else ChartRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (transactions.isEmpty()) {
                EmptyStateTransactions()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions) { log ->
                        val sdf = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
                        val formattedDate = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }

                        Card(
                            border = BorderStroke(1.dp, GrayBorder),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val isBuy = log.type == "BUY"
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (isBuy) ChartGreen.copy(alpha = 0.12f) else ChartRed.copy(alpha = 0.12f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isBuy) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = log.type,
                                            tint = if (isBuy) ChartGreen else ChartRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${log.type} ${log.symbol}",
                                            color = DeepPrimaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formattedDate,
                                            color = SlateGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val logCost = log.quantity * log.price
                                    Text(
                                        text = "$${String.format("%,.2f", logCost)}",
                                        color = DeepPrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${log.quantity} units @ $${String.format("%.2f", log.price)}",
                                        color = SlateGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPortfolioState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inventory,
            contentDescription = "Empty Holdings",
            tint = SlateGray.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Investment Assets Left",
            fontWeight = FontWeight.Bold,
            color = DarkNavyText,
            fontSize = 15.sp
        )
        Text(
            text = "Open the Markets tab to buy and logs records again.",
            color = SlateGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
    }
}

@Composable
fun EmptyStateTransactions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = "Empty Logs",
            tint = SlateGray.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Orders Registered Yet",
            fontWeight = FontWeight.Bold,
            color = DarkNavyText,
            fontSize = 15.sp
        )
        Text(
            text = "Buy or sell shares to log history entries here.",
            color = SlateGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
    }
}

// ----------------- SETTINGS SCREEN -----------------

@Composable
fun SettingsScreen(viewModel: DashboardViewModel) {
    val simulationEnabled by viewModel.simulationEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "System Settings",
            color = DarkNavyText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Control variables, resets, and parameters safely",
            color = SlateGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            border = BorderStroke(1.dp, GrayBorder),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Interactive Market Simulator",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavyText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "Live Simulated Flashes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepPrimaryText
                        )
                        Text(
                            text = "Toggle automatic price fluctuations every 3.5 seconds.",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                    Switch(
                        checked = simulationEnabled,
                        onCheckedChange = { viewModel.setSimulationEnabled(it) },
                        modifier = Modifier.testTag("simulation_switch")
                    )
                }

                Divider(color = GrayBorder.copy(alpha = 0.5f))

                Text(
                    text = "Wipe & Seed Controls",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavyText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.resetDatabase() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_db_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Reset Defaults", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.wipeDatabase() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wipe_db_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Wipe Portfolio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            border = BorderStroke(1.dp, GrayBorder),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Workspace Security Check",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavyText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GEMINI_API_KEY",
                            fontSize = 11.sp,
                            color = SlateGray,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "AI Studio Key Injection",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepPrimaryText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE2F0D9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Active Sandbox",
                            color = Color(0xFF386B11),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ----------------- TRADING DIALOG OVERLAY -----------------

data class TradeAssetInfo(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeDialog(
    asset: TradeAssetInfo,
    onDismiss: () -> Unit,
    onExecute: (symbol: String, name: String, category: String, qty: Double, price: Double, isBuy: Boolean) -> Unit
) {
    var sharesText by remember { mutableStateOf("10") }
    val shares = sharesText.toDoubleOrNull() ?: 1.0
    val totalCost = shares * asset.currentPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("trade_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, GrayBorder, RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header details
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .background(LightBlueAccent, RoundedCornerShape(100))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = asset.category,
                            color = PrimaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = asset.symbol,
                        color = DarkNavyText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = asset.name,
                        color = SlateGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "$${String.format("%.2f", asset.currentPrice)} per unit",
                        color = DeepPrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Divider(color = GrayBorder)

                // Quantitative controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Specify Quantity / Shares",
                        color = SlateGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = sharesText,
                        onValueChange = { sharesText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trade_quantity_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = GrayBorder
                        )
                    )

                    // Quick share adjustment shortcuts to satisfy great design UX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 10, 50).forEach { amt ->
                            Button(
                                onClick = { sharesText = amt.toString() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BottomBarBackground,
                                    contentColor = DeepPrimaryText
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "+$amt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Estimated Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BottomBarBackground, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Cost",
                        color = SlateGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$${String.format("%,.2f", totalCost)}",
                        color = DarkNavyText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: BUY and SELL double CTA buttons to avoid dead-ends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = BottomBarBackground, contentColor = SlateGray),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text = "Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onExecute(asset.symbol, asset.name, asset.category, shares, asset.currentPrice, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChartGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("commit_buy_btn")
                    ) {
                        Text(text = "Buy Asset", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onExecute(asset.symbol, asset.name, asset.category, shares, asset.currentPrice, false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChartRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("commit_sell_btn")
                    ) {
                        Text(text = "Sell Asset", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------- AUXILIARY CALCULATIONS -----------------

data class PortfolioSummary(
    val totalValue: Double,
    val stocksValue: Double,
    val fundsValue: Double,
    val retirementValue: Double
)

private fun calculatePortfolioValues(
    holdings: List<Holding>,
    marketPrices: Map<String, Double>
): PortfolioSummary {
    var stocks = 0.0
    var funds = 0.0
    var retire = 0.0

    holdings.forEach { holding ->
        val currentPrice = marketPrices[holding.symbol] ?: holding.averagePrice
        val value = holding.quantity * currentPrice
        when (holding.category) {
            "STOCKS" -> stocks += value
            "FUNDS" -> funds += value
            "RETIREMENT" -> retire += value
        }
    }

    // Default matching exactly the base design numbers ($842,510.42) if database holds nothing else
    val finalTotal = if (holdings.isEmpty()) 842510.42 else (stocks + funds + retire)

    return PortfolioSummary(
        totalValue = finalTotal,
        stocksValue = if (holdings.isEmpty()) 412500.0 else stocks,
        fundsValue = if (holdings.isEmpty()) 205200.0 else funds,
        retirementValue = if (holdings.isEmpty()) 224810.42 else retire
    )
}

// Return dynamic performance coords depending on selected timeframe
private fun getPerformanceCoordinates(filter: String, totalNetWorth: Double): List<Float> {
    val scale = (totalNetWorth / 842510.42).toFloat()
    return when (filter) {
        "1D" -> listOf(838000f, 839000f, 841000f, 840000f, 841500f, 842510.42f).map { it * scale }
        "1W" -> listOf(834000f, 836000f, 833000f, 838000f, 839000f, 840000f, 842510.42f).map { it * scale }
        "1M" -> listOf(822000f, 825000f, 830000f, 827000f, 835000f, 838000f, 841500f, 842510.42f).map { it * scale }
        "3M" -> listOf(805000f, 812000f, 808000f, 815000f, 822000f, 830000f, 827000f, 838000f, 842510.42f).map { it * scale }
        "1Y" -> listOf(750000f, 762000f, 770000f, 765000f, 785000f, 792000f, 804000f, 815000f, 830000f, 842510.42f).map { it * scale }
        else -> listOf(520000f, 560000f, 610000f, 595000f, 680000f, 720000f, 740000f, 785000f, 810000f, 842510.42f).map { it * scale }
    }
}

private fun formatCostValue(value: Double, precision: Int = 1): String {
    return if (value >= 1_000_000) {
        String.format("%.${precision}fM", value / 1_000_000.0)
    } else if (value >= 1_000) {
        String.format("%.${precision}fk", value / 1_000.0)
    } else {
        String.format("%.2f", value)
    }
}
