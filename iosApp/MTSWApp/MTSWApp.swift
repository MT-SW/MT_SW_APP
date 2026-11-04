import SwiftUI
import MTSWShared

@main
struct MTSWApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
                .onOpenURL { url in
                    MainViewControllerKt.handleIosDeepLink(url: url.absoluteString)
                }
        }
    }
}
