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
import WidgetKit

/// Wspólny folder aplikacji i widżetu (App Group). Z niego widżet czyta statystyki zapisane przez aplikację.
private let appGroup = "group.pl.sw.mt"

struct StatsData {
    var title: String
    var chip: String
    var connected: Bool
    var updated: Date
    var updatedText: String?
    var lines: [String]
}

struct StatsEntry: TimelineEntry {
    let date: Date
    let data: StatsData?
}

private func readStats() -> StatsData? {
    guard let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup),
          let raw = try? Data(contentsOf: folder.appendingPathComponent("widget-stats.json")),
          let json = try? JSONSerialization.jsonObject(with: raw) as? [String: Any] else { return nil }
    return StatsData(
        title: json["title"] as? String ?? "MT_SW",
        chip: json["chip"] as? String ?? "",
        connected: json["connected"] as? Bool ?? false,
        updated: Date(timeIntervalSince1970: json["updated"] as? Double ?? 0),
        updatedText: json["updatedText"] as? String,
        lines: json["lines"] as? [String] ?? []
    )
}

private var isPolish: Bool { Locale.current.language.languageCode?.identifier == "pl" }

struct StatsProvider: TimelineProvider {
    func placeholder(in context: Context) -> StatsEntry {
        StatsEntry(date: Date(), data: StatsData(title: "MT_SW", chip: "3/9", connected: true, updated: Date(), updatedText: nil,
                                                 lines: ["3 z 9 węzłów online", "0 nieprzeczytanych"]))
    }

    func getSnapshot(in context: Context, completion: @escaping (StatsEntry) -> Void) {
        completion(StatsEntry(date: Date(), data: readStats() ?? placeholder(in: context).data))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<StatsEntry>) -> Void) {
        let entry = StatsEntry(date: Date(), data: readStats())
        completion(Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(15 * 60))))
    }
}

struct StatsView: View {
    let entry: StatsEntry

    var body: some View {
        if let data = entry.data {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(data.title).font(.system(size: 13, weight: .semibold)).lineLimit(1)
                    Spacer()
                    if !data.chip.isEmpty {
                        Text(data.chip).font(.system(size: 12, weight: .medium)).foregroundStyle(.secondary)
                    }
                }
                let columns = [GridItem(.flexible(), alignment: .topLeading), GridItem(.flexible(), alignment: .topLeading)]
                LazyVGrid(columns: columns, alignment: .leading, spacing: 2) {
                    ForEach(Array(data.lines.prefix(14).enumerated()), id: \.offset) { _, line in
                        Text(line).font(.system(size: 10)).lineLimit(2).minimumScaleFactor(0.8)
                    }
                }
                Spacer(minLength: 0)
                // The app writes this text in its own language; the phone's language is only the fallback.
                Text(data.updatedText ?? ((isPolish ? "Zaktualizowano " : "Updated ") + data.updated.formatted(date: .omitted, time: .shortened)))
                    .font(.system(size: 9)).foregroundStyle(.secondary)
            }
        } else {
            Text(isPolish ? "Otwórz aplikację MT_SW, aby zobaczyć statystyki." : "Open the MT_SW app to see the statistics.")
                .font(.system(size: 12)).foregroundStyle(.secondary)
        }
    }
}

struct MTSWStatsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MTSWStatsWidget", provider: StatsProvider()) { entry in
            if #available(iOS 17.0, *) {
                StatsView(entry: entry).containerBackground(.fill.tertiary, for: .widget)
            } else {
                StatsView(entry: entry).padding()
            }
        }
        .configurationDisplayName(isPolish ? "Statystyki MT_SW" : "MT_SW statistics")
        .description(isPolish ? "Statystyki radia i sieci mesh, jak w powiadomieniu na Androidzie." : "Radio and mesh statistics, like the notification on Android.")
        .supportedFamilies([.systemMedium])
    }
}

@main
struct MTSWWidgetBundle: WidgetBundle {
    var body: some Widget { MTSWStatsWidget() }
}
