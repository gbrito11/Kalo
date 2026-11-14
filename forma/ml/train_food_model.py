"""
Treina o modelo de reconhecimento de comida do Forma e exporta-o para:
  - food_model.onnx + labels.txt     -> Android (composeApp/src/androidMain/assets/)
  - FoodClassifier.mlpackage         -> iPhone (iosApp/Forma/Models/)

Base: EfficientNet-B0 pré-treinada no ImageNet, afinada no Food-101 (101 pratos, 101 000 fotos).
Opcional: juntar classes tuas (ex.: francesinha, bacalhau à Brás) com --extra_dir.

Como correr no Google Colab (grátis, com GPU):
  1. Runtime > Change runtime type > T4 GPU
  2. !pip install -q torch torchvision onnx onnxscript coremltools
  3. Faz upload deste ficheiro e corre:
     !python train_food_model.py --epochs 4 --fine_tune_epochs 3
  4. Descarrega a pasta out/ (food_model.onnx, labels.txt, FoodClassifier.mlpackage.zip)

Tempo aproximado numa T4: ~10-12 min por época. Precisão esperada no Food-101: ~80-85% (top-1).
"""
import argparse
import os
import shutil
import time

import torch
import torch.nn as nn
from torch.utils.data import ConcatDataset, DataLoader, Dataset
from torchvision import datasets, models, transforms

MEAN = [0.485, 0.456, 0.406]
STD = [0.229, 0.224, 0.225]
SIZE = 224


class Normalized(nn.Module):
    """Envolve o modelo: recebe píxeis 0-255 (NCHW), normaliza lá dentro e devolve probabilidades.
    Assim o Android e o iPhone só têm de passar a imagem em bruto."""

    def __init__(self, net):
        super().__init__()
        self.net = net
        self.register_buffer("mean", torch.tensor(MEAN).view(1, 3, 1, 1) * 255.0)
        self.register_buffer("std", torch.tensor(STD).view(1, 3, 1, 1) * 255.0)

    def forward(self, x):
        x = (x - self.mean) / self.std
        return torch.softmax(self.net(x), dim=1)


class Offset(Dataset):
    """Desloca as etiquetas de um dataset (para juntar classes extra a seguir às 101)."""

    def __init__(self, ds, offset):
        self.ds, self.offset = ds, offset

    def __len__(self):
        return len(self.ds)

    def __getitem__(self, i):
        x, y = self.ds[i]
        return x, y + self.offset


def build_data(root, extra_dir, batch, workers):
    train_tf = transforms.Compose([
        transforms.RandomResizedCrop(SIZE, scale=(0.6, 1.0)),
        transforms.RandomHorizontalFlip(),
        transforms.ColorJitter(0.2, 0.2, 0.2),
        transforms.ToTensor(),
        transforms.Normalize(MEAN, STD),
    ])
    val_tf = transforms.Compose([
        transforms.Resize(256),
        transforms.CenterCrop(SIZE),
        transforms.ToTensor(),
        transforms.Normalize(MEAN, STD),
    ])
    train = datasets.Food101(root, split="train", transform=train_tf, download=True)
    val = datasets.Food101(root, split="test", transform=val_tf, download=True)
    labels = list(train.classes)

    if extra_dir:
        # extra_dir/<nome_da_classe>/*.jpg  (usa os nomes de ml/README.md para a app reconhecer as kcal)
        extra_train = datasets.ImageFolder(extra_dir, transform=train_tf)
        extra_val = datasets.ImageFolder(extra_dir, transform=val_tf)
        n = len(extra_train)
        idx = torch.randperm(n, generator=torch.Generator().manual_seed(0)).tolist()
        cut = max(1, int(n * 0.15))
        offset = len(labels)
        train = ConcatDataset([train] + [Offset(torch.utils.data.Subset(extra_train, idx[cut:]), offset)] * 3)  # repete para equilibrar
        val = ConcatDataset([val, Offset(torch.utils.data.Subset(extra_val, idx[:cut]), offset)])
        labels += extra_train.classes
        print(f"Classes extra: {extra_train.classes} ({n} imagens)")

    dl_train = DataLoader(train, batch_size=batch, shuffle=True, num_workers=workers, pin_memory=True, drop_last=True)
    dl_val = DataLoader(val, batch_size=batch, shuffle=False, num_workers=workers, pin_memory=True)
    return dl_train, dl_val, labels


def evaluate(model, dl, device):
    model.eval()
    correct = total = 0
    with torch.no_grad(), torch.autocast(device_type=device.type, enabled=device.type == "cuda"):
        for x, y in dl:
            x, y = x.to(device, non_blocking=True), y.to(device, non_blocking=True)
            correct += (model(x).argmax(1) == y).sum().item()
            total += y.numel()
    return correct / max(1, total)


