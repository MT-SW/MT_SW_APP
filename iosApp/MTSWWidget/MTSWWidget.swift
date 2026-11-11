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

/// Co ile sekund widżet zmienia pokazywany węzeł (ulubione i pozostałe pokazują się na zmianę).
private let rotationSeconds: Double = 300

/// Jedna sekcja statystyk (Sieć, Węzeł, Radio, Zasoby, Łączność) z nagłówkiem.
struct StatsGroup {
    var title: String
    var lines: [String]
}

/// Odczyty jednego węzła (ulubionego albo innego z sieci).
struct NodeCard {
    var title: String
    var lines: [String]
}

struct StatsData {
    var title: String
    var chip: String
    var connected: Bool
    var updated: Date
    var updatedText: String?
    var lines: [String]
    var groups: [StatsGroup]
    var favorites: [NodeCard]
    var others: [NodeCard]
}

struct StatsEntry: TimelineEntry {
    let date: Date
    let data: StatsData?
    /// Numer kolejnego okresu zmiany węzła; liczony z czasu, żeby odświeżenie danych nie wracało do pierwszego węzła.
    let step: Int
}

private func readCards(_ raw: Any?) -> [NodeCard] {
    (raw as? [[String: Any]] ?? []).map {
        NodeCard(title: $0["title"] as? String ?? "", lines: $0["lines"] as? [String] ?? [])
    }
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
        lines: json["lines"] as? [String] ?? [],
        groups: (json["groups"] as? [[String: Any]] ?? []).map {
            StatsGroup(title: $0["title"] as? String ?? "", lines: $0["lines"] as? [String] ?? [])
        },
        favorites: readCards(json["favorites"]),
        others: readCards(json["others"])
    )
}

private var isPolish: Bool { Locale.current.language.languageCode?.identifier == "pl" }

private func stepOf(_ date: Date) -> Int { Int(date.timeIntervalSince1970 / rotationSeconds) }

struct StatsProvider: TimelineProvider {
    func placeholder(in context: Context) -> StatsEntry {
        let card = NodeCard(title: "MT_SW", lines: ["85%", "Czas pracy: 3d"])
        return StatsEntry(
            date: Date(),
            data: StatsData(title: "MT_SW", chip: "3/9", connected: true, updated: Date(), updatedText: nil,
                            lines: ["3 z 9 węzłów online", "0 nieprzeczytanych"],
                            groups: [StatsGroup(title: isPolish ? "Sieć" : "Network",
                                                lines: ["3 z 9 węzłów online", "0 nieprzeczytanych"])],
                            favorites: [card], others: [card]),
            step: 0)
    }

    func getSnapshot(in context: Context, completion: @escaping (StatsEntry) -> Void) {
        completion(StatsEntry(date: Date(), data: readStats() ?? placeholder(in: context).data, step: stepOf(Date())))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<StatsEntry>) -> Void) {
        let data = readStats()
        let now = Date()
        // Wpisy co kilka minut z góry: widżet sam zmienia węzeł na zmianę, bez budzenia aplikacji.
        let entries = (0..<12).map { index -> StatsEntry in
            let date = now.addingTimeInterval(Double(index) * rotationSeconds)
            return StatsEntry(date: date, data: data, step: stepOf(date))
        }
        completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(rotationSeconds * 12))))
    }
}

/// Złoty kolor motywu MT_SW (nagłówki sekcji).
private let gold = Color(red: 0.96, green: 0.78, blue: 0.22)

/// Gdzie przeciąć listę sekcji, żeby kolumny były jak najbardziej wyrównane (nagłówek liczy się jak wiersz).
private func splitIndex(_ groups: [StatsGroup]) -> Int {
    let weights = groups.map { $0.lines.count + 1 }
    let total = weights.reduce(0, +)
    var best = 0
    var bestDiff = Int.max
    var left = 0
    for index in 0...groups.count {
        let diff = abs(total - 2 * left)
        if diff < bestDiff { bestDiff = diff; best = index }
        if index < groups.count { left += weights[index] }
    }
    return best
}

private func longest(_ groups: [StatsGroup]) -> Int {
    groups.flatMap { [$0.title] + $0.lines }.map { $0.count }.max() ?? 1
}

private func rows(_ groups: [StatsGroup]) -> Int { groups.reduce(0) { $0 + $1.lines.count + 1 } }

struct GroupColumn: View {
    let groups: [StatsGroup]
    let size: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: size * 0.4) {
            ForEach(Array(groups.enumerated()), id: \.offset) { _, group in
                VStack(alignment: .leading, spacing: 1) {
                    if !group.title.isEmpty {
                        Text(group.title.uppercased())
                            .font(.system(size: size * 0.85, weight: .bold))
                            .foregroundStyle(gold)
                    }
                    ForEach(Array(group.lines.enumerated()), id: \.offset) { _, line in
                        Text(line).font(.system(size: size)).lineLimit(1).minimumScaleFactor(0.7)
                    }
                }
            }
        }
    }
}

/// Sekcje w dwóch kolumnach. Wszystkie wiersze mają ten sam rozmiar pisma, dobrany do miejsca (szerokości i wysokości);
/// kolumna z dłuższymi wierszami dostaje więcej miejsca, więc nic nie jest zmniejszane tylko po jednej stronie.
struct GroupsView: View {
    let groups: [StatsGroup]
    let maxSize: CGFloat

