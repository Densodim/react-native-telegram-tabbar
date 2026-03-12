/**
 * TelegramTabBarView.swift
 *
 * Native floating tab bar — Telegram-style, iOS implementation.
 *
 * Architecture: 2-layer floating pill
 *   Layer 1 — blurView:    UIVisualEffectView (.systemUltraThinMaterialDark) clipped to pill shape.
 *   Layer 2 — contentView: Clear UIView on top of blur; holds all TabButtonViews.
 *
 * Icons are rendered via Core Graphics from serialised SVG primitives (SvgElement).
 * All numeric and dot-badge data is accepted but deferred to a future milestone
 * (stubs kept for ABI stability).
 *
 * React Native bridge: props arrive via TelegramTabBarViewManager / TelegramTabBarViewManager.m.
 * Events leave via onTabPress / onTabLongPress RCTBubblingEventBlock callbacks.
 */

import UIKit

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Data models (mirror TypeScript spec; numeric fields come in as String)
// ─────────────────────────────────────────────────────────────────────────────

/// One SVG primitive element passed from JavaScript.
/// All optional geometry fields arrive as String from the JS bridge;
/// the SvgElement(from:) initialiser converts them to CGFloat.
struct SvgElement {
    let type: String            // "path" | "circle" | "line" | "polyline" | "polygon" | "rect"

    // path
    var d: String?

    // circle
    var cx: CGFloat?
    var cy: CGFloat?
    var r: CGFloat?

    // line
    var x1: CGFloat?
    var y1: CGFloat?
    var x2: CGFloat?
    var y2: CGFloat?

    // polyline / polygon
    var points: String?

    // rect
    var x: CGFloat?
    var y: CGFloat?
    var elementWidth: CGFloat?   // mirrors TS `width` (avoids clash with UIView.bounds.width)
    var elementHeight: CGFloat?  // mirrors TS `height`
    var rx: CGFloat?
    var ry: CGFloat?
}

extension SvgElement {
    /// Initialise from the dictionary that arrives over the RN bridge.
    /// All numeric geometry fields are serialised as String by the JS layer.
    init(from dict: [String: Any]) {
        type = dict["type"] as? String ?? "path"
        d    = dict["d"]    as? String

        // Helper: try String → CGFloat, then direct NSNumber → CGFloat
        func cgf(_ key: String) -> CGFloat? {
            if let s = dict[key] as? String, let v = Double(s) { return CGFloat(v) }
            if let n = dict[key] as? NSNumber                   { return CGFloat(n.doubleValue) }
            return nil
        }

        cx            = cgf("cx")
        cy            = cgf("cy")
        r             = cgf("r")
        x1            = cgf("x1")
        y1            = cgf("y1")
        x2            = cgf("x2")
        y2            = cgf("y2")
        points        = dict["points"] as? String
        x             = cgf("x")
        y             = cgf("y")
        elementWidth  = cgf("width")
        elementHeight = cgf("height")
        rx            = cgf("rx")
        ry            = cgf("ry")
    }
}

/// One tab item passed from JavaScript.
struct TabData {
    let key: String
    let title: String
    let svgPaths: [SvgElement]
}

/// Colour theme passed from JavaScript.
struct TabBarTheme {
    var backgroundColor: String = "#1C1C1E"
    var activeColor: String     = "#FFFFFF"
    var inactiveColor: String   = "#636366"
    var indicatorColor: String  = "#0A84FF"
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TelegramTabBarView
// ─────────────────────────────────────────────────────────────────────────────

/// The root floating-pill tab bar view.
/// Designed to fill the full screen (set style position: 'absolute', fill parent).
/// Internally it positions the pill at the bottom with floating margins.
@objc class TelegramTabBarView: UIView {

    // MARK: Subviews
    private let blurView: UIVisualEffectView     // Layer 1 — frosted glass background
    private let contentView: UIView              // Layer 2 — icons + labels, always crisp
    private var tabViews: [TabButtonView] = []

    // MARK: State
    private var tabs: [TabData]      = []
    private var activeIndex: Int     = 0
    private var theme: TabBarTheme   = TabBarTheme()
    private var bottomInsetPt: CGFloat = 0
    private(set) var isTabBarVisible: Bool = true

    // MARK: RN event callbacks
    @objc var onTabPress: RCTBubblingEventBlock?
    @objc var onTabLongPress: RCTBubblingEventBlock?

    // MARK: Layout constants (points)
    private let floatingMarginH: CGFloat      = 16
    private let floatingMarginBottom: CGFloat = 12
    private let barHeight: CGFloat            = 60
    private let cornerRadius: CGFloat         = 28

