@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.legalteamwork.silverscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.legalteamwork.silverscreen.command.CommandWindowCompose
import org.legalteamwork.silverscreen.menu.MenuBarCompose
import org.legalteamwork.silverscreen.re.EditingPanel
import org.legalteamwork.silverscreen.resources.AppTheme
import org.legalteamwork.silverscreen.resources.Dimens
import org.legalteamwork.silverscreen.resources.EditingPanelTheme
import org.legalteamwork.silverscreen.rm.ResourceManagerCompose
import org.legalteamwork.silverscreen.vp.VideoPanel
import org.legalteamwork.silverscreen.toolbar.ToolbarPanel
import org.legalteamwork.silverscreen.windows.block.column
import org.legalteamwork.silverscreen.windows.block.row
import org.legalteamwork.silverscreen.windows.block.terminal
import org.legalteamwork.silverscreen.windows.block.with
import org.legalteamwork.silverscreen.windows.data.DimensionsScope

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppScope.App() {
    val windowBlock = column(
        Dimens.UPPER_WINDOW_WEIGHT with row(
            Dimens.RESOURCE_WINDOW_WEIGHT with terminal(
                Dimens.RESOURCE_WINDOW_MIN_WIDTH,
                Dimens.RESOURCE_WINDOW_MIN_HEIGHT,
                Dimens.RESOURCE_WINDOW_MAX_WIDTH,
                Dimens.RESOURCE_WINDOW_MAX_HEIGHT,
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        AppTheme.VIDEO_PANEL_BACKGROUND_COLOR, RoundedCornerShape(Dimens.WINDOW_CORNER_RADIUS)
                    )
                ) {
                    ResourceManagerCompose()
                }
            },
            Dimens.PREVIEW_WINDOW_WEIGHT with terminal(
                Dimens.PREVIEW_WINDOW_MIN_WIDTH,
                Dimens.PREVIEW_WINDOW_MIN_HEIGHT,
                Dimens.PREVIEW_WINDOW_MAX_WIDTH,
                Dimens.PREVIEW_WINDOW_MAX_HEIGHT,
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        AppTheme.VIDEO_PANEL_BACKGROUND_COLOR, RoundedCornerShape(Dimens.WINDOW_CORNER_RADIUS)
                    )
                ) {
                    VideoPanel.compose()
                }
            },
            Dimens.COMMANDS_WINDOW_WEIGHT with terminal(
                Dimens.COMMANDS_WINDOW_MIN_WIDTH,
                Dimens.COMMANDS_WINDOW_MIN_HEIGHT,
                Dimens.COMMANDS_WINDOW_MAX_WIDTH,
                Dimens.COMMANDS_WINDOW_MAX_HEIGHT,
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        AppTheme.COMMAND_WINDOW_BACKGROUND, RoundedCornerShape(Dimens.WINDOW_CORNER_RADIUS)
                    )
                ) {
                    CommandWindowCompose()
                }
            }
        ),
        Dimens.TIMELINE_WINDOW_WEIGHT with terminal(
            Dimens.TIMELINE_WINDOW_MIN_WIDTH,
            Dimens.TIMELINE_WINDOW_MIN_HEIGHT,
            Dimens.TIMELINE_WINDOW_MAX_WIDTH,
            Dimens.TIMELINE_WINDOW_MAX_HEIGHT,
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    EditingPanelTheme.LONG_MARK_INTERVAL_COLOR, RoundedCornerShape(Dimens.WINDOW_CORNER_RADIUS)
                )
            ) {
                Column (verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ToolbarPanel(Modifier.fillMaxWidth().height(48.dp))
                    //Spacer(modifier = Modifier.height(2.dp)) // добавляем отступ 8dp
                    EditingPanel(height)
                }
            }
        }
    )

    Surface(color = AppTheme.SURFACE_COLOR) {
        Column {
            MenuBarCompose()
            BoxWithConstraints(Modifier.fillMaxSize().padding(Dimens.MARGIN_SIZE)) {
                val dimensionsScope = DimensionsScope(maxWidth, maxHeight)
                windowBlock.content.invoke(dimensionsScope)
            }
        }
    }
}

