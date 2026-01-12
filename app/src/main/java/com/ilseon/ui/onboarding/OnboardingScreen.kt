package com.ilseon.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ilseon.R
import com.ilseon.ui.theme.MutedDetail
import com.ilseon.ui.theme.MutedTeal

@Composable
fun OnboardingScreen(onOnboardingFinished: () -> Unit) {
    val slides = listOf(
        OnboardingSlide(
            title = "Welcome to Ilseon",
            description = "A minimalist executive-function assistant designed to reduce mental noise and help you focus on your current priority."
        ),
        OnboardingSlide(
            title = "The Dashboard",
            description = "Your \"Front Line\" of focus. We hide the chaos and show only what matters right now. No more overwhelming lists."
        ),
        OnboardingSlide(
            title = "Build Momentum",
            description = "Turn focus into a habit. Earn Momentum points for every task completed and reflection captured. Watch your daily streak grow as you find your flow."
        ),
        OnboardingSlide(
            title = "Idea Inbox",
            description = "A dual-system approach separating transient \"scratchpad\" thoughts from long-term knowledge. Capture instantly and refine later when you have the energy."
        ),
        OnboardingSlide(
            title = "Voice Inbox",
            description = "Think faster than you can type? Record quality voice memos to capture brilliance while you're on the move."
        ),
        OnboardingSlide(
            title = "The Widget",
            description = "Stay present without opening the app. The home screen widget shows your current priority and has shortcuts for quick capture."
        ),
        OnboardingSlide(
            title = "Your Data is Yours",
            description = "Everything is saved locally on your device. No cloud, no tracking, and no hidden subscriptions. Just pure focus."
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Ilseon Logo",
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 32.dp)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingSlideContent(slides[page])
        }

        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MutedTeal else MutedDetail
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(16.dp)
                .height(40.dp)
        ) {
            if (pagerState.currentPage == slides.size - 1) {
                Button(
                    onClick = onOnboardingFinished,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Get Started")
                }
            }
        }
    }
}

@Composable
fun OnboardingSlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = slide.title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = slide.description,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}
