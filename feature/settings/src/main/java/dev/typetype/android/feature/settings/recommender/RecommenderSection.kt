package dev.typetype.android.feature.settings.recommender

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import dev.typetype.android.R

enum class RecommenderSection(val titleRes: Int, val descriptionRes: Int, val icon: ImageVector) {
    Overview(R.string.algo_section_overview, R.string.algo_section_overview_description, Icons.Outlined.PieChart),
    Algorithm(R.string.algo_section_algorithm, R.string.algo_section_algorithm_description, Icons.AutoMirrored.Outlined.ViewList),
    Guide(R.string.algo_section_guide, R.string.algo_section_guide_description, Icons.Outlined.Explore),
    Data(R.string.algo_section_data, R.string.algo_section_data_description, Icons.Outlined.Storage),
    Profile(R.string.algo_section_profile, R.string.algo_section_profile_description, Icons.Outlined.Person),
    Details(R.string.algo_section_backstage, R.string.algo_section_backstage_description, Icons.Outlined.Build),
    Feedback(R.string.algo_section_feedback, R.string.algo_section_feedback_description, Icons.Outlined.Forum),
}
