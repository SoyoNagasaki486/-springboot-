// Lays out the overall use case diagram (图2-1) and writes a primitive list for
// render.ps1.
//
// The 11 use cases of 表2-2 are stacked in one column ordered by privilege
// (administrator-only at the top, student-only at the bottom), so each actor
// owns a contiguous slice of the column. Association lines use orthogonal
// routing: a horizontal run from the actor to that actor's own elbow strip, a
// vertical run down the strip, then a horizontal stub into the leftmost or
// rightmost point of the ellipse. Elbow strips are disjoint per actor and sit
// beside the ellipses, so no line can graze a use case it does not belong to and
// no two lines cross. The layout is verified before it is written out.
const fs = require('fs');
const path = require('path');

// ---------------------------------------------------------------- content ----
// Use cases and actors follow 表2-2 用例词汇表 of the thesis.
const S = '学生', T = '教师', A = '管理员';
const COLUMN = [
  { name: '维护实验室信息', actors: [A] },
  { name: '管理公告与日志', actors: [A] },
  { name: '审核准入资格', actors: [T, A] },
  { name: '审核预约申请', actors: [T, A] },
  { name: '管理仪器设备', actors: [T, A] },
  { name: '管理安全信息', actors: [T, A] },
  { name: '注册与登录', actors: [S, T, A] },
  { name: '学习安全课程', actors: [S, T] },
  { name: '参加在线考试', actors: [S, T] },
  { name: '提交实验室预约', actors: [S] },
  { name: '查询准入资格', actors: [S] },
];

// ----------------------------------------------------------------- geometry ---
const CANVAS = { w: 14.0, h: 13.2 };
const BOUND = { x1: 6.05, y1: 0.95, x2: 10.35, y2: 12.75 };
const ELL = { w: 3.2, h: 0.85 };
const COL_CX = 8.2;
const Y0 = 1.6;
const STEP = 1.06;
const half = { a: ELL.w / 2, b: ELL.h / 2 };

// actors sit outside the boundary; each owns a vertical elbow strip. The entry
// offset shifts where a line meets the ellipse so that two actors sharing a use
// case never draw overlapping stubs.
const ACTORS = {
  [A]: { x: 2.55, y: 4.1, anchorX: 2.95, strip: [4.15, 5.47], entry: -0.15 },
  [S]: { x: 2.55, y: 9.9, anchorX: 2.95, strip: [5.55, 6.19], entry: 0.15 },
  [T]: { x: 12.95, y: 6.2, anchorX: 12.55, strip: [10.40, 11.72], entry: -0.15 },
};
const ELBOW_STEP = 0.22;

const cyOf = (i) => Y0 + i * STEP;

const placed = COLUMN.map((uc, i) => ({ ...uc, cx: COL_CX, cy: cyOf(i) }));

// build orthogonal polylines
const paths = [];
for (const key of [A, S, T]) {
  const actor = ACTORS[key];
  const targets = placed
    .map((uc, idx) => ({ uc, idx }))
    .filter((t) => t.uc.actors.includes(key))
    .sort((a, b) => a.uc.cy - b.uc.cy);
  const fromLeft = actor.anchorX < COL_CX;
  targets.forEach((t, k) => {
    const elbowX = actor.strip[0] + k * ELBOW_STEP;
    const entryY = t.uc.cy + actor.entry;
    const inset = half.a * Math.sqrt(Math.max(0, 1 - ((actor.entry / half.b) ** 2)));
    const rimX = fromLeft ? COL_CX - inset : COL_CX + inset;
    const points = [
      [actor.anchorX, actor.y],
      [elbowX, actor.y],
      [elbowX, entryY],
      [rimX, entryY],
    ];
    if (!fromLeft) points.reverse();
    paths.push({ actor: key, uc: t.uc, points });
  });
}

