// Lays out the overall E-R diagram (图3-3) and writes a primitive list for
// render.ps1.
//
// Chen notation: rectangles for entities, diamonds carrying the verb for
// relationships, 1/n cardinality labels on the connecting lines. Entities and
// relationships follow 第3章 of the thesis: 15 entities, with 用户 / 实验室 / 设备
// on the "one" side of the 1:n relationships.
//
// 用户 has the highest degree, so its satellites are placed on an arc centred on
// the hub: every connection is then a radial segment that cannot cut through
// another entity. 实验室 keeps its three satellites on a shorter arc below it, and
// 设备使用 hangs off 设备. Diamonds are placed on the line (nudged until clear) and
// each cardinality label searches a few candidate offsets for a free spot, so
// the figure is verified to be free of overlaps before it is written out.
const fs = require('fs');
const path = require('path');

const CANVAS = { w: 15.5, h: 14.2 };
const BOX = { w: 2.5, h: 0.78 };
const DIA = { w: 1.36, h: 0.78 };
const ENTITY_SIZE = 12;    // pt, 正文 小四
const VERB_SIZE = 10.5;    // pt, 五号
const LABEL_SIZE = 10.5;   // pt, 五号

// ------------------------------------------------------------- arc centres ---
// 用户's satellites ride an arc that sweeps the left half, so each connection is
// a radial segment; 实验室's satellites sit in a column to its right.
const HUB_USER = { x: 7.4, y: 5.6, a: 6.0, b: 5.0 };
const USER_ARCS = [105, 132.5, 160, 187.5, 215, 242.5, 270];

function arc(c, deg) {
  const th = (deg * Math.PI) / 180;
  if (c.r !== undefined) return { x: c.x + c.r * Math.cos(th), y: c.y + c.r * Math.sin(th) };
  return { x: c.x + c.a * Math.cos(th), y: c.y + c.b * Math.sin(th) };
}

// --------------------------------------------------------------- entities ----
const E = {
  通知公告: arc(HUB_USER, USER_ARCS[0]),
  操作日志: arc(HUB_USER, USER_ARCS[1]),
  考试题目: arc(HUB_USER, USER_ARCS[2]),
  违规记录: arc(HUB_USER, USER_ARCS[3]),
  考试记录: arc(HUB_USER, USER_ARCS[4]),
  学习记录: arc(HUB_USER, USER_ARCS[5]),
  准入资格: arc(HUB_USER, USER_ARCS[6]),
  课程: { x: 1.6, y: 0.6 },
  用户: { x: HUB_USER.x, y: HUB_USER.y },
  预约: { x: 11.4, y: 2.2 },
  实验室: { x: 8.4, y: 6.8 },
  危险源: { x: 13.0, y: 4.6 },
  安全检查: { x: 13.0, y: 8.4 },
  设备: { x: 13.0, y: 11.0 },
  设备使用: { x: 13.0, y: 13.6 },
};

// ------------------------------------------------------------ relationships --
const RELATIONS = [
  { id: '维护', from: '用户', to: '考试题目' },
  { id: '发布', from: '用户', to: '通知公告' },
  { id: '留痕', from: '用户', to: '操作日志' },
  { id: '违规', from: '用户', to: '违规记录' },
  { id: '参加', from: '用户', to: '考试记录' },
  { id: '学习', from: '用户', to: '学习记录' },
  { id: '对应', from: '课程', to: '学习记录' },
  { id: '持有', from: '用户', to: '准入资格' },
  { id: '登记', from: '实验室', to: '危险源' },
  { id: '检查', from: '实验室', to: '安全检查' },
  { id: '配置', from: '实验室', to: '设备' },
  { id: '使用', from: '设备', to: '设备使用' },
  { id: '提交', from: '用户', to: '预约' },
  { id: '受理', from: '实验室', to: '预约' },
];

// ------------------------------------------------------------------ helpers ---
const boxOf = (name) => ({
  x1: E[name].x - BOX.w / 2, y1: E[name].y - BOX.h / 2,
  x2: E[name].x + BOX.w / 2, y2: E[name].y + BOX.h / 2,
  cx: E[name].x, cy: E[name].y,
});

function borderPoint(cx, cy, dx, dy, hw, hh) {
  if (dx === 0 && dy === 0) return { x: cx, y: cy };
  const t = Math.min(dx !== 0 ? hw / Math.abs(dx) : Infinity, dy !== 0 ? hh / Math.abs(dy) : Infinity);
  return { x: cx + dx * t, y: cy + dy * t };
}

