package com.floresvalencia.echoguard.data.map

import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.MapPoint
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.model.distanceTo

/** Elementos del plano que ayudan a evacuar (además de las salidas). */
enum class FeatureKind(val label: String) {
    SAFE_ZONE("Zona segura"),
    MEETING_POINT("Punto de reunión"),
    STAIRS("Escalera"),
    EXTINGUISHER("Extintor"),
}

data class MapFeature(val kind: FeatureKind, val name: String, val position: MapPoint)

/** Capas que se pueden mostrar u ocultar en el mapa interactivo. */
enum class MapLayer(val label: String) {
    EXITS("Salidas"),
    SAFE_ZONES("Zonas seguras"),
    STAIRS("Escaleras"),
    EXTINGUISHERS("Extintores"),
    ROVERS("Rovers"),
}

/** Camino de evacuación calculado sobre los pasillos. */
data class EvacPath(
    val exit: ExitRoute,
    val points: List<MapPoint>,
    val distanceM: Int,
    /** Salidas descartadas porque están bloqueadas. */
    val avoided: List<ExitRoute>,
) {
    val isAlternative: Boolean get() = avoided.isNotEmpty()
}

/**
 * Plano del Bloque A como un grafo de pasillos. La ruta se calcula con Dijkstra:
 * nunca pasa por una salida bloqueada y penaliza las congestionadas.
 */
object SchoolMap {
    /** Penalización (en metros) de una salida congestionada. */
    const val CONGESTION_PENALTY_M = 25f

    // Nodos: cruces de pasillo, puertas de aula y salidas (coordenadas en metros).
    private val nodes: Map<String, MapPoint> = mapOf(
        "J8" to MapPoint(8f, 17f), "J20" to MapPoint(20f, 17f), "C" to MapPoint(30f, 17f),
        "J40" to MapPoint(40f, 17f), "J52" to MapPoint(52f, 17f),
        "N" to MapPoint(30f, 8f), "S" to MapPoint(30f, 26f),
        "EXIT_A" to MapPoint(60f, 17f), "EXIT_B" to MapPoint(30f, 0f), "EXIT_C" to MapPoint(30f, 34f),
        "D_3A" to MapPoint(8f, 14f), "D_3B" to MapPoint(20f, 14f), "D_3C" to MapPoint(40f, 14f),
        "D_LAB" to MapPoint(52f, 14f), "D_4A" to MapPoint(8f, 20f), "D_DIR" to MapPoint(20f, 20f),
        "D_PATIO" to MapPoint(40f, 20f),
    )

    private val edges: List<Pair<String, String>> = listOf(
        "J8" to "J20", "J20" to "C", "C" to "J40", "J40" to "J52", "J52" to "EXIT_A",
        "C" to "N", "N" to "EXIT_B", "C" to "S", "S" to "EXIT_C",
        "D_3A" to "J8", "D_4A" to "J8", "D_3B" to "J20", "D_DIR" to "J20",
        "D_3C" to "J40", "D_PATIO" to "J40", "D_LAB" to "J52",
    )

    /** Puerta de cada aula o ambiente (id del aula → nodo). */
    val rooms: Map<String, String> = mapOf(
        "3A" to "D_3A", "3B" to "D_3B", "3C" to "D_3C", "LAB" to "D_LAB",
        "4A" to "D_4A", "DIR" to "D_DIR", "PATIO" to "D_PATIO",
    )

    /** Sin aula conocida se parte del cruce central. */
    const val DEFAULT_START = "C"

    val features: List<MapFeature> = listOf(
        MapFeature(FeatureKind.SAFE_ZONE, "Patio interior", MapPoint(47f, 27f)),
        MapFeature(FeatureKind.MEETING_POINT, "Punto de reunión", MapPoint(30f, 32f)),
        MapFeature(FeatureKind.STAIRS, "Escalera norte", MapPoint(33f, 4f)),
        MapFeature(FeatureKind.STAIRS, "Escalera este", MapPoint(56f, 22f)),
        MapFeature(FeatureKind.EXTINGUISHER, "Extintor pasillo oeste", MapPoint(14f, 15f)),
        MapFeature(FeatureKind.EXTINGUISHER, "Extintor pasillo este", MapPoint(46f, 19f)),
        MapFeature(FeatureKind.EXTINGUISHER, "Extintor dirección", MapPoint(24f, 22f)),
    )

    private val exitNode = mapOf("A" to "EXIT_A", "B" to "EXIT_B", "C" to "EXIT_C")

    fun pointOf(room: String?): MapPoint = nodes.getValue(rooms[room] ?: DEFAULT_START)

    /**
     * Mejor ruta desde [room] (id de aula; null = cruce central) hacia una salida.
     * Devuelve null si todas las salidas están bloqueadas.
     */
    fun route(room: String?, exits: List<ExitRoute>): EvacPath? {
        val start = rooms[room] ?: DEFAULT_START
        val (dist, prev) = dijkstra(start)
        val avoided = exits.filter { it.status == RouteStatus.BLOCKED }
        val best = exits
            .filter { it.status != RouteStatus.BLOCKED && exitNode[it.id] in dist }
            .minByOrNull { cost(it, dist) }
            ?: return null
        val target = exitNode.getValue(best.id)
        val path = generateSequence(target) { prev[it] }.toList().reversed().map { nodes.getValue(it) }
        // Las salidas bloqueadas solo cuentan como "evitadas" si habrían sido más cortas.
        val shorterBlocked = avoided.filter { b -> exitNode[b.id]?.let { dist[it] }?.let { it < dist.getValue(target) } == true }
        return EvacPath(best, path, dist.getValue(target).toInt(), shorterBlocked)
    }

    private fun cost(e: ExitRoute, dist: Map<String, Float>): Float =
        dist.getValue(exitNode.getValue(e.id)) + if (e.status == RouteStatus.CONGESTED) CONGESTION_PENALTY_M else 0f

    private fun dijkstra(start: String): Pair<Map<String, Float>, Map<String, String>> {
        val adj = edges.flatMap { (a, b) -> listOf(a to b, b to a) }.groupBy({ it.first }, { it.second })
        val dist = mutableMapOf(start to 0f)
        val prev = mutableMapOf<String, String>()
        val open = mutableSetOf(start)
        while (open.isNotEmpty()) {
            val u = open.minBy { dist.getValue(it) }
            open.remove(u)
            for (v in adj[u].orEmpty()) {
                val d = dist.getValue(u) + nodes.getValue(u).distanceTo(nodes.getValue(v))
                if (d < (dist[v] ?: Float.MAX_VALUE)) {
                    dist[v] = d
                    prev[v] = u
                    open.add(v)
                }
            }
        }
        return dist to prev
    }

    /** Salida más cercana a un punto (para saber cuál bloquea un humo o un fuego). */
    fun nearestExit(p: MapPoint, exits: List<ExitRoute>): ExitRoute? = exits.minByOrNull { it.position.distanceTo(p) }
}
