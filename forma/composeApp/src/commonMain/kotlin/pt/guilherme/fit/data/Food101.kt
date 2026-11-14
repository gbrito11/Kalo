package pt.guilherme.fit.data

import pt.guilherme.fit.model.Macros

/**
 * Tabela para o modelo local (treinado no dataset Food-101 + classes extra).
 * Valores aproximados para UMA porção típica. O utilizador ajusta a porção na app.
 */
data class LocalFoodInfo(val label: String, val namePt: String, val portionGrams: Double, val portion: Macros)

object Food101 {
    private fun x(label: String, pt: String, g: Double, kcal: Double, p: Double, c: Double, f: Double) =
        LocalFoodInfo(label, pt, g, Macros(kcal, p, c, f))

    private val table: List<LocalFoodInfo> = listOf(
        x("apple_pie", "Tarte de maçã", 125.0, 300.0, 2.5, 43.0, 14.0),
        x("baby_back_ribs", "Entrecosto grelhado", 250.0, 690.0, 50.0, 10.0, 50.0),
        x("baklava", "Baklava", 60.0, 250.0, 4.0, 28.0, 14.0),
        x("beef_carpaccio", "Carpaccio de vaca", 100.0, 190.0, 21.0, 2.0, 11.0),
        x("beef_tartare", "Tártaro de vaca", 150.0, 300.0, 30.0, 3.0, 19.0),
        x("beet_salad", "Salada de beterraba", 200.0, 180.0, 5.0, 18.0, 10.0),
        x("beignets", "Sonhos / beignets", 100.0, 380.0, 6.0, 45.0, 19.0),
        x("bibimbap", "Bibimbap", 450.0, 560.0, 25.0, 80.0, 15.0),
        x("bread_pudding", "Pudim de pão", 150.0, 330.0, 8.0, 48.0, 12.0),
        x("breakfast_burrito", "Burrito de pequeno-almoço", 250.0, 520.0, 24.0, 45.0, 26.0),
        x("bruschetta", "Bruschetta", 120.0, 220.0, 6.0, 28.0, 9.0),
        x("caesar_salad", "Salada César", 250.0, 360.0, 18.0, 15.0, 26.0),
        x("cannoli", "Cannoli", 90.0, 300.0, 7.0, 33.0, 16.0),
        x("caprese_salad", "Salada caprese", 200.0, 330.0, 18.0, 6.0, 26.0),
        x("carrot_cake", "Bolo de cenoura", 110.0, 400.0, 4.5, 50.0, 21.0),
        x("ceviche", "Ceviche", 200.0, 200.0, 28.0, 12.0, 4.0),
        x("cheese_plate", "Tábua de queijos", 100.0, 380.0, 23.0, 2.0, 31.0),
        x("cheesecake", "Cheesecake", 120.0, 400.0, 7.0, 32.0, 28.0),
        x("chicken_curry", "Caril de frango", 350.0, 450.0, 32.0, 18.0, 27.0),
        x("chicken_quesadilla", "Quesadilla de frango", 220.0, 560.0, 32.0, 40.0, 30.0),
        x("chicken_wings", "Asas de frango", 200.0, 500.0, 40.0, 4.0, 36.0),
        x("chocolate_cake", "Bolo de chocolate", 110.0, 410.0, 5.0, 52.0, 21.0),
        x("chocolate_mousse", "Mousse de chocolate", 100.0, 300.0, 5.0, 25.0, 20.0),
        x("churros", "Churros", 100.0, 420.0, 5.0, 45.0, 25.0),
        x("clam_chowder", "Sopa cremosa de amêijoa", 300.0, 300.0, 14.0, 25.0, 16.0),
        x("club_sandwich", "Sanduíche club", 250.0, 590.0, 32.0, 45.0, 31.0),
        x("crab_cakes", "Bolinhos de caranguejo", 150.0, 340.0, 20.0, 15.0, 22.0),
        x("creme_brulee", "Leite-creme / crème brûlée", 120.0, 320.0, 5.0, 28.0, 21.0),
        x("croque_madame", "Croque madame", 250.0, 620.0, 33.0, 38.0, 37.0),
        x("cup_cakes", "Cupcake", 70.0, 270.0, 2.5, 38.0, 12.0),
        x("deviled_eggs", "Ovos recheados", 100.0, 200.0, 11.0, 1.0, 17.0),
        x("donuts", "Donut", 75.0, 300.0, 4.0, 34.0, 17.0),
        x("dumplings", "Dumplings / gyoza cozidos", 150.0, 300.0, 12.0, 36.0, 12.0),
        x("edamame", "Edamame", 150.0, 180.0, 17.0, 13.0, 8.0),
        x("eggs_benedict", "Ovos Benedict", 250.0, 600.0, 26.0, 30.0, 42.0),
        x("escargots", "Caracóis com manteiga", 100.0, 230.0, 14.0, 2.0, 18.0),
        x("falafel", "Falafel", 150.0, 500.0, 20.0, 47.0, 27.0),
        x("filet_mignon", "Lombo de vaca (filet mignon)", 200.0, 460.0, 54.0, 0.0, 26.0),
        x("fish_and_chips", "Peixe frito com batatas fritas", 350.0, 840.0, 35.0, 80.0, 42.0),
        x("foie_gras", "Foie gras", 50.0, 230.0, 6.0, 2.0, 22.0),
        x("french_fries", "Batatas fritas", 150.0, 470.0, 5.0, 60.0, 23.0),
        x("french_onion_soup", "Sopa de cebola gratinada", 350.0, 370.0, 16.0, 35.0, 18.0),
        x("french_toast", "Rabanadas / french toast", 150.0, 380.0, 12.0, 45.0, 17.0),
        x("fried_calamari", "Lulas fritas", 150.0, 330.0, 23.0, 20.0, 17.0),
        x("fried_rice", "Arroz frito", 300.0, 500.0, 14.0, 70.0, 18.0),
        x("frozen_yogurt", "Iogurte gelado", 150.0, 190.0, 5.0, 33.0, 4.0),
        x("garlic_bread", "Pão de alho", 80.0, 280.0, 6.0, 33.0, 14.0),
        x("gnocchi", "Gnocchi", 300.0, 480.0, 12.0, 75.0, 15.0),
        x("greek_salad", "Salada grega", 250.0, 290.0, 9.0, 12.0, 23.0),
        x("grilled_cheese_sandwich", "Tosta de queijo", 150.0, 450.0, 17.0, 35.0, 27.0),
        x("grilled_salmon", "Salmão grelhado", 180.0, 370.0, 38.0, 0.0, 23.0),
        x("guacamole", "Guacamole", 100.0, 160.0, 2.0, 9.0, 14.0),
        x("gyoza", "Gyoza", 150.0, 330.0, 13.0, 34.0, 15.0),
        x("hamburger", "Hambúrguer", 250.0, 600.0, 32.0, 42.0, 33.0),
        x("hot_and_sour_soup", "Sopa agridoce chinesa", 350.0, 160.0, 10.0, 15.0, 6.0),
        x("hot_dog", "Cachorro-quente", 150.0, 400.0, 14.0, 32.0, 24.0),
        x("huevos_rancheros", "Huevos rancheros", 300.0, 470.0, 21.0, 38.0, 26.0),
        x("hummus", "Húmus", 100.0, 166.0, 8.0, 14.0, 10.0),
        x("ice_cream", "Gelado", 100.0, 210.0, 3.5, 24.0, 11.0),
        x("lasagna", "Lasanha", 350.0, 560.0, 30.0, 45.0, 28.0),
        x("lobster_bisque", "Creme de lagosta", 300.0, 330.0, 12.0, 18.0, 23.0),
        x("lobster_roll_sandwich", "Sanduíche de lagosta", 200.0, 440.0, 25.0, 35.0, 22.0),
        x("macaroni_and_cheese", "Massa com queijo", 300.0, 600.0, 24.0, 60.0, 29.0),
        x("macarons", "Macarons", 50.0, 200.0, 3.5, 26.0, 9.0),
        x("miso_soup", "Sopa miso", 250.0, 80.0, 6.0, 8.0, 3.0),
        x("mussels", "Mexilhões", 250.0, 290.0, 30.0, 10.0, 12.0),
        x("nachos", "Nachos", 250.0, 750.0, 22.0, 70.0, 42.0),
        x("omelette", "Omelete", 180.0, 330.0, 22.0, 3.0, 26.0),
        x("onion_rings", "Anéis de cebola fritos", 120.0, 400.0, 5.0, 40.0, 24.0),
        x("oysters", "Ostras", 120.0, 100.0, 11.0, 6.0, 3.0),
        x("pad_thai", "Pad thai", 350.0, 640.0, 26.0, 80.0, 23.0),
        x("paella", "Paelha", 400.0, 600.0, 32.0, 75.0, 18.0),
        x("pancakes", "Panquecas", 200.0, 460.0, 12.0, 60.0, 19.0),
        x("panna_cotta", "Panna cotta", 120.0, 300.0, 4.0, 25.0, 21.0),
        x("peking_duck", "Pato à Pequim", 250.0, 600.0, 35.0, 30.0, 38.0),
        x("pho", "Pho", 500.0, 450.0, 30.0, 55.0, 10.0),
        x("pizza", "Pizza (2 fatias)", 220.0, 590.0, 25.0, 66.0, 24.0),
        x("pork_chop", "Costeleta de porco", 200.0, 460.0, 50.0, 0.0, 28.0),
        x("poutine", "Poutine", 350.0, 740.0, 22.0, 75.0, 40.0),
        x("prime_rib", "Costela de vaca assada", 250.0, 750.0, 55.0, 0.0, 58.0),
        x("pulled_pork_sandwich", "Sanduíche de porco desfiado", 250.0, 560.0, 32.0, 52.0, 24.0),
        x("ramen", "Ramen", 550.0, 600.0, 26.0, 75.0, 22.0),
        x("ravioli", "Ravioli", 300.0, 520.0, 22.0, 60.0, 21.0),
        x("red_velvet_cake", "Bolo red velvet", 110.0, 420.0, 4.5, 55.0, 21.0),
        x("risotto", "Risotto", 350.0, 520.0, 14.0, 70.0, 19.0),
        x("samosa", "Chamuça / samosa", 120.0, 330.0, 6.0, 35.0, 18.0),
        x("sashimi", "Sashimi", 150.0, 200.0, 32.0, 0.0, 7.0),
        x("scallops", "Vieiras", 150.0, 200.0, 25.0, 8.0, 7.0),
        x("seaweed_salad", "Salada de algas", 100.0, 70.0, 1.5, 10.0, 3.0),
        x("shrimp_and_grits", "Camarão com papas de milho", 350.0, 550.0, 30.0, 45.0, 27.0),
        x("spaghetti_bolognese", "Esparguete à bolonhesa", 400.0, 620.0, 30.0, 75.0, 20.0),
        x("spaghetti_carbonara", "Esparguete à carbonara", 350.0, 700.0, 28.0, 75.0, 31.0),
        x("spring_rolls", "Crepes chineses", 150.0, 380.0, 9.0, 40.0, 20.0),
        x("steak", "Bife", 220.0, 520.0, 55.0, 0.0, 33.0),
        x("strawberry_shortcake", "Bolo de morango com natas", 130.0, 340.0, 4.0, 42.0, 17.0),
        x("sushi", "Sushi (8 peças)", 250.0, 380.0, 15.0, 70.0, 5.0),
        x("tacos", "Tacos (2)", 200.0, 420.0, 22.0, 34.0, 22.0),
        x("takoyaki", "Takoyaki", 150.0, 300.0, 10.0, 35.0, 13.0),
        x("tiramisu", "Tiramisu", 130.0, 380.0, 7.0, 36.0, 22.0),
        x("tuna_tartare", "Tártaro de atum", 150.0, 250.0, 30.0, 4.0, 12.0),
        x("waffles", "Waffles", 150.0, 440.0, 11.0, 50.0, 21.0),
    )

