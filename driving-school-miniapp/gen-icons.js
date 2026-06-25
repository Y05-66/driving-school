const fs = require('fs');
const zlib = require('zlib');

// 81x81 像素 PNG 生成器
function createPNG(width, height, drawFn) {
  const pixels = [];
  for (let y = 0; y < height; y++) {
    pixels.push(0); // filter byte
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
  ihdr[8] = 8; ihdr[9] = 6; // 8bit RGBA

  return Buffer.concat([
    sig,
    chunk('IHDR', ihdr),
    chunk('IDAT', compressed),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

// 判断点是否在圆内
function inCircle(cx, cy, r, x, y) {
  return (x - cx) ** 2 + (y - cy) ** 2 <= r * r;
}

// 判断点是否在矩形内
function inRect(rx, ry, rw, rh, x, y) {
  return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
}

// 首页图标 (房子)
function drawHome(color) {
  return (x, y, w, h) => {
    const cx = w / 2, cy = h / 2;
    // 屋顶三角
    const roofTop = 15, roofBottom = 40, roofLeft = 15, roofRight = 66;
    const inRoof = y >= roofTop && y <= roofBottom &&
      x >= roofLeft + (y - roofTop) * (cx - roofLeft) / (roofBottom - roofTop) - 2 &&
      x <= roofRight - (y - roofTop) * (roofRight - cx) / (roofBottom - roofTop) + 2;
    // 屋身
    const inBody = inRect(22, 38, 37, 28, x, y);
    // 门
    const inDoor = inRect(35, 48, 12, 18, x, y);
    if ((inRoof || inBody) && !inDoor) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

// 课时图标 (日历)
function drawCalendar(color) {
  return (x, y, w, h) => {
    // 日历外框
    const inOuter = inRect(12, 18, 57, 50, x, y);
    // 日历顶部
    const inTop = inRect(12, 18, 57, 14, x, y);
    // 日历主体
    const inBody = inRect(12, 32, 57, 36, x, y);
    // 两个挂钩
    const hook1 = inRect(26, 14, 4, 10, x, y);
    const hook2 = inRect(51, 14, 4, 10, x, y);
    // 日期点
    const dotSize = 4;
    const dots = [
      [24, 42], [37, 42], [50, 42],
      [24, 54], [37, 54], [50, 54],
    ];
    const inDot = dots.some(([dx, dy]) => inRect(dx, dy, dotSize, dotSize, x, y));

    if (inTop || hook1 || hook2) return [...color, 255];
    if (inBody && !inDot) return [...color, 255];
    if (inDot) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

// 通知图标 (铃铛)
function drawBell(color) {
  return (x, y, w, h) => {
    const cx = w / 2;
    // 铃铛主体 (椭圆)
    const bellTop = 16, bellBottom = 52;
    const bellWidth = 22;
    const normalizedY = (y - bellTop) / (bellBottom - bellTop);
    const bellRadius = bellWidth * (0.3 + 0.7 * normalizedY);
    const inBell = y >= bellTop && y <= bellBottom &&
      Math.abs(x - cx) <= bellRadius;
    // 铃铛底部横条
    const inBar = inRect(18, 54, 45, 6, x, y);
    // 顶部小圆
    const inTopCircle = inCircle(cx, 14, 5, x, y);
    // 底部小三角
    const inTriangle = y >= 60 && y <= 68 && Math.abs(x - cx) <= (68 - y) * 0.8;

    if (inBell || inBar || inTopCircle || inTriangle) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

// 用户图标 (人)
function drawUser(color) {
  return (x, y, w, h) => {
    const cx = w / 2;
    // 头部圆形
    const inHead = inCircle(cx, 24, 14, x, y);
    // 身体 (半椭圆)
    const bodyTop = 44, bodyBottom = 70;
    const normalizedY = (y - bodyTop) / (bodyBottom - bodyTop);
    const bodyRadius = 28 * (0.5 + 0.5 * normalizedY);
    const inBody = y >= bodyTop && y <= bodyBottom && Math.abs(x - cx) <= bodyRadius;

    if (inHead || inBody) return [...color, 255];
    return [0, 0, 0, 0];
  };
}

const size = 81;
const gray = [148, 163, 184];
const purple = [99, 102, 241];

const icons = [
  { name: 'tab-home', draw: drawHome },
  { name: 'tab-lesson', draw: drawCalendar },
  { name: 'tab-notify', draw: drawBell },
  { name: 'tab-user', draw: drawUser },
];

icons.forEach(({ name, draw }) => {
  fs.writeFileSync(`src/static/${name}.png`, createPNG(size, size, draw(gray)));
  fs.writeFileSync(`src/static/${name}-active.png`, createPNG(size, size, draw(purple)));
  console.log(`✅ ${name}.png / ${name}-active.png`);
});

console.log('\n图标生成完成！');
