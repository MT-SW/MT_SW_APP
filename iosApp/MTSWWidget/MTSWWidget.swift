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
import SwiftUI
import WidgetKit

/// Wspólny folder aplikacji i widżetu (App Group). Z niego widżet czyta statystyki zapisane przez aplikację.
private let appGroup = "group.pl.sw.mt"

/// Co ile sekund widżet zmienia pokazywany węzeł (ulubione i pozostałe pokazują się na zmianę).
private let rotationSeconds: Double = 60
/// Ile wpisów (minut) widżet ma z góry; po tym czasie sam ponownie czyta dane z pliku.
private let timelineEntries = 15

/// Jedna sekcja statystyk (Sieć, Węzeł, Radio, Zasoby, Łączność) z nagłówkiem.
struct StatsGroup {
    var title: String
    var lines: [String]
    /// W której z dwóch kolumn ma stać sekcja (0 lewa, 1 prawa); aplikacja układa: sieć i radio po lewej, reszta po prawej.
    var column: Int = 0
}

/// Odczyty jednego węzła (ulubionego albo innego z sieci).
struct NodeCard {
    var title: String
    var lines: [String]
    /// Krótka nazwa i kolory (ARGB) plakietki węzła, jak na mapie.
    var short: String = ""
    var color: UInt32 = 0
    var textColor: UInt32 = 0
}

/// Kolor z liczby ARGB zapisanej przez aplikację.
private func argb(_ value: UInt32) -> Color {
    Color(.sRGB,
          red: Double((value >> 16) & 0xFF) / 255,
          green: Double((value >> 8) & 0xFF) / 255,
          blue: Double(value & 0xFF) / 255,
          opacity: 1)
}

/// Plakietka węzła: kolor węzła i jego krótka nazwa (tak jak na mapie).
struct NodeChip: View {
    let short: String
    let color: UInt32
    let textColor: UInt32

    var body: some View {
        if !short.isEmpty {
            Text(short)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(argb(textColor))
                .lineLimit(1)
                .padding(.horizontal, 7)
                .padding(.vertical, 2)
                .background(argb(color), in: Capsule())
        }
    }
}

struct StatsData {
    var title: String
    var chip: String
    var short: String = ""
    var color: UInt32 = 0
    var textColor: UInt32 = 0
    var connected: Bool
    var updated: Date
    var updatedText: String?
    var lines: [String]
    var groups: [StatsGroup]
    var favorites: [NodeCard]
    var others: [NodeCard]
    var network: [NodeCard]
    /// Wybór z ustawień aplikacji: co pokazuje mały widżet (0 nasz węzeł, 1 ulubione, 2 sieć) i drugie pole dużego (0 ulubione, 1 sieć).
    var small: Int = 0
    var second: Int = 0
}

struct StatsEntry: TimelineEntry {
    let date: Date
    let data: StatsData?
    /// Numer kolejnego okresu zmiany węzła; liczony z czasu, żeby odświeżenie danych nie wracało do pierwszego węzła.
    let step: Int
    let config: MTSWConfigIntent
}

/// Ustawienia widżetu: wybór (co pokazuje) jest w aplikacji i przychodzi razem ze statystykami.
struct MTSWConfigIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Statystyki MT_SW" }
    static var description: IntentDescription {
        IntentDescription("Wybierz, co pokazuje widżet. Średni: nasz węzeł albo ulubione albo cała sieć na zmianę. Duży: u góry nasz węzeł, niżej do wyboru ulubione lub cała sieć. Największy (4x6): nasz węzeł, ulubione i cała sieć.")
    }

    init() {}
}

private func readCards(_ raw: Any?) -> [NodeCard] {
    (raw as? [[String: Any]] ?? []).map {
        NodeCard(title: $0["title"] as? String ?? "", lines: $0["lines"] as? [String] ?? [],
                 short: $0["short"] as? String ?? "",
                 color: (($0["color"] as? NSNumber)?.uint32Value) ?? 0,
                 textColor: (($0["textColor"] as? NSNumber)?.uint32Value) ?? 0)
    }
}