    /** Classes extra que podes treinar (pratos portugueses). Ver ml/README.md. */
    private val extra: List<LocalFoodInfo> = listOf(
        x("bacalhau_bras", "Bacalhau à Brás", 350.0, 600.0, 35.0, 30.0, 38.0),
        x("francesinha", "Francesinha", 550.0, 1300.0, 70.0, 80.0, 75.0),
        x("caldo_verde", "Caldo verde", 300.0, 160.0, 5.0, 18.0, 8.0),
        x("pastel_de_nata", "Pastel de nata", 60.0, 200.0, 3.0, 24.0, 10.0),
        x("arroz_de_pato", "Arroz de pato", 400.0, 720.0, 38.0, 70.0, 30.0),
        x("bifana", "Bifana", 200.0, 450.0, 28.0, 40.0, 19.0),
        x("sopa_legumes", "Sopa de legumes", 300.0, 120.0, 3.0, 17.0, 5.0),
        x("frango_assado", "Frango assado", 250.0, 480.0, 55.0, 0.0, 28.0),
    )

    private val byLabel = (table + extra).associateBy { it.label }

    /** As 101 classes do Food-101, por ordem alfabética (igual ao tfds). */
    val labels: List<String> = table.map { it.label }

    fun get(label: String): LocalFoodInfo? = byLabel[label.trim().lowercase().replace(' ', '_')]
}
