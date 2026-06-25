#!/bin/bash

echo ""
echo "========================================"
echo "  驾校管理系统小程序 - 启动脚本"
echo "========================================"
echo ""

# 获取本机 IP
echo "[1/3] 获取本机 IP..."
LOCAL_IP=$(ifconfig 2>/dev/null | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -1)

if [ -z "$LOCAL_IP" ]; then
  LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
fi

if [ -z "$LOCAL_IP" ]; then
  LOCAL_IP="127.0.0.1"
fi

echo "      本机 IP: $LOCAL_IP"
echo ""

# 更新配置
echo "[2/3] 更新配置文件..."
node update-ip.js
echo ""

# 启动小程序
echo "[3/3] 启动微信小程序开发..."
echo ""
npm run dev:mp-weixin