private func readStats() -> StatsData? {
    guard let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup),
          let raw = try? Data(contentsOf: folder.appendingPathComponent("widget-stats.json")),
          let json = try? JSONSerialization.jsonObject(with: raw) as? [String: Any] else { return nil }
    return StatsData(
        title: json["title"] as? String ?? "MT_SW",
        chip: json["chip"] as? String ?? "",
        short: json["short"] as? String ?? "",
        color: (json["color"] as? NSNumber)?.uint32Value ?? 0,
        textColor: (json["textColor"] as? NSNumber)?.uint32Value ?? 0,
        connected: json["connected"] as? Bool ?? false,
        updated: Date(timeIntervalSince1970: json["updated"] as? Double ?? 0),
        updatedText: json["updatedText"] as? String,
        lines: json["lines"] as? [String] ?? [],
        groups: (json["groups"] as? [[String: Any]] ?? []).map {
            StatsGroup(title: $0["title"] as? String ?? "", lines: $0["lines"] as? [String] ?? [],
                       column: $0["column"] as? Int ?? 0)
        },
        favorites: readCards(json["favorites"]),
        others: readCards(json["others"]),
        network: readCards(json["network"]),
        small: json["small"] as? Int ?? 0,
        second: json["second"] as? Int ?? 0
    )
}

private var isPolish: Bool { Locale.current.language.languageCode?.identifier == "pl" }

private func stepOf(_ date: Date) -> Int { Int(date.timeIntervalSince1970 / rotationSeconds) }

struct StatsProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> StatsEntry {
        let card = NodeCard(title: "MT_SW", lines: ["85%", "Czas pracy: 3d"])
        return StatsEntry(
            date: Date(),
            data: StatsData(title: "MT_SW", chip: "3/9", connected: true, updated: Date(), updatedText: nil,
                            lines: ["3 z 9 węzłów online", "0 nieprzeczytanych"],
                            groups: [StatsGroup(title: isPolish ? "Sieć" : "Network",
                                                lines: ["3 z 9 węzłów online", "0 nieprzeczytanych"])],
                            favorites: [card], others: [card], network: [card]),
            step: 0, config: MTSWConfigIntent())
    }

    func snapshot(for configuration: MTSWConfigIntent, in context: Context) async -> StatsEntry {
        StatsEntry(date: Date(), data: readStats() ?? placeholder(in: context).data, step: stepOf(Date()), config: configuration)
    }

    func timeline(for configuration: MTSWConfigIntent, in context: Context) async -> Timeline<StatsEntry> {
        let data = readStats()
        let now = Date()
        // Wpisy co minutę z góry: widżet sam zmienia węzeł na zmianę, bez budzenia aplikacji.
        let entries = (0..<timelineEntries).map { index -> StatsEntry in
            let date = now.addingTimeInterval(Double(index) * rotationSeconds)
            return StatsEntry(date: date, data: data, step: stepOf(date), config: configuration)
        }
        return Timeline(entries: entries, policy: .after(now.addingTimeInterval(rotationSeconds * Double(timelineEntries))))
    }
}

/// Złoty kolor motywu MT_SW (nagłówki sekcji).
private let gold = Color(red: 0.96, green: 0.78, blue: 0.22)

