package zastrow.tom.stepmanager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "step_preferences"
)

class DataStore(private val context: Context) {

    companion object {
        private val LAST_UPDATED = stringPreferencesKey("step_count")
        private val STEP_CELL = stringPreferencesKey("step_cell")
        private val TIME_CELL = stringPreferencesKey("time_cell")
    }

    // Observe the saved lastUpdated value.
    val lastUpdated: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_UPDATED] ?: ""
    }

    // Save a new lastUpdated value.
    suspend fun saveLastUpdated(lastUpdated: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_UPDATED] = lastUpdated
        }
    }

    suspend fun saveSheetCells(stepCell: String, timeCell: String) {
        context.dataStore.edit { preferences ->
            preferences[STEP_CELL] = stepCell
            preferences[TIME_CELL] = timeCell
        }
    }

    suspend fun getSheetCells(): Pair<String, String> {
        val preferences = context.dataStore.data.first()

        val stepCell = preferences[STEP_CELL] ?: "D1"
        val timeCell = preferences[TIME_CELL] ?: "E1"

        return Pair(stepCell, timeCell)
    }
}
