import UIKit

/// Native lookups into the Compose drawables, so native screens and the Compose UI share one copy
/// of every image. The Kotlin framework's build copies each module's `composeResources` into the
/// app bundle under `compose-resources/composeResources/<resources package>/`, the same files
/// Compose's `painterResource` reads on iOS.
enum ComposeDrawables {
    private static let cache = NSCache<NSString, UIImage>()

    /// The PNG drawable `name` from the Compose resources package `package`, e.g.
    /// `ComposeDrawables.image("onboarding_app_icon", package: "chefmate.client.onboarding.public.generated.resources")`.
    /// Decoded images are cached, since screens read them from `body`.
    static func image(_ name: String, package: String) -> UIImage? {
        let key = "\(package)/\(name)" as NSString
        if let cached = cache.object(forKey: key) { return cached }
        guard let url = Bundle.main.resourceURL?
            .appendingPathComponent("compose-resources/composeResources/\(package)/drawable/\(name).png"),
              let image = UIImage(contentsOfFile: url.path)
        else { return nil }
        cache.setObject(image, forKey: key)
        return image
    }
}