    var body: some View {
        let split = splitIndex(groups)
        let left = Array(groups[..<split])
        let right = Array(groups[split...])
        GeometryReader { geo in
            let gap: CGFloat = 12
            let total = max(geo.size.width - gap, 50)
            let leftChars = CGFloat(max(longest(left), 6))
            let rightChars = CGFloat(max(longest(right), 6))
            let byWidth = total / ((leftChars + rightChars) * 0.52)
            let tall = CGFloat(max(rows(left), rows(right), 1))
            let byHeight = geo.size.height / (tall * 1.3 + 0.8)
            let size = min(maxSize, max(7.5, min(byWidth, byHeight)))
            let leftWidth = total * leftChars / (leftChars + rightChars)
            HStack(alignment: .top, spacing: gap) {
                GroupColumn(groups: left, size: size).frame(width: leftWidth, alignment: .topLeading)
                GroupColumn(groups: right, size: size).frame(width: total - leftWidth, alignment: .topLeading)
            }
        }
    }
}

/// Statystyki naszego węzła (to, co widać na najmniejszym, średnim widżecie).
struct OwnStatsView: View {
    let data: StatsData

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(data.title).font(.system(size: 14, weight: .semibold)).lineLimit(1)
                Spacer()
                if !data.chip.isEmpty {
                    Text(data.chip).font(.system(size: 13, weight: .medium)).foregroundStyle(.secondary)
                }
            }
            if data.groups.isEmpty {
                let columns = [GridItem(.flexible(), alignment: .topLeading), GridItem(.flexible(), alignment: .topLeading)]
                LazyVGrid(columns: columns, alignment: .leading, spacing: 2) {
                    ForEach(Array(data.lines.prefix(14).enumerated()), id: \.offset) { _, line in
                        Text(line).font(.system(size: 11)).lineLimit(2).minimumScaleFactor(0.8)
                    }
                }
                Spacer(minLength: 0)
            } else {
                GroupsView(groups: data.groups, maxSize: 12)
            }
            // The app writes this text in its own language; the phone's language is only the fallback.
            Text(data.updatedText ?? ((isPolish ? "Zaktualizowano " : "Updated ") + data.updated.formatted(date: .omitted, time: .shortened)))
                .font(.system(size: 9)).foregroundStyle(.secondary)
        }
    }
}

/// Karta jednego węzła z listy (ulubione albo pozostałe), pokazywana na zmianę.
struct NodeCardView: View {
    let label: String
    let cards: [NodeCard]
    let step: Int
    let empty: String

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label.uppercased()).font(.system(size: 9, weight: .bold)).foregroundStyle(gold)
            if cards.isEmpty {
                Text(empty).font(.system(size: 12)).foregroundStyle(.secondary)
            } else {
                let card = cards[step % cards.count]
                HStack {
                    Text(card.title).font(.system(size: 13, weight: .semibold)).lineLimit(1)
                    Spacer()
                    if cards.count > 1 {
                        Text("\(step % cards.count + 1)/\(cards.count)")
                            .font(.system(size: 10)).foregroundStyle(.secondary)
                    }
                }
                let columns = [GridItem(.flexible(), alignment: .topLeading), GridItem(.flexible(), alignment: .topLeading)]
                LazyVGrid(columns: columns, alignment: .leading, spacing: 2) {
                    ForEach(Array(card.lines.enumerated()), id: \.offset) { _, line in
                        Text(line).font(.system(size: 11)).lineLimit(1).minimumScaleFactor(0.7)
                    }
                }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }
}

struct StatsView: View {
    let entry: StatsEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let data = entry.data {
            let favorite = NodeCardView(
                label: isPolish ? "Ulubiony węzeł" : "Favorite node", cards: data.favorites, step: entry.step,
                empty: isPolish ? "Brak ulubionych węzłów." : "No favorite nodes.")
            let other = NodeCardView(
                label: isPolish ? "Inny węzeł sieci" : "Other node", cards: data.others, step: entry.step,
                empty: isPolish ? "Brak innych węzłów." : "No other nodes.")
            switch family {
            case .systemLarge:
                // Pierwsza połowa: nasz węzeł. Druga: ulubione na zmianę.
                VStack(alignment: .leading, spacing: 6) {
                    OwnStatsView(data: data).frame(maxHeight: .infinity)
                    Divider()
                    favorite.frame(maxHeight: .infinity)
                }
            case .systemExtraLarge:
                // Największy (iPad): nasz węzeł, ulubione i pozostałe węzły sieci na zmianę.
                HStack(alignment: .top, spacing: 14) {
                    OwnStatsView(data: data).frame(maxWidth: .infinity)
                    Divider()
                    VStack(alignment: .leading, spacing: 6) {
                        favorite.frame(maxHeight: .infinity)
                        Divider()
                        other.frame(maxHeight: .infinity)
                    }
                    .frame(maxWidth: .infinity)
                }
            default:
                OwnStatsView(data: data)
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
        .description(isPolish
            ? "Statystyki radia i sieci mesh. Większe rozmiary pokazują też na zmianę ulubione i pozostałe węzły."
            : "Radio and mesh statistics. Bigger sizes also show favorite and other nodes in turn.")
        .supportedFamilies([.systemMedium, .systemLarge, .systemExtraLarge])
    }
}

@main
struct MTSWWidgetBundle: WidgetBundle {
    var body: some Widget { MTSWStatsWidget() }
}
