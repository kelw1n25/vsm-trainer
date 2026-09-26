package ru.vsm.trainer.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Запись журнала новеллы: реплика, мысль, слова рассказчика, решение игрока или истёкший таймер. */
@Serializable
data class HistoryEntry(val kind: String, val speaker: String? = null, val text: String)

/** Где игрок остановился в прохождении: узел, прочитанные реплики, журнал. */
@Serializable
data class RunProgress(
    val scenarioId: String,
    val nodeId: String,
    val lineIndex: Int,
    val history: List<HistoryEntry>,
    val choices: List<String>,
    val ending: String? = null,
)

/** Что игрок уже видел в сценарии за все прохождения: сцены (можно пропустить) и открытые финалы. */
@Serializable
data class ScenarioMemory(val seenNodes: List<String> = emptyList(), val endings: List<String> = emptyList(), val playthroughs: Int = 0)

@Serializable
private data class Store(
    val runs: Map<String, RunProgress> = emptyMap(),
    val scenarios: Map<String, ScenarioMemory> = emptyMap(),
    val auto: Boolean = false,
    val muted: Boolean = false,
)

/**
 * Прогресс чтения новеллы на устройстве — как localStorage сайта (`story/persistence.ts`): место в истории,
 * журнал, виденные сцены, «Авто» и «Звук». Решения и шкалы хранит сервер: подделать их отсюда нельзя.
 */
class StoryMemory(private val store: KeyValueStore, private val json: Json) {
    private fun key(employeeId: Int) = "vsm_story_v1:$employeeId"

    private fun read(employeeId: Int): Store =
        store.getString(key(employeeId))?.let { runCatching { json.decodeFromString(Store.serializer(), it) }.getOrNull() } ?: Store()

    private fun write(employeeId: Int, value: Store) {
        // Журналы старых прохождений не нужны вечно — держим последние
        val runs = value.runs.entries.toList().takeLast(MAX_RUNS).associate { it.key to it.value }
        store.putString(key(employeeId), json.encodeToString(Store.serializer(), value.copy(runs = runs)))
    }

    @Synchronized
    fun progress(employeeId: Int, runId: String): RunProgress? = read(employeeId).runs[runId]

    @Synchronized
    fun saveProgress(employeeId: Int, runId: String, progress: RunProgress) {
        val current = read(employeeId)
        write(employeeId, current.copy(runs = current.runs - runId + (runId to progress)))
    }

    @Synchronized
    fun scenario(employeeId: Int, scenarioId: String): ScenarioMemory = read(employeeId).scenarios[scenarioId] ?: ScenarioMemory()

    @Synchronized
    fun rememberScene(employeeId: Int, scenarioId: String, nodeId: String) {
        val current = read(employeeId)
        val memory = current.scenarios[scenarioId] ?: ScenarioMemory()
        if (nodeId in memory.seenNodes) return
        write(employeeId, current.copy(scenarios = current.scenarios + (scenarioId to memory.copy(seenNodes = memory.seenNodes + nodeId))))
    }

    @Synchronized
    fun rememberEnding(employeeId: Int, scenarioId: String, runId: String, ending: String) {
        val current = read(employeeId)
        val memory = current.scenarios[scenarioId] ?: ScenarioMemory()
        val progress = current.runs[runId]
        // Финал одного прохождения засчитываем один раз, даже если экран открыли повторно
        val counted = progress?.ending != null
        val updated = memory.copy(
            playthroughs = memory.playthroughs + if (counted) 0 else 1,
            endings = if (ending in memory.endings) memory.endings else memory.endings + ending,
        )
        val runs = if (progress != null) current.runs + (runId to progress.copy(ending = ending)) else current.runs
        write(employeeId, current.copy(scenarios = current.scenarios + (scenarioId to updated), runs = runs))
    }

    @Synchronized
    fun auto(employeeId: Int) = read(employeeId).auto

    @Synchronized
    fun saveAuto(employeeId: Int, auto: Boolean) = write(employeeId, read(employeeId).copy(auto = auto))

    @Synchronized
    fun muted(employeeId: Int) = read(employeeId).muted

    @Synchronized
    fun saveMuted(employeeId: Int, muted: Boolean) = write(employeeId, read(employeeId).copy(muted = muted))

    private companion object {
        const val MAX_RUNS = 20
    }
}
