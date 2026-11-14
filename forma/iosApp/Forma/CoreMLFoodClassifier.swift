import Foundation
import CoreML
import Vision
import UIKit
import ComposeApp

/// Corre o modelo FoodClassifier.mlpackage (gerado em ml/) no iPhone, sem internet.
/// Se o modelo não estiver na pasta Forma/Models, isModelAvailable() devolve false
/// e a app usa o Gemini.
final class CoreMLFoodClassifier: NSObject, IosFoodClassifier {
    private lazy var model: VNCoreMLModel? = {
        guard let url = Bundle.main.url(forResource: "FoodClassifier", withExtension: "mlmodelc") else { return nil }
        let config = MLModelConfiguration()
        config.computeUnits = .all
        guard let ml = try? MLModel(contentsOf: url, configuration: config) else { return nil }
        return try? VNCoreMLModel(for: ml)
    }()

    func isModelAvailable() -> Bool { model != nil }

    func classify(jpeg: Data, completion: @escaping ([ClassifierResult]) -> Void) {
        guard let model = model, let image = UIImage(data: jpeg), let cg = image.cgImage else {
            completion([])
            return
        }
        let request = VNCoreMLRequest(model: model) { req, _ in
            let observations = (req.results as? [VNClassificationObservation]) ?? []
            completion(observations.prefix(5).map {
                ClassifierResult(label: $0.identifier, confidence: Double($0.confidence))
            })
        }
        request.imageCropAndScaleOption = .centerCrop
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                try VNImageRequestHandler(cgImage: cg, orientation: .up).perform([request])
            } catch {
                completion([])
            }
        }
    }
}
