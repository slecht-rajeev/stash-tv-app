package com.kb2.stashtvapp.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("stash_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FEATURED_STUDIO_IDS = "featured_studio_ids"
    }

    fun saveFeaturedStudioIds(ids: List<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_FEATURED_STUDIO_IDS, ids.toSet())
            .apply()
    }

    fun getFeaturedStudioIds(): List<String> {
        return sharedPreferences.getStringSet(KEY_FEATURED_STUDIO_IDS, emptySet())?.toList() ?: emptyList()
    }
}
