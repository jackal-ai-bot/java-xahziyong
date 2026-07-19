# java-xah (java Xray Argo Hysteria2)

## ⚙️ 配置项说明

```yaml
app:
  # 服务器域名或IP，直连访问的有效域名
  domain: "example.com"
  # 服务器开放端口，Xray 和 Hysteria2 的主监听端口
  port: "25565"
  # 用户身份验证唯一标识符。若未设置，将自动随机生成
  uuid: "2584b733-9095-4bec-a7d5-62b473540f7a"
  # Xray 核心版本号
  xray-version: "25.10.15"
  # Hysteria2 核心版本号
  hy2-version: "2.6.5"
  # Cloudflared 版本号
  argo-version: "2025.10.0"
  # Argo Tunnel 的访问域名，启用固定隧道需要设置
  argo-domain: ""
  # Argo Tunnel 的访问令牌，启用固定隧道需要设置
  argo-token: ""
  # 节点备注的前缀标识
  remarks-prefix: ""
  # 哪吒探针(v1) Agent 版本号，为空则自动获取最新版本
  nezha-version: ""
  # 哪吒面板 Agent 连接地址，格式为 域名或IP:端口，例如 data.example.com:8008；留空则不启用哪吒探针
  nezha-server: ""
  # 哪吒面板中对应用户的 Agent 连接密钥（非 agent_secret_key）
  nezha-client-secret: ""
  # 哪吒面板 Agent 连接地址是否启用 TLS
  nezha-tls: "false"
```

> 哪吒探针 Agent 的 uuid 直接复用上方的 `app.uuid`，无需单独配置。

## 📢 使用说明与免责声明

- 使用本项目时，请在引用、发布或分发时 **注明项目来源**。
- 本项目仅用于 **技术研究和学习使用**，不得用于任何违法用途。
- 作者不对因使用本项目导致的任何数据损失、网络封禁、账户封禁或法律责任承担任何责任。
- 使用本项目即表示您已同意自行承担相关风险与责任。