/// Dzieli sekcje po kolei na [parts] kolumn o podobnej liczbie wierszy (nagłówek liczy się jak wiersz).
private func splitGroups(_ groups: [StatsGroup], into parts: Int) -> [[StatsGroup]] {
    guard parts > 1, groups.count > 1 else { return [groups] }
    let weights = groups.map { Double($0.lines.count + 1) }
    let target = weights.reduce(0, +) / Double(parts)
    var result: [[StatsGroup]] = [[]]
    var used = 0.0
    for (index, group) in groups.enumerated() {
        let leftParts = parts - result.count
        let leftGroups = groups.count - index
        if !result[result.count - 1].isEmpty, leftParts > 0, leftGroups >= leftParts,
           used + weights[index] / 2 > target * Double(result.count) {
            result.append([])
        }
        result[result.count - 1].append(group)
        used += weights[index]
    }
    return result
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

/// Gotowy układ sekcji: kolumny, ich szerokości i wspólny rozmiar pisma.
private struct GroupsLayout {
    let columns: [[StatsGroup]]
    let widths: [CGFloat]
    let size: CGFloat
}

private func groupsLayout(_ columns: [[StatsGroup]], area: CGSize, gap: CGFloat, maxSize: CGFloat) -> GroupsLayout {
    let total = max(area.width - gap * CGFloat(columns.count - 1), 50)
    let chars = columns.map { CGFloat(max(longest($0), 6)) }
    let byWidth = total / (chars.reduce(0, +) * 0.52)
    let tall = CGFloat(max(columns.map { rows($0) }.max() ?? 1, 1))
    let byHeight = area.height / (tall * 1.3 + 0.8)
    let size = min(maxSize, max(7.5, min(byWidth, byHeight)))
    let sum = chars.reduce(0, +)
    return GroupsLayout(columns: columns, widths: chars.map { total * $0 / sum }, size: size)
}

/// Sekcje w jednej, dwóch albo trzech kolumnach. Wybierany jest układ, w którym pismo wychodzi największe; wszystkie
/// wiersze mają ten sam rozmiar, a kolumna z dłuższymi wierszami dostaje więcej miejsca.
struct GroupsView: View {
    let groups: [StatsGroup]
    let maxSize: CGFloat

    var body: some View {
        GeometryReader { geo in
            let gap: CGFloat = 10
            // Sekcje z wyznaczoną kolumną (statystyki naszego węzła) idą dokładnie tak; inne układają się same.
            let fixed = groups.contains { $0.column == 1 }
            let options: [GroupsLayout] = fixed
                ? [groupsLayout([groups.filter { $0.column == 0 }, groups.filter { $0.column == 1 }].filter { !$0.isEmpty },
                                area: geo.size, gap: gap, maxSize: maxSize)]
                : (1...max(1, min(3, groups.count))).map {
                    groupsLayout(splitGroups(groups, into: $0), area: geo.size, gap: gap, maxSize: maxSize)
                }
            let best = options.dropFirst().reduce(options[0]) { $1.size > $0.size + 0.25 ? $1 : $0 }
            HStack(alignment: .top, spacing: gap) {
                ForEach(Array(best.columns.enumerated()), id: \.offset) { index, column in
                    GroupColumn(groups: column, size: best.size).frame(width: best.widths[index], alignment: .topLeading)
                }
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
                NodeChip(short: data.short, color: data.color, textColor: data.textColor)
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
                GroupsView(groups: data.groups, maxSize: 15)
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
            Text(label.uppercased()).font(.system(size: 10, weight: .bold)).foregroundStyle(gold)
            if cards.isEmpty {
                Text(empty).font(.system(size: 12)).foregroundStyle(.secondary)
            } else {
                let card = cards[step % cards.count]
                HStack {
                    Text(card.title).font(.system(size: 15, weight: .semibold)).lineLimit(1)
                    Spacer()
                    NodeChip(short: card.short, color: card.color, textColor: card.textColor)
                    if cards.count > 1 {
                        Text("\(step % cards.count + 1)/\(cards.count)")
                            .font(.system(size: 10)).foregroundStyle(.secondary)
                    }
                }
                GroupsView(groups: [StatsGroup(title: "", lines: card.lines)], maxSize: 17)
            }
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }
}

/// Średni widżet: w pierwszym okresie pełne statystyki naszego węzła (jak zawsze), potem kolejno wybrane węzły
/// (ulubione albo cała sieć), i od nowa.
@ViewBuilder
private func withOwn(cards: [NodeCard], label: String, entry: StatsEntry, data: StatsData) -> some View {
    let position = entry.step % (cards.count + 1)
    if position == 0 {
        OwnStatsView(data: data)
    } else {
        NodeCardView(label: label, cards: cards, step: position - 1,
                     empty: isPolish ? "Brak węzłów." : "No nodes.")
    }
}

/// Czy to rozmiar 4x6 na iPhonie (iOS 27); ten rozmiar istnieje tylko w nowym SDK, więc jest za sprawdzeniem wersji.
private func isExtraLargePortrait(_ family: WidgetFamily) -> Bool {
    #if compiler(>=6.4)
    if #available(iOS 27.0, *) { return family == .systemExtraLargePortrait }
    #endif
    return false
}

/// Rozmiary, które widżet oferuje; 4x6 na iPhonie dochodzi od iOS 27.
private var widgetFamilies: [WidgetFamily] {
    // Trzy małe rozmiary dochodzą na ekran blokady (iPhone): okrągły, prostokątny i w jednej linii.
    var families: [WidgetFamily] = [
        .systemMedium, .systemLarge, .systemExtraLarge,
        .accessoryCircular, .accessoryRectangular, .accessoryInline,
    ]
    #if compiler(>=6.4)
    if #available(iOS 27.0, *) { families.append(.systemExtraLargePortrait) }
    #endif
    return families
}

/// Największy widżet: nasz węzeł, ulubione i reszta sieci (pionowo, gdy jest wyższy niż szerszy; inaczej w dwóch kolumnach).
struct ExtraLargeView: View {
    let data: StatsData
    let favorite: NodeCardView
    let other: NodeCardView

    var body: some View {
        GeometryReader { geo in
            if geo.size.height > geo.size.width {
                VStack(alignment: .leading, spacing: 6) {
                    OwnStatsView(data: data).frame(maxHeight: .infinity)
                    Divider()
                    favorite.frame(maxHeight: .infinity)
                    Divider()
                    other.frame(maxHeight: .infinity)
                }
            } else {
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
            }
        }
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
            // Mniejsze widżety: cała sieć bez naszego węzła, razem z ulubionymi.
            let network = NodeCardView(
                label: isPolish ? "Węzeł sieci" : "Network node", cards: data.network, step: entry.step,
                empty: isPolish ? "Brak innych węzłów." : "No other nodes.")
            switch family {
            case .accessoryInline:
                // Ekran blokady, jedna linia: nazwa naszego węzła i ile węzłów jest online.
                Text("\(data.title) \(data.chip)")
            case .accessoryCircular:
                ZStack {
                    AccessoryWidgetBackground()
                    Text(data.chip).font(.system(size: 15, weight: .bold)).minimumScaleFactor(0.5).lineLimit(1)
                }
            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 1) {
                    HStack {
                        Text(data.title).font(.system(size: 13, weight: .semibold)).lineLimit(1)
                        Spacer(minLength: 2)
                        Text(data.chip).font(.system(size: 13, weight: .bold))
                    }
                    ForEach(Array(data.lines.prefix(2).enumerated()), id: \.offset) { _, line in
                        Text(line).font(.system(size: 11)).lineLimit(1).minimumScaleFactor(0.7)
                    }
                }
            case .systemLarge:
                // Góra: zawsze nasz węzeł. Dół: ulubione albo cała sieć na zmianę (do wyboru w ustawieniach widżetu).
                VStack(alignment: .leading, spacing: 6) {
                    OwnStatsView(data: data).frame(maxHeight: .infinity)
                    Divider()
                    (data.second == 0 ? favorite : network).frame(maxHeight: .infinity)
                }
            case .systemExtraLarge:
                // Szeroki (iPad): nasz węzeł, ulubione i reszta sieci (bez ulubionych i bez nas).
                ExtraLargeView(data: data, favorite: favorite, other: other)
            default:
                if isExtraLargePortrait(family) {
                    // 4x6 na iPhonie (iOS 27): to osobny rozmiar systemExtraLargePortrait.
                    ExtraLargeView(data: data, favorite: favorite, other: other)
                } else {
                switch data.small {
                case 1: withOwn(cards: data.favorites, label: isPolish ? "Ulubiony węzeł" : "Favorite node", entry: entry, data: data)
                case 2: withOwn(cards: data.network, label: isPolish ? "Węzeł sieci" : "Network node", entry: entry, data: data)
                default: OwnStatsView(data: data)
                }
                }
            }
        } else {
            Text(isPolish ? "Otwórz aplikację MT_SW, aby zobaczyć statystyki." : "Open the MT_SW app to see the statistics.")
                .font(.system(size: 12)).foregroundStyle(.secondary)
        }
    }
}

struct MTSWStatsWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "MTSWStatsWidget", intent: MTSWConfigIntent.self, provider: StatsProvider()) { entry in
            StatsView(entry: entry).containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName(isPolish ? "Statystyki MT_SW" : "MT_SW statistics")
        .description(isPolish
            ? "Średni: nasz węzeł (albo ulubione, albo cała sieć na zmianę). Duży: nasz węzeł u góry, niżej ulubione lub cała sieć na zmianę. Największy (4x6): nasz węzeł, ulubione i cała sieć. Na ekranie blokady (iPhone) są też trzy małe widżety: okrągły, prostokątny i w jednej linii — nazwa naszego węzła i ile węzłów jest online. Co pokazuje, wybierasz w aplikacji (Ustawienia → Widżet statystyk)."
            : "Medium: our node (or favorites, or the whole network in turn). Large: our node on top, below favorites or the whole network in turn. Extra large (4x6): our node, favorites and the whole network. The lock screen (iPhone) also has three small widgets: round, rectangular and one line — our node name and how many nodes are online. What it shows is chosen in the app (Settings → Statistics widget).")
        .supportedFamilies(widgetFamilies)
    }
}

@main
struct MTSWWidgetBundle: WidgetBundle {
    var body: some Widget { MTSWStatsWidget() }
}
