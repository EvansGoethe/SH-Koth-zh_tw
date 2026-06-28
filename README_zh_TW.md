# SH-KoTH (繁體中文)

[![BuiltByBit](https://img.shields.io/badge/BBB-Profile-00bfff?logo=bit&logoColor=white)](https://builtbybit.com/resources/sh-koth.76419/)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a?logo=modrinth&logoColor=white)](https://modrinth.com/plugin/sh-koth)
[![Documentation](https://img.shields.io/badge/Docs-Available-blue?logo=gitbook)](https://docs.smartshub.dev/sh-koth/intro/introduction/)
[![Support](https://img.shields.io/badge/Support-Discord-5865f2?logo=discord&logoColor=white)](https://discord.smartshub.dev/)

[English](README.md)

---

## 📖 關於
**SH-Koth** 是一款免費且開源的 King of the Hill (KoTH，山丘之王) 搶點插件。其設計旨在兼顧易用性與強大功能，為您的伺服器提供更聰明、更高效的 KoTH 活動管理方案，並具備多種優化遊戲體驗與伺服器管理的特色功能。

SH-Koth 的主要目標是為您的伺服器提供一個穩定且高效的 KoTH 活動解決方案。

---

## 🚀 快速連結
- 📥 [在 Modrinth 下載](https://modrinth.com/plugin/sh-koth)
- 🛒 [在 BuiltByBit 下載/贊助](https://builtbybit.com/resources/sh-koth.76419/)
- 📚 [閱讀官方文件](https://docs.smartshub.dev/sh-koth/intro/introduction/)
- 💬 [加入 Discord 獲取支援](https://discord.smartshub.dev/)

---

## 🌐 多語言支援說明 (本 Fork 擴充功能)
本分支（Fork）為 SH-Koth 額外新增了**多語言切換與本地化系統**，並附帶完整的**繁體中文 (zh_tw)** 語言檔案。

### 如何使用中文語系：
1. 將編譯好的 JAR 檔放入 `plugins/` 資料夾，並啟動一次伺服器以生成設定。
2. 打開 `plugins/SH-Koth/configuration/hooks.yml` 檔案。
3. 在最上方將 `language` 修改為 `zh_tw`：
   ```yaml
   language: zh_tw
   ```
4. 儲存檔案並在遊戲中執行 `/koth reload` 即可。

---

## 🛠️ 參與貢獻
We welcome contributions from the community!  
我們非常歡迎社群參與貢獻！
您可以透過以下方式提供協助：

1. **Fork** 本專案並建立一個新的分支。
2. 遵守 [程式碼規範](#-程式碼規範) 指引。
3. 送出 **Pull Request** 並詳細說明您的修改內容。

---

## 🎨 程式碼規範
- 我們遵循 **Oracle Java 規範**，並針對可讀性進行了微調。
- 保持方法（Methods）簡短且專注於單一功能。
- 撰寫具體、有意義的 Commit 訊息。

---

## 🐛 回報問題
- 請至 [Issues 頁面](https://github.com/EvansGoethe/SH-Koth-zh_tw/issues) 回報 Bug 或提出功能建議。
- 回報時請務必附上伺服器版本、插件版本以及重現步驟。

---

## 🕳️ 技術債 (Technical Debt)
- GUI 系統（主要是 Koth 建立範本的填寫 GUI 與 YAML 建立器，並非直接針對 GUI 介面本身）。
- 部分佔位符（Placeholders）與特定的 Koth ID 強烈綁定。

---

## 📜 授權條款
本專案基於 **[CC-BY-NC-SA 4.0](LICENSE.md)** 條款進行授權。
只要在**非商業用途**的前提下，您有權自由分享並修改程式碼，但必須標註原作者，且修改後的版本必須採用相同的授權條款釋出。

---

## 🙌 開發團隊
由 **Smarts Hub** 用 ❤️ 開發。
感謝所有貢獻者以及 Minecraft 社群提供的回饋與支持。
