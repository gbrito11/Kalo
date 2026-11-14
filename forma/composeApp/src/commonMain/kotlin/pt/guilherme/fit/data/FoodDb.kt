package pt.guilherme.fit.data

import pt.guilherme.fit.model.Food
import pt.guilherme.fit.model.FoodCategory
import pt.guilherme.fit.model.FoodCategory.*
import pt.guilherme.fit.model.Macros
import pt.guilherme.fit.model.Restriction

/**
 * Base de alimentos (valores por 100 g, crus salvo indicação).
 * Valores aproximados de tabelas públicas (INSA / USDA). Servem para estimar, não são exatos.
 */
object FoodDb {

    private fun f(
        key: String, name: String, kcal: Double, p: Double, c: Double, fat: Double,
        cat: FoodCategory, allergens: Set<Restriction> = emptySet(), syn: List<String> = emptyList(),
        staple: Boolean = false, unit: Double? = null, unitName: String? = null, vegan: Boolean? = null,
    ): Food {
        val base = Food(key, name, Macros(kcal, p, c, fat), cat, allergens, syn, staple, unit, unitName)
        return if (vegan != null) base.copy(vegan = vegan) else base
    }

    val all: List<Food> = listOf(
        // Carne e aves
        f("frango", "Peito de frango", 120.0, 23.0, 0.0, 2.6, POULTRY, syn = listOf("frango", "peito de frango", "galinha", "bife de frango")),
        f("coxa_frango", "Coxa de frango sem pele", 145.0, 19.0, 0.0, 7.5, POULTRY, syn = listOf("coxa", "perna de frango")),
        f("peru", "Peito de peru", 114.0, 23.6, 0.0, 1.6, POULTRY, syn = listOf("peru", "bife de peru")),
        f("vaca", "Bife de vaca magro", 140.0, 21.0, 0.0, 6.0, MEAT, syn = listOf("vaca", "bife", "novilho")),
        f("carne_picada", "Carne picada de vaca (5% gordura)", 137.0, 21.0, 0.0, 5.0, MEAT, syn = listOf("carne picada", "picada")),
        f("porco", "Lombo de porco", 143.0, 21.0, 0.0, 6.0, MEAT, syn = listOf("porco", "lombo", "febras")),
        f("fiambre", "Fiambre de peru", 104.0, 17.0, 2.0, 3.0, PROCESSED_MEAT, syn = listOf("fiambre")),
        f("presunto", "Presunto", 250.0, 28.0, 0.0, 15.0, PROCESSED_MEAT),
        f("chourico", "Chouriço", 455.0, 24.0, 2.0, 38.0, PROCESSED_MEAT),
        // Peixe e marisco
        f("salmao", "Salmão", 208.0, 20.0, 0.0, 13.0, FISH, setOf(Restriction.FISH)),
        f("bacalhau", "Bacalhau demolhado", 90.0, 20.0, 0.0, 0.9, FISH, setOf(Restriction.FISH)),
        f("pescada", "Pescada", 80.0, 17.0, 0.0, 1.0, FISH, setOf(Restriction.FISH), listOf("pescada", "peixe branco", "maruca", "abrótea")),
        f("atum", "Atum ao natural (lata)", 116.0, 26.0, 0.0, 1.0, FISH, setOf(Restriction.FISH), listOf("atum", "lata de atum"), unit = 80.0, unitName = "lata"),
        f("sardinha", "Sardinha", 135.0, 20.0, 0.0, 6.0, FISH, setOf(Restriction.FISH)),
        f("camarao", "Camarão", 85.0, 20.0, 0.0, 0.5, SEAFOOD, setOf(Restriction.SHELLFISH), listOf("camarão", "gambas")),
        f("polvo", "Polvo", 82.0, 15.0, 2.2, 1.0, SEAFOOD, setOf(Restriction.SHELLFISH)),
        // Ovos e laticínios
        f("ovo", "Ovo", 143.0, 12.6, 0.7, 9.5, EGG, setOf(Restriction.EGG), listOf("ovo", "ovos"), unit = 55.0, unitName = "ovo"),
        f("claras", "Claras de ovo", 52.0, 11.0, 0.7, 0.2, EGG, setOf(Restriction.EGG), listOf("claras")),
        f("leite", "Leite meio-gordo", 47.0, 3.3, 4.8, 1.6, DAIRY, setOf(Restriction.LACTOSE), listOf("leite")),
        f("iogurte_grego", "Iogurte grego natural magro", 59.0, 10.0, 3.6, 0.4, DAIRY, setOf(Restriction.LACTOSE), listOf("iogurte grego", "skyr"), unit = 170.0, unitName = "copo"),
        f("iogurte", "Iogurte natural", 61.0, 3.5, 4.7, 3.3, DAIRY, setOf(Restriction.LACTOSE), listOf("iogurte"), unit = 125.0, unitName = "iogurte"),
        f("queijo_fresco", "Queijo fresco", 140.0, 11.0, 3.0, 9.0, DAIRY, setOf(Restriction.LACTOSE), listOf("queijo fresco", "requeijão")),
        f("queijo", "Queijo flamengo", 340.0, 25.0, 1.0, 27.0, DAIRY, setOf(Restriction.LACTOSE), listOf("queijo", "flamengo", "queijo em fatias")),
        f("mozzarella", "Mozzarella", 250.0, 18.0, 2.0, 19.0, DAIRY, setOf(Restriction.LACTOSE)),
        f("feta", "Queijo feta", 264.0, 14.0, 4.0, 21.0, DAIRY, setOf(Restriction.LACTOSE)),
        f("parmesao", "Parmesão", 392.0, 36.0, 3.0, 26.0, DAIRY, setOf(Restriction.LACTOSE), listOf("parmesão", "parmigiano")),
        f("manteiga", "Manteiga", 717.0, 0.9, 0.1, 81.0, DAIRY, setOf(Restriction.LACTOSE)),
        f("whey", "Proteína whey", 380.0, 78.0, 8.0, 5.0, SUPPLEMENT, setOf(Restriction.LACTOSE), listOf("whey", "proteína em pó"), vegan = false),
        // Proteína vegetal e leguminosas
        f("tofu", "Tofu firme", 120.0, 13.0, 2.0, 7.0, PLANT_PROTEIN, setOf(Restriction.SOY)),
        f("grao", "Grão-de-bico cozido", 139.0, 7.2, 22.0, 2.6, LEGUME, syn = listOf("grão", "grão de bico", "grao-de-bico")),
        f("feijao", "Feijão cozido", 110.0, 7.0, 16.0, 0.5, LEGUME, syn = listOf("feijão", "feijão vermelho", "feijão preto")),
        f("lentilhas", "Lentilhas cozidas", 116.0, 9.0, 20.0, 0.4, LEGUME),
        f("ervilhas", "Ervilhas", 81.0, 5.4, 14.0, 0.4, LEGUME),
        f("hummus", "Húmus", 166.0, 8.0, 14.0, 10.0, LEGUME, syn = listOf("húmus", "hummus")),
        f("bebida_soja", "Bebida de soja", 39.0, 3.3, 2.5, 1.8, PLANT_PROTEIN, setOf(Restriction.SOY), listOf("leite de soja", "bebida de soja")),
        f("bebida_aveia", "Bebida de aveia", 45.0, 1.0, 6.5, 1.5, GRAIN, setOf(Restriction.GLUTEN), listOf("leite de aveia", "bebida de aveia")),
        // Cereais e tubérculos
        f("arroz", "Arroz branco (cru)", 360.0, 7.0, 79.0, 0.6, GRAIN, syn = listOf("arroz")),
        f("arroz_integral", "Arroz integral (cru)", 362.0, 7.5, 76.0, 2.7, GRAIN),
        f("massa", "Massa (crua)", 357.0, 12.5, 72.0, 1.5, GRAIN, setOf(Restriction.GLUTEN), listOf("massa", "esparguete", "macarrão", "penne", "fusilli")),
        f("massa_integral", "Massa integral (crua)", 348.0, 13.0, 66.0, 2.5, GRAIN, setOf(Restriction.GLUTEN)),
        f("pao", "Pão de mistura", 265.0, 9.0, 49.0, 3.2, GRAIN, setOf(Restriction.GLUTEN), listOf("pão", "carcaça", "pão de forma"), unit = 50.0, unitName = "pão"),
        f("pao_integral", "Pão integral", 250.0, 12.0, 41.0, 3.5, GRAIN, setOf(Restriction.GLUTEN), listOf("pão integral"), unit = 30.0, unitName = "fatia"),
        f("tortilha", "Tortilha de trigo", 310.0, 8.0, 52.0, 7.5, GRAIN, setOf(Restriction.GLUTEN), listOf("tortilha", "wrap"), unit = 40.0, unitName = "tortilha"),
        f("aveia", "Flocos de aveia", 379.0, 13.0, 60.0, 7.0, GRAIN, setOf(Restriction.GLUTEN), listOf("aveia", "flocos de aveia")),
        f("granola", "Granola", 470.0, 10.0, 60.0, 20.0, GRAIN, setOf(Restriction.GLUTEN, Restriction.NUTS)),
        f("quinoa", "Quinoa (crua)", 368.0, 14.0, 64.0, 6.0, GRAIN),
        f("cuscuz", "Cuscuz (cru)", 376.0, 13.0, 77.0, 0.6, GRAIN, setOf(Restriction.GLUTEN), listOf("cuscuz", "couscous")),
        f("batata", "Batata", 77.0, 2.0, 17.0, 0.1, TUBER, syn = listOf("batata", "batatas")),
        f("batata_doce", "Batata-doce", 86.0, 1.6, 20.0, 0.1, TUBER, syn = listOf("batata doce", "batata-doce")),
        f("milho", "Milho (lata)", 81.0, 3.0, 15.0, 1.2, VEGETABLE),
        // Legumes
        f("tomate", "Tomate", 18.0, 0.9, 3.9, 0.2, VEGETABLE, syn = listOf("tomate", "tomate cherry")),
        f("tomate_pelado", "Tomate pelado / polpa", 24.0, 1.2, 4.0, 0.2, VEGETABLE, syn = listOf("tomate pelado", "polpa de tomate", "passata", "tomate em lata")),
        f("alface", "Alface / mistura de folhas", 15.0, 1.4, 2.9, 0.2, VEGETABLE, syn = listOf("alface", "salada", "folhas")),
        f("rucula", "Rúcula", 25.0, 2.6, 3.7, 0.7, VEGETABLE, syn = listOf("rúcula", "rucula")),
        f("espinafres", "Espinafres", 23.0, 2.9, 3.6, 0.4, VEGETABLE, syn = listOf("espinafre", "espinafres")),
        f("brocolos", "Brócolos", 34.0, 2.8, 7.0, 0.4, VEGETABLE, syn = listOf("brócolos", "brocolos", "brócolo")),
        f("cenoura", "Cenoura", 41.0, 0.9, 10.0, 0.2, VEGETABLE, syn = listOf("cenoura", "cenouras")),
        f("cebola", "Cebola", 40.0, 1.1, 9.3, 0.1, VEGETABLE, syn = listOf("cebola", "cebolas")),
        f("pimento", "Pimento", 31.0, 1.0, 6.0, 0.3, VEGETABLE, syn = listOf("pimento", "pimentos")),
        f("curgete", "Curgete", 17.0, 1.2, 3.1, 0.3, VEGETABLE, syn = listOf("curgete", "courgette", "abobrinha")),
        f("beringela", "Beringela", 25.0, 1.0, 6.0, 0.2, VEGETABLE),
        f("cogumelos", "Cogumelos", 22.0, 3.1, 3.3, 0.3, VEGETABLE, syn = listOf("cogumelos", "champignons")),
        f("pepino", "Pepino", 15.0, 0.7, 3.6, 0.1, VEGETABLE),
        f("couve", "Couve", 32.0, 3.0, 5.0, 0.5, VEGETABLE, syn = listOf("couve", "couve portuguesa", "couve galega", "kale")),
        f("feijao_verde", "Feijão-verde", 31.0, 1.8, 7.0, 0.2, VEGETABLE, syn = listOf("feijão verde", "feijao verde")),
        f("abobora", "Abóbora", 26.0, 1.0, 6.5, 0.1, VEGETABLE, syn = listOf("abóbora", "abobora")),
        f("abacate", "Abacate", 160.0, 2.0, 8.5, 14.7, FRUIT, unit = 150.0, unitName = "abacate"),
        f("azeitonas", "Azeitonas", 145.0, 1.0, 3.8, 15.0, FRUIT),
        // Fruta
        f("banana", "Banana", 89.0, 1.1, 23.0, 0.3, FRUIT, unit = 120.0, unitName = "banana"),
        f("maca", "Maçã", 52.0, 0.3, 14.0, 0.2, FRUIT, syn = listOf("maçã", "maca", "maçãs"), unit = 150.0, unitName = "maçã"),
        f("pera", "Pera", 57.0, 0.4, 15.0, 0.1, FRUIT, unit = 160.0, unitName = "pera"),
        f("laranja", "Laranja", 47.0, 0.9, 12.0, 0.1, FRUIT, unit = 160.0, unitName = "laranja"),
        f("kiwi", "Kiwi", 61.0, 1.1, 15.0, 0.5, FRUIT, unit = 75.0, unitName = "kiwi"),
        f("morangos", "Morangos", 32.0, 0.7, 7.7, 0.3, FRUIT, syn = listOf("morango", "morangos")),
        f("frutos_vermelhos", "Frutos vermelhos", 50.0, 0.7, 12.0, 0.3, FRUIT, syn = listOf("frutos vermelhos", "mirtilos", "framboesas")),
        f("limao", "Limão", 29.0, 1.1, 9.0, 0.3, FRUIT, syn = listOf("limão", "limao"), staple = true),
        // Gorduras, frutos secos, doces
        f("azeite", "Azeite", 884.0, 0.0, 0.0, 100.0, FAT, staple = true),
        f("amendoas", "Amêndoas", 579.0, 21.0, 22.0, 50.0, NUT_SEED, setOf(Restriction.NUTS), listOf("amêndoa", "amêndoas", "amendoas")),
        f("nozes", "Nozes", 654.0, 15.0, 14.0, 65.0, NUT_SEED, setOf(Restriction.NUTS), listOf("noz", "nozes")),
        f("manteiga_amendoim", "Manteiga de amendoim", 588.0, 25.0, 20.0, 50.0, NUT_SEED, setOf(Restriction.PEANUT), listOf("manteiga de amendoim", "pasta de amendoim")),
        f("chia", "Sementes de chia", 486.0, 17.0, 42.0, 31.0, NUT_SEED, syn = listOf("chia", "sementes de chia")),
        f("leite_coco", "Leite de coco", 230.0, 2.3, 6.0, 24.0, FAT, syn = listOf("leite de coco")),
        f("mel", "Mel", 304.0, 0.3, 82.0, 0.0, SWEET, vegan = false),
        f("chocolate_negro", "Chocolate negro 70%", 598.0, 7.8, 46.0, 43.0, SWEET, vegan = false),
        // Condimentos (assume-se que há em casa)
        f("sal", "Sal", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("pimenta", "Pimenta", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("alho", "Alho", 149.0, 6.4, 33.0, 0.5, CONDIMENT, staple = true),
        f("oregaos", "Orégãos", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("colorau", "Colorau / paprika", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("caril", "Caril em pó", 325.0, 14.0, 56.0, 14.0, CONDIMENT, staple = true),
        f("cominhos", "Cominhos", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("canela", "Canela", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("louro", "Louro", 0.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("vinagre", "Vinagre", 18.0, 0.0, 0.0, 0.0, CONDIMENT, staple = true),
        f("molho_soja", "Molho de soja", 53.0, 8.0, 4.9, 0.6, CONDIMENT, setOf(Restriction.SOY, Restriction.GLUTEN), listOf("molho de soja", "soja")),
        f("mostarda", "Mostarda", 66.0, 4.0, 6.0, 3.0, CONDIMENT),
    )

    private val byKey = all.associateBy { it.key }

    fun get(key: String): Food? = byKey[key]

    fun search(query: String, limit: Int = 20): List<Food> {
        val q = TextNorm.norm(query)
        if (q.isBlank()) return all.filter { !it.staple }.take(limit)
        return all
            .map { food -> food to TextNorm.matchScore(q, food.name, food.synonyms) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}

object TextNorm {
    private val accents = mapOf(
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
        'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
        'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c', 'ñ' to 'n',
    )

    fun norm(s: String): String =
        s.trim().lowercase().map { accents[it] ?: it }.joinToString("")
            .replace('-', ' ').replace(Regex("\\s+"), " ")

    /** 0 = não bate; maior = melhor. */
    fun matchScore(normQuery: String, name: String, synonyms: List<String>): Int {
        var best = 0
        for (cand in listOf(name) + synonyms) {
            val c = norm(cand)
            val score = when {
                c == normQuery -> 100
                c.startsWith(normQuery) -> 80
                c.contains(normQuery) -> 60
                normQuery.length >= 4 && normQuery.contains(c) && c.length >= 3 -> 40
                else -> 0
            }
            if (score > best) best = score
        }
        return best
    }

    /** Remove o "s" final simples para comparar plural/singular. */
    fun singular(s: String): String = if (s.length > 3 && s.endsWith("s")) s.dropLast(1) else s
}