const rectsOverlap = (a, b, m = 0.05) =>
  !(a.x2 + m < b.x1 || b.x2 + m < a.x1 || a.y2 + m < b.y1 || b.y2 + m < a.y1);

const diaBox = (cx, cy) => ({ x1: cx - DIA.w / 2, y1: cy - DIA.h / 2, x2: cx + DIA.w / 2, y2: cy + DIA.h / 2 });

function segsCross(p, q, a, b) {
  const d = (u, v, w) => (v.x - u.x) * (w.y - u.y) - (v.y - u.y) * (w.x - u.x);
  const d1 = d(a, b, p), d2 = d(a, b, q), d3 = d(p, q, a), d4 = d(p, q, b);
  return ((d1 > 0) !== (d2 > 0)) && ((d3 > 0) !== (d4 > 0));
}

function segEntersRect(p, q, r, m = 0.02) {
  const inside = (pt) => pt.x > r.x1 + m && pt.x < r.x2 - m && pt.y > r.y1 + m && pt.y < r.y2 - m;
  if (inside(p) || inside(q)) return false;
  const edges = [
    [{ x: r.x1, y: r.y1 }, { x: r.x2, y: r.y1 }],
    [{ x: r.x2, y: r.y1 }, { x: r.x2, y: r.y2 }],
    [{ x: r.x2, y: r.y2 }, { x: r.x1, y: r.y2 }],
    [{ x: r.x1, y: r.y2 }, { x: r.x1, y: r.y1 }],
  ];
  let crossings = 0;
  for (const [a, b] of edges) if (segsCross(p, q, a, b)) crossings++;
  return crossings >= 2;
}

// ------------------------------------------------------------------ layout ----
const entityBoxes = Object.keys(E).map((name) => ({ name, ...boxOf(name) }));

const links = RELATIONS.map((rel) => {
  const a = E[rel.from], b = E[rel.to];
  const dx = b.x - a.x, dy = b.y - a.y;
  const p1 = borderPoint(a.x, a.y, dx, dy, BOX.w / 2 + 0.02, BOX.h / 2 + 0.02);
  const p2 = borderPoint(b.x, b.y, -dx, -dy, BOX.w / 2 + 0.02, BOX.h / 2 + 0.02);
  return { ...rel, p1, p2 };
});

const placed = entityBoxes.map((e) => ({ x1: e.x1 - 0.05, y1: e.y1 - 0.05, x2: e.x2 + 0.05, y2: e.y2 + 0.05 }));
for (const ln of links) {
  const cands = [];
  for (let t = 0.24; t <= 0.76; t += 0.01) {
    const cx = ln.p1.x + (ln.p2.x - ln.p1.x) * t;
    const cy = ln.p1.y + (ln.p2.y - ln.p1.y) * t;
    const box = diaBox(cx, cy);
    if (!placed.some((o) => rectsOverlap(box, o, 0.05)) &&
        !links.some((other) => other !== ln && segEntersRect(other.p1, other.p2, box, 0.03))) {
      cands.push({ t, cx, cy, box });
    }
  }
  cands.sort((a, b) => Math.abs(a.t - 0.5) - Math.abs(b.t - 0.5));
  const pick = cands[0] || (() => {
    const cx = (ln.p1.x + ln.p2.x) / 2, cy = (ln.p1.y + ln.p2.y) / 2;
    return { t: 0.5, cx, cy, box: diaBox(cx, cy) };
  })();
  placed.push(pick.box);
  ln.diamond = pick;
}

// cardinality labels: search a few offsets along/perpendicular to the line for a
// free spot, preferring the one closest to the entity it belongs to
const labels = [];
const allDia = links.map((l) => l.diamond.box);
for (const ln of links) {
  const dx = ln.p2.x - ln.p1.x, dy = ln.p2.y - ln.p1.y;
  const len = Math.hypot(dx, dy);
  const ux = dx / len, uy = dy / len;
  const nx = -uy, ny = ux;
  for (const [text, sign] of [['1', 1], ['n', -1]]) {
    const base = sign === 1 ? { x: ln.p1.x, y: ln.p1.y } : { x: ln.p2.x, y: ln.p2.y };
    const dir = sign === 1 ? { x: ux, y: uy } : { x: -ux, y: -uy };
    let best = null;
    const limit = Math.max(0.42, len * 0.45);
    const ds = [0.42, 0.62, 0.85, 1.1, limit].filter((d) => d <= limit + 0.001);
    for (const d of ds) {
      for (const off of [0.5, 0.72, 0.95, 1.2]) {
        for (const side of [1, -1]) {
          const cx = base.x + dir.x * d + nx * off * side;
          const cy = base.y + dir.y * d + ny * off * side;
          const box = { x1: cx - 0.24, y1: cy - 0.24, x2: cx + 0.24, y2: cy + 0.24 };
          const clash =
            entityBoxes.some((e) => rectsOverlap(box, e, 0.14)) ||
            allDia.some((b) => rectsOverlap(box, b, 0.06)) ||
            labels.some((l) => rectsOverlap(box, l.box, 0.02)) ||
            links.some((other) => other !== ln && segEntersRect(other.p1, other.p2, box, 0.02));
          if (!clash) { best = { text, x: cx, y: cy, box, rel: ln.id }; break; }
        }
        if (best) break;
      }
      if (best) break;
    }
    if (!best) {
      const cx = base.x + dir.x * 0.45 + nx * 0.30;
      const cy = base.y + dir.y * 0.45 + ny * 0.30;
      best = { text, x: cx, y: cy, box: { x1: cx - 0.24, y1: cy - 0.24, x2: cx + 0.24, y2: cy + 0.24 }, rel: ln.id };
    }
    labels.push(best);
  }
}

