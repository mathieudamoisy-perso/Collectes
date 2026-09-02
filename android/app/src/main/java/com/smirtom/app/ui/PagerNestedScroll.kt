package com.smirtom.app.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll

val LocalPagerNestedScroll = compositionLocalOf<NestedScrollConnection?> { null }

@Composable
fun rememberPagerNestedScrollConnection(pagerState: PagerState): NestedScrollConnection =
    PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Horizontal)

@Composable
fun Modifier.pagerNestedScroll(): Modifier {
    val connection = LocalPagerNestedScroll.current ?: return this
    return nestedScroll(connection)
}
