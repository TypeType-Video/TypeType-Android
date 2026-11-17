package dev.typetype.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.typetype.android.R
import dev.typetype.android.domain.preferences.HomeLayout

@Composable
internal fun HomeLayoutToggle(layout: HomeLayout, onLayout: (HomeLayout) -> Unit) {
    val label = stringResource(R.string.home_layout_label)
    val options = listOf(
        HomeLayout.Grid to stringResource(R.string.home_layout_grid),
        HomeLayout.Rows to stringResource(R.string.home_layout_rows),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.semantics { contentDescription = label }) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = layout == value,
                    onClick = { onLayout(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(text)
                }
            }
        }
    }
}
