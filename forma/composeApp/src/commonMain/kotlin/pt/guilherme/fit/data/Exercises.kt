package pt.guilherme.fit.data

import pt.guilherme.fit.model.GymEquipment
import pt.guilherme.fit.model.GymEquipment.*
import pt.guilherme.fit.model.Muscle
import pt.guilherme.fit.model.Pattern
import pt.guilherme.fit.model.Pattern.*

data class Exercise(
    val id: String,
    val name: String,
    val pattern: Pattern,
    val muscles: List<Muscle>,
    /** precisa de todo este equipamento (vazio = peso corporal) */
    val equipment: Set<GymEquipment>,
    /** 1 = fácil, 3 = técnico/difícil */
    val level: Int,
    /** dá para aumentar a carga facilmente */
    val loadable: Boolean,
    val cue: String,
    /** repetições fixas (ex.: prancha em segundos) */
    val repsOverride: String? = null,
)

object Exercises {
    private fun e(
        id: String, name: String, pattern: Pattern, muscles: List<Muscle>, eq: Set<GymEquipment>,
        level: Int, loadable: Boolean, cue: String, reps: String? = null,
    ) = Exercise(id, name, pattern, muscles, eq, level, loadable, cue, reps)

    private val legs = listOf(Muscle.QUADS, Muscle.GLUTES)
    private val post = listOf(Muscle.HAMSTRINGS, Muscle.GLUTES)

