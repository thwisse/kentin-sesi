package io.github.thwisse.kentinsesi.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.local.db.FilterPresetDao
import io.github.thwisse.kentinsesi.data.local.db.FilterPresetEntity
import io.github.thwisse.kentinsesi.data.local.preferences.FilterPreferences
import io.github.thwisse.kentinsesi.data.model.FilterCriteria
import io.github.thwisse.kentinsesi.data.model.FilterPreset
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class FilterRepositoryImpl @Inject constructor(
    private val dao: FilterPresetDao,
    private val prefs: FilterPreferences,
    @ApplicationContext private val context: Context
) : FilterRepository {

    companion object {
        const val SYSTEM_DEFAULT_ID = "system_default"
        const val SYSTEM_DEFAULT_NAME = "Ana Filtre"
    }

    override fun observePresets(): Flow<List<FilterPreset>> {
        return dao.observeAll().map { list -> list.map { it.toModel() } }
    }

    override suspend fun ensureSystemDefaultExists(): Resource<Unit> {
        return try {
            val alreadyCreated = prefs.examplePresetCreated.first()
            if (alreadyCreated) return Resource.Success(Unit)

            val existing = dao.getSystemDefault()
            if (existing != null) {
                prefs.setExamplePresetCreated(true)
                return Resource.Success(Unit)
            }

            val systemDefault = FilterPresetEntity(
                id = SYSTEM_DEFAULT_ID,
                name = SYSTEM_DEFAULT_NAME,
                districts = listOf("İskenderun"),
                categories = emptyList(),
                statuses = listOf("new", "in_progress"),
                onlyMyPosts = false,
                isSystemDefault = true,
                isDefault = true
            )

            dao.insert(systemDefault)
            prefs.setExamplePresetCreated(true)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_filter_default_create))
        }
    }

    override suspend fun savePreset(name: String, criteria: FilterCriteria): Resource<Unit> {
        if (name.isBlank()) return Resource.Error(context.getString(R.string.error_filter_name_blank))

        return try {
            val entity = FilterPresetEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                districts = criteria.districts,
                categories = criteria.categories,
                statuses = criteria.statuses,
                onlyMyPosts = criteria.onlyMyPosts,
                isSystemDefault = false,
                isDefault = false
            )
            dao.insert(entity)
            Resource.Success(Unit)
        } catch (e: SQLiteConstraintException) {
            Resource.Error(context.getString(R.string.error_filter_name_exists))
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.filter_save_failed))
        }
    }

    override suspend fun deletePreset(id: String): Resource<Unit> {
        return try {
            val preset = dao.getById(id) ?: return Resource.Success(Unit)

            dao.deleteById(id)

            val lastAppliedId = prefs.lastAppliedPresetId.first()
            if (lastAppliedId == id) {
                prefs.setLastAppliedPresetId(null)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_filter_delete))
        }
    }

    override suspend fun setDefaultPreset(id: String): Resource<Unit> {
        return try {
            val preset = dao.getById(id) ?: return Resource.Error(context.getString(R.string.error_filter_not_found))
            dao.clearDefaultFlag()
            dao.setDefaultFlag(preset.id)

            prefs.setLastAppliedPresetId(preset.id)
            prefs.setLastCriteria(null)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_filter_default_set))
        }
    }

    override suspend fun getDefaultPreset(): FilterPreset? {
        return (dao.getDefault() ?: dao.getSystemDefault())?.toModel()
    }

    override suspend fun getPresetById(id: String): FilterPreset? {
        return dao.getById(id)?.toModel()
    }

    override fun observeLastAppliedPresetId(): Flow<String?> {
        return prefs.lastAppliedPresetId
    }

    override suspend fun setLastAppliedPresetId(id: String?): Resource<Unit> {
        return try {
            prefs.setLastAppliedPresetId(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_filter_last_save))
        }
    }

    override fun observeLastCriteria(): Flow<FilterCriteria?> {
        return prefs.lastCriteria
    }

    override suspend fun setLastCriteria(criteria: FilterCriteria?): Resource<Unit> {
        return try {
            prefs.setLastCriteria(criteria)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_filter_last_save))
        }
    }

    private fun FilterPresetEntity.toModel(): FilterPreset {
        return FilterPreset(
            id = id,
            name = name,
            criteria = FilterCriteria(
                districts = districts,
                categories = categories,
                statuses = statuses,
                onlyMyPosts = onlyMyPosts
            ),
            isSystemDefault = isSystemDefault,
            isDefault = isDefault
        )
    }
}