    // MARK: Init

    override init(frame: CGRect) {
        let blurEffect = UIBlurEffect(style: .systemUltraThinMaterialDark)
        blurView    = UIVisualEffectView(effect: blurEffect)
        contentView = UIView()
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) not supported") }

    // MARK: Setup

    private func setupView() {
        backgroundColor        = .clear
        isUserInteractionEnabled = true

        // ── Layer 1: blur pill ──────────────────────────────────────────────
        blurView.layer.cornerRadius = cornerRadius
        blurView.clipsToBounds      = true
        addSubview(blurView)

        // ── Layer 2: content overlay ────────────────────────────────────────
        contentView.backgroundColor        = .clear
        contentView.layer.cornerRadius     = cornerRadius
        contentView.clipsToBounds          = true
        addSubview(contentView)

        // ── Drop shadow on self (not on blurView — blur clips its shadow) ───
        layer.shadowColor   = UIColor.black.cgColor
        layer.shadowOffset  = CGSize(width: 0, height: 4)
        layer.shadowRadius  = 16
        layer.shadowOpacity = 0.35
    }

    // MARK: Layout

    override func layoutSubviews() {
        super.layoutSubviews()

        let pillFrame = CGRect(
            x:      floatingMarginH,
            y:      bounds.height - barHeight - bottomInsetPt - floatingMarginBottom,
            width:  bounds.width - floatingMarginH * 2,
            height: barHeight
        )

        blurView.frame    = pillFrame
        contentView.frame = pillFrame

        // Update shadow path for performance (avoids off-screen shadow rasterisation)
        layer.shadowPath = UIBezierPath(
            roundedRect:  pillFrame,
            cornerRadius: cornerRadius
        ).cgPath

        layoutTabViews()
    }

    private func layoutTabViews() {
        guard !tabViews.isEmpty else { return }
        let count = CGFloat(tabViews.count)
        let w = contentView.bounds.width  / count
        let h = contentView.bounds.height
        for (i, tv) in tabViews.enumerated() {
            tv.frame = CGRect(x: CGFloat(i) * w, y: 0, width: w, height: h)
        }
    }

    // MARK: Public API (called from TelegramTabBarViewManager)

    @objc func setTabs(_ newTabs: [TabData]) {
        tabs = newTabs
        rebuildTabViews()
    }

    @objc func setActiveIndex(_ index: Int) {
        guard index != activeIndex, index >= 0, index < tabViews.count else { return }
        let old    = activeIndex
        activeIndex = index
        animateColorTransition(old: old, new: index)
    }

    @objc func setThemeColors(_ newTheme: TabBarTheme) {
        theme = newTheme
        applyThemeToAll()
    }

    @objc func setBottomInset(_ inset: CGFloat) {
        guard inset != bottomInsetPt else { return }
        bottomInsetPt = inset
        setNeedsLayout()
    }

    @objc func setTabVisible(_ visible: Bool) {
        guard visible != isTabBarVisible else { return }
        isTabBarVisible = visible

        // Translate pill fully below the screen edge when hiding
        let hiddenTranslationY = bounds.height - blurView.frame.minY + 16
        UIView.animate(
            withDuration: 0.25,
            delay: 0,
            options: [.curveEaseInOut, .allowUserInteraction]
        ) {
            let t: CGAffineTransform = visible
                ? .identity
                : CGAffineTransform(translationX: 0, y: hiddenTranslationY)
            self.blurView.transform    = t
            self.contentView.transform = t
        }
    }

    // MARK: Badges (stub — future milestone)

    @objc func setBadges(_ badges: [[String: Any]]) {
        // Reserved for numeric badge / dot-badge rendering.
        // No-op in this revision; included for ABI stability with ViewManager.
    }

    // MARK: Private helpers

    private func rebuildTabViews() {
        tabViews.forEach { $0.removeFromSuperview() }
        tabViews = []

        for (i, tab) in tabs.enumerated() {
            let tv = TabButtonView()
            tv.configure(tab: tab, isActive: i == activeIndex, theme: theme)

            let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
            let longPress = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
            tv.addGestureRecognizer(tap)
            tv.addGestureRecognizer(longPress)
            tv.tag = i

            contentView.addSubview(tv)
            tabViews.append(tv)
        }

        setNeedsLayout()
        layoutIfNeeded()
    }

    @objc private func handleTap(_ gesture: UITapGestureRecognizer) {
        guard let tv = gesture.view else { return }
        let idx = tv.tag
        guard idx >= 0, idx < tabs.count else { return }
        animateBounce(tv)
        onTabPress?(["key": tabs[idx].key])
    }

