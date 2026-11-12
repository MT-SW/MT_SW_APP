/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import SwiftUI
import MTSWShared
import WidgetKit

@main
struct MTSWApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Kotlin zapisuje nowe statystyki i daje znać tym powiadomieniem: widżet odświeża się od razu.
        NotificationCenter.default.addObserver(forName: Notification.Name("MTSWWidgetRefresh"), object: nil, queue: .main) { _ in
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
                .onOpenURL { url in
                    MainViewControllerKt.handleIosDeepLink(url: url.absoluteString)
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active {
                        saveWidgetSizes()
                        openPendingLink()
                    }
                }
        }
    }
}

/// Skrót Siri/Skróty zostawia w pliku adres, który ma się otworzyć po uruchomieniu aplikacji (np. ekran SOS).
private func openPendingLink() {
    guard let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.pl.sw.mt")
    else { return }
    let file = folder.appendingPathComponent("pending-link.txt")
    guard let link = try? String(contentsOf: file, encoding: .utf8), !link.isEmpty else { return }
    try? FileManager.default.removeItem(at: file)
    MainViewControllerKt.handleIosDeepLink(url: link)
}

/// Zapisuje, w jakich rozmiarach widżet stoi na ekranie głównym; ustawienia aplikacji pokazują opcje tych rozmiarów.
private func saveWidgetSizes() {
    WidgetCenter.shared.getCurrentConfigurations { result in
        guard case .success(let infos) = result,
              let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.pl.sw.mt")
        else { return }
        let names = infos.filter { $0.kind == "MTSWStatsWidget" }.map { info -> String in
            switch info.family {
            case .systemMedium: return "medium"
            case .systemLarge: return "large"
            case .systemExtraLarge: return "extra"
            default: return String(describing: info.family) == "systemExtraLargePortrait" ? "extra" : "other"
            }
        }
        try? (["ok"] + names).joined(separator: ",")
            .write(to: folder.appendingPathComponent("widget-sizes.txt"), atomically: true, encoding: .utf8)
    }
}
