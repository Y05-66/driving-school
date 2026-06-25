const fs = require('fs');
const zlib = require('zlib');

function createPNG(width, height, drawFn) {
  const pixels = [];
  for (let y = 0; y < height; y++) {
    pixels.push(0);
    for (let x = 0; x < width; x++) {
      const [r, g, b, a] = drawFn(x, y, width, height);
      pixels.push(r, g, b, a);
    }
  }
  const raw = Buffer.from(pixels);
  const compressed = zlib.deflateSync(raw);

  function crc32(buf) {
    let crc = 0xffffffff;
    const table = [];
    for (let i = 0; i < 256; i++) {
      let c = i;
      for (let j = 0; j < 8; j++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
      table[i] = c;
    }
    for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
    return (crc ^ 0xffffffff) >>> 0;
  }

  function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length);
    const typeB = Buffer.from(type);
    const crcData = Buffer.concat([typeB, data]);
    const crcB = Buffer.alloc(4);
    crcB.writeUInt32BE(crc32(crcData));
    return Buffer.concat([len, typeB, data, crcB]);
  }

  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6;

  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', compressed), chunk('IEND', Buffer.alloc(0))]);
}

function lerp(a, b, t) { return Math.round(a + (b - a) * t); }
function dist(x1, y1, x2, y2) { return Math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2); }

function gradientColor(x, y, w, h, c1, c2) {
  const t = (x + y) / (w + h);
  return [lerp(c1[0], c2[0], t), lerp(c1[1], c2[1], t), lerp(c1[2], c2[2], t)];
}

// 首页图标 - 渐变房子
function drawHomeGradient(c1, c2) {
  return (x, y, w, h) => {
    const cx = w / 2;
    const color = gradientColor(x, y, w, h, c1, c2);
    // 屋顶
    if (y >= 12 && y <= 38) {
      const halfW = (y - 12) * 1.4;
      if (Math.abs(x - cx) <= halfW + 2) return [...color, 255];
    }
    // 屋身
    if (y >= 36 && y <= 66 && x >= 20 && x <= 62) {
      // 门
      if (x >= 34 && x <= 48 && y >= 46) return [0, 0, 0, 0];
      return [...color, 255];
    }
    // 烟囱
    if (x >= 50 && x <= 58 && y >= 12 && y <= 26) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

// 日历图标 - 渐变日历
function drawCalendarGradient(c1, c2) {
  return (x, y, w, h) => {
    const color = gradientColor(x, y, w, h, c1, c2);
    // 外框
    if (x >= 12 && x <= 69 && y >= 16 && y <= 68) {
      // 顶部
      if (y <= 30) return [...color, 255];
      // 日期点
      const gridX = (x - 18) % 14;
      const gridY = (y - 36) % 14;
      if (gridX < 8 && gridY < 8) return [...color, 255];
      return [0, 0, 0, 0];
    }
    // 挂钩
    if ((x >= 26 && x <= 30 || x >= 52 && x <= 56) && y >= 10 && y <= 20) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

// 铃铛图标 - 渐变铃铛
function drawBellGradient(c1, c2) {
  return (x, y, w, h) => {
    const cx = w / 2;
    const color = gradientColor(x, y, w, h, c1, c2);
    // 铃铛主体
    if (y >= 14 && y <= 52) {
      const t = (y - 14) / 38;
      const radius = 8 + t * 18;
      if (Math.abs(x - cx) <= radius) return [...color, 255];
    }
    // 底部横条
    if (y >= 52 && y <= 58 && x >= 16 && x <= 66) return [...color, 255];
    // 顶部小球
    if (dist(x, y, cx, 12) <= 6) return [...color, 255];
    // 底部小圆
    if (dist(x, y, cx, 62) <= 5) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

// 用户图标 - 渐变用户
function drawUserGradient(c1, c2) {
  return (x, y, w, h) => {
    const cx = w / 2;
    const color = gradientColor(x, y, w, h, c1, c2);
    // 头部
    if (dist(x, y, cx, 22) <= 15) return [...color, 255];
    // 身体
    if (y >= 42 && y <= 70) {
      const t = (y - 42) / 28;
      const radius = 18 + t * 12;
      if (Math.abs(x - cx) <= radius) return [...color, 255];
    }
    return [0, 0, 0, 0];
  };
}

const size = 81;

// 灰色系（未选中）
const gray1 = [160, 170, 190];
const gray2 = [130, 140, 160];

// 紫色系（选中）
const purple1 = [99, 102, 241];
const purple2 = [139, 92, 246];

const icons = [
  { name: 'tab-home', draw: drawHomeGradient },
  { name: 'tab-lesson', draw: drawCalendarGradient },
  { name: 'tab-notify', draw: drawBellGradient },
  { name: 'tab-user', draw: drawUserGradient },
];

icons.forEach(({ name, draw }) => {
  fs.writeFileSync(`src/static/${name}.png`, createPNG(size, size, draw(gray1, gray2)));
  fs.writeFileSync(`src/static/${name}-active.png`, createPNG(size, size, draw(purple1, purple2)));
  console.log(`✅ ${name}.png / ${name}-active.png`);
});

console.log('\n渐变图标生成完成！');
