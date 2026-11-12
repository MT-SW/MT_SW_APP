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

import AppIntents
import Foundation

private let shortcutAppGroup = "group.pl.sw.mt"

/// Skrót Siri i aplikacji Skróty: czyta na głos stan sieci (te same statystyki, które pokazuje widżet).
struct NetworkStatusIntent: AppIntent {
    static var title: LocalizedStringResource = "Stan sieci MT_SW"
    static var description = IntentDescription("Podaje stan sieci mesh: ile węzłów jest online i podstawowe statystyki.")
    static var openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let polish = Locale.current.language.languageCode?.identifier == "pl"
        guard let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: shortcutAppGroup),
              let raw = try? Data(contentsOf: folder.appendingPathComponent("widget-stats.json")),
              let json = try? JSONSerialization.jsonObject(with: raw) as? [String: Any] else {
            return .result(dialog: IntentDialog(stringLiteral: polish
                ? "Otwórz aplikację MT_SW, aby zobaczyć stan sieci."
                : "Open the MT_SW app to see the network status."))
        }
        let title = json["title"] as? String ?? "MT_SW"
        let lines = (json["lines"] as? [String] ?? []).prefix(3)
        let text = ([title] + lines).joined(separator: ". ")
        return .result(dialog: IntentDialog(stringLiteral: text))
    }
}

struct MTSWShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: NetworkStatusIntent(),
            phrases: [
                "Stan sieci w \(.applicationName)",
                "Network status in \(.applicationName)",
            ],
            shortTitle: "Stan sieci",
            systemImageName: "antenna.radiowaves.left.and.right"
        )
    }
}