    @objc private func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .began, let tv = gesture.view else { return }
        let idx = tv.tag
        guard idx >= 0, idx < tabs.count else { return }
        onTabLongPress?(["key": tabs[idx].key])
    }

    /// Scale bounce: grow → slightly shrink → back to identity (Telegram-style).
    private func animateBounce(_ view: UIView) {
        UIView.animateKeyframes(
            withDuration: 0.32,
            delay: 0,
            options: [.calculationModeLinear]
        ) {
            UIView.addKeyframe(withRelativeStartTime: 0.00, relativeDuration: 0.33) {
                view.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
            }
            UIView.addKeyframe(withRelativeStartTime: 0.33, relativeDuration: 0.27) {
                view.transform = CGAffineTransform(scaleX: 0.93, y: 0.93)
            }
            UIView.addKeyframe(withRelativeStartTime: 0.60, relativeDuration: 0.40) {
                view.transform = .identity
            }
        }
    }

    private func animateColorTransition(old: Int, new: Int) {
        guard old < tabViews.count, new < tabViews.count else { return }
        UIView.animate(withDuration: 0.2) {
            self.tabViews[old].setActive(false, theme: self.theme)
            self.tabViews[new].setActive(true,  theme: self.theme)
        }
    }

    private func applyThemeToAll() {
        for (i, tv) in tabViews.enumerated() {
            tv.setActive(i == activeIndex, theme: theme)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TabButtonView
// ─────────────────────────────────────────────────────────────────────────────

/// A single tab cell: SVG icon above a text label.
private final class TabButtonView: UIView {

    private let iconView = SvgIconView()
    private let label    = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear

        // Icon view
        addSubview(iconView)

        // Label
        label.textAlignment          = .center
        label.font                   = .systemFont(ofSize: 10, weight: .medium)
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor     = 0.8
        label.numberOfLines          = 1
        addSubview(label)
    }

    required init?(coder: NSCoder) { fatalError() }

    override func layoutSubviews() {
        super.layoutSubviews()

        let iconSize: CGFloat = 24
        let labelH:   CGFloat = 14
        let gap:      CGFloat = 2
        let totalH            = iconSize + gap + labelH
        let startY            = (bounds.height - totalH) / 2

        iconView.frame = CGRect(
            x:      (bounds.width - iconSize) / 2,
            y:      startY,
            width:  iconSize,
            height: iconSize
        )
        label.frame = CGRect(
            x:      4,
            y:      startY + iconSize + gap,
            width:  bounds.width - 8,
            height: labelH
        )
    }

    func configure(tab: TabData, isActive: Bool, theme: TabBarTheme) {
        label.text = tab.title
        iconView.setSvgPaths(tab.svgPaths)
        setActive(isActive, theme: theme)
    }

    func setActive(_ active: Bool, theme: TabBarTheme) {
        let color = active
            ? UIColor(hexString: theme.activeColor)   ?? .white
            : UIColor(hexString: theme.inactiveColor) ?? UIColor(white: 0.4, alpha: 1)
        iconView.setColor(color)
        label.textColor = color

        // Bold label weight when active — mirrors Android implementation
        label.font = active
            ? .systemFont(ofSize: 10, weight: .semibold)
            : .systemFont(ofSize: 10, weight: .medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - SvgIconView
// ─────────────────────────────────────────────────────────────────────────────

/// Renders an array of SvgElement descriptors via Core Graphics.
/// Icons are assumed to be in a 24 × 24 viewBox (Lucide convention).
/// All elements are stroked (not filled) with `strokeColor`, matching
/// the Lucide stroke-based icon style.
private final class SvgIconView: UIView {

    private var paths: [SvgElement] = []
    private var iconColor: UIColor  = .white

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        isOpaque        = false
        contentMode     = .redraw   // re-draw when frame changes
    }

    required init?(coder: NSCoder) { fatalError() }

    func setSvgPaths(_ elements: [SvgElement]) {
        paths = elements
        setNeedsDisplay()
    }

    func setColor(_ color: UIColor) {
        iconColor = color
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        ctx.saveGState()

        // Scale from 24-pt viewBox → actual bounds
        let scale = min(rect.width, rect.height) / 24.0
        ctx.translateBy(x: rect.midX - 12 * scale, y: rect.midY - 12 * scale)
        ctx.scaleBy(x: scale, y: scale)

        // Common stroke style
        ctx.setStrokeColor(iconColor.cgColor)
        ctx.setFillColor(UIColor.clear.cgColor)
        ctx.setLineWidth(2.0)
        ctx.setLineCap(.round)
        ctx.setLineJoin(.round)

        for element in paths {
            switch element.type {

            case "path":
                guard let d = element.d,
                      let cgPath = SVGPathParser.parsePath(d) else { break }
                ctx.addPath(cgPath)
                ctx.strokePath()

            case "circle":
                guard let cx = element.cx, let cy = element.cy, let r = element.r else { break }
                ctx.strokeEllipse(in: CGRect(x: cx - r, y: cy - r, width: r * 2, height: r * 2))

            case "line":
                guard let x1 = element.x1, let y1 = element.y1,
                      let x2 = element.x2, let y2 = element.y2 else { break }
                ctx.move(to: CGPoint(x: x1, y: y1))
                ctx.addLine(to: CGPoint(x: x2, y: y2))
                ctx.strokePath()

            case "polyline", "polygon":
                guard let points = element.points,
                      let cgPath = SVGPathParser.parsePolyline(points, close: element.type == "polygon") else { break }
                ctx.addPath(cgPath)
                ctx.strokePath()

            case "rect":
                guard let x = element.x, let y = element.y,
                      let w = element.elementWidth, let h = element.elementHeight else { break }
                let rx = element.rx ?? element.ry ?? 0
                let path = UIBezierPath(
                    roundedRect: CGRect(x: x, y: y, width: w, height: h),
                    cornerRadius: rx
                )
                ctx.addPath(path.cgPath)
                ctx.strokePath()

            default:
                break
            }
        }

        ctx.restoreGState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - SVGPathParser
// ─────────────────────────────────────────────────────────────────────────────

/// Minimal SVG path `d` attribute parser.
/// Supports: M m L l H h V v C c S s Q q T t A a Z z
/// Q, T, S, and A commands have simplified implementations sufficient for
/// the Lucide icon set (which uses mostly M, L, C, and Z).
private enum SVGPathParser {

    /// Parse an SVG `d` string into a CGPath. Returns nil if the string
    /// contains no renderable data.
    static func parsePath(_ d: String) -> CGPath? {
        let path = CGMutablePath()
        var scanner = SVGScanner(d)

        var curX: CGFloat = 0, curY: CGFloat = 0
        var startX: CGFloat = 0, startY: CGFloat = 0
        // Last cubic control point (for S command reflection)
        var lastCPX: CGFloat = 0, lastCPY: CGFloat = 0
        // Last quadratic control point (for T command reflection)
        var lastQPX: CGFloat = 0, lastQPY: CGFloat = 0
        var lastCmd: Character = "M"

        while !scanner.isAtEnd {
            // A command letter, OR an implicit repeat using the previous command
            let cmd: Character
            if let c = scanner.peekLetter() {
                scanner.advance()
                cmd = c
            } else {
                // Implicit lineto after moveto, or implicit repeat of last cmd
                switch lastCmd {
                case "M": cmd = "L"
                case "m": cmd = "l"
                default:  cmd = lastCmd
                }
            }
            lastCmd = cmd

            switch cmd {

            // ── Move ──────────────────────────────────────────────────────
            case "M":
                guard let (x, y) = scanner.scanPair() else { break }
                curX = x; curY = y
                path.move(to: .init(x: curX, y: curY))
                startX = curX; startY = curY
                // Subsequent coord pairs are implicit lineto
                while let (lx, ly) = scanner.optionalScanPair() {
                    curX = lx; curY = ly
                    path.addLine(to: .init(x: curX, y: curY))
                }
            case "m":
                guard let (dx, dy) = scanner.scanPair() else { break }
                curX += dx; curY += dy
                path.move(to: .init(x: curX, y: curY))
                startX = curX; startY = curY
                while let (dx2, dy2) = scanner.optionalScanPair() {
                    curX += dx2; curY += dy2
                    path.addLine(to: .init(x: curX, y: curY))
                }

            // ── Line ──────────────────────────────────────────────────────
            case "L":
                while let (x, y) = scanner.optionalScanPair(required: lastCmd == "L") {
                    curX = x; curY = y
                    path.addLine(to: .init(x: curX, y: curY))
                }
            case "l":
                while let (dx, dy) = scanner.optionalScanPair(required: lastCmd == "l") {
                    curX += dx; curY += dy
                    path.addLine(to: .init(x: curX, y: curY))
                }

            // ── Horizontal / Vertical ────────────────────────────────────
            case "H":
                while let x = scanner.optionalScanNumber(required: lastCmd == "H") {
                    curX = x
                    path.addLine(to: .init(x: curX, y: curY))
                }
            case "h":
                while let dx = scanner.optionalScanNumber(required: lastCmd == "h") {
                    curX += dx
                    path.addLine(to: .init(x: curX, y: curY))
                }
            case "V":
                while let y = scanner.optionalScanNumber(required: lastCmd == "V") {
                    curY = y
                    path.addLine(to: .init(x: curX, y: curY))
                }
            case "v":
                while let dy = scanner.optionalScanNumber(required: lastCmd == "v") {
                    curY += dy
                    path.addLine(to: .init(x: curX, y: curY))
                }

            // ── Cubic Bézier ─────────────────────────────────────────────
            case "C":
                while let (x1, y1) = scanner.optionalScanPair(required: lastCmd == "C"),
                      let (x2, y2) = scanner.scanPair(),
                      let (x,  y)  = scanner.scanPair() {
                    lastCPX = x2; lastCPY = y2
                    curX = x; curY = y
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: x1,   y: y1),
                        control2: .init(x: x2,   y: y2)
                    )
                }
            case "c":
                while let (dx1, dy1) = scanner.optionalScanPair(required: lastCmd == "c"),
                      let (dx2, dy2) = scanner.scanPair(),
                      let (dx,  dy)  = scanner.scanPair() {
                    let cp2x = curX + dx2; let cp2y = curY + dy2
                    lastCPX = cp2x; lastCPY = cp2y
                    curX += dx; curY += dy
                    path.addCurve(
                        to:       .init(x: curX,          y: curY),
                        control1: .init(x: curX - dx + dx1 , y: curY - dy + dy1),
                        control2: .init(x: cp2x,          y: cp2y)
                    )
                }

            // ── Smooth cubic (S/s) — reflect previous control point ──────
            case "S":
                while let (x2, y2) = scanner.optionalScanPair(required: lastCmd == "S"),
                      let (x,  y)  = scanner.scanPair() {
                    let rx1 = 2 * curX - lastCPX
                    let ry1 = 2 * curY - lastCPY
                    lastCPX = x2; lastCPY = y2
                    curX = x; curY = y
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: rx1,  y: ry1),
                        control2: .init(x: x2,   y: y2)
                    )
                }
            case "s":
                while let (dx2, dy2) = scanner.optionalScanPair(required: lastCmd == "s"),
                      let (dx,  dy)  = scanner.scanPair() {
                    let rx1 = 2 * curX - lastCPX
                    let ry1 = 2 * curY - lastCPY
                    let cp2x = curX + dx2; let cp2y = curY + dy2
                    lastCPX = cp2x; lastCPY = cp2y
                    curX += dx; curY += dy
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: rx1,  y: ry1),
                        control2: .init(x: cp2x, y: cp2y)
                    )
                }

            // ── Quadratic Bézier (Q/q) ───────────────────────────────────
            case "Q":
                while let (x1, y1) = scanner.optionalScanPair(required: lastCmd == "Q"),
                      let (x,  y)  = scanner.scanPair() {
                    lastQPX = x1; lastQPY = y1
                    // Elevate quadratic to cubic for CGPath
                    let cp1x = curX + (2.0/3.0) * (x1 - curX)
                    let cp1y = curY + (2.0/3.0) * (y1 - curY)
                    let cp2x = x    + (2.0/3.0) * (x1 - x)
                    let cp2y = y    + (2.0/3.0) * (y1 - y)
                    curX = x; curY = y
                    lastCPX = cp2x; lastCPY = cp2y
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: cp1x, y: cp1y),
                        control2: .init(x: cp2x, y: cp2y)
                    )
                }
            case "q":
                while let (dx1, dy1) = scanner.optionalScanPair(required: lastCmd == "q"),
                      let (dx,  dy)  = scanner.scanPair() {
                    let ax1 = curX + dx1; let ay1 = curY + dy1
                    lastQPX = ax1; lastQPY = ay1
                    let cp1x = curX + (2.0/3.0) * dx1
                    let cp1y = curY + (2.0/3.0) * dy1
                    let ex   = curX + dx; let ey = curY + dy
                    let cp2x = ex   + (2.0/3.0) * (ax1 - ex)
                    let cp2y = ey   + (2.0/3.0) * (ay1 - ey)
                    curX = ex; curY = ey
                    lastCPX = cp2x; lastCPY = cp2y
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: cp1x, y: cp1y),
                        control2: .init(x: cp2x, y: cp2y)
                    )
                }

            // ── Smooth quadratic (T/t) — reflect previous QP ────────────
            case "T":
                while let (x, y) = scanner.optionalScanPair(required: lastCmd == "T") {
                    let rx1 = 2 * curX - lastQPX
                    let ry1 = 2 * curY - lastQPY
                    lastQPX = rx1; lastQPY = ry1
                    let cp1x = curX + (2.0/3.0) * (rx1 - curX)
                    let cp1y = curY + (2.0/3.0) * (ry1 - curY)
                    let cp2x = x    + (2.0/3.0) * (rx1 - x)
                    let cp2y = y    + (2.0/3.0) * (ry1 - y)
                    curX = x; curY = y
                    lastCPX = cp2x; lastCPY = cp2y
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: cp1x, y: cp1y),
                        control2: .init(x: cp2x, y: cp2y)
                    )
                }
            case "t":
                while let (dx, dy) = scanner.optionalScanPair(required: lastCmd == "t") {
                    let rx1 = 2 * curX - lastQPX
                    let ry1 = 2 * curY - lastQPY
                    lastQPX = rx1; lastQPY = ry1
                    let ex   = curX + dx; let ey = curY + dy
                    let cp1x = curX + (2.0/3.0) * (rx1 - curX)
                    let cp1y = curY + (2.0/3.0) * (ry1 - curY)
                    let cp2x = ex   + (2.0/3.0) * (rx1 - ex)
                    let cp2y = ey   + (2.0/3.0) * (ry1 - ey)
                    curX = ex; curY = ey
                    lastCPX = cp2x; lastCPY = cp2y
                    path.addCurve(
                        to:       .init(x: curX, y: curY),
                        control1: .init(x: cp1x, y: cp1y),
                        control2: .init(x: cp2x, y: cp2y)
                    )
                }

            // ── Arc (A/a) — approximate with cubic Bézier ────────────────
            case "A":
                while let rx = scanner.optionalScanNumber(required: lastCmd == "A"),
                      let ry             = scanner.scanNumber(),
                      let xAxisRotation  = scanner.scanNumber(),
                      let largeArcFlag   = scanner.scanNumber(),
                      let sweepFlag      = scanner.scanNumber(),
                      let (x, y)         = scanner.scanPair() {
                    let arcs = arcToCubicCurves(
                        x1: curX, y1: curY, x2: x, y2: y,
                        rx: rx, ry: ry, angle: xAxisRotation,
                        largeArcFlag: largeArcFlag != 0,
                        sweepFlag: sweepFlag != 0
                    )
                    for arc in arcs {
                        path.addCurve(
                            to:       .init(x: arc.x,   y: arc.y),
                            control1: .init(x: arc.x1,  y: arc.y1),
                            control2: .init(x: arc.x2,  y: arc.y2)
                        )
                    }
                    curX = x; curY = y
                    lastCPX = curX; lastCPY = curY
                }
            case "a":
                while let rx = scanner.optionalScanNumber(required: lastCmd == "a"),
                      let ry             = scanner.scanNumber(),
                      let xAxisRotation  = scanner.scanNumber(),
                      let largeArcFlag   = scanner.scanNumber(),
                      let sweepFlag      = scanner.scanNumber(),
                      let (dx, dy)        = scanner.scanPair() {
                    let ex = curX + dx; let ey = curY + dy
                    let arcs = arcToCubicCurves(
                        x1: curX, y1: curY, x2: ex, y2: ey,
                        rx: rx, ry: ry, angle: xAxisRotation,
                        largeArcFlag: largeArcFlag != 0,
                        sweepFlag: sweepFlag != 0
                    )
                    for arc in arcs {
                        path.addCurve(
                            to:       .init(x: arc.x,  y: arc.y),
                            control1: .init(x: arc.x1, y: arc.y1),
                            control2: .init(x: arc.x2, y: arc.y2)
                        )
                    }
                    curX = ex; curY = ey
                    lastCPX = curX; lastCPY = curY
                }

            // ── Close ─────────────────────────────────────────────────────
            case "Z", "z":
                path.closeSubpath()
                curX = startX; curY = startY

            default:
                break
            }
        }

        return path.isEmpty ? nil : path
    }

    /// Parse a `points` attribute string (polyline / polygon).
    static func parsePolyline(_ points: String, close: Bool) -> CGPath? {
        var scanner = SVGScanner(points)
        let path = CGMutablePath()
        var first = true
        while let (x, y) = scanner.optionalScanPair() {
            if first { path.move(to: .init(x: x, y: y)); first = false }
            else      { path.addLine(to: .init(x: x, y: y)) }
        }
        if close { path.closeSubpath() }
        return path.isEmpty ? nil : path
    }

    // ── Arc → cubic helper (standard parametric approximation) ──────────────

    private struct CubicCurve {
        var x1, y1, x2, y2, x, y: CGFloat
    }

    private static func arcToCubicCurves(
        x1: CGFloat, y1: CGFloat,
        x2: CGFloat, y2: CGFloat,
        rx: CGFloat, ry: CGFloat,
        angle: CGFloat,
        largeArcFlag: Bool,
        sweepFlag: Bool
    ) -> [CubicCurve] {

        guard rx != 0 && ry != 0 else {
            // Degenerate arc — treat as line
            return [CubicCurve(x1: x1, y1: y1, x2: x2, y2: y2, x: x2, y: y2)]
        }

        let phi = angle * .pi / 180
        let cosPhi = cos(phi); let sinPhi = sin(phi)

        // Step 1: mid-point method
        let dx = (x1 - x2) / 2; let dy = (y1 - y2) / 2
        let x1p =  cosPhi * dx + sinPhi * dy
        let y1p = -sinPhi * dx + cosPhi * dy

        var rx2 = abs(rx); var ry2 = abs(ry)
        let lambda = (x1p * x1p) / (rx2 * rx2) + (y1p * y1p) / (ry2 * ry2)
        if lambda > 1 { let sqrtL = sqrt(lambda); rx2 *= sqrtL; ry2 *= sqrtL }

        let rxSq = rx2 * rx2; let rySq = ry2 * ry2
        let x1pSq = x1p * x1p; let y1pSq = y1p * y1p
        var num = rxSq * rySq - rxSq * y1pSq - rySq * x1pSq
        let den = rxSq * y1pSq + rySq * x1pSq
        if den == 0 { return [] }
        num = max(0, num)
        let sq = sqrt(num / den)
        let sign: CGFloat = (largeArcFlag == sweepFlag) ? -1 : 1
        let cxp =  sign * sq * rx2 * y1p / ry2
        let cyp = -sign * sq * ry2 * x1p / rx2

        let cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2
        let cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2

        let ux = (x1p - cxp) / rx2; let uy = (y1p - cyp) / ry2
        let vx = (-x1p - cxp) / rx2; let vy = (-y1p - cyp) / ry2

        var startAngle = vectorAngle(ux: 1, uy: 0, vx: ux, vy: uy)
        var dAngle     = vectorAngle(ux: ux, uy: uy, vx: vx, vy: vy)

        if !sweepFlag && dAngle > 0  { dAngle -= 2 * .pi }
        if  sweepFlag && dAngle < 0  { dAngle += 2 * .pi }

        // Split arc into segments ≤ 90°
        let segCount = Int(ceil(abs(dAngle) / (.pi / 2)))
        guard segCount > 0 else { return [] }
        let segAngle = dAngle / CGFloat(segCount)
        let alpha = sin(segAngle) * (sqrt(4 + 3 * tan(segAngle / 2) * tan(segAngle / 2)) - 1) / 3

        var curves: [CubicCurve] = []
        var curAngle = startAngle
        var px = cos(curAngle); var py = sin(curAngle)
        var prevX = cosPhi * rx2 * px - sinPhi * ry2 * py + cx
        var prevY = sinPhi * rx2 * px + cosPhi * ry2 * py + cy

        for _ in 0 ..< segCount {
            let nextAngle = curAngle + segAngle
            let nx = cos(nextAngle); let ny = sin(nextAngle)

            let dx1 = -px * alpha; let dy1 = -py * alpha
            let dx2 =  nx * alpha; let dy2 =  ny * alpha

            let cp1x = cosPhi * rx2 * (px + dy1) - sinPhi * ry2 * (py - dx1) + cx
            let cp1y = sinPhi * rx2 * (px + dy1) + cosPhi * ry2 * (py - dx1) + cy
            let cp2x = cosPhi * rx2 * (nx - dy2) - sinPhi * ry2 * (ny + dx2) + cx
            let cp2y = sinPhi * rx2 * (nx - dy2) + cosPhi * ry2 * (ny + dx2) + cy
            let endX  = cosPhi * rx2 * nx - sinPhi * ry2 * ny + cx
            let endY  = sinPhi * rx2 * nx + cosPhi * ry2 * ny + cy

            curves.append(CubicCurve(x1: cp1x, y1: cp1y, x2: cp2x, y2: cp2y, x: endX, y: endY))
            curAngle = nextAngle
            px = nx; py = ny
            prevX = endX; prevY = endY
        }

        return curves
    }

    private static func vectorAngle(ux: CGFloat, uy: CGFloat, vx: CGFloat, vy: CGFloat) -> CGFloat {
        let dot = ux * vx + uy * vy
        let len = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        guard len != 0 else { return 0 }
        let cosA = max(-1, min(1, dot / len))
        let sign: CGFloat = (ux * vy - uy * vx) < 0 ? -1 : 1
        return sign * acos(cosA)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - SVGScanner
// ─────────────────────────────────────────────────────────────────────────────

/// Lightweight tokeniser for SVG path / points strings.
/// Handles: whitespace, commas, signed/unsigned numbers, scientific notation.
private struct SVGScanner {

    private let s: [Character]
    private var idx: Int

    var isAtEnd: Bool { idx >= s.count }

    init(_ string: String) {
        s   = Array(string)
        idx = 0
    }

    // MARK: Command scanning

    /// Return the current character if it is a letter, and advance past it.
    mutating func peekLetter() -> Character? {
        skipWS()
        guard !isAtEnd else { return nil }
        let c = s[idx]
        guard c.isLetter else { return nil }
        return c
    }

    mutating func advance() {
        if idx < s.count { idx += 1 }
    }

    // MARK: Number scanning

    /// Scan a number, returning nil if none is available.
    mutating func scanNumber() -> CGFloat? {
        skipWS()
        guard !isAtEnd else { return nil }
        var raw = ""
        var seenDot = false; var seenExp = false

        // Sign
        if s[idx] == "-" || s[idx] == "+" {
            raw.append(s[idx]); idx += 1
        }
        guard !isAtEnd else { return nil }

        while idx < s.count {
            let c = s[idx]
            if c.isNumber {
                raw.append(c); idx += 1
            } else if c == "." && !seenDot && !seenExp {
                seenDot = true; raw.append(c); idx += 1
            } else if (c == "e" || c == "E") && !seenExp && !raw.isEmpty {
                seenExp = true; raw.append(c); idx += 1
                // Optional sign after exponent
                if idx < s.count && (s[idx] == "-" || s[idx] == "+") {
                    raw.append(s[idx]); idx += 1
                }
            } else {
                break
            }
        }
        guard !raw.isEmpty, raw != "-", raw != "+",
              let v = Double(raw) else { return nil }
        return CGFloat(v)
    }

    /// Scan two consecutive numbers into a (CGFloat, CGFloat) pair.
    mutating func scanPair() -> (CGFloat, CGFloat)? {
        guard let x = scanNumber(), let y = scanNumber() else { return nil }
        return (x, y)
    }

    /// Scan an optional number; `required` forces at least one read.
    mutating func optionalScanNumber(required: Bool = false) -> CGFloat? {
        let saved = idx
        skipWS()
        // Bail out early if the next character is a command letter
        if !isAtEnd && s[idx].isLetter { return nil }
        let v = scanNumber()
        if v == nil && !required { idx = saved }
        return v
    }

    /// Scan an optional pair; `required` forces at least one read.
    mutating func optionalScanPair(required: Bool = false) -> (CGFloat, CGFloat)? {
        let saved = idx
        guard let x = optionalScanNumber(required: required) else { return nil }
        guard let y = scanNumber() else { idx = saved; return nil }
        return (x, y)
    }

    // MARK: Private

    private mutating func skipWS() {
        while idx < s.count && (s[idx].isWhitespace || s[idx] == ",") {
            idx += 1
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - UIColor + hex
// ─────────────────────────────────────────────────────────────────────────────

extension UIColor {
    /// Initialise from a CSS hex string: `#RGB`, `#RRGGBB`, or `#AARRGGBB`.
    /// Returns nil for malformed input.
    convenience init?(hexString: String) {
        var hex = hexString.trimmingCharacters(in: .whitespacesAndNewlines)
        if hex.hasPrefix("#") { hex = String(hex.dropFirst()) }

        // Expand 3-char shorthand: #RGB → #RRGGBB
        if hex.count == 3 {
            hex = hex.map { "\($0)\($0)" }.joined()
        }

        guard hex.count == 6 || hex.count == 8 else { return nil }

        // Prepend full alpha if only RRGGBB supplied
        let full = hex.count == 6 ? "FF" + hex : hex

        var rgba: UInt64 = 0
        guard Scanner(string: full).scanHexInt64(&rgba) else { return nil }

        let a = CGFloat((rgba >> 24) & 0xFF) / 255
        let r = CGFloat((rgba >> 16) & 0xFF) / 255
        let g = CGFloat((rgba >>  8) & 0xFF) / 255
        let b = CGFloat( rgba        & 0xFF) / 255
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
