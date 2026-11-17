package com.spcode.tnpscprep.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(val label: String, val icon: ImageVector)

/**
 * The bottom bar: a floating rounded pill-shaped bar with a shadow. A highlight "pill" SLIDES to the selected tab on a
 * springy curve (it overshoots slightly = the "pop"), the selected icon grows and bounces, and the label brightens.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
    /** Optional round action button in the MIDDLE of the bar (the Focus button). Not a tab: it never gets selected. */
    center: NavItem? = null, onCenter: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 18.dp,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            BoxWithConstraints(Modifier.padding(6.dp)) {
                val slots = items.size + if (center != null) 1 else 0
                val centerSlot = items.size / 2
                val itemWidth = maxWidth / slots
                // tab index -> slot (tabs after the middle button shift one slot to the right); no tab selected (-1) hides the pill
                val selectedSlot = if (selected < 0) 0 else if (center != null && selected >= centerSlot) selected + 1 else selected
                val pillOffset by animateDpAsState(
                    targetValue = itemWidth * selectedSlot,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "navPill",
                )
                val pillAlpha by animateFloatAsState(if (selected < 0) 0f else 1f, label = "navPillAlpha")
                Box(
                    Modifier
                        .offset(x = pillOffset)
                        .width(itemWidth)
                        .height(58.dp)
                        .padding(horizontal = 3.dp)
                        .graphicsLayer { alpha = pillAlpha }
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (slot in 0 until slots) {
                        if (center != null && slot == centerSlot) {
                            CenterButton(center, Modifier.width(itemWidth)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onCenter() }
                        } else {
                            val i = if (center != null && slot > centerSlot) slot - 1 else slot
                            NavButton(
                                item = items[i],
                                selected = i == selected,
                                modifier = Modifier.width(itemWidth),
                                onClick = {
                                    if (i != selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(i)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The round Focus button: crimson, icon only (the label is its accessibility description). */
@Composable
private fun CenterButton(item: NavItem, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "centerScale")
    Box(modifier.height(58.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(52.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary)
                .semantics { contentDescription = item.label }
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp)) }
    }
}

@Composable
private fun NavButton(item: NavItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "navIconScale",
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) -2f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "navIconLift",
    )
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint",
    )
    Column(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(24.dp))
            .semantics { this.selected = selected }
            .clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            item.icon, contentDescription = null, tint = tint,
            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale; translationY = lift * density },
        )
        Text(
            item.label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
        )
    }
}
