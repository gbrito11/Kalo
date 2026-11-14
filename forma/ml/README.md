# Modelo local de reconhecimento de comida

O modelo corre no telemóvel e não precisa de internet. A app funciona sem ele: nesse caso as fotos são analisadas só com o Gemini.

## 1. Treinar (Google Colab, grátis)

1. Abre [colab.research.google.com](https://colab.research.google.com) e cria um notebook novo.
2. Vai a **Runtime → Change runtime type → T4 GPU**.
3. Faz upload de `train_food_model.py` (ícone da pasta, à esquerda) e corre:

```
!pip install -q torch torchvision onnx onnxscript coremltools
!python train_food_model.py --epochs 4 --fine_tune_epochs 3
```

Demora cerca de 1h20 (o Food-101 tem 5 GB e descarrega sozinho). No fim, a pasta `out/` tem:

| Ficheiro | Para onde vai |
|---|---|
| `food_model.onnx` | `composeApp/src/androidMain/assets/` |
| `labels.txt` | `composeApp/src/androidMain/assets/` |
| `FoodClassifier.mlpackage.zip` | descompacta para `iosApp/Forma/Models/` |

Se o Colab desligar a meio, corre de novo com menos épocas (`--epochs 2 --fine_tune_epochs 2`). O modelo fica um pouco menos preciso.

## 2. Pôr o modelo na app sem o meter no Git

Os ficheiros são grandes (~16 MB cada), por isso o `.gitignore` deixa-os de fora. Os workflows do GitHub vão buscá-los a um release chamado `model`:

1. No GitHub: **Releases → Draft a new release → tag `model`**.
2. Anexa `food_model.onnx`, `labels.txt` e `FoodClassifier.mlpackage.zip` e publica.
3. O próximo build (Android e iOS) já inclui o modelo.

## 3. Juntar pratos portugueses (opcional)

O Food-101 não tem comida portuguesa. Para a app reconhecer pratos teus, cria esta estrutura com 50 a 150 fotos por prato (fotos tuas ou de pesquisa de imagens):

```
extra/
  bacalhau_bras/      *.jpg
  francesinha/        *.jpg
  caldo_verde/        *.jpg
  pastel_de_nata/     *.jpg
  arroz_de_pato/      *.jpg
  bifana/             *.jpg
  sopa_legumes/       *.jpg
  frango_assado/      *.jpg
```

Usa exatamente estes nomes de pasta: a app tem as calorias destes pratos na tabela `Food101.kt`. Para outro prato, acrescenta uma linha a `extra` em `composeApp/src/commonMain/kotlin/pt/guilherme/fit/data/Food101.kt`.

Depois corre:

```
!python train_food_model.py --extra_dir extra --epochs 4 --fine_tune_epochs 3
```

## Limites

- O modelo reconhece **o prato**, não a quantidade. A porção vem de uma tabela (porção típica) e ajustas na app com o seletor.
- Pratos misturados ou com molho escondido baixam a certeza. Abaixo do limite que escolheres nas definições, a app passa a foto ao Gemini (no modo "Local, e Gemini se não tiver certeza").