    val all: List<Exercise> = listOf(
        // Agachamento
        e("squat_bw", "Agachamento com peso corporal", SQUAT, legs, emptySet(), 1, false, "Peito alto, joelhos na direção dos pés, desce até as coxas ficarem paralelas."),
        e("goblet_db", "Agachamento goblet com halter", SQUAT, legs, setOf(DUMBBELLS), 1, true, "Halter junto ao peito, cotovelos por dentro dos joelhos."),
        e("goblet_kb", "Agachamento goblet com kettlebell", SQUAT, legs, setOf(KETTLEBELL), 1, true, "Kettlebell junto ao peito, tronco direito."),
        e("leg_press", "Leg press", SQUAT, legs, setOf(MACHINES), 1, true, "Não tires a lombar do encosto, não estiques os joelhos por completo."),
        e("back_squat", "Agachamento com barra", SQUAT, legs, setOf(BARBELL, SQUAT_RACK), 3, true, "Barra nos trapézios, abdómen contraído, desce controlado."),
        // Dobradiça da anca
        e("glute_bridge", "Ponte de glúteos", HINGE, post, emptySet(), 1, false, "Empurra pelos calcanhares e aperta os glúteos em cima."),
        e("rdl_db", "Peso morto romeno com halteres", HINGE, post, setOf(DUMBBELLS), 2, true, "Joelhos ligeiramente fletidos, leva a anca atrás, costas neutras."),
        e("kb_swing", "Kettlebell swing", HINGE, post, setOf(KETTLEBELL), 2, true, "O movimento vem da anca, não dos braços."),
        e("rdl_bb", "Peso morto romeno com barra", HINGE, post, setOf(BARBELL), 2, true, "Barra rente às pernas, costas neutras."),
        e("deadlift", "Peso morto", HINGE, post + Muscle.BACK, setOf(BARBELL), 3, true, "Barra sobre o meio do pé, empurra o chão, costas neutras."),
        e("hip_thrust_bb", "Hip thrust com barra", HINGE, listOf(Muscle.GLUTES), setOf(BARBELL, BENCH), 2, true, "Costas apoiadas no banco, sobe até a anca ficar alinhada."),
        e("band_pull_through", "Pull-through com elástico", HINGE, post, setOf(BANDS), 1, false, "Leva a anca atrás e estende com força."),
        // Afundos
        e("lunge_bw", "Afundos alternados", LUNGE, legs, emptySet(), 1, false, "Passo largo, joelho de trás quase toca no chão."),
        e("split_squat", "Agachamento búlgaro (pé numa cadeira)", LUNGE, legs, emptySet(), 2, false, "Pé de trás apoiado, desce na vertical."),
        e("lunge_db", "Afundos com halteres", LUNGE, legs, setOf(DUMBBELLS), 2, true, "Tronco direito, desce controlado."),
        e("step_up", "Subida ao banco", LUNGE, legs, emptySet(), 1, false, "Sobe com a perna da frente, sem dar impulso com a de trás."),
        // Empurrar horizontal
        e("pushup_incline", "Flexões inclinadas (mãos numa mesa)", H_PUSH, listOf(Muscle.CHEST, Muscle.TRICEPS), emptySet(), 1, false, "Corpo em linha reta, peito até à mesa."),
        e("pushup", "Flexões", H_PUSH, listOf(Muscle.CHEST, Muscle.TRICEPS), emptySet(), 2, false, "Cotovelos a 45°, abdómen contraído."),
        e("db_bench", "Supino com halteres", H_PUSH, listOf(Muscle.CHEST, Muscle.TRICEPS), setOf(DUMBBELLS, BENCH), 2, true, "Omoplatas juntas, desce até ao peito."),
        e("db_floor_press", "Floor press com halteres", H_PUSH, listOf(Muscle.CHEST, Muscle.TRICEPS), setOf(DUMBBELLS), 1, true, "Deitado no chão, desce até os cotovelos tocarem no chão."),
        e("bb_bench", "Supino com barra", H_PUSH, listOf(Muscle.CHEST, Muscle.TRICEPS), setOf(BARBELL, BENCH), 3, true, "Pés firmes, barra até ao meio do peito."),
        e("chest_press", "Chest press (máquina)", H_PUSH, listOf(Muscle.CHEST, Muscle.TRICEPS), setOf(MACHINES), 1, true, "Pega à altura do peito, empurra sem bloquear os cotovelos."),
        e("cable_fly", "Crossover nos cabos", H_PUSH, listOf(Muscle.CHEST), setOf(CABLES), 2, true, "Braços ligeiramente fletidos, junta as mãos à frente do peito."),
        // Empurrar vertical
        e("pike_pushup", "Flexões em pico", V_PUSH, listOf(Muscle.SHOULDERS, Muscle.TRICEPS), emptySet(), 2, false, "Anca alta, cabeça desce entre as mãos."),
        e("db_ohp", "Press de ombros com halteres", V_PUSH, listOf(Muscle.SHOULDERS, Muscle.TRICEPS), setOf(DUMBBELLS), 1, true, "Sem arquear a lombar, sobe até esticar os braços."),
        e("band_ohp", "Press de ombros com elástico", V_PUSH, listOf(Muscle.SHOULDERS), setOf(BANDS), 1, false, "Pisa o elástico e empurra para cima."),
        e("bb_ohp", "Press militar com barra", V_PUSH, listOf(Muscle.SHOULDERS, Muscle.TRICEPS), setOf(BARBELL), 3, true, "Glúteos contraídos, barra sobe junto à cara."),
        e("machine_ohp", "Shoulder press (máquina)", V_PUSH, listOf(Muscle.SHOULDERS), setOf(MACHINES), 1, true, "Costas no encosto, empurra para cima."),
        // Puxar horizontal
        e("table_row", "Remo invertido debaixo de uma mesa", H_PULL, listOf(Muscle.BACK, Muscle.BICEPS), emptySet(), 1, false, "Corpo em linha, puxa o peito até à mesa."),
        e("db_row", "Remo com halter", H_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(DUMBBELLS), 1, true, "Apoia uma mão, puxa o halter até à anca."),
        e("band_row", "Remo com elástico", H_PULL, listOf(Muscle.BACK), setOf(BANDS), 1, false, "Junta as omoplatas no fim do movimento."),
        e("bb_row", "Remo com barra", H_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(BARBELL), 3, true, "Tronco inclinado a 45°, barra até ao umbigo."),
        e("cable_row", "Remo sentado no cabo", H_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(CABLES), 1, true, "Peito alto, puxa até ao abdómen."),
        e("machine_row", "Remo (máquina)", H_PULL, listOf(Muscle.BACK), setOf(MACHINES), 1, true, "Peito no apoio, puxa com os cotovelos."),
        // Puxar vertical
        e("negative_pullup", "Elevações negativas", V_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(PULLUP_BAR), 2, false, "Sobe com ajuda e desce devagar em 3-5 segundos.", "4-6"),
        e("pullup", "Elevações", V_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(PULLUP_BAR), 3, false, "Desce com os braços esticados, queixo acima da barra."),
        e("band_pulldown", "Puxada com elástico", V_PULL, listOf(Muscle.BACK), setOf(BANDS), 1, false, "Prende o elástico em cima e puxa até ao peito."),
        e("lat_pulldown", "Puxada alta", V_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(CABLES), 1, true, "Puxa a barra até ao peito, sem balançar."),
        e("lat_pulldown_m", "Puxada alta (máquina)", V_PULL, listOf(Muscle.BACK, Muscle.BICEPS), setOf(MACHINES), 1, true, "Puxa até ao peito, controla a subida."),
        // Braços
        e("db_curl", "Curl de bíceps com halteres", ISO_ARMS_BI, listOf(Muscle.BICEPS), setOf(DUMBBELLS), 1, true, "Cotovelos colados ao corpo."),
        e("bb_curl", "Curl com barra", ISO_ARMS_BI, listOf(Muscle.BICEPS), setOf(BARBELL), 1, true, "Sem balançar o tronco."),
        e("band_curl", "Curl com elástico", ISO_ARMS_BI, listOf(Muscle.BICEPS), setOf(BANDS), 1, false, "Controla a descida."),
        e("cable_curl", "Curl no cabo", ISO_ARMS_BI, listOf(Muscle.BICEPS), setOf(CABLES), 1, true, "Cotovelos fixos."),
        e("chin_iso", "Isometria em elevação (queixo acima da barra)", ISO_ARMS_BI, listOf(Muscle.BICEPS, Muscle.BACK), setOf(PULLUP_BAR), 2, false, "Aguenta em cima o máximo que conseguires.", "15-30 s"),
        e("bench_dips", "Fundos numa cadeira", ISO_ARMS_TRI, listOf(Muscle.TRICEPS), emptySet(), 1, false, "Ombros para trás, desce até 90° no cotovelo."),
        e("diamond_pushup", "Flexões diamante", ISO_ARMS_TRI, listOf(Muscle.TRICEPS, Muscle.CHEST), emptySet(), 2, false, "Mãos juntas debaixo do peito."),
        e("db_triceps", "Extensão de tríceps com halter", ISO_ARMS_TRI, listOf(Muscle.TRICEPS), setOf(DUMBBELLS), 1, true, "Cotovelos a apontar para cima, sem os abrir."),
        e("cable_pushdown", "Tríceps no cabo (pushdown)", ISO_ARMS_TRI, listOf(Muscle.TRICEPS), setOf(CABLES), 1, true, "Cotovelos fixos ao lado do corpo."),
        e("band_triceps", "Extensão de tríceps com elástico", ISO_ARMS_TRI, listOf(Muscle.TRICEPS), setOf(BANDS), 1, false, "Estica bem no fim."),
        // Ombros (isolamento)
        e("lateral_raise", "Elevações laterais com halteres", ISO_SHOULDER, listOf(Muscle.SHOULDERS), setOf(DUMBBELLS), 1, true, "Sobe até à altura dos ombros, sem balançar."),
        e("band_lateral", "Elevações laterais com elástico", ISO_SHOULDER, listOf(Muscle.SHOULDERS), setOf(BANDS), 1, false, "Controla a descida."),
        e("face_pull", "Face pull no cabo", ISO_SHOULDER, listOf(Muscle.SHOULDERS, Muscle.BACK), setOf(CABLES), 1, true, "Puxa a corda até à cara, cotovelos altos."),
        e("band_pull_apart", "Pull-apart com elástico", ISO_SHOULDER, listOf(Muscle.SHOULDERS, Muscle.BACK), setOf(BANDS), 1, false, "Braços esticados, abre até ao peito."),
        // Pernas (isolamento)
        e("leg_ext", "Extensão de pernas (máquina)", ISO_LEGS, listOf(Muscle.QUADS), setOf(MACHINES), 1, true, "Sobe até esticar e controla a descida."),
        e("leg_curl", "Curl femoral (máquina)", ISO_LEGS, listOf(Muscle.HAMSTRINGS), setOf(MACHINES), 1, true, "Anca colada ao banco."),
        e("slider_curl", "Curl femoral deitado com toalha", ISO_LEGS, listOf(Muscle.HAMSTRINGS), emptySet(), 2, false, "Ponte de glúteos e puxa os calcanhares para ti."),
        e("wall_sit", "Cadeira na parede", ISO_LEGS, listOf(Muscle.QUADS), emptySet(), 1, false, "Coxas paralelas ao chão.", "30-60 s"),
        // Gémeos
        e("calf_bw", "Elevação de gémeos", CALF, listOf(Muscle.CALVES), emptySet(), 1, false, "Sobe ao máximo e pausa em cima.", "15-20"),
        e("calf_db", "Elevação de gémeos com halteres", CALF, listOf(Muscle.CALVES), setOf(DUMBBELLS), 1, true, "Num degrau, desce bem o calcanhar."),
        // Core
        e("plank", "Prancha", CORE, listOf(Muscle.CORE), emptySet(), 1, false, "Corpo em linha, glúteos contraídos.", "30-60 s"),
        e("dead_bug", "Dead bug", CORE, listOf(Muscle.CORE), emptySet(), 1, false, "Lombar colada ao chão, estica braço e perna opostos.", "8-12 por lado"),
        e("side_plank", "Prancha lateral", CORE, listOf(Muscle.CORE), emptySet(), 1, false, "Anca alta.", "20-40 s por lado"),
        e("hanging_knee", "Elevação de joelhos suspenso", CORE, listOf(Muscle.CORE), setOf(PULLUP_BAR), 2, false, "Sem balançar, sobe os joelhos ao peito.", "8-15"),
        e("pallof", "Pallof press", CORE, listOf(Muscle.CORE), setOf(BANDS), 1, false, "Resiste à rotação, braços esticados à frente.", "10 por lado"),
        e("cable_pallof", "Pallof press no cabo", CORE, listOf(Muscle.CORE), setOf(CABLES), 1, true, "Resiste à rotação.", "10 por lado"),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Exercise? = byId[id]

    fun available(equipment: Set<GymEquipment>): List<Exercise> =
        all.filter { (equipment + GymEquipment.BODYWEIGHT).containsAll(it.equipment) }
}
