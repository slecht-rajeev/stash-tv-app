/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kb2.stashtvapp.data.util

object StringConstants {
    object Scene {
        const val StatusReleased = "Released"
        const val BudgetDefault = "$10M"
        const val WorldWideGrossDefault = "$20M"

        object Reviewer {
            const val FreshTomatoes = "Fresh Tomatoes"
            const val FreshTomatoesImageUrl = ""
            const val ReviewerName = "Rater"
            const val ImageUrl = ""
            const val DefaultCount = "1.8M"
            const val DefaultRating = "9.2"
        }
    }

    object Assets {
        const val Top250Scenes = "scenes.json"
        const val MostPopularScenes = "scenes.json"
        const val InTheaters = "scenes.json"
        const val MostPopularTVShows = "scenes.json"
        const val SceneCategories = "sceneCategories.json"
        const val SceneCast = "sceneCast.json"
    }

    object Exceptions {
        const val UnknownException = "Unknown Exception!"
        const val InvalidCategoryId = "Invalid category ID!"
    }

    object Composable {
        object ContentDescription {
            fun scenePoster(sceneName: String) = "Scene poster of $sceneName"
            fun image(imageName: String) = "image of $imageName"
            const val ScenesCarousel = "Scenes Carousel"
            const val UserAvatar = "User Profile Button"
            const val DashboardSearchButton = "Dashboard Search Button"
            const val BrandLogoImage = "Brand Logo Image"
            const val FilterSelected = "Filter Selected"
            fun reviewerName(name: String) = "$name's logo"
        }

        const val CategoryDetailsFailureSubject = "category details"
        const val ScenesFailureSubject = "scenes"
        const val SceneDetailsFailureSubject = "scene details"
        const val HomeScreenTrendingTitle = "Trending"
        const val HomeScreenNowPlayingScenesTitle = "Now Playing Scenes"
        const val PopularFilmsThisWeekTitle = "Popular films this week"
        const val BingeWatchDramasTitle = "Bingewatch dramas"
        const val TVShowsTitle = "TV Shows"
        fun sceneDetailsScreenSimilarTo(name: String) = "Similar to $name"
        fun reviewCount(count: String) = "$count reviews"

        object Placeholders {
            const val AboutSectionTitle = "About StashApp"
            const val AboutSectionDescription = "Welcome to StashApp! We are a new and" +
                " exciting streaming platform that offers a vast selection of scenes," +
                " TV shows, and original content for you to enjoy. Our team is dedicated" +
                " to providing an intuitive and seamless streaming experience for all" +
                " users. With a simple and intuitive interface, you can easily find and" +
                " watch your favourite content in just a few clicks. We are constantly" +
                " updating and expanding our library, so there is always something new" +
                " to discover. We also offer personalised recommendations based on your" +
                " viewing history, so you can easily find new and exciting content to" +
                " enjoy. Thank you for choosing StashApp for all of your entertainment" +
                " needs. We hope you have a great time streaming!"
            const val AboutSectionAppVersionTitle = "Application Version"
            const val LanguageSectionTitle = "Language"
            val LanguageSectionItems = listOf(
                "English (US)",
                "English (UK)",
                "Français",
                "Española",
                "हिंदी"
            )
            const val SearchHistorySectionTitle = "Search history"
            const val SearchHistoryClearAll = "Clear All"
            val SampleSearchHistory = listOf(
                "The Light Knight",
                "Iceberg",
                "Jungle Gump",
                "The Devilfather",
                "Space Wars",
                "The Lion Queen"
            )
            const val SubtitlesSectionTitle = "Settings"
            const val SubtitlesSectionSubtitlesItem = "Subtitles"
            const val SubtitlesSectionLanguageItem = "Subtitles Language"
            const val SubtitlesSectionLanguageValue = "English"
            const val AccountsSelectionSwitchAccountsTitle = "Switch accounts"
            const val AccountsSelectionSwitchAccountsEmail = "jack@StashApp.com"
            const val AccountsSelectionLogOut = "Log out"
            const val AccountsSelectionChangePasswordTitle = "Change password"
            const val AccountsSelectionChangePasswordValue = "••••••••••••••"
            const val AccountsSelectionAddNewAccountTitle = "Add new account"
            const val AccountsSelectionViewSubscriptionsTitle = "View subscriptions"
            const val AccountsSelectionDeleteAccountTitle = "Delete account"
            const val HelpAndSupportSectionTitle = "Help and Support"
            const val HelpAndSupportSectionListItemIconDescription = "select section"
            const val HelpAndSupportSectionFAQItem = "FAQ's"
            const val HelpAndSupportSectionPrivacyItem = "Privacy Policy"
            const val HelpAndSupportSectionContactItem = "Contact us on"
            const val HelpAndSupportSectionContactValue = "support@StashApp.com"
        }

        const val VideoPlayerControlPlaylistButton = "Playlist Button"
        const val VideoPlayerControlClosedCaptionsButton = "Playlist Button"
        const val VideoPlayerControlSettingsButton = "Playlist Button"
        const val VideoPlayerControlPlayPauseButton = "Playlist Button"
        const val VideoPlayerControlForward = "Fast forward 10 seconds"
        const val VideoPlayerControlSkipNextButton = "Skip to the next scene"
        const val VideoPlayerControlSkipPreviousButton = "Skip to the previous scene"
        const val VideoPlayerControlRepeatAll = "Repeat all scenes"
        const val VideoPlayerControlRepeatOne = "Repeat scene"
        const val VideoPlayerControlRepeatNone = "No repeat"
        const val VideoPlayerControlRepeatButton = "Repeat Button"
    }
}