def train_epochs(model, dl_train, dl_val, device, epochs, lr, params):
    if epochs <= 0:
        return
    opt = torch.optim.AdamW(params, lr=lr, weight_decay=1e-4)
    sched = torch.optim.lr_scheduler.OneCycleLR(opt, max_lr=lr, total_steps=epochs * len(dl_train))
    scaler = torch.amp.GradScaler("cuda", enabled=device.type == "cuda")
    loss_fn = nn.CrossEntropyLoss(label_smoothing=0.1)
    for ep in range(epochs):
        model.train()
        t0, running = time.time(), 0.0
        for i, (x, y) in enumerate(dl_train):
            x, y = x.to(device, non_blocking=True), y.to(device, non_blocking=True)
            opt.zero_grad(set_to_none=True)
            with torch.autocast(device_type=device.type, enabled=device.type == "cuda"):
                loss = loss_fn(model(x), y)
            scaler.scale(loss).backward()
            scaler.step(opt)
            scaler.update()
            sched.step()
            running += loss.item()
            if i % 200 == 0:
                print(f"  época {ep + 1} passo {i}/{len(dl_train)} loss {running / (i + 1):.3f}", flush=True)
        acc = evaluate(model, dl_val, device)
        print(f"Época {ep + 1}/{epochs}: precisão validação {acc:.3f} ({time.time() - t0:.0f}s)", flush=True)


def export(net, labels, out):
    os.makedirs(out, exist_ok=True)
    wrapped = Normalized(net.cpu()).eval()
    dummy = torch.rand(1, 3, SIZE, SIZE) * 255

    with open(os.path.join(out, "labels.txt"), "w") as f:
        f.write("\n".join(labels) + "\n")

    # Android (ONNX Runtime)
    onnx_path = os.path.join(out, "food_model.onnx")
    torch.onnx.export(wrapped, dummy, onnx_path, input_names=["image"], output_names=["probs"], opset_version=17)
    # junta os pesos num só ficheiro (a app Android lê só o food_model.onnx)
    import onnx

    m = onnx.load(onnx_path, load_external_data=True)
    for extra in (onnx_path + ".data",):
        if os.path.exists(extra):
            os.remove(extra)
    onnx.save_model(m, onnx_path, save_as_external_data=False)
    print("ONNX:", onnx_path, f"{os.path.getsize(onnx_path) / 1e6:.1f} MB")

    # iPhone (Core ML)
    try:
        import coremltools as ct

        traced = torch.jit.trace(wrapped, dummy)
        ml = ct.convert(
            traced,
            inputs=[ct.ImageType(name="image", shape=(1, 3, SIZE, SIZE), scale=1.0, bias=[0, 0, 0], color_layout=ct.colorlayout.RGB)],
            classifier_config=ct.ClassifierConfig(labels),
            convert_to="mlprogram",
            minimum_deployment_target=ct.target.iOS16,
            compute_precision=ct.precision.FLOAT16,
        )
        ml.short_description = "Forma: reconhecimento de pratos (Food-101 + extra)"
        pkg = os.path.join(out, "FoodClassifier.mlpackage")
        if os.path.exists(pkg):
            shutil.rmtree(pkg)
        ml.save(pkg)
        shutil.make_archive(pkg, "zip", out, "FoodClassifier.mlpackage")
        print("Core ML:", pkg + ".zip")
    except Exception as e:  # o Android continua a funcionar mesmo que isto falhe
        print("Aviso: conversão Core ML falhou:", e)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    ap.add_argument("--extra_dir", default=None, help="pasta com subpastas por classe (pratos teus)")
    ap.add_argument("--epochs", type=int, default=4, help="épocas só com a cabeça nova")
    ap.add_argument("--fine_tune_epochs", type=int, default=3, help="épocas com a rede toda")
    ap.add_argument("--batch", type=int, default=64)
    ap.add_argument("--workers", type=int, default=2)
    ap.add_argument("--out", default="out")
    args = ap.parse_args()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print("Dispositivo:", device)
    dl_train, dl_val, labels = build_data(args.data, args.extra_dir, args.batch, args.workers)
    print(f"{len(labels)} classes")

    net = models.efficientnet_b0(weights=models.EfficientNet_B0_Weights.IMAGENET1K_V1)
    net.classifier[1] = nn.Linear(net.classifier[1].in_features, len(labels))
    net = net.to(device)

    # 1) só a última camada
    for p in net.features.parameters():
        p.requires_grad = False
    train_epochs(net, dl_train, dl_val, device, args.epochs, 3e-3, net.classifier.parameters())

    # 2) afinação da rede toda com taxa baixa
    for p in net.features.parameters():
        p.requires_grad = True
    train_epochs(net, dl_train, dl_val, device, args.fine_tune_epochs, 3e-4, net.parameters())

    os.makedirs(args.out, exist_ok=True)
    torch.save({"state_dict": net.state_dict(), "labels": labels}, os.path.join(args.out, "food_model.pt"))
    export(net, labels, args.out)
    print("Feito. Vê ml/README.md para pôr os ficheiros na app.")


if __name__ == "__main__":
    main()
