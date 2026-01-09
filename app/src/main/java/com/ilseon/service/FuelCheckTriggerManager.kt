import android.content.SharedPreferences
import javax.inject.Inject
import kotlin.random.Random
import androidx.core.content.edit

interface FuelCheckTrigger {
    suspend fun shouldShowFuelCheck(): Boolean
    fun onFuelCheckShown()
}

class FuelCheckTriggerManager @Inject constructor(
    private val preferences: SharedPreferences
) : FuelCheckTrigger {

    private val random = Random.Default

    override suspend fun shouldShowFuelCheck(): Boolean {
        val lastCheckTime = preferences.getLong("last_fuel_check", 0L)
        val completedTasks = preferences.getInt("tasks_since_fuel_check", 0)
        val now = System.currentTimeMillis()

        // Check on startup if more than 4 hours since last check
        val hoursSinceLastCheck = (now - lastCheckTime) / (1000 * 60 * 60)
        if (hoursSinceLastCheck >= 4) return true

        // Check after 3-5 completed tasks (random threshold)
        val taskThreshold = preferences.getInt("task_threshold", random.nextInt(3, 6))
        if (completedTasks >= taskThreshold) return true

        // 10% random chance on any trigger
        if (random.nextFloat() < 0.10f && hoursSinceLastCheck >= 1) return true

        return false
    }

    override fun onFuelCheckShown() {
        preferences.edit {
            putLong("last_fuel_check", System.currentTimeMillis())
                .putInt("tasks_since_fuel_check", 0)
                .putInt("task_threshold", random.nextInt(3, 6)) // Reset random threshold
        }
    }

    fun onTaskCompleted() {
        val current = preferences.getInt("tasks_since_fuel_check", 0)
        preferences.edit { putInt("tasks_since_fuel_check", current + 1) }
    }
}