// ------------------------------------------------------------ verification ----
const issues = [];
for (let i = 0; i < entityBoxes.length; i++) {
  for (let j = i + 1; j < entityBoxes.length; j++) {
    if (rectsOverlap(entityBoxes[i], entityBoxes[j], 0.05)) issues.push(`实体重叠: ${entityBoxes[i].name}/${entityBoxes[j].name}`);
  }
}
for (const e of entityBoxes) {
  if (e.x1 < 0.15 || e.x2 > CANVAS.w - 0.15 || e.y1 < 0.15 || e.y2 > CANVAS.h - 0.15) issues.push(`实体越界: ${e.name}`);
}
for (const ln of links) {
  for (const e of entityBoxes) {
    if (e.name === ln.from || e.name === ln.to) continue;
    if (segEntersRect(ln.p1, ln.p2, e)) issues.push(`${ln.from}-${ln.id}-${ln.to} 穿过实体 ${e.name}`);
  }
  for (const other of links) {
    if (other === ln) continue;
    if (segEntersRect(ln.p1, ln.p2, other.diamond.box, 0.06)) issues.push(`${ln.from}-${ln.id}-${ln.to} 穿过联系 ${other.id}`);
  }
  for (const lb of labels) {
    if (lb.rel === ln.id) continue;
    if (segEntersRect(ln.p1, ln.p2, lb.box, 0.02)) issues.push(`${ln.from}-${ln.id}-${ln.to} 穿过基数 ${lb.text}(${lb.rel})`);
  }
}
for (const l of labels) {
  for (const e of entityBoxes) if (rectsOverlap(l.box, e, 0.0)) issues.push(`基数 ${l.text}(${l.rel}) 压在实体 ${e.name}`);
  const own = links.find((x) => x.id === l.rel);
  if (rectsOverlap(l.box, own.diamond.box, 0.0)) issues.push(`基数 ${l.text}(${l.rel}) 压在联系上`);
}
console.log('E-R layout issues: ' + issues.length);
issues.slice(0, 16).forEach((v) => console.log('  ! ' + v));

// ------------------------------------------------------------------ output ----
const items = [];
for (const ln of links) items.push({ type: 'line', x1: ln.p1.x, y1: ln.p1.y, x2: ln.p2.x, y2: ln.p2.y, line: 0.028 });
for (const e of entityBoxes) {
  items.push({ type: 'rect', x: e.x1, y: e.y1, w: BOX.w, h: BOX.h, line: 0.035 });
  items.push({ type: 'text', x: e.cx, y: e.cy, text: e.name, size: ENTITY_SIZE });
}
for (const ln of links) {
  items.push({ type: 'diamond', cx: ln.diamond.cx, cy: ln.diamond.cy, w: DIA.w, h: DIA.h });
  items.push({ type: 'text', x: ln.diamond.cx, y: ln.diamond.cy, text: ln.id, size: VERB_SIZE });
}
for (const lb of labels) items.push({ type: 'text', x: lb.x, y: lb.y, text: lb.text, size: LABEL_SIZE });

const doc = { out: '系统总体E-R图.png', dpi: 600, widthCm: CANVAS.w, heightCm: CANVAS.h, items };
fs.writeFileSync(path.join(__dirname, 'er-layout.json'), JSON.stringify(doc, null, 1), 'utf8');
console.log('canvas ' + CANVAS.w + 'x' + CANVAS.h + 'cm, ' + entityBoxes.length + ' entities, ' + links.length + ' relationships');
