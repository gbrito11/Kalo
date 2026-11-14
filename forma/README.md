# Forma — app de fitness (Kotlin Multiplatform)

Uma app para Android e iPhone com o mesmo código Kotlin (Compose Multiplatform).

## O que faz

| Área | Funções |
|---|---|
| **Perfil e metas** | Sexo, idade, altura, peso atual e objetivo, nível de atividade, ritmo (kg/semana). Calcula a basal (Mifflin-St Jeor), o gasto diário, o défice ou excedente, os macros, a água e o tempo até ao objetivo. |
| **Alimentação** | Tipo de dieta (mediterrânica, equilibrada, rica em proteína, low carb, cetogénica, vegetariana, vegan, pescetariana), alergias e intolerâncias, alimentos de que não gostas, tempo máximo por refeição e número de refeições por dia. |
| **Diário** | Registo por refeição: foto com IA, pesquisa numa base de ~95 alimentos, receitas ou valores manuais. |
| **Fotos com IA** | Modelo no telemóvel (offline, treinado por ti em `ml/`) e/ou Gemini (cloud, plano grátis). Por defeito usa o local e, se tiver pouca certeza, passa ao Gemini. Ajustas a porção e escolhes alternativas antes de guardar. |
| **Despensa** | Indicas os ingredientes e o equipamento de cozinha que tens. A app sugere receitas ordenadas pelo que já tens e diz o que falta. |
| **Plano de refeições** | Plano semanal que respeita as calorias, a dieta, as restrições, o tempo e a despensa. Gera a lista de compras. |
| **Treino** | Plano por dias/semana, duração, experiência e equipamento (peso corporal, halteres, barra, máquinas...). Divisões de corpo inteiro, superior/inferior ou empurrar/puxar/pernas. Cardio extra se o objetivo for perder gordura. Checklist, temporizador de descanso e histórico. |
| **Calendário** | Mês com treinos planeados e feitos, refeições planeadas e dias acima da meta. Cada dia abre o detalhe. |
| **Saúde** | Apple Health (iPhone) e Health Connect (Android): lê passos, kcal ativas, minutos de exercício e peso (o que o relógio, o Strava, etc. lá escrevem) e escreve água, peso e nutrição. Opção de usar as kcal ativas reais na meta diária. |
| **Coach** | Botão que manda o resumo do dia ao Gemini e devolve 2-3 ações concretas. |
| **Água e peso** | Botões rápidos (+250/330/500 ml), gráfico do peso com linha do objetivo. |

Os dados ficam num ficheiro no telemóvel. Não há servidor nem conta.

## Estrutura

```
composeApp/src/
  commonMain/   lógica e ecrãs (partilhado entre Android e iOS)
    model/      dados (perfil, registos, receitas, treino)
    data/       base de alimentos, receitas, exercícios, tabela do modelo local, gravação
    domain/     cálculos: metas, receitas, plano de refeições, gerador de treinos, datas
    ai/         Gemini + escolha entre modelo local e cloud
    ui/         ecrãs Compose
  commonTest/   testes da lógica
  androidMain/  Health Connect, ONNX Runtime, câmara
  iosMain/      câmara, ficheiros, ponte para o Swift
iosApp/         app iOS (SwiftUI + HealthKit + Core ML), projeto gerado com XcodeGen
ml/             script para treinar o teu modelo de comida (Google Colab)
.github/        builds automáticos (APK Android e .ipa iOS)
```

## Instalar no iPhone sem Mac

O Xcode só existe em macOS, por isso o build do iPhone é feito num Mac gratuito do GitHub Actions.

### 1. Pôr o código no GitHub

1. Cria um repositório (pode ser privado) em github.com.
2. Na pasta do projeto:
   ```
   git init
   git add .
   git commit -m "Forma"
   git branch -M main
   git remote add origin https://github.com/<teu-user>/<repo>.git
   git push -u origin main
   ```
3. O push arranca os workflows. Em **Actions → iOS (.ipa para Sideloadly)** espera que fique verde (~15-20 min a primeira vez).
4. Em baixo, em **Artifacts**, descarrega `Forma-ipa` e descompacta: tens o `Forma.ipa`.

Repositórios privados têm 2000 minutos grátis por mês, mas os runners de macOS contam a dobrar ou mais. Um repositório público não tem limite.

### 2. Instalar com o Sideloadly (Windows)

1. Instala o **iTunes** e o **iCloud** da Apple (versões do site da Apple, não as da Microsoft Store).
2. Instala o [Sideloadly](https://sideloadly.io).
3. Liga o iPhone por cabo e aceita "Confiar neste computador".
4. Arrasta o `Forma.ipa` para o Sideloadly, mete o teu Apple ID e carrega em **Start**.
5. No iPhone:
   - **Definições → Privacidade e segurança → Modo de programador → ligar** (o iPhone reinicia).
   - **Definições → Geral → VPN e gestão de dispositivos →** o teu Apple ID **→ Confiar**.

Com um Apple ID grátis a app dura **7 dias**. Depois voltas a carregar em Start no Sideloadly (os dados mantêm-se). Com a conta de programador paga (99 €/ano) dura 1 ano.

**Apple Health com conta grátis:** a app pede a permissão do HealthKit. Se o Sideloadly ou o iPhone recusarem essa permissão com conta grátis, desliga "Usar kcal ativas reais" e o resto da app funciona sem o Apple Health.

## Android

Em **Actions → Android (APK + testes)** descarrega `Forma-apk` e instala o `.apk` no telemóvel. Para o Health Connect funcionar, a app Health Connect tem de estar instalada (vem de série no Android 14+).

Para desenvolver: abre a pasta no Android Studio e corre a configuração `composeApp`.

## Gemini (análise de fotos e coach)

1. Vai a [aistudio.google.com](https://aistudio.google.com) → **Get API key** → cria a chave (grátis).
2. Na app: ícone do perfil → **Análise de fotos** → cola a chave.

O plano grátis tem limite diário de pedidos e o Google pode usar os pedidos para melhorar os modelos. Se o nome do modelo deixar de existir, muda-o nas definições (ex.: `gemini-2.5-flash`, `gemini-2.5-flash-lite`).

## Modelo local (opção 2)

Vê `ml/README.md`. Resumo: treinas no Google Colab, publicas os ficheiros num release `model` do GitHub e os builds incluem-nos automaticamente.

## Testes

A lógica (metas, filtros de receitas, plano de refeições, gerador de treinos, datas) tem testes em `composeApp/src/commonTest`. Correm nos dois workflows:

```
./gradlew :composeApp:testDebugUnitTest
```

## Notas

- As calorias da base de alimentos e da tabela do modelo são aproximadas. A meta é uma estimativa: se em 2-3 semanas o peso não mexer como previsto, ajusta o nível de atividade ou o ritmo.
- A app nunca desce abaixo de 1500 kcal (homens) / 1200 kcal (mulheres) nem faz défices maiores que 25% do gasto.
