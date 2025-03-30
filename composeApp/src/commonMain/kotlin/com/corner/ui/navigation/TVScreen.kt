package com.corner.ui.navigation

enum class TVScreen(val title: String) {
    VideoScreen("Video Screen"),
    SearchScreen("Search Screen"),
    HistoryScreen("History Screen"),
    SettingsScreen("Settings Screen"),
    DetailScreen("Detail Screen"),
    DLNAPlayerScreen("DLNA Player Screen")
}

enum class SearchScreen(val title: String) {
    Search("Search"),
    SearchResult("Search Result"),
}