// ------------------------------------------------------------- verification --
function segHitsEllipse(p1, p2, cx, cy, margin) {
  const a = half.a + margin, b = half.b + margin;
  const dx = p2[0] - p1[0], dy = p2[1] - p1[1];
  const fx = p1[0] - cx, fy = p1[1] - cy;
  const A2 = (dx * dx) / (a * a) + (dy * dy) / (b * b);
  const B2 = 2 * ((fx * dx) / (a * a) + (fy * dy) / (b * b));
  const C2 = (fx * fx) / (a * a) + (fy * fy) / (b * b) - 1;
  const disc = B2 * B2 - 4 * A2 * C2;
  if (disc < 0 || A2 === 0) return false;
  const sq = Math.sqrt(disc);
  const lo = Math.min((-B2 - sq) / (2 * A2), (-B2 + sq) / (2 * A2));
  const hi = Math.max((-B2 - sq) / (2 * A2), (-B2 + sq) / (2 * A2));
  return hi > 0.02 && lo < 0.98;
}

function segsCross(a1, a2, b1, b2) {
  const d = (p, q, r) => (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0]);
  const d1 = d(b1, b2, a1), d2 = d(b1, b2, a2);
  const d3 = d(a1, a2, b1), d4 = d(a1, a2, b2);
  return ((d1 > 0) !== (d2 > 0)) && ((d3 > 0) !== (d4 > 0));
}

const issues = [];
// use cases must stay inside the boundary
for (const uc of placed) {
  if (uc.cx - half.a < BOUND.x1 || uc.cx + half.a > BOUND.x2) issues.push(`${uc.name} 超出边界横向`);
  if (uc.cy - half.b < BOUND.y1 || uc.cy + half.b > BOUND.y2) issues.push(`${uc.name} 超出边界纵向`);
}
for (const p of paths) {
  for (const uc of placed) {
    if (uc === p.uc) continue;
    for (let i = 0; i + 1 < p.points.length; i++) {
      if (segHitsEllipse(p.points[i], p.points[i + 1], uc.cx, uc.cy, 0.05)) {
        issues.push(`${p.actor}->${p.uc.name} 穿过 ${uc.name}`);
      }
    }
  }
}
for (let i = 0; i < paths.length; i++) {
  for (let j = i + 1; j < paths.length; j++) {
    if (paths[i].actor === paths[j].actor) continue;
    let crossed = false;
    for (let a = 0; a + 1 < paths[i].points.length && !crossed; a++) {
      for (let b = 0; b + 1 < paths[j].points.length && !crossed; b++) {
        if (segsCross(paths[i].points[a], paths[i].points[a + 1], paths[j].points[b], paths[j].points[b + 1])) {
          crossed = true;
        }
      }
    }
    if (crossed) issues.push(`${paths[i].actor}->${paths[i].uc.name} 与 ${paths[j].actor}->${paths[j].uc.name} 交叉`);
  }
}
console.log('use-case layout issues: ' + issues.length);
issues.slice(0, 10).forEach((v) => console.log('  ! ' + v));

// ------------------------------------------------------------------ output ----
const items = [];
items.push({ type: 'text', x: (BOUND.x1 + BOUND.x2) / 2, y: 0.42, text: '高校实验室安全准入与预约管理系统', size: 12, bold: true, font: 'hei' });
items.push({ type: 'rect', x: BOUND.x1, y: BOUND.y1, w: BOUND.x2 - BOUND.x1, h: BOUND.y2 - BOUND.y1, line: 0.045, fill: 'none' });
for (const p of paths) {
  const pts = p.points.map((q) => [q[0], q[1]]);
  items.push({ type: 'path', points: pts, closed: false, line: 0.028 });
}
for (const uc of placed) {
  items.push({ type: 'ellipse', cx: uc.cx, cy: uc.cy, w: ELL.w, h: ELL.h, line: 0.035 });
  items.push({ type: 'text', x: uc.cx, y: uc.cy, text: uc.name, size: 12 });
}
for (const key of [A, S, T]) {
  items.push({ type: 'actor', x: ACTORS[key].x, y: ACTORS[key].y, label: key });
}

const doc = { out: '系统总体用例图.png', dpi: 600, widthCm: CANVAS.w, heightCm: CANVAS.h, items };
fs.writeFileSync(path.join(__dirname, 'usecase-layout.json'), JSON.stringify(doc, null, 1), 'utf8');
console.log('canvas ' + CANVAS.w + 'x' + CANVAS.h + 'cm, ' + items.length + ' items, ' + paths.length + ' associations');
