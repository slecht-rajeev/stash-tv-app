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

package com.kb2.stashtvapp.data.repositories

import com.kb2.stashtvapp.data.entities.ThumbnailType
import com.kb2.stashtvapp.data.entities.toScene
import com.kb2.stashtvapp.data.util.AssetsReader
import com.kb2.stashtvapp.data.util.StringConstants
import javax.inject.Inject

class SceneDataSource @Inject constructor(
    assetsReader: AssetsReader
) {

    private val top250SceneDataReader = CachedDataReader {
        readSceneData(assetsReader, StringConstants.Assets.Top250Scenes)
    }

    private val mostPopularSceneDataReader = SceneDataReader {
        readSceneData(assetsReader, StringConstants.Assets.MostPopularScenes).map {
            it.toScene()
        }
    }

    private val sceneDataReader = SceneDataReader {
        top250SceneDataReader.read().map {
            it.toScene()
        }
    }

    private var sceneWithLongThumbnailDataReader: SceneDataReader = CachedDataReader {
        top250SceneDataReader.read().map {
            it.toScene(ThumbnailType.Long)
        }
    }

    private val nowPlayingSceneDataReader: SceneDataReader = SceneDataReader {
        readSceneData(assetsReader, StringConstants.Assets.InTheaters).subList(0, 10).map {
            it.toScene()
        }
    }

    suspend fun getSceneList(thumbnailType: ThumbnailType = ThumbnailType.Standard) =
        when (thumbnailType) {
            ThumbnailType.Standard -> sceneDataReader.read()
            ThumbnailType.Long -> sceneWithLongThumbnailDataReader.read()
        }

    suspend fun getFeaturedSceneList() =
        sceneWithLongThumbnailDataReader.read().filterIndexed { index, _ ->
            listOf(1, 3, 5, 7, 9).contains(index)
        }

    suspend fun getTrendingSceneList() =
        mostPopularSceneDataReader.read().subList(0, 10)

    suspend fun getTop10SceneList() =
        sceneWithLongThumbnailDataReader.read().subList(20, 30)

    suspend fun getNowPlayingSceneList() =
        nowPlayingSceneDataReader.read()

    suspend fun getPopularFilmThisWeek() =
        mostPopularSceneDataReader.read().subList(11, 20)

    suspend fun getFavoriteSceneList() =
        sceneDataReader.read().subList(0, 28)
}
