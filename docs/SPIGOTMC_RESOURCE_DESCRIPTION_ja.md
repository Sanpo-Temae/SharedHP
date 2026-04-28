# SharedHP

**Requires Paper. Spigot/Bukkit are not supported.**

SharedHP は、登録したプレイヤーたちが 1つのHPを共有する Minecraft Java版 Paper専用プラグインです。

## 対応環境

- Minecraft: 1.21.11
- Paper: 1.21.11
- Java: 21
- Spigot / Bukkit / CraftBukkit: 非対応

## 主な機能

- `/sharedhp add <player>` で参加者を登録
- 登録済みオンライン参加者が1つの共有HPを持つ
- ダメージを共有HPから1回分だけ減算
- 回復量はデフォルト25%
- `/sharedhp heal <percent>` で回復効率を変更
- ボスバーで共有HPを表示
- 吸収ハートを無効化
- 被ダメージランキングを表示

## 導入方法

1. サーバーを停止します。
2. `SharedHP-1.0.0.jar` を `plugins` フォルダに入れます。
3. サーバーを起動します。
4. `/plugins` で SharedHP が緑色表示されることを確認します。
5. `/sharedhp add <player>` で参加者を登録します。
6. `/sharedhp start` で開始します。

更新時は `/reload` ではなく、`stop -> Jar入れ替え -> 起動` を推奨します。

## コマンド

- `/sharedhp status`
- `/sharedhp list`
- `/sharedhp damage`
- `/sharedhp add <player>`
- `/sharedhp remove <player>`
- `/sharedhp start`
- `/sharedhp stop`
- `/sharedhp reset`
- `/sharedhp set <value>`
- `/sharedhp heal <percent>`
- `/sharedhp damage reset`

## 権限

- `sharedhp.view`
- `sharedhp.admin`

## 注意

Paper専用です。Spigot/Bukkit/CraftBukkit では動作保証しません。
