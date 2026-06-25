/**
 * 自动获取本机 IP 并更新小程序后端地址
 * 用法: node update-ip.js
 */

const os = require('os')
const fs = require('fs')
const path = require('path')

function getLocalIP() {
  const interfaces = os.networkInterfaces()
  const candidates = []

  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.internal || iface.family !== 'IPv4') continue
      // 跳过 VMware/VirtualBox 虚拟网卡
      if (name.includes('VMware') || name.includes('VirtualBox') || name.includes('vEthernet')) continue
      candidates.push({ name, ip: iface.address })
    }
  }

  // 优先 10.x.x.x（公司/校园网）
  const found10 = candidates.find(c => c.ip.startsWith('10.'))
  if (found10) return found10.ip

  // 其次 192.168.x.x（家庭/办公）
  const found192 = candidates.find(c => c.ip.startsWith('192.168.'))
  if (found192) return found192.ip

  // 再次 172.16-31.x.x
  const found172 = candidates.find(c => /^172\.(1[6-9]|2\d|3[01])\./.test(c.ip))
  if (found172) return found172.ip

  // 兜底
  return candidates.length > 0 ? candidates[0].ip : '127.0.0.1'
}

const ip = getLocalIP()
const port = 8080
const baseUrl = `http://${ip}:${port}/api`

console.log(`\n  🌐 本机 IP: ${ip}`)
console.log(`  🔗 后端地址: ${baseUrl}\n`)

// 更新小程序 config.js（BASE_URL 定义在此文件中）
const configFile = path.join(__dirname, 'src/utils/config.js')
if (fs.existsSync(configFile)) {
  let content = fs.readFileSync(configFile, 'utf-8')
  content = content.replace(
    /export const BASE_URL = ['"].*?['"]/,
    `export const BASE_URL = '${baseUrl}'`
  )
  fs.writeFileSync(configFile, content, 'utf-8')
  console.log('  ✅ 已更新 src/utils/config.js')
}

// 更新 Web 前端 vite.config.js (如果存在)
const viteConfigFile = path.join(__dirname, '../driving-school-frontend/vite.config.js')
if (fs.existsSync(viteConfigFile)) {
  let content = fs.readFileSync(viteConfigFile, 'utf-8')
  content = content.replace(
    /target: ['"]http:\/\/.*?['"]/,
    `target: 'http://${ip}:${port}'`
  )
  fs.writeFileSync(viteConfigFile, content, 'utf-8')
  console.log('  ✅ 已更新 driving-school-frontend/vite.config.js')
}

console.log('\n  💡 提示: 确保后端已启动，然后运行 npm run dev:mp-weixin\n